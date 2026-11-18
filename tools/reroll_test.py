#!/usr/bin/env python3
"""Dedicated-server reroll matrix for Villager Reroller.

One row is a Minecraft version, a loader, and Trade Cycling on or off.
The client joins that dedicated server, rerolls for at least 30 seconds in
survival, then 30 seconds in creative while flying, then locks the open trade.
A disconnect, kick, exception, or crash fails the row. A version that cannot
be launched is blocked, which is not a pass.
"""

from __future__ import annotations

import argparse
import json
import os
import shutil
import subprocess
import sys
import threading
import time
import urllib.request
from concurrent.futures import ThreadPoolExecutor, as_completed
from datetime import datetime, timezone
from pathlib import Path

import validate

ROOT = validate.ROOT
KICK_MARKERS = (
    "Invalid move player packet received",
    "invalid_player_movement",
)
FAIL_MARK = "villager_reroller server harness FAIL"
PASS_MARK = "villager_reroller server harness PASS"
SERVER_READY = "villager_reroller PROOF_WORLD_READY"
OPTIONS = validate.OPTIONS
PORT_LOCK = threading.Lock()
NEXT_PORT = 26000


def main(argv: list[str] | None = None) -> int:
    args = parse_args(argv)
    if args.self_test:
        self_test()
        print("reroll_test self-test passed")
        return 0
    if not args.skip_unit:
        if unit_tests() != 0:
            return 1
        if args.unit_only:
            return 0
    matrix = validate.load_matrix(ROOT)
    versions = list(args.versions)
    reason = ""
    if args.changed:
        versions, reason = changed_versions(matrix["versions"])
        print(reason, flush=True)
        if not versions:
            write_reports(Path(args.output), [], {}, False, reason)
            return 0
    selected = validate.select_matrix(matrix, versions, args.loaders)
    if not selected:
        raise SystemExit("no versions matched")
    output = ROOT / args.output
    output.mkdir(parents=True, exist_ok=True)
    if args.dry_run:
        for spec in selected:
            print(f"{spec['version']} {spec['loader']}")
        return 0
    java25 = validate.find_java(25)
    os.environ["JAVA_HOME"] = str(java25.parent.parent)
    if not args.skip_build and args.client_jar is None:
        validate.gradle_assemble(selected, matrix["vcs"])
    validate.ensure_launcher_lib()
    if any(spec["version"] == "26.3" for spec in selected):
        try:
            validate.ensure_shim()
        except (OSError, subprocess.CalledProcessError) as error:
            for spec in selected:
                if spec["version"] == "26.3":
                    spec["installError"] = f"26.3 needs tools/libglx-srgb.so ({error})"
    cache = ROOT / "build" / "validation-minecraft"
    try:
        validate.install_runtimes(cache, [spec for spec in selected if not spec.get("installError")])
        validate.download_dependencies(cache, [spec for spec in selected if not spec.get("installError")])
    except SystemExit as error:
        print(error, file=sys.stderr)
        return 1
    for spec in selected:
        if spec.get("installError"):
            continue
        jar = Path(args.client_jar) if args.client_jar else validate.find_release_jar(spec)
        if jar is None or not jar.is_file():
            spec["installError"] = "release jar was not built"
            continue
        spec["jar"] = jar
        server_jar = Path(args.server_jar) if args.server_jar else jar
        if not server_jar.is_file():
            spec["installError"] = f"server jar missing: {server_jar}"
            continue
        spec["serverJar"] = server_jar
    modes = []
    if not args.tc_only:
        modes.append(False)
    if not args.no_tc:
        modes.append(True)
    jobs = args.jobs if args.jobs > 0 else validate.auto_jobs()
    args.kept = {}
    if args.resume:
        previous = output / "results.json"
        if previous.is_file():
            for row in json.loads(previous.read_text()).get("rows", []):
                reason = row.get("failureReason") or ""
                # A missing lectern was the command dispatcher. Retry those.
                stale = "librarian or lectern missing" in reason or "dedicated server exited" in reason or "did not enter" in reason
                if row.get("status") == "pass" or (row.get("status") == "fail" and not stale):
                    args.kept[(row["version"], row["loader"], row["tradeCycling"])] = row
            print(f"resume kept {len(args.kept)} passing rows", flush=True)
    print(f"reroll matrix {len(selected)} loaders x {len(modes)} trade-cycling modes, jobs={jobs}", flush=True)
    started = time.time()
    results: dict[tuple[str, str, bool], dict] = {}
    if not xvfb_ok():
        for spec in selected:
            for tc in modes:
                results[(spec["version"], spec["loader"], tc)] = blocked(
                    spec, tc, "xvfb-run is not on PATH, so the scripted client has no display"
                )
    else:
        with ThreadPoolExecutor(max_workers=jobs) as pool:
            futures = [pool.submit(run_loader, cache, output, spec, modes, args) for spec in selected]
            for future in as_completed(futures):
                for row in future.result():
                    key = (row["version"], row["loader"], row["tradeCycling"])
                    results[key] = row
                    write_reports(output, selected, results, True, reason)
    elapsed = round(time.time() - started, 1)
    report = write_reports(output, selected, results, False, reason, elapsed)
    print(render_markdown(report), flush=True)
    failed = [row for row in report["rows"] if row["status"] != "pass"]
    print(f"elapsed {validate.fmt_duration(elapsed)}  {len(report['rows']) - len(failed)}/{len(report['rows'])} passed", flush=True)
    return 1 if failed else 0


