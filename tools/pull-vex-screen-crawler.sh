#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BUILD_ROOT="$ROOT/app/build"
DESTINATION="${1:-$ROOT/vex-screen-crawler}"

if [[ ! -d "$BUILD_ROOT" ]]; then
  echo "ERROR: app/build does not exist yet." >&2
  echo "Run VexScreenCrawlerInstrumentedTest first." >&2
  exit 1
fi

FOUND=0

# Delivery output represents THIS collection only. Never merge fresh evidence into a stale local tree.
rm -rf "$DESTINATION"
mkdir -p "$DESTINATION"

while IFS= read -r source; do
  FOUND=1
  echo "Collecting ScreenCrawler output:"
  echo "  $source"
  cp -R "$source/." "$DESTINATION/"
done < <(find "$BUILD_ROOT" -type d -name vex-screen-crawler -print)

if [[ "$FOUND" -eq 0 ]]; then
  echo "ERROR: no returned ScreenCrawler outputs found under:" >&2
  echo "  $BUILD_ROOT" >&2
  echo >&2
  echo "Run VexScreenCrawlerInstrumentedTest, then filter Logcat with:" >&2
  echo "  tag:VexScreenCrawler" >&2
  echo "and confirm COMPLETE ... screenshots=<n>." >&2
  exit 1
fi

echo
echo "Done: $DESTINATION"
echo "Screenshot batches:"
find "$DESTINATION" -type d -name screenshots -print
