#!/bin/bash
# Downscale design/wallpapers-src/wp_*.{webp,jpg} (4K, 16:9) to 1920x1080 JPEG assets for the APK.
set -euo pipefail
cd "$(dirname "$0")/.."
OUT=app/src/main/assets/wallpapers
mkdir -p "$OUT"
for src in design/wallpapers-src/wp_*.webp design/wallpapers-src/wp_*.jpg; do
  [ -e "$src" ] || continue
  name=$(basename "${src%.*}")
  sips -z 1080 1920 -s format jpeg -s formatOptions 85 "$src" --out "$OUT/$name.jpg" >/dev/null
done
ls "$OUT" | wc -l; du -sh "$OUT"