def parse_args(argv: list[str] | None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Run the Villager Reroller dedicated-server matrix.")
    parser.add_argument("--version", action="append", default=[], help="Minecraft version. Repeat or separate with commas.")
    parser.add_argument("--loader", action="append", default=[], help="fabric, forge, neoforge, or a comma-separated mix.")
    parser.add_argument("--changed", action="store_true", help="Run versions touched since merge-base with origin/main. Shared sources run the full list.")
    parser.add_argument("--jobs", type=int, default=0, help="Parallel dedicated servers. 0 picks from CPU and memory, capped at 2.")
    parser.add_argument("--retries", type=int, default=1, help="Extra attempts after a startup timeout.")
    parser.add_argument("--timeout-minutes", type=float, default=20, help="Wall clock for one client attempt.")
    parser.add_argument("--hold-seconds", type=int, default=30, help="Seconds of reroll in survival, then again in creative flight.")
    parser.add_argument("--heap-mb", type=int, default=1536, help="Client heap. The server uses 1024.")
    parser.add_argument("--output", default="artifacts/reroll-test", help="Summary directory, relative to the repo.")
    parser.add_argument("--skip-build", action="store_true")
    parser.add_argument("--skip-unit", action="store_true")
    parser.add_argument("--unit-only", action="store_true")
    parser.add_argument("--no-tc", action="store_true", help="Skip the Trade Cycling rows.")
    parser.add_argument("--tc-only", action="store_true", help="Skip the job-site rows.")
    parser.add_argument("--keep-runs", action="store_true")
    parser.add_argument("--dry-run", action="store_true")
    parser.add_argument("--self-test", action="store_true")
    parser.add_argument("--expect-kick", action="store_true", help="Pass only when the server kicks for an invalid move packet.")
    parser.add_argument("--resume", action="store_true", help="Keep rows that already passed in the output file.")
    parser.add_argument("--client-jar", help="Use this client jar instead of the version's release jar.")
    parser.add_argument("--server-jar", help="Use this server jar instead of the client jar.")
    args = parser.parse_args(argv)
    args.versions = validate.split_flags(args.version)
    args.loaders = validate.split_flags(args.loader)
    bad = [loader for loader in args.loaders if loader not in ("fabric", "forge", "neoforge")]
    if bad:
        parser.error("unknown loader: " + ", ".join(bad))
    if args.no_tc and args.tc_only:
        parser.error("--no-tc and --tc-only cannot both be set")
    if args.hold_seconds < 30 and not args.expect_kick:
        parser.error("--hold-seconds must be at least 30")
    return args


def unit_tests() -> int:
    env = os.environ.copy()
    java = validate.find_java(25)
    env["JAVA_HOME"] = str(java.parent.parent)
    print("unit tests", flush=True)
    completed = subprocess.run(
        [str(ROOT / "gradlew"), ":1.21.11:test", ":forge:1.21.11:test", ":neoforge:1.21.11:test", "--console=plain"],
        cwd=ROOT,
        env=env,
    )
    return completed.returncode


def changed_versions(known: list[str]) -> tuple[list[str], str]:
    base = subprocess.check_output(["git", "merge-base", "HEAD", "origin/main"], cwd=ROOT, text=True).strip()
    names: set[str] = set()
    for args in (["git", "diff", "--name-only", base, "HEAD"], ["git", "diff", "--name-only"], ["git", "diff", "--name-only", "--cached"]):
        text = subprocess.check_output(args, cwd=ROOT, text=True)
        names.update(line for line in text.splitlines() if line)
    shared = ("src/", "forge/", "neoforge/", "tools/", "build.gradle.kts", "settings.gradle.kts", "stonecutter.gradle.kts")
    if any(name.startswith(prefix) or name.endswith(prefix) for name in names for prefix in shared):
        return list(known), "shared sources changed, so --changed runs every supported version"
    picked = [version for version in known if any(version in name for name in names)]
    return picked, "versions named in the diff: " + (", ".join(picked) if picked else "none")


def xvfb_ok() -> bool:
    return shutil.which("xvfb-run") is not None


def run_loader(cache: Path, output: Path, spec: dict, modes: list[bool], args: argparse.Namespace) -> list[dict]:
    rows = []
    kept = getattr(args, "kept", {})
    for tc in modes:
        previous = kept.get((spec["version"], spec["loader"], tc))
        if previous is not None:
            rows.append(previous)
            continue
        try:
            rows.append(run_mode(cache, output, spec, tc, args))
        except Exception as error:  # noqa: BLE001 - one loader must not cancel the table
            print(f"crash {spec['version']} {spec['loader']} tc={tc}: {error}", flush=True)
            rows.append(blocked(spec, tc, str(error)[:500]))
    return rows


def run_mode(cache: Path, output: Path, spec: dict, trade_cycling: bool, args: argparse.Namespace) -> dict:
    label = f"{spec['version']} {spec['loader']} tc={trade_cycling}"
    if spec.get("installError"):
        print(f"blocked {label}: {spec['installError']}", flush=True)
        return blocked(spec, trade_cycling, spec["installError"])
    attempts = args.retries + 1
    last = None
    for attempt in range(1, attempts + 1):
        last = one_attempt(cache, output, spec, trade_cycling, args, attempt)
        if last["status"] == "pass" or not retryable(last) or attempt == attempts:
            break
        print(f"retry {attempt}/{args.retries} {label}: {last['failureReason']}", flush=True)
    print(f"{last['status']} {label}: {last['failureReason']}", flush=True)
    return last


def retryable(row: dict) -> bool:
    if row["status"] == "pass" or row["status"] == "blocked":
        return False
    reason = (row.get("failureReason") or "").lower()
    if "invalid move" in reason or "kicked" in reason:
        return False
    return row.get("timedOut", False) or any(hint in reason for hint in (
        "connection refused",
        "did not enter",
        "title screen",
        "proof world",
        "timed out",
        "wall-clock",
    ))


def one_attempt(cache: Path, output: Path, spec: dict, trade_cycling: bool, args: argparse.Namespace, attempt: int) -> dict:
    started = time.time()
    mode = "with-tc" if trade_cycling else "without-tc"
    run_dir = ROOT / "build" / "reroll-runs" / spec["version"] / spec["loader"] / f"{mode}-{attempt}"
    if run_dir.exists():
        shutil.rmtree(run_dir)
    client_dir = run_dir / "client"
    shots = run_dir / "shots"
    shots.mkdir(parents=True)
    port = allocate_port()
    runtime = ROOT / "build" / "reroll-servers" / spec["version"] / spec["loader"]
    server = None
    try:
        install_server(spec, runtime)
    except Exception as error:  # noqa: BLE001 - the row records the installer failure
        return blocked(spec, trade_cycling, f"dedicated server install failed: {error}"[:500])
    prepare_server(runtime, spec, trade_cycling, port, cache)
    java = validate.find_java(validate.java_major_for(spec["version"]))
    server_log = run_dir / "server.log"
    server_cmd = server_command(runtime, spec, java)
    server_env = game_env(java, spec, client=False)
    print(f"start server {spec['version']} {spec['loader']} {mode} :{port}", flush=True)
    with server_log.open("w") as handle:
        server = subprocess.Popen(server_cmd, cwd=runtime, env=server_env, stdout=handle, stderr=subprocess.STDOUT, start_new_session=True)
    try:
        ready = wait_ready(server, server_log, 8 * 60)
        if ready is not None:
            text = server_log.read_text(errors="replace") if server_log.is_file() else ""
            return finish_row(spec, trade_cycling, started, "fail", ready, "not-run", "not-run", None, text, False, 0)
        prepare_client(client_dir, spec, trade_cycling, cache)
        client_cmd = client_command(cache / "minecraft", spec, client_dir, shots, java, args, port, trade_cycling)
        client_log = run_dir / "client.log"
        wrapped = ["xvfb-run", "-a", "-s", "-screen 0 1280x720x24", "--", *client_cmd]
        print(f"start client {spec['version']} {spec['loader']} {mode}", flush=True)
        with client_log.open("w") as handle:
            client = subprocess.Popen(wrapped, cwd=client_dir, env=game_env(java, spec, client=True), stdout=handle, stderr=subprocess.STDOUT, start_new_session=True)
        timed_out = False
        try:
            deadline = time.time() + args.timeout_minutes * 60
            while True:
                server_text = read_text(server_log)
                client_text = combined_client(client_dir, shots, client_log)
                if kick_line(server_text + "\n" + client_text):
                    break
                result = read_text(shots / "harness-result.txt")
                if result.startswith("PASS") or result.startswith("FAIL") or FAIL_MARK in client_text or PASS_MARK in client_text:
                    time.sleep(2)
                    break
                if client.poll() is not None or server.poll() is not None:
                    break
                if time.time() > deadline:
                    timed_out = True
                    break
                time.sleep(2)
        finally:
            terminate(client)
        server_text = read_text(server_log)
        client_text = combined_client(client_dir, shots, client_log)
        result = read_text(shots / "harness-result.txt")
        row = classify(spec, trade_cycling, client_text, server_text, result, client.returncode, timed_out, args.expect_kick)
        row["durationSeconds"] = round(time.time() - started, 1)
        row["screenshot"] = publish_screenshot(output, spec, mode, shots)
        if row["status"] != "pass":
            publish_log(output, spec, mode, server_text, client_text)
        else:
            stale = output / "logs" / spec["version"] / spec["loader"] / f"{mode}.log"
            stale.unlink(missing_ok=True)
            if not args.keep_runs:
                shutil.rmtree(run_dir, ignore_errors=True)
        return row
    finally:
        terminate(server)


def blocked(spec: dict, trade_cycling: bool, reason: str) -> dict:
    return {
        "version": spec["version"],
        "loader": spec["loader"],
        "tradeCycling": trade_cycling,
        "survival": "not-run",
        "creative": "not-run",
        "status": "blocked",
        "failureReason": reason[:500],
        "durationSeconds": 0,
        "screenshot": None,
        "timedOut": False,
    }


def finish_row(spec, trade_cycling, started, status, reason, survival, creative, screenshot, log_text, timed_out, exit_code) -> dict:
    return {
        "version": spec["version"],
        "loader": spec["loader"],
        "tradeCycling": trade_cycling,
        "survival": survival,
        "creative": creative,
        "status": status,
        "failureReason": (reason or "")[:500],
        "durationSeconds": round(time.time() - started, 1),
        "screenshot": screenshot,
        "timedOut": timed_out,
        "exitCode": exit_code,
        "logExcerpt": "\n".join(log_text.splitlines()[-20:]),
    }


def install_server(spec: dict, server_dir: Path) -> None:
    server_dir.mkdir(parents=True, exist_ok=True)
    marker = server_dir / ".installed"
    if marker.is_file():
        return
    java = str(validate.find_java(validate.java_major_for(spec["version"])))
    if spec["loader"] == "fabric":
        jar = server_dir / "server.jar"
        download(fabric_server_url(spec), jar)
        (server_dir / "eula.txt").write_text("eula=true\n")
    else:
        installer = server_dir / "installer.jar"
        download(loader_installer_url(spec), installer)
        subprocess.run([java, "-jar", str(installer), "--installServer"], cwd=server_dir, check=True, timeout=900)
        installer.unlink(missing_ok=True)
    marker.write_text(spec["loader"] + "\n")


def fabric_server_url(spec: dict) -> str:
    with urllib.request.urlopen("https://meta.fabricmc.net/v2/versions/installer", timeout=60) as response:
        installers = json.loads(response.read().decode())
    installer = next(item["version"] for item in installers if item.get("stable"))
    return (
        "https://meta.fabricmc.net/v2/versions/loader/"
        f"{spec['version']}/{spec['fabricLoader']}/{installer}/server/jar"
    )


def loader_installer_url(spec: dict) -> str:
    if spec["loader"] == "neoforge":
        version = spec["neoVersion"]
        return f"https://maven.neoforged.net/releases/net/neoforged/neoforge/{version}/neoforge-{version}-installer.jar"
    coordinate = spec["forgeCoordinate"]
    return f"https://maven.minecraftforge.net/net/minecraftforge/forge/{coordinate}/forge-{coordinate}-installer.jar"


def download(url: str, destination: Path) -> None:
    destination.parent.mkdir(parents=True, exist_ok=True)
    print(f"download {url}", flush=True)
    request = urllib.request.Request(url, headers={"User-Agent": "villager-reroller-reroll-test"})
    with urllib.request.urlopen(request, timeout=120) as response, destination.open("wb") as handle:
        shutil.copyfileobj(response, handle)


def prepare_server(server_dir: Path, spec: dict, trade_cycling: bool, port: int, cache: Path) -> None:
    world = server_dir / "world"
    if world.exists():
        shutil.rmtree(world)
    mods = server_dir / "mods"
    if mods.exists():
        shutil.rmtree(mods)
    mods.mkdir()
    shutil.copy(spec["serverJar"], mods / spec["serverJar"].name)
    for group, artifact, version in validate.dependency_coords(spec, trade_cycling):
        dep = validate.dep_path(cache, group, artifact, version)
        shutil.copy(dep, mods / dep.name)
    (server_dir / "eula.txt").write_text("eula=true\n")
    (server_dir / "server.properties").write_text(
        "\n".join([
            "online-mode=false",
            "enforce-secure-profile=false",
            "prevent-proxy-connections=false",
            "white-list=false",
            "spawn-protection=0",
            "difficulty=peaceful",
            "gamemode=survival",
            "level-type=flat",
            "spawn-monsters=false",
            "view-distance=6",
            "simulation-distance=6",
            "max-tick-time=180000",
            f"server-port={port}",
            "motd=reroll-test",
            "",
        ])
    )


def server_command(server_dir: Path, spec: dict, java: Path) -> list[str]:
    props = [
        "-Dvillager_reroller.proofWorld=true",
        "-Dvillager_reroller.proofJobSite=true",
        "-Djava.net.preferIPv4Stack=true",
    ]
    unix = sorted(server_dir.glob("libraries/**/unix_args.txt"))
    if unix:
        return [str(java), "-Xmx1024m", "-Xms512m", *props, f"@{unix[0]}", "nogui"]
    jar = server_dir / "server.jar"
    if not jar.is_file():
        found = [path for path in server_dir.glob("*.jar") if path.name != "installer.jar"]
        if not found:
            raise RuntimeError(f"no server jar in {server_dir}")
        jar = found[0]
    return [str(java), "-Xmx1024m", "-Xms512m", *props, "-jar", str(jar), "nogui"]


def prepare_client(client_dir: Path, spec: dict, trade_cycling: bool, cache: Path) -> None:
    mods = client_dir / "mods"
    mods.mkdir(parents=True)
    shutil.copy(spec["jar"], mods / spec["jar"].name)
    for group, artifact, version in validate.dependency_coords(spec, trade_cycling):
        dep = validate.dep_path(cache, group, artifact, version)
        shutil.copy(dep, mods / dep.name)
    (client_dir / "options.txt").write_text(OPTIONS)
    # A 60 second stress run can cycle faster than the player-facing cap of 500.
    config = client_dir / "config"
    config.mkdir(parents=True, exist_ok=True)
    (config / "villager_reroller.json").write_text('{"maxAttempts": 100000}\n')


def allocate_port() -> int:
    global NEXT_PORT
    with PORT_LOCK:
        port = NEXT_PORT
        NEXT_PORT += 1
        return port


def client_command(minecraft_dir: Path, spec: dict, client_dir: Path, shots: Path, java: Path, args: argparse.Namespace, port: int, trade_cycling: bool) -> list[str]:
    jvm = [
        "-Dvillager_reroller.serverHarness=without",
        "-Dvillager_reroller.proofMatrix=true",
        "-Dvillager_reroller.proofJobSite=true",
        f"-Dvillager_reroller.proofHoldSeconds={args.hold_seconds}",
        f"-Dvillager_reroller.serverAddress=127.0.0.1:{port}",
        f"-Dvillager_reroller.artifacts={shots}",
        f"-Dvillager_reroller.loader={spec['loader']}",
        "-Dvillager_reroller.timeoutScale=4",
    ]
    if trade_cycling:
        jvm.append("-Dvillager_reroller.proofTradeCycling=true")
    return validate.launch_command(
        minecraft_dir,
        spec,
        client_dir,
        shots,
        java,
        args.heap_mb,
        jvm_arguments=jvm,
        allow_multiplayer=True,
    )


def game_env(java: Path, spec: dict, client: bool) -> dict:
    env = os.environ.copy()
    env["JAVA_HOME"] = str(java.parent.parent)
    env["LIBGL_ALWAYS_SOFTWARE"] = "1"
    env["GALLIUM_DRIVER"] = "llvmpipe"
    env["MESA_LOADER_DRIVER_OVERRIDE"] = "llvmpipe"
    env["SDL_AUDIODRIVER"] = "dummy"
    env["ALSOFT_DRIVERS"] = "null"
    if client and spec["version"] == "26.3":
        env["LD_PRELOAD"] = str(ROOT / "tools" / "libglx-srgb.so")
    return env


def wait_ready(server: subprocess.Popen, log_path: Path, seconds: int):
    deadline = time.time() + seconds
    while time.time() < deadline:
        if server.poll() is not None:
            text = read_text(log_path)
            line = next((item for item in text.splitlines() if "ERROR" in item or "Exception" in item), "")
            return f"dedicated server exited {server.returncode}: {line[:300] or 'no error line'}"
        text = read_text(log_path)
        if SERVER_READY in text:
            return None
        if "Invalid move player packet" in text:
            return None
        time.sleep(2)
    return "dedicated server did not log PROOF_WORLD_READY"


def combined_client(client_dir: Path, shots: Path, stdout_log: Path) -> str:
    parts = [read_text(stdout_log), read_text(client_dir / "logs" / "latest.log"), read_text(shots / "harness-result.txt")]
    return "\n".join(parts)


def read_text(path: Path) -> str:
    if not path.is_file():
        return ""
    return path.read_text(errors="replace")


def kick_line(text: str) -> str:
    for line in text.splitlines():
        if any(marker in line for marker in KICK_MARKERS):
            return line.strip()[:500]
    return ""


def classify(spec, trade_cycling, client_text, server_text, result, exit_code, timed_out, expect_kick) -> dict:
    blob = client_text + "\n" + server_text
    kick = kick_line(blob)
    fields = parse_result(result)
    crash = crash_line(blob)
    if expect_kick:
        status = "pass" if kick else "fail"
        reason = "" if kick else "pre-fix client was not kicked for an invalid move packet"
        if crash and not kick:
            reason = crash
        return row_from(spec, trade_cycling, status, reason or kick, "fail" if kick else "not-run", "not-run", timed_out, exit_code)
    survival = fields.get("survival", "not-run")
    creative = fields.get("creative", "not-run")
    reason = ""
    passed = result.startswith("PASS") and PASS_MARK in blob and not kick and not crash
    if kick:
        passed = False
        reason = kick
        if survival == "not-run":
            survival = "fail"
    elif crash:
        passed = False
        reason = crash
    elif result.startswith("FAIL") or FAIL_MARK in blob:
        passed = False
        reason = fields.get("reason") or fail_line(blob) or "harness failed"
    elif timed_out:
        passed = False
        reason = "wall-clock timeout before the harness finished"
    elif not passed:
        reason = f"harness did not pass (exit {exit_code})"
    if passed and (survival != "pass" or creative != "pass" or fields.get("lock") != "pass"):
        passed = False
        reason = "result file did not record survival, creative, and lock"
    return row_from(spec, trade_cycling, "pass" if passed else "fail", reason, survival, creative, timed_out, exit_code)


def row_from(spec, trade_cycling, status, reason, survival, creative, timed_out, exit_code) -> dict:
    return {
        "version": spec["version"],
        "loader": spec["loader"],
        "tradeCycling": trade_cycling,
        "survival": survival,
        "creative": creative,
        "status": status,
        "failureReason": (reason or "")[:500],
        "timedOut": timed_out,
        "exitCode": exit_code,
    }


def parse_result(result: str) -> dict:
    fields = {}
    for line in result.splitlines()[1:]:
        if "=" in line:
            key, value = line.split("=", 1)
            fields[key.strip()] = value.strip()
    return fields


def fail_line(text: str) -> str:
    for line in text.splitlines():
        if FAIL_MARK in line:
            return line.split(FAIL_MARK, 1)[1].strip()[:500]
    return ""


def crash_line(text: str) -> str:
    needles = ("MixinApplyError", "MixinTransformerError", "ModResolutionException", "Exception in server tick loop", "Failed to start the minecraft server")
    for line in text.splitlines():
        if any(needle in line for needle in needles):
            return line.strip()[:500]
    return ""


def publish_screenshot(output: Path, spec: dict, mode: str, shots: Path) -> str | None:
    preferred = [shots / "trade-screen.png", shots / "trade-screen-locked.png"]
    present = [path for path in preferred if path.is_file()]
    if not present:
        present = sorted(shots.glob("*.png"))
    if not present:
        return None
    destination = output / "screenshots" / spec["version"] / spec["loader"] / mode
    destination.mkdir(parents=True, exist_ok=True)
    shutil.copy(present[0], destination / "trade-screen.png")
    for path in shots.glob("*.png"):
        shutil.copy(path, destination / path.name)
    return str((destination / "trade-screen.png").relative_to(ROOT))


def publish_log(output: Path, spec: dict, mode: str, server_text: str, client_text: str) -> None:
    destination = output / "logs" / spec["version"] / spec["loader"]
    destination.mkdir(parents=True, exist_ok=True)
    (destination / f"{mode}.log").write_text(server_text + "\n----- client -----\n" + client_text)


def terminate(proc: subprocess.Popen | None) -> None:
    if proc is None or proc.poll() is not None:
        return
    try:
        os.killpg(proc.pid, 15)
    except (ProcessLookupError, PermissionError):
        proc.terminate()
    try:
        proc.wait(timeout=20)
    except subprocess.TimeoutExpired:
        try:
            os.killpg(proc.pid, 9)
        except (ProcessLookupError, PermissionError):
            proc.kill()


def write_reports(output: Path, selected: list[dict], results: dict, partial: bool, changed_reason: str, elapsed: float = 0) -> dict:
    rows = []
    for spec in selected:
        for tc in (False, True):
            row = results.get((spec["version"], spec["loader"], tc))
            if row is None:
                continue
            rows.append({k: v for k, v in row.items() if k not in ("timedOut", "exitCode", "logExcerpt")})
    report = {
        "generatedAt": datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
        "partial": partial,
        "changed": changed_reason,
        "elapsedSeconds": elapsed,
        "rows": rows,
    }
    output.mkdir(parents=True, exist_ok=True)
    (output / "results.json").write_text(json.dumps(report, indent=2) + "\n")
    (output / "results.md").write_text(render_markdown(report))
    return report


def render_markdown(report: dict) -> str:
    lines = [
        "# Villager Reroller dedicated-server matrix",
        "",
        f"Generated {report['generatedAt']}.",
        "",
    ]
    if report.get("changed"):
        lines.append(report["changed"])
        lines.append("")
    if report.get("elapsedSeconds"):
        lines.append(f"Elapsed {validate.fmt_duration(report['elapsedSeconds'])}.")
        lines.append("")
    lines.append("NeoForge starts at 1.20.2. 1.19 through 1.20.1 have no NeoForge build, so those loaders are omitted.")
    lines.append("")
    lines.append("| Version | Loader | Trade Cycling | Survival | Creative | Result | Failure reason |")
    lines.append("| --- | --- | --- | --- | --- | --- | --- |")
    passed = 0
    for row in report["rows"]:
        if row["status"] == "pass":
            passed += 1
        tc = "on" if row["tradeCycling"] else "off"
        lines.append(
            "| {version} | {loader} | {tc} | {survival} | {creative} | {status} | {reason} |".format(
                version=row["version"],
                loader=row["loader"],
                tc=tc,
                survival=row["survival"],
                creative=row["creative"],
                status=row["status"],
                reason=validate.cell(row.get("failureReason") or ""),
            )
        )
    lines.append("")
    lines.append(f"{passed}/{len(report['rows'])} rows passed. blocked is not a pass.")
    lines.append("")
    return "\n".join(lines)


def self_test() -> None:
    kick = classify(
        {"version": "26.3", "loader": "fabric"},
        False,
        "villager_reroller server harness PASS\n",
        "Invalid move player packet received",
        "PASS\nsurvival=pass\ncreative=pass\nlock=pass\n",
        0,
        False,
        False,
    )
    assert kick["status"] == "fail" and "Invalid move" in kick["failureReason"], kick
    assert not retryable(kick)
    good = classify(
        {"version": "1.21.1", "loader": "fabric"},
        False,
        "villager_reroller server harness PASS mode=without survival=pass creative=pass lock=pass\n",
        "",
        "PASS\nsurvival=pass\ncreative=pass\nlock=pass\ntradeCycling=false\nattempts=3\nbroke=true\nreplaced=true\n",
        0,
        False,
        False,
    )
    assert good["status"] == "pass", good
    missing = classify(
        {"version": "1.21.1", "loader": "forge"},
        True,
        "",
        "",
        "PASS\nsurvival=pass\ncreative=not-run\nlock=pass\n",
        0,
        False,
        False,
    )
    assert missing["status"] == "fail"
    expected = classify(
        {"version": "26.3", "loader": "fabric"},
        False,
        "",
        "[Server] Invalid move player packet received",
        "",
        1,
        False,
        True,
    )
    assert expected["status"] == "pass", expected
    quiet = classify(
        {"version": "26.3", "loader": "fabric"},
        False,
        "PASS",
        "",
        "PASS\nsurvival=pass\ncreative=pass\nlock=pass\n",
        0,
        False,
        True,
    )
    assert quiet["status"] == "fail" and "not kicked" in quiet["failureReason"]
    versions, reason = changed_versions(["1.21.1", "26.3"])
    assert isinstance(versions, list) and reason
    report = {
        "generatedAt": "2026-11-18T00:00:00Z",
        "partial": False,
        "changed": "",
        "elapsedSeconds": 65,
        "rows": [
            {
                "version": "26.3",
                "loader": "fabric",
                "tradeCycling": False,
                "survival": "fail",
                "creative": "not-run",
                "status": "fail",
                "failureReason": "Invalid move player packet received",
            },
            blocked({"version": "1.19", "loader": "forge"}, False, "xvfb-run is not on PATH"),
        ],
    }
    markdown = render_markdown(report)
    assert "Invalid move player packet received" in markdown
    assert "| blocked |" in markdown
    assert "1m 5s" in markdown
    timeout = {"status": "fail", "failureReason": "wall-clock timeout before the harness finished", "timedOut": True}
    assert retryable(timeout)
    assert not retryable(kick)


if __name__ == "__main__":
    try:
        sys.exit(main())
    except KeyboardInterrupt:
        print("interrupted", file=sys.stderr)
        sys.exit(130)
