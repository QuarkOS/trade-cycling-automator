#!/usr/bin/env bash
# Verification helper for trade-cycling-automator. See ../SKILL.md.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../.." && pwd)"
SKILL="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
EVIDENCE="$SKILL/evidence/wiki-price-rejection"
export JAVA_HOME="${JAVA_HOME:-/opt/jdk25}"
export PATH="$JAVA_HOME/bin:$PATH"

TEST_TASK=":1.21.11:test"
TEST_CLASS="dev.tradecycling.automator.price.LibrarianBookPricesTest"
TEST_METHOD="rejectsLevelsAndPricesOutsideTheWiki"
TEST_FILTER="${TEST_CLASS}.${TEST_METHOD}"
XML_NAME="TEST-${TEST_CLASS}.xml"
BUILD_XML="$ROOT/versions/1.21.11/build/test-results/test/$XML_NAME"
EVIDENCE_XML="$EVIDENCE/$XML_NAME"

die() {
  echo "verify: $*" >&2
  exit 1
}

require_java25() {
  [[ -x "$JAVA_HOME/bin/java" ]] || die "Java 25 is not at $JAVA_HOME/bin/java"
  local version
  version="$("$JAVA_HOME/bin/java" -version 2>&1 || true)"
  echo "$version"
  echo "$version" | grep -q 'version "25' || die "JAVA_HOME is not Java 25: $JAVA_HOME"
}

launch() {
  echo "Launch: Gradle wrapper on Java 25. No Minecraft client is started."
  require_java25 >/dev/null
  (cd "$ROOT" && ./gradlew --version)
  echo "Left nothing running. Drive with: $0 drive wiki-price-rejection"
}

doctor() {
  local ok=0
  require_java25 >/dev/null || ok=1
  [[ -x "$ROOT/gradlew" ]] || { echo "missing executable gradlew"; ok=1; }
  grep -q 'stonecutter active "1.21.11"' "$ROOT/stonecutter.gradle.kts" \
    || { echo "active Stonecutter version is not 1.21.11"; ok=1; }
  local test_src="$ROOT/src/test/java/dev/tradecycling/automator/price/LibrarianBookPricesTest.java"
  [[ -f "$test_src" ]] || { echo "missing $test_src"; ok=1; }
  grep -q "$TEST_METHOD" "$test_src" || { echo "test method $TEST_METHOD is gone"; ok=1; }
  [[ -f "$ROOT/src/main/java/dev/tradecycling/automator/price/LibrarianBookPrices.java" ]] \
    || { echo "missing LibrarianBookPrices.java"; ok=1; }
  [[ -f "$ROOT/src/main/resources/assets/trade-cycling-automator/librarian_book_prices.json" ]] \
    || { echo "missing librarian_book_prices.json"; ok=1; }

  echo "Jars (warning only if absent):"
  local version
  for version in 1.21.1 1.21.11 26.1.2 26.2 26.3; do
    local jar="$ROOT/versions/$version/build/libs/trade-cycling-automator-1.0.0+$version.jar"
    if [[ -f "$jar" ]]; then
      echo "  present  $jar"
    else
      echo "  absent   $jar"
    fi
  done
  echo "Client: :1.21.11:runClient exists in Loom and is not part of this doctor. Shared run directory would be $ROOT/run."
  if [[ "$ok" -ne 0 ]]; then
    die "doctor failed"
  fi
  echo "doctor ok"
}

assert_xml() {
  local xml="$1"
  [[ -f "$xml" ]] || die "missing JUnit XML $xml"
  python3 - "$xml" "$TEST_METHOD" <<'PY'
import sys
import xml.etree.ElementTree as ET

path, method = sys.argv[1], sys.argv[2]
suite = ET.parse(path).getroot()
failures = suite.attrib.get("failures", "")
errors = suite.attrib.get("errors", "")
tests = int(suite.attrib.get("tests", "0"))
names = [node.attrib.get("name", "") for node in suite.findall("testcase")]
wanted = method + "()"
if failures != "0" or errors != "0" or tests < 1 or wanted not in names:
    sys.stderr.write(
        f"xml not a pass for {wanted}: tests={tests} failures={failures} errors={errors} cases={names}\n"
    )
    sys.exit(1)
print(f"xml ok tests={tests} failures={failures} errors={errors} case={wanted}")
PY
}

drive() {
  local feature="${1:-}"
  case "$feature" in
    wiki-price-rejection) ;;
    enchantment-level-price|start-stop-keybind|librarian-only-stop|attempt-cap)
      echo "No Gradle JUnit driver for ${feature}. Refusing to report a pass." >&2
      echo "See $SKILL/features/${feature}.md" >&2
      exit 2
      ;;
    *)
      echo "Unknown feature '${feature}'." >&2
      echo "Known: wiki-price-rejection enchantment-level-price start-stop-keybind librarian-only-stop attempt-cap" >&2
      exit 2
      ;;
  esac

  mkdir -p "$EVIDENCE"
  local cmd_line="JAVA_HOME=$JAVA_HOME ./gradlew $TEST_TASK --tests $TEST_FILTER --rerun-tasks"
  printf '%s\n' "$cmd_line" | tee "$EVIDENCE/command.txt"

  set +e
  (cd "$ROOT" && ./gradlew "$TEST_TASK" --tests "$TEST_FILTER" --rerun-tasks) 2>&1 | tee "$EVIDENCE/gradle-test.log"
  local code="${PIPESTATUS[0]}"
  set -e
  printf '%s\n' "$code" > "$EVIDENCE/gradle-exit-code"

  if [[ -f "$BUILD_XML" ]]; then
    cp -f "$BUILD_XML" "$EVIDENCE_XML"
  fi
  if [[ "$code" -ne 0 ]]; then
    echo "Gradle exited $code. Log: $EVIDENCE/gradle-test.log" >&2
    exit "$code"
  fi
  assert_xml "$EVIDENCE_XML"
  echo "evidence $EVIDENCE_XML"
}

cleanup() {
  echo "Cleanup: :1.21.11:cleanTest. Evidence under $EVIDENCE is kept."
  (cd "$ROOT" && ./gradlew :1.21.11:cleanTest)
  if [[ -e "$BUILD_XML" ]]; then
    die "cleanTest left $BUILD_XML"
  fi
  if [[ -f "$EVIDENCE_XML" ]]; then
    echo "evidence kept $EVIDENCE_XML"
  else
    echo "no evidence file yet at $EVIDENCE_XML"
  fi
}

usage() {
  echo "usage: $0 launch|doctor|drive <feature>|cleanup" >&2
  exit 2
}

main() {
  local cmd="${1:-}"
  shift || true
  case "$cmd" in
    launch) launch ;;
    doctor) doctor ;;
    drive) [[ $# -eq 1 ]] || usage; drive "$1" ;;
    cleanup) cleanup ;;
    *) usage ;;
  esac
}

main "$@"
