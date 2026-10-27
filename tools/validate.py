#!/usr/bin/env python3
"""Launch built Villager Reroller jars in production Minecraft and record the result.

The Gradle dev clients run exploded classes. This tool copies the release jar
into a real Fabric, Forge, or NeoForge mods folder, starts that Minecraft
version under Xvfb, and reads the in-jar harness result.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import re
import shutil
import signal
import subprocess
import sys
import tempfile
import threading
import time
import tomllib
import urllib.error
import urllib.parse
import urllib.request
import uuid
import zipfile
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MOD_VERSION = "1.1.0"
CHECKS = (
    ("modLoaded", "villager_reroller harness check mod-loaded"),
    ("configScreen", "villager_reroller harness check config-screen"),
    ("tradePanel", "villager_reroller harness check trade-panel"),
    ("rerollMatched", "villager_reroller harness check reroll-matched"),
)
PASS_MARK = "client harness passed"
FAIL_MARK = "client harness failed:"
LAG_HINTS = (
    "did not open",
    "never loaded",
    "did not claim",
    "still running after the deadline",
    "world did not load",
    "title screen did not open",
    "create-world screen did not open",
    "world presets never loaded",
    "merchant screen did not open",
    "merchant screen did not reopen",
    "reroll screenshot was not written",
    "name scroll did not reach the far end",
    "librarian did not claim",
    "player left before",
    "wall-clock timeout",
    "timed out",
)
MOD_ERROR = re.compile(
    r"(MixinApplyError|MixinTransformerError|ModResolutionException|"
    r"Failed to load mod[^\n]{0,80}villager_reroller|"
    r"Incompatible mod set)",
    re.IGNORECASE,
)
LIVE_PROCS: set[subprocess.Popen] = set()
LIVE_LOCK = threading.Lock()


def main(argv: list[str] | None = None) -> int:
    args = parse_args(argv)
    if args.self_test:
        self_test()
        print("self-test passed")
        return 0
    matrix = load_matrix(ROOT)
    selected = select_matrix(matrix, args.versions, args.loaders)
    if not selected:
        print("no versions matched the filter", file=sys.stderr)
        return 2
    if not args.skip_build:
        gradle_assemble(selected, matrix["vcs"])
        if os.environ.get("VALIDATE_FROM_GRADLE") != "1":
            subprocess.run([str(ROOT / "gradlew"), "--stop"], cwd=ROOT, check=False)
    rows = []
    missing = False
    for spec in selected:
        jar = find_release_jar(spec)
        if jar is None:
            missing = True
            print(f"missing jar for {spec['version']} {spec['loader']}", file=sys.stderr)
        spec["jar"] = jar
        spec["sha256"] = sha256(jar) if jar else ""
    if args.dry_run:
        for spec in selected:
            print(f"{spec['loader']:6} {spec['version']:8} {spec['jar'] or 'MISSING'} {spec['sha256'][:12]}")
        return 1 if missing else 0
    if missing:
        return 1
    output = (ROOT / args.output).resolve()
    output.mkdir(parents=True, exist_ok=True)
    ensure_shim()
    ensure_launcher_lib()
    cache = ROOT / "build" / "validation-minecraft"
    cache.mkdir(parents=True, exist_ok=True)
    install_runtimes(cache, selected)
    download_dependencies(cache, selected)
    jobs = args.jobs if args.jobs > 0 else auto_jobs()
    print(f"launching {len(selected)} jars x 2 modes with jobs={jobs}", flush=True)
    modes = ("without-tc", "with-tc")
    results: dict[tuple[str, str, str], dict] = {}
    report_lock = threading.Lock()

    def store(spec: dict, mode: str, result: dict) -> None:
        with report_lock:
            results[(spec["version"], spec["loader"], mode)] = result
            write_reports(output, selected, results, partial=True)

    futures = []
    with ThreadPoolExecutor(max_workers=jobs) as pool:
        for spec in selected:
            for mode in modes:
                futures.append(pool.submit(run_mode, cache, output, spec, mode, args))
        try:
            for future in as_completed(futures):
                spec, mode, result = future.result()
                store(spec, mode, result)
                print(
                    f"{result['status']:4} {spec['version']} {spec['loader']} {mode} "
                    f"{fmt_duration(result['durationSeconds'])} {result['failureReason'] or ''}",
                    flush=True,
                )
        except KeyboardInterrupt:
            stop_live_processes()
            write_reports(output, selected, results, partial=True)
            print("interrupted; partial report written", file=sys.stderr)
            return 130
    report = write_reports(output, selected, results, partial=False)
    failed = [row for row in report["results"] if row["failureReason"]]
    print(f"wrote {output / 'report.md'}")
    print(f"{len(report['results']) - len(failed)}/{len(report['results'])} passed")
    return 1 if failed else 0


def parse_args(argv: list[str] | None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Validate Villager Reroller release jars in production Minecraft.")
    parser.add_argument("--version", action="append", default=[], help="Minecraft version. Repeat or separate with commas.")
    parser.add_argument("--loader", action="append", default=[], help="fabric, forge, neoforge, or a comma-separated mix.")
    parser.add_argument("--jobs", type=int, default=0, help="Parallel clients. 0 picks from CPU and memory, capped at 2.")
    parser.add_argument("--retries", type=int, default=1, help="Extra attempts after a lag timeout.")
    parser.add_argument("--timeout-minutes", type=float, default=40, help="Wall clock for one attempt.")
    parser.add_argument("--heap-mb", type=int, default=2048, help="Client heap size.")
    parser.add_argument("--output", default="artifacts/validation", help="Report directory, relative to the repo.")
    parser.add_argument("--skip-build", action="store_true", help="Use jars already in build/libs.")
    parser.add_argument("--keep-runs", action="store_true", help="Keep the per-run game directories under build/validation-runs.")
    parser.add_argument("--dry-run", action="store_true", help="Print the selected jars and exit.")
    parser.add_argument("--self-test", action="store_true", help="Check parsers and the report writer, then exit.")
    args = parser.parse_args(argv)
    args.versions = split_flags(args.version)
    args.loaders = split_flags(args.loader)
    bad = [loader for loader in args.loaders if loader not in ("fabric", "forge", "neoforge")]
    if bad:
        parser.error("unknown loader: " + ", ".join(bad))
    return args


def split_flags(values: list[str]) -> list[str]:
    out = []
    for value in values:
        out.extend(part.strip() for part in value.split(",") if part.strip())
    return out


def load_matrix(root: Path) -> dict:
    settings = (root / "settings.gradle.kts").read_text()
    versions_match = re.search(r"versions\(([^)]*)\)", settings)
    if versions_match is None:
        raise SystemExit("settings.gradle.kts has no versions(...) list")
    versions = re.findall(r'"([^"]+)"', versions_match.group(1))
    active = re.search(r'stonecutter active "([^"]+)"', (root / "stonecutter.gradle.kts").read_text())
    vcs = active.group(1) if active else "1.21.11"
    props = tomllib.loads((root / "stonecutter.properties.toml").read_text())
    loader = props["deps"]["fabric_loader"]
    forge_text = (root / "forge" / "build.gradle.kts").read_text()
    forge_nodes = {}
    for match in re.finditer(
        r'"([^"]+)"\s*->\s*ForgeNode\(\s*"([^"]+)"\s*,\s*"([^"]+)"\s*,\s*(\d+)\s*,\s*"([^"]+)"\s*,\s*"([^"]+)"',
        forge_text,
    ):
        forge_nodes[match.group(1)] = {
            "coordinate": match.group(2),
            "tradeCycling": match.group(6),
        }
    neo_text = (root / "neoforge" / "build.gradle.kts").read_text()
    neo_nodes = {}
    for match in re.finditer(
        r'"([^"]+)"\s*->\s*NeoForgeNode\(\s*"([^"]+)"\s*,\s*(\d+)\s*,\s*"([^"]+)"\s*,\s*"([^"]+)"',
        neo_text,
    ):
        neo_nodes[match.group(1)] = {
            "neoVersion": match.group(2),
            "tradeCycling": match.group(5),
        }
    entries = []
    for version in versions:
        table = props[version]["deps"]
        forge = forge_nodes.get(version)
        if forge is None:
            raise SystemExit(f"no Forge coordinate for {version}")
        shared = {
            "version": version,
            "fabricLoader": loader,
            "fabricApi": table["fabric_api"],
            "fabricTradeCycling": table["trade_cycling"],
            "forgeCoordinate": forge["coordinate"],
            "forgeTradeCycling": forge["tradeCycling"],
        }
        entries.append({**shared, "loader": "fabric"})
        entries.append({**shared, "loader": "forge"})
        neo = neo_nodes.get(version)
        if neo is not None:
            entries.append({
                **shared,
                "loader": "neoforge",
                "neoVersion": neo["neoVersion"],
                "neoTradeCycling": neo["tradeCycling"],
            })
    return {"versions": versions, "vcs": vcs, "entries": entries}


def select_matrix(matrix: dict, versions: list[str], loaders: list[str]) -> list[dict]:
    known = set(matrix["versions"])
    unknown = [version for version in versions if version not in known]
    if unknown:
        raise SystemExit("unknown version: " + ", ".join(unknown))
    selected = []
    for entry in matrix["entries"]:
        if versions and entry["version"] not in versions:
            continue
        if loaders and entry["loader"] not in loaders:
            continue
        selected.append(dict(entry))
    return selected


def gradle_assemble(selected: list[dict], vcs: str) -> None:
    java_home = str(find_java(25).parent.parent)
    tasks = []
    matrix_entries = load_matrix(ROOT)["entries"]
    selected_keys = {(entry["version"], entry["loader"]) for entry in selected}
    full = selected_keys == {(entry["version"], entry["loader"]) for entry in matrix_entries}
    if full:
        tasks = ["assemble"]
    else:
        for version, loader in sorted(selected_keys):
            if loader == "fabric" and version == vcs:
                tasks.append(":assemble")
            elif loader == "fabric":
                tasks.append(f":{version}:assemble")
            elif loader == "forge":
                tasks.append(f":forge:{version}:assemble")
            else:
                tasks.append(f":neoforge:{version}:assemble")
    env = os.environ.copy()
    env["JAVA_HOME"] = java_home
    env["PATH"] = str(Path(java_home) / "bin") + os.pathsep + env.get("PATH", "")
    command = [str(ROOT / "gradlew"), *tasks, "--console=plain"]
    print("running " + " ".join(command), flush=True)
    completed = subprocess.run(command, cwd=ROOT, env=env)
    if completed.returncode != 0:
        raise SystemExit(f"gradle exited {completed.returncode}")


def find_release_jar(spec: dict) -> Path | None:
    name = f"villager_reroller-{spec['loader']}-{MOD_VERSION}+{spec['version']}.jar"
    matches = []
    for path in ROOT.rglob(name):
        posix = path.as_posix()
        if "/build/libs/" not in posix:
            continue
        if any(part.startswith(".") for part in path.parts):
            continue
        matches.append(path)
    if not matches:
        return None
    return sorted(matches, key=lambda path: (len(path.parts), str(path)))[0]


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def find_java(major: int) -> Path:
    candidates = {
        17: [Path("/opt/jdk17/bin/java"), Path("/usr/lib/jvm/java-17-openjdk-amd64/bin/java")],
        21: [Path("/usr/lib/jvm/java-21-openjdk-amd64/bin/java"), Path("/opt/jdk21/bin/java")],
        25: [Path("/opt/jdk25/bin/java"), Path("/usr/lib/jvm/java-25-openjdk-amd64/bin/java")],
    }
    env = os.environ.get(f"JAVA_{major}_HOME")
    if env:
        candidates[major].insert(0, Path(env) / "bin" / "java")
    for candidate in candidates[major]:
        if candidate.is_file():
            return candidate
    raise SystemExit(f"Java {major} was not found. Install it at /opt/jdk{major} or set JAVA_{major}_HOME.")


def java_major_for(version: str) -> int:
    parts = [int(part) for part in version.split(".")]
    if parts[0] >= 26:
        return 25
    if parts[0] > 1 or parts[1] > 20 or (parts[1] == 20 and len(parts) > 2 and parts[2] >= 5):
        return 21
    return 17


def auto_jobs() -> int:
    meminfo = Path("/proc/meminfo").read_text()
    available = int(re.search(r"MemAvailable:\s+(\d+)", meminfo).group(1))
    total = int(re.search(r"MemTotal:\s+(\d+)", meminfo).group(1))
    usable = max(available, total - 2 * 1024 * 1024)
    if os.environ.get("VALIDATE_FROM_GRADLE") == "1":
        usable -= 3 * 1024 * 1024
    by_mem = max(1, usable // (3 * 1024 * 1024))
    by_cpu = max(1, (os.cpu_count() or 1) // 2)
    return max(1, min(by_mem, by_cpu, 2))


def ensure_shim() -> Path:
    source = ROOT / "tools" / "libglx_srgb.c"
    output = ROOT / "tools" / "libglx-srgb.so"
    if not output.exists() or output.stat().st_mtime < source.stat().st_mtime:
        subprocess.run(
            ["gcc", "-shared", "-fPIC", "-O2", "-o", str(output), str(source), "-ldl"],
            check=True,
        )
    tmp = Path("/tmp/libglx-srgb.so")
    if not tmp.exists() or tmp.stat().st_mtime < output.stat().st_mtime:
        shutil.copy(output, tmp)
    return output


def ensure_launcher_lib() -> None:
    try:
        import minecraft_launcher_lib.install  # noqa: F401
        from minecraft_launcher_lib.mod_loader._neoforge import Neoforge  # noqa: F401
    except ImportError:
        subprocess.run(
            [sys.executable, "-m", "pip", "install", "--user", "minecraft-launcher-lib>=8"],
            check=True,
        )
    import minecraft_launcher_lib.install as install_mod

    install_mod.install_jvm_runtime = lambda *args, **kwargs: None


def install_runtimes(cache: Path, selected: list[dict]) -> None:
    import minecraft_launcher_lib.fabric as fabric
    import minecraft_launcher_lib.forge as forge
    from minecraft_launcher_lib.install import install_minecraft_version
    from minecraft_launcher_lib.mod_loader._neoforge import Neoforge

    minecraft_dir = cache / "minecraft"
    minecraft_dir.mkdir(parents=True, exist_ok=True)
    seen = set()
    for spec in selected:
        key = (spec["version"], spec["loader"])
        if key in seen:
            continue
        seen.add(key)
        java = str(find_java(java_major_for(spec["version"])))
        version_id = installed_version_id(spec)
        marker = minecraft_dir / "versions" / version_id / f"{version_id}.json"
        if marker.is_file():
            print(f"runtime ready {version_id}", flush=True)
            continue
        print(f"installing {version_id}", flush=True)
        status = {"last": None}

        def set_status(text: str, label: str = version_id) -> None:
            if text != status["last"]:
                status["last"] = text
                print(f"  [{label}] {text}", flush=True)

        callback = {"setStatus": set_status, "setProgress": lambda _value: None, "setMax": lambda _value: None}
        try:
            if spec["loader"] == "fabric":
                fabric.install_fabric(spec["version"], minecraft_dir, spec["fabricLoader"], callback=callback, java=java)
            elif spec["loader"] == "neoforge":
                # launcher-lib 8's ModLoader.install rejects 26.x and betas because it
                # rewrites NeoForge versions as 1.<major>.<minor>. The installer jar
                # already knows its Minecraft version, so call it directly.
                install_minecraft_version(spec["version"], minecraft_dir, callback=callback)
                Neoforge().install(spec["version"], str(minecraft_dir), callback, java, spec["neoVersion"])
                install_minecraft_version(version_id, minecraft_dir, callback=callback)
            else:
                forge.install_forge_version(spec["forgeCoordinate"], minecraft_dir, callback=callback, java=java)
        except Exception as error:  # noqa: BLE001 - keep going so the table records the installer failure
            spec["installError"] = f"installer failed: {error}"[:800]
            print(f"install failed {version_id}: {error}", flush=True)
            continue
        if not marker.is_file():
            found = runtime_dirs(minecraft_dir, spec)
            if not found:
                spec["installError"] = f"installer did not create {marker}"
                print(spec["installError"], flush=True)
                continue
            print(f"runtime installed as {found[0].name}", flush=True)


def runtime_dirs(minecraft_dir: Path, spec: dict) -> list[Path]:
    versions_dir = minecraft_dir / "versions"
    patterns = [f"{spec['version']}*{spec['loader']}*"]
    if spec["loader"] == "neoforge":
        patterns.append(f"neoforge-{spec['neoVersion']}")
        patterns.append(f"*{spec['neoVersion']}*")
    found = []
    for pattern in patterns:
        for path in versions_dir.glob(pattern):
            if path.is_dir() and path not in found:
                found.append(path)
    return found


def installed_version_id(spec: dict) -> str:
    if spec["loader"] == "fabric":
        return f"fabric-loader-{spec['fabricLoader']}-{spec['version']}"
    if spec["loader"] == "neoforge":
        return f"neoforge-{spec['neoVersion']}"
    vanilla, forge_part = spec["forgeCoordinate"].split("-", 1)
    return f"{vanilla}-forge-{forge_part}"


def download_dependencies(cache: Path, selected: list[dict]) -> None:
    needed = []
    for spec in selected:
        needed.extend(dependency_coords(spec, with_trade_cycling=False))
        needed.extend(dependency_coords(spec, with_trade_cycling=True))
    unique = []
    seen = set()
    for coord in needed:
        if coord in seen:
            continue
        seen.add(coord)
        unique.append(coord)
    for group, artifact, version in unique:
        destination = dep_path(cache, group, artifact, version)
        if jar_is_usable(destination, artifact):
            continue
        if destination.exists():
            destination.unlink()
        destination.parent.mkdir(parents=True, exist_ok=True)
        cached = gradle_cache_jar(group, artifact, version)
        if cached is not None:
            shutil.copy(cached, destination)
        elif artifact == "fabric-api":
            # 1.19 and 1.19.1 Maven artifacts are the base module only.
            download_fabric_api_bundle(version, destination)
        else:
            download_maven(group, artifact, version, destination)
        if not jar_is_usable(destination, artifact):
            if destination.exists():
                destination.unlink()
            raise SystemExit(f"{group}:{artifact}:{version} did not resolve to a usable jar")


def dependency_coords(spec: dict, with_trade_cycling: bool) -> list[tuple[str, str, str]]:
    coords = []
    if spec["loader"] == "fabric":
        coords.append(("net.fabricmc.fabric-api", "fabric-api", spec["fabricApi"]))
        if with_trade_cycling:
            coords.append(("maven.modrinth", "trade-cycling", spec["fabricTradeCycling"]))
    elif spec["loader"] == "neoforge":
        if with_trade_cycling:
            coords.append(("maven.modrinth", "trade-cycling", spec["neoTradeCycling"]))
    elif with_trade_cycling:
        coords.append(("maven.modrinth", "trade-cycling", spec["forgeTradeCycling"]))
    return coords


def dep_path(cache: Path, group: str, artifact: str, version: str) -> Path:
    return cache / "mods" / f"{artifact}-{version}.jar"


def jar_is_usable(path: Path, artifact: str) -> bool:
    if not path.is_file() or path.stat().st_size <= 0:
        return False
    if artifact != "fabric-api":
        return True
    try:
        with zipfile.ZipFile(path) as archive:
            return any(name.startswith("META-INF/jars/") and name.endswith(".jar") for name in archive.namelist())
    except zipfile.BadZipFile:
        return False


def gradle_cache_jar(group: str, artifact: str, version: str) -> Path | None:
    base = Path.home() / ".gradle" / "caches" / "modules-2" / "files-2.1" / group / artifact / version
    if not base.is_dir():
        return None
    jars = [
        path
        for path in base.rglob("*.jar")
        if not path.name.endswith(("-sources.jar", "-javadoc.jar", "-dev.jar"))
        and jar_is_usable(path, artifact)
    ]
    return max(jars, key=lambda path: path.stat().st_size) if jars else None


def download_fabric_api_bundle(version: str, destination: Path) -> None:
    if "+" not in version:
        raise RuntimeError(f"fabric-api version {version} has no Minecraft suffix")
    game = version.split("+", 1)[1]
    query = urllib.parse.urlencode(
        {
            "game_versions": json.dumps([game]),
            "loaders": json.dumps(["fabric"]),
        }
    )
    url = "https://api.modrinth.com/v2/project/fabric-api/version?" + query
    request = urllib.request.Request(url, headers={"User-Agent": "villager-reroller-validate"})
    with urllib.request.urlopen(request, timeout=60) as response:
        payload = json.loads(response.read().decode())
    match = next((item for item in payload if item.get("version_number") == version), None)
    if match is None:
        found = ", ".join(item.get("version_number", "?") for item in payload[:8])
        raise RuntimeError(f"Modrinth has no Fabric API {version} (saw {found})")
    files = [item for item in match.get("files") or [] if str(item.get("filename", "")).endswith(".jar")]
    chosen = next((item for item in files if item.get("primary")), None) or (files[0] if files else None)
    if chosen is None:
        raise RuntimeError(f"Modrinth Fabric API {version} has no jar")
    download(chosen["url"], destination)


def download_maven(group: str, artifact: str, version: str, destination: Path) -> None:
    repos = []
    if group.startswith("net.fabricmc"):
        repos.append("https://maven.fabricmc.net/")
    repos.append("https://api.modrinth.com/maven/")
    repos.append("https://libraries.minecraft.net/")
    errors = []
    for repo in repos:
        for version_token in (version, version.replace("+", "%2B")):
            url = maven_url(repo, group, artifact, version_token)
            try:
                download(url, destination)
                return
            except Exception as error:  # noqa: BLE001 - try the next repository
                errors.append(f"{url} ({error})")
                if destination.exists():
                    destination.unlink()
    raise SystemExit(f"could not download {group}:{artifact}:{version}\n" + "\n".join(errors))


def maven_url(repo: str, group: str, artifact: str, version: str) -> str:
    group_path = group.replace(".", "/")
    filename = f"{artifact}-{version}.jar"
    return f"{repo}{group_path}/{artifact}/{version}/{filename}"


def download(url: str, destination: Path) -> None:
    destination.parent.mkdir(parents=True, exist_ok=True)
    temporary = destination.with_suffix(destination.suffix + ".part")
    request = urllib.request.Request(url, headers={"User-Agent": "villager-reroller-validate"})
    last_error: Exception | None = None
    for attempt in range(4):
        try:
            with urllib.request.urlopen(request, timeout=120) as response, temporary.open("wb") as handle:
                shutil.copyfileobj(response, handle)
            temporary.replace(destination)
            return
        except (urllib.error.URLError, TimeoutError, OSError) as error:
            last_error = error
            time.sleep(2 ** attempt)
    raise RuntimeError(last_error)


def run_mode(cache: Path, output: Path, spec: dict, mode: str, args: argparse.Namespace) -> tuple[dict, str, dict]:
    if spec.get("installError"):
        failed = empty_failure(spec["installError"])
        return spec, mode, failed
    attempts = args.retries + 1
    result = None
    try:
        return _run_mode_attempts(cache, output, spec, mode, args, attempts)
    except Exception as error:  # noqa: BLE001 - one client must not cancel the table
        print(f"crash {spec['version']} {spec['loader']} {mode}: {error}", flush=True)
        failed = empty_failure(str(error)[:500])
        return spec, mode, failed


def empty_failure(reason: str) -> dict:
    return {
        "status": "fail",
        "failureReason": reason,
        "durationSeconds": 0,
        "attempts": 1,
        "checks": {name: False for name, _marker in CHECKS},
        "screenshot": None,
        "exitCode": None,
        "timedOut": False,
        "logExcerpt": reason,
    }


def _run_mode_attempts(cache: Path, output: Path, spec: dict, mode: str, args: argparse.Namespace, attempts: int) -> tuple[dict, str, dict]:
    result = None
    for attempt in range(1, attempts + 1):
        result = one_attempt(cache, output, spec, mode, args, attempt)
        result["attempts"] = attempt
        if result["status"] == "pass" or not retryable(result) or attempt == attempts:
            break
        print(
            f"retry {attempt}/{args.retries} {spec['version']} {spec['loader']} {mode}: {result['failureReason']}",
            flush=True,
        )
    return spec, mode, result


def retryable(result: dict) -> bool:
    if result["status"] == "pass":
        return False
    if result.get("timedOut"):
        return True
    reason = (result.get("failureReason") or "").lower()
    if "out of memory" in reason or "killed" in reason:
        return True
    return any(hint in reason for hint in LAG_HINTS)


def one_attempt(cache: Path, output: Path, spec: dict, mode: str, args: argparse.Namespace, attempt: int) -> dict:
    started = time.time()
    run_dir = ROOT / "build" / "validation-runs" / spec["version"] / spec["loader"] / f"{mode}-{attempt}"
    if run_dir.exists():
        shutil.rmtree(run_dir)
    game_dir = run_dir / "game"
    shots = run_dir / "shots"
    game_dir.mkdir(parents=True)
    shots.mkdir()
    (game_dir / "mods").mkdir()
    (game_dir / "eula.txt").write_text("eula=true\n")
    (game_dir / "options.txt").write_text(OPTIONS)
    mod_jar = spec["jar"]
    shutil.copy(mod_jar, game_dir / "mods" / mod_jar.name)
    for group, artifact, version in dependency_coords(spec, with_trade_cycling=mode == "with-tc"):
        dep = dep_path(cache, group, artifact, version)
        shutil.copy(dep, game_dir / "mods" / dep.name)
    java = find_java(java_major_for(spec["version"]))
    command = launch_command(cache / "minecraft", spec, game_dir, shots, java, args.heap_mb)
    env = os.environ.copy()
    env["JAVA_HOME"] = str(java.parent.parent)
    env["LIBGL_ALWAYS_SOFTWARE"] = "1"
    env["GALLIUM_DRIVER"] = "llvmpipe"
    env["MESA_LOADER_DRIVER_OVERRIDE"] = "llvmpipe"
    env["SDL_AUDIODRIVER"] = "dummy"
    env["ALSOFT_DRIVERS"] = "null"
    if spec["version"] == "26.3":
        env["LD_PRELOAD"] = str(ROOT / "tools" / "libglx-srgb.so")
    log_path = run_dir / "stdout.log"
    wrapped = ["xvfb-run", "-a", "-s", "-screen 0 1280x720x24", "--", *command]
    print(f"start {spec['version']} {spec['loader']} {mode} attempt {attempt}", flush=True)
    with log_path.open("w") as log_handle:
        proc = subprocess.Popen(
            wrapped,
            cwd=game_dir,
            env=env,
            stdout=log_handle,
            stderr=subprocess.STDOUT,
            start_new_session=True,
        )
    with LIVE_LOCK:
        LIVE_PROCS.add(proc)
    timed_out = False
    try:
        deadline = time.time() + args.timeout_minutes * 60
        grace_until = None
        while True:
            text = combined_log(game_dir, shots, log_path)
            if PASS_MARK in text or (shots / "harness-result.txt").is_file() and (shots / "harness-result.txt").read_text().startswith("PASS"):
                grace_until = grace_until or time.time() + 90
            if FAIL_MARK in text:
                grace_until = grace_until or time.time() + 20
            if proc.poll() is not None:
                break
            if grace_until and time.time() > grace_until:
                terminate(proc)
                break
            if time.time() > deadline:
                timed_out = True
                terminate(proc)
                break
            time.sleep(2)
    finally:
        with LIVE_LOCK:
            LIVE_PROCS.discard(proc)
    text = combined_log(game_dir, shots, log_path)
    result_file = ""
    result_path = shots / "harness-result.txt"
    if result_path.is_file():
        result_file = result_path.read_text(errors="replace")
    classified = classify_log(text, result_file, proc.returncode, timed_out)
    classified["durationSeconds"] = round(time.time() - started, 1)
    classified["attempt"] = attempt
    screenshot = publish_screenshots(output, spec, mode, shots)
    classified["screenshot"] = screenshot
    if classified["status"] == "pass" and screenshot is None:
        classified["status"] = "fail"
        classified["failureReason"] = "harness passed but no screenshot was written"
    if classified["status"] != "pass":
        publish_log(output, spec, mode, text)
    if not args.keep_runs and classified["status"] == "pass":
        shutil.rmtree(run_dir, ignore_errors=True)
    return classified


OPTIONS = """\
pauseOnLostFocus:false
renderDistance:2
simulationDistance:5
maxFps:20
enableVsync:false
guiScale:2
onboardAccessibility:false
skipMultiplayerWarning:true
narrator:0
soundCategory_master:0.0
tutorialStep:none
hideBundleTutorial:true
skipRealms32bitWarning:true
particles:2
renderClouds:false
entityShadows:false
"""


def launch_command(minecraft_dir: Path, spec: dict, game_dir: Path, shots: Path, java: Path, heap_mb: int) -> list[str]:
    import minecraft_launcher_lib.command as command

    version_id = installed_version_id(spec)
    if not (minecraft_dir / "versions" / version_id / f"{version_id}.json").is_file():
        matches = runtime_dirs(minecraft_dir, spec)
        if not matches:
            raise RuntimeError(f"no installed runtime for {spec['version']} {spec['loader']}")
        version_id = matches[0].name
    options = {
        "username": "Player",
        "uuid": offline_uuid("Player"),
        "token": "0",
        "executablePath": str(java),
        "jvmArguments": [
            f"-Xmx{heap_mb}m",
            "-Xms512m",
            "-Dvillager_reroller.harness=true",
            f"-Dvillager_reroller.artifacts={shots}",
            f"-Dvillager_reroller.loader={spec['loader']}",
            "-Dvillager_reroller.timeoutScale=8",
            "-Djava.net.preferIPv4Stack=true",
        ],
        "gameDirectory": str(game_dir),
        "customResolution": True,
        "resolutionWidth": "1280",
        "resolutionHeight": "720",
        "disableMultiplayer": True,
        "launcherName": "villager-reroller-validate",
        "launcherVersion": "1.0",
    }
    built = command.get_minecraft_command(version_id, str(minecraft_dir), options)
    return force_offline(built)


def force_offline(command: list[str]) -> list[str]:
    out = []
    index = 0
    while index < len(command):
        token = command[index]
        if token == "--userType" and index + 1 < len(command):
            out.extend(["--userType", "legacy"])
            index += 2
            continue
        out.append(token)
        index += 1
    return out


def offline_uuid(name: str) -> str:
    digest = bytearray(hashlib.md5(("OfflinePlayer:" + name).encode("utf-8")).digest())
    digest[6] = (digest[6] & 0x0F) | 0x30
    digest[8] = (digest[8] & 0x3F) | 0x80
    return str(uuid.UUID(bytes=bytes(digest)))


def combined_log(game_dir: Path, shots: Path, stdout_log: Path) -> str:
    parts = []
    for path in (stdout_log, game_dir / "logs" / "latest.log", shots / "harness-result.txt"):
        if path.is_file():
            parts.append(path.read_text(errors="replace"))
    return "\n".join(parts)


def classify_log(text: str, result_file: str, exit_code: int | None, timed_out: bool) -> dict:
    checks = {name: marker in text for name, marker in CHECKS}
    fail_line = ""
    for line in text.splitlines():
        if FAIL_MARK in line:
            fail_line = line.split(FAIL_MARK, 1)[1].strip()
    mod_errors = []
    for line in text.splitlines():
        if FAIL_MARK in line or "could not write harness result" in line:
            continue
        if MOD_ERROR.search(line) or "[villager_reroller/ERROR]" in line or "[villager_reroller/FATAL]" in line:
            mod_errors.append(line.strip())
    passed = (result_file.startswith("PASS") or PASS_MARK in text) and all(checks.values()) and not fail_line and not mod_errors
    reason = None
    if not passed:
        if mod_errors:
            reason = mod_errors[0][:500]
        elif fail_line:
            reason = fail_line[:500]
        elif timed_out:
            reason = "wall-clock timeout before the harness finished"
        elif not checks["modLoaded"]:
            reason = f"mod did not load (exit {exit_code})"
        else:
            missing = [name for name, ok in checks.items() if not ok]
            reason = "missing checks: " + ", ".join(missing)
    return {
        "status": "pass" if passed else "fail",
        "checks": checks,
        "failureReason": reason,
        "exitCode": exit_code,
        "timedOut": timed_out,
        "logExcerpt": "\n".join(text.splitlines()[-40:]),
    }


def publish_screenshots(output: Path, spec: dict, mode: str, shots: Path) -> str | None:
    preferred = ["reroll-cycling.png", "panel-trade-cycling.png"] if mode == "with-tc" else ["reroll-found.png", "trade-panel.png", "name-scroll-mid.png"]
    preferred.extend(["config-screen.png", "merchant-button.png"])
    present = [shots / name for name in preferred if (shots / name).is_file()]
    present.extend(sorted(path for path in shots.glob("*.png") if path not in present))
    if not present:
        return None
    destination_dir = output / "screenshots" / spec["version"] / spec["loader"]
    destination_dir.mkdir(parents=True, exist_ok=True)
    primary = destination_dir / f"{mode}.png"
    shutil.copy(present[0], primary)
    extra = destination_dir / mode
    extra.mkdir(exist_ok=True)
    for path in shots.glob("*.png"):
        shutil.copy(path, extra / path.name)
    return str(primary.relative_to(ROOT))


def publish_log(output: Path, spec: dict, mode: str, text: str) -> None:
    destination = output / "logs" / spec["version"] / spec["loader"]
    destination.mkdir(parents=True, exist_ok=True)
    lines = text.splitlines()
    if len(lines) > 4000:
        lines = lines[-4000:]
    (destination / f"{mode}.log").write_text("\n".join(lines) + "\n")


def terminate(proc: subprocess.Popen) -> None:
    try:
        os.killpg(proc.pid, signal.SIGTERM)
    except ProcessLookupError:
        return
    try:
        proc.wait(timeout=15)
    except subprocess.TimeoutExpired:
        try:
            os.killpg(proc.pid, signal.SIGKILL)
        except ProcessLookupError:
            pass


def stop_live_processes() -> None:
    with LIVE_LOCK:
        procs = list(LIVE_PROCS)
    for proc in procs:
        terminate(proc)


def previous_rows(output: Path) -> dict[tuple[str, str], dict]:
    path = output / "report.json"
    if not path.is_file():
        return {}
    try:
        payload = json.loads(path.read_text())
    except json.JSONDecodeError:
        return {}
    rows = {}
    for row in payload.get("results") or []:
        version = row.get("version")
        loader = row.get("loader")
        if version and loader:
            rows[(version, loader)] = row
    return rows


def write_reports(output: Path, selected: list[dict], results: dict, partial: bool) -> dict:
    previous = previous_rows(output)
    fresh: dict[tuple[str, str], dict] = {}
    for spec in selected:
        without = results.get((spec["version"], spec["loader"], "without-tc"))
        with_tc = results.get((spec["version"], spec["loader"], "with-tc"))
        reasons = []
        if without and without["status"] != "pass":
            reasons.append("without Trade Cycling: " + (without["failureReason"] or "failed"))
        elif without is None and not partial:
            reasons.append("without Trade Cycling: not run")
        if with_tc and with_tc["status"] != "pass":
            reasons.append("with Trade Cycling: " + (with_tc["failureReason"] or "failed"))
        elif with_tc is None and not partial:
            reasons.append("with Trade Cycling: not run")
        duration = 0.0
        if without:
            duration += without["durationSeconds"]
        if with_tc:
            duration += with_tc["durationSeconds"]
        fresh[(spec["version"], spec["loader"])] = {
            "version": spec["version"],
            "loader": spec["loader"],
            "jar": str(spec["jar"].relative_to(ROOT)) if spec.get("jar") else None,
            "sha256": spec.get("sha256") or "",
            "durationSeconds": round(duration, 1),
            "withoutTradeCycling": public_mode(without),
            "withTradeCycling": public_mode(with_tc),
            "failureReason": "; ".join(reasons),
        }
    rows = []
    for entry in load_matrix(ROOT)["entries"]:
        key = (entry["version"], entry["loader"])
        if key in fresh:
            rows.append(fresh[key])
        elif key in previous:
            rows.append(previous[key])
    report = {
        "generatedAt": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()),
        "partial": partial,
        "results": rows,
    }
    (output / "report.json").write_text(json.dumps(report, indent=2) + "\n")
    (output / "report.md").write_text(render_markdown(report))
    return report


def public_mode(result: dict | None) -> dict | None:
    if result is None:
        return None
    return {
        "status": result["status"],
        "durationSeconds": result["durationSeconds"],
        "attempts": result.get("attempts") or result.get("attempt"),
        "screenshot": result.get("screenshot"),
        "checks": result.get("checks"),
        "failureReason": result.get("failureReason"),
        "exitCode": result.get("exitCode"),
        "logExcerpt": None if result["status"] == "pass" else result.get("logExcerpt"),
    }


def render_markdown(report: dict) -> str:
    lines = [
        "# Villager Reroller release-jar validation",
        "",
        f"Generated {report['generatedAt']}.",
        "",
        "| Version | Loader | Without TC | With TC | JAR SHA-256 | Duration | Failure reason |",
        "| --- | --- | --- | --- | --- | --- | --- |",
    ]
    passed = 0
    for row in report["results"]:
        if not row["failureReason"] and row["withoutTradeCycling"] and row["withTradeCycling"]:
            passed += 1
        lines.append(
            "| {version} | {loader} | {without} | {with_tc} | `{sha}` | {duration} | {reason} |".format(
                version=cell(row["version"]),
                loader=cell(row["loader"]),
                without=mode_cell(row["withoutTradeCycling"]),
                with_tc=mode_cell(row["withTradeCycling"]),
                sha=row["sha256"] or "-",
                duration=fmt_duration(row["durationSeconds"]),
                reason=cell(row["failureReason"]),
            )
        )
    lines.append("")
    lines.append(f"{passed}/{len(report['results'])} rows passed.")
    lines.append("")
    return "\n".join(lines)


def mode_cell(result: dict | None) -> str:
    if result is None:
        return "not run"
    return result["status"]


def cell(value: str) -> str:
    return value.replace("|", "\\|").replace("\n", " ")


def fmt_duration(seconds: float) -> str:
    whole = int(round(seconds))
    minutes, secs = divmod(whole, 60)
    hours, minutes = divmod(minutes, 60)
    if hours:
        return f"{hours}h {minutes}m {secs}s"
    if minutes:
        return f"{minutes}m {secs}s"
    return f"{secs}s"


def self_test() -> None:
    matrix = load_matrix(ROOT)
    assert len(matrix["versions"]) == 20, matrix["versions"]
    assert len(matrix["entries"]) == 54
    assert matrix["entries"][0]["loader"] == "fabric"
    assert matrix["entries"][1]["loader"] == "forge"
    neo_entries = [entry for entry in matrix["entries"] if entry["loader"] == "neoforge"]
    assert len(neo_entries) == 14
    neo_versions = {entry["version"] for entry in neo_entries}
    assert "1.19" not in neo_versions and "1.19.4" not in neo_versions and "1.20.1" not in neo_versions
    assert {"1.20.2", "1.21.1", "1.21.6", "1.21.9", "26.3"} <= neo_versions
    neo_by_version = {entry["version"]: entry for entry in neo_entries}
    assert neo_by_version["1.21.1"]["neoVersion"] == "21.1.265"
    assert neo_by_version["1.21.6"]["neoVersion"] == "21.6.20-beta"
    assert neo_by_version["26.3"]["neoTradeCycling"] == "neoforge-1.0.23+26.3"
    assert installed_version_id(neo_by_version["1.20.2"]) == "neoforge-20.2.93"
    assert dependency_coords(neo_by_version["1.21.1"], False) == []
    assert dependency_coords(neo_by_version["1.21.1"], True) == [("maven.modrinth", "trade-cycling", "neoforge-1.21.1-1.0.23")]
    assert matrix["entries"][0]["fabricLoader"]
    assert matrix["entries"][1]["forgeCoordinate"].startswith(matrix["versions"][0])
    filtered = select_matrix(matrix, ["1.21.1"], ["fabric"])
    assert len(filtered) == 1 and filtered[0]["version"] == "1.21.1"
    assert java_major_for("1.19.4") == 17
    assert java_major_for("1.20.4") == 17
    assert java_major_for("1.20.6") == 21
    assert java_major_for("1.21") == 21
    assert java_major_for("26.3") == 25
    assert offline_uuid("Player") == offline_uuid("Player")
    assert offline_uuid("Player") != offline_uuid("Steve")
    pass_log = "\n".join(marker for _name, marker in CHECKS) + "\n" + PASS_MARK + " (cycling=false)"
    classified = classify_log(pass_log, "PASS\n", 0, False)
    assert classified["status"] == "pass", classified
    lag = classify_log("1.21.1 client harness failed: world did not load, screen=null", "", 1, False)
    assert lag["status"] == "fail" and retryable(lag)
    crash = classify_log("java.lang.NoSuchMethodError: Minecraft.getInstance", "", 255, False)
    assert crash["status"] == "fail" and not retryable(crash)
    with tempfile.TemporaryDirectory() as tmp:
        stub = Path(tmp) / "stub.jar"
        bundle = Path(tmp) / "bundle.jar"
        with zipfile.ZipFile(stub, "w") as archive:
            archive.writestr("fabric.mod.json", "{}")
        with zipfile.ZipFile(bundle, "w") as archive:
            archive.writestr("META-INF/jars/fabric-events-interaction-v0.jar", b"not-a-jar")
        assert not jar_is_usable(stub, "fabric-api")
        assert jar_is_usable(bundle, "fabric-api")
        report_dir = Path(tmp) / "out"
        report_dir.mkdir()
        (report_dir / "report.json").write_text(json.dumps({
            "results": [{
                "version": "1.19",
                "loader": "fabric",
                "sha256": "old",
                "durationSeconds": 1,
                "withoutTradeCycling": {"status": "pass"},
                "withTradeCycling": {"status": "pass"},
                "failureReason": "",
            }]
        }))
        kept = write_reports(
            report_dir,
            [{
                "version": "1.21.1",
                "loader": "fabric",
                "jar": ROOT / "README.md",
                "sha256": "new",
            }],
            {
                ("1.21.1", "fabric", "without-tc"): {
                    "status": "pass",
                    "failureReason": None,
                    "durationSeconds": 2,
                    "attempts": 1,
                    "screenshot": None,
                    "checks": {},
                    "exitCode": 0,
                    "logExcerpt": "",
                }
            },
            partial=True,
        )
        by_key = {(row["version"], row["loader"]): row for row in kept["results"]}
        assert by_key[("1.19", "fabric")]["sha256"] == "old"
        assert by_key[("1.21.1", "fabric")]["sha256"] == "new"
    hard = classify_log(
        "villager_reroller harness check mod-loaded\n"
        "1.21.1 client harness failed: name reaches the price at offset 3",
        "",
        1,
        False,
    )
    assert hard["status"] == "fail" and not retryable(hard)
    mixin = classify_log(PASS_MARK + "\n" + "\n".join(marker for _n, marker in CHECKS) + "\nMixinApplyError boom", "PASS\n", 0, False)
    assert mixin["status"] == "fail"
    forced = force_offline(["java", "--userType", "msa", "--username", "Player"])
    assert forced[forced.index("--userType") + 1] == "legacy"
    report = {
        "generatedAt": "2026-10-24T00:00:00Z",
        "partial": False,
        "results": [
            {
                "version": "1.21.1",
                "loader": "fabric",
                "sha256": "abc",
                "durationSeconds": 65,
                "withoutTradeCycling": {"status": "pass"},
                "withTradeCycling": {"status": "fail"},
                "failureReason": "with Trade Cycling: pipe | and\nnewline",
            }
        ],
    }
    markdown = render_markdown(report)
    assert "1m 5s" in markdown
    assert "pipe \\| and newline" in markdown
    assert "| fail |" in markdown or "| pass | fail |" in markdown
    print(markdown)


if __name__ == "__main__":
    try:
        sys.exit(main())
    except KeyboardInterrupt:
        stop_live_processes()
        sys.exit(130)
