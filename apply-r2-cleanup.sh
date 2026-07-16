#!/usr/bin/env bash
set -euo pipefail

TARGET="app/src/main/res/drawable/app_icon.xml"

if [ -f "$TARGET" ]; then
  rm -f "$TARGET"
  echo "Removed obsolete duplicate resource: $TARGET"
else
  echo "No obsolete app_icon.xml found."
fi

if [ ! -f "app/src/main/res/drawable/app_icon.png" ]; then
  echo "ERROR: app_icon.png is missing."
  exit 1
fi

duplicates="$(
  find app/src/main/res -type f -printf '%h/%f\n' |
  awk '
    {
      full=$0
      name=$0
      sub(/^.*\//, "", name)
      sub(/\.[^.]+$/, "", name)
      dir=full
      sub(/\/[^/]+$/, "", dir)
      key=dir "/" name
      count[key]++
      files[key]=files[key] "\n  - " full
    }
    END {
      for (key in count) {
        if (count[key] > 1) {
          print key files[key]
        }
      }
    }
  '
)"

if [ -n "$duplicates" ]; then
  echo "Duplicate Android resource names still exist:"
  printf '%s\n' "$duplicates"
  exit 1
fi

echo "Android resource duplicate check passed."
