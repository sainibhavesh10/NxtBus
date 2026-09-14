#!/usr/bin/env bash
#
# setup_osrm.sh
#
# One-time preprocessing step for the self-hosted OSRM 'osrm' service in
# docker-compose.yml. Downloads an OpenStreetMap extract and runs the
# osrm-extract / osrm-partition / osrm-customize pipeline against the
# 'foot' profile, producing data/osrm/map.osrm* which the osrm-routed
# container serves.
#
# Usage:
#   ./scripts/setup_osrm.sh <geofabrik_pbf_url>
#
# Find your region's extract at https://download.geofabrik.de/
# Example (California):
#   ./scripts/setup_osrm.sh https://download.geofabrik.de/north-america/us/california-latest.osm.pbf
#
# Re-run this whenever you want to refresh the map data. It's safe to
# re-run; it overwrites the previous processed files.

set -euo pipefail

URL="${1:?Usage: $0 <osm_pbf_download_url>  (see https://download.geofabrik.de/)}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
OSRM_DATA_DIR="$PROJECT_ROOT/data/osrm"

mkdir -p "$OSRM_DATA_DIR"
cd "$OSRM_DATA_DIR"

FILENAME="$(basename "$URL")"
BASENAME="${FILENAME%.osm.pbf}"

echo "==> Downloading OSM extract: $FILENAME"
if [ -f "$FILENAME" ]; then
  echo "    Already present, skipping download (delete the file to force a re-download)."
else
  curl -L -o "$FILENAME" "$URL"
fi

echo "==> Extracting (foot profile)..."
docker run --rm -v "$OSRM_DATA_DIR:/data" osrm/osrm-backend \
  osrm-extract -p /opt/foot.lua "/data/$FILENAME"

echo "==> Renaming outputs to map.osrm*"
for f in "$BASENAME".osrm*; do
  new_name="map.osrm${f#"$BASENAME".osrm}"
  mv "$f" "$new_name"
done

echo "==> Partitioning (MLD)..."
docker run --rm -v "$OSRM_DATA_DIR:/data" osrm/osrm-backend \
  osrm-partition /data/map.osrm

echo "==> Customizing (MLD)..."
docker run --rm -v "$OSRM_DATA_DIR:/data" osrm/osrm-backend \
  osrm-customize /data/map.osrm

echo ""
echo "Done. Start the routing server with:"
echo "  docker compose up -d osrm"
echo ""
echo "Test it with:"
echo "  curl 'http://localhost:5001/route/v1/foot/13.388,52.517;13.397,52.529?overview=false'"