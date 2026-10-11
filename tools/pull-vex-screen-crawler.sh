#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
APP_GRADLE="$ROOT/app/build.gradle.kts"
DESTINATION="${1:-$ROOT/vex-screen-crawler}"

if ! command -v adb >/dev/null 2>&1; then
  echo "ERROR: adb is not on PATH." >&2
  exit 1
fi

PACKAGE_ID="$(
  sed -n 's/^[[:space:]]*applicationId[[:space:]]*=[[:space:]]*"\([^"]*\)".*/\1/p' "$APP_GRADLE" |
    head -n 1
)"

if [[ -z "$PACKAGE_ID" ]]; then
  echo "ERROR: could not derive applicationId from $APP_GRADLE" >&2
  exit 1
fi

REMOTE="/sdcard/Android/data/$PACKAGE_ID/files/vex-screen-crawler"
SERIAL="${ANDROID_SERIAL:-}"

if [[ -z "$SERIAL" ]]; then
  DEVICES="$(adb devices | awk 'NR > 1 && $2 == "device" { print $1 }')"
  COUNT="$(printf '%s\n' "$DEVICES" | sed '/^$/d' | wc -l | tr -d ' ')"

  case "$COUNT" in
    0)
      echo "ERROR: no connected adb device/emulator is ready." >&2
      exit 1
      ;;
    1)
      SERIAL="$DEVICES"
      ;;
    *)
      echo "ERROR: multiple adb targets are connected:" >&2
      printf '%s\n' "$DEVICES" >&2
      echo >&2
      echo "Choose the target once, then rerun:" >&2
      echo "  ANDROID_SERIAL=<serial> bash tools/pull-vex-screen-crawler.sh" >&2
      exit 2
      ;;
  esac
fi

if ! adb -s "$SERIAL" shell "test -d '$REMOTE'" >/dev/null 2>&1; then
  echo "ERROR: no ScreenCrawler artifacts found on $SERIAL at:" >&2
  echo "  $REMOTE" >&2
  echo "Run VexScreenCrawlerInstrumentedTest directly first; an explicit crawler run must produce screenshot evidence." >&2
  exit 1
fi

mkdir -p "$DESTINATION"

echo "Pulling ScreenCrawler evidence"
echo "  device: $SERIAL"
echo "  from:   $REMOTE"
echo "  to:     $DESTINATION"

adb -s "$SERIAL" pull "$REMOTE/." "$DESTINATION"

echo
echo "Done: $DESTINATION"
