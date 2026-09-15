#!/usr/bin/env bash
#
# setup_osrm.sh
#
# One-time preprocessing step for the self-hosted OSRM services in
# docker-compose.yml ('osrm-foot' for foot routing, 'osrm-car' for driving/bus
# routing). Downloads an OpenStreetMap extract once and runs the
# osrm-extract / osrm-partition / osrm-customize pipeline against the
# requested profile, producing data/osrm/<profile>/map.osrm* which the
# matching osrm-routed container serves.
#
# Usage:
#   ./scripts/setup_osrm.sh <geofabrik_pbf_url> [profile]
#
#   profile: foot (default) | car | bicycle
#
# Find your region's extract at https://download.geofabrik.de/
# Examples:
#   ./scripts/setup_osrm.sh https://download.geofabrik.de/asia/india/northern-zone-latest.osm.pbf foot
#   ./scripts/setup_osrm.sh https://download.geofabrik.de/asia/india/northern-zone-latest.osm.pbf car
#
# The .pbf is downloaded once into data/osrm/ and reused for every
# profile you prep (each profile still needs its own extract/partition/
# customize run -- the resulting graph differs per profile).
#
# Re-run this whenever you want to refresh the map data or add a
# profile. Safe to re-run; overwrites that profile's previous output.
#
# For a from-scratch project build you need BOTH profiles:
#   ./scripts/setup_osrm.sh <pbf_url> foot
#   ./scripts/setup_osrm.sh <pbf_url> car
#   docker compose up -d osrm-foot osrm-car

set -euo pipefail

URL="${1:?Usage: $0 <osm_pbf_download_url> [foot|car|bicycle]  (see https://download.geofabrik.de/)}"
PROFILE="${2:-foot}"

case "$PROFILE" in
  foot|car|bicycle) ;;
  *) echo "Unknown profile '$PROFILE'. Use: foot | car | bicycle" >&2; exit 1 ;;
esac

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
OSRM_ROOT="$PROJECT_ROOT/data/osrm"
PROFILE_DIR="$OSRM_ROOT/$PROFILE"

mkdir -p "$OSRM_ROOT" "$PROFILE_DIR"

FILENAME="$(basename "$URL")"
BASENAME="${FILENAME%.osm.pbf}"
SHARED_PBF="$OSRM_ROOT/$FILENAME"

echo "==> Downloading OSM extract: $FILENAME"
if [ -f "$SHARED_PBF" ]; then
  echo "    Already present at data/osrm/$FILENAME, skipping download."
else
  curl -L -o "$SHARED_PBF" "$URL"
fi

echo "==> Preparing profile: $PROFILE"
cp -f "$SHARED_PBF" "$PROFILE_DIR/$FILENAME"

echo "==> Extracting ($PROFILE profile)..."
docker run --rm -v "$PROFILE_DIR:/data" osrm/osrm-backend \
  osrm-extract -p "/opt/$PROFILE.lua" "/data/$FILENAME"

echo "==> Renaming outputs to map.osrm*"
(
  cd "$PROFILE_DIR"
  for f in "$BASENAME".osrm*; do
    new_name="map.osrm${f#"$BASENAME".osrm}"
    mv "$f" "$new_name"
  done
)

echo "==> Partitioning (MLD)..."
docker run --rm -v "$PROFILE_DIR:/data" osrm/osrm-backend \
  osrm-partition /data/map.osrm

echo "==> Customizing (MLD)..."
docker run --rm -v "$PROFILE_DIR:/data" osrm/osrm-backend \
  osrm-customize /data/map.osrm

# The per-profile copy of the .pbf is no longer needed once extraction
# is done; remove it to save disk (the shared copy in data/osrm/ stays).
rm -f "$PROFILE_DIR/$FILENAME"

echo ""
echo "Done. Start the routing server with:"
echo "  docker compose up -d osrm-$PROFILE"
echo ""
echo "Test it with:"
if [ "$PROFILE" = "foot" ]; then
  echo "  curl 'http://localhost:5001/route/v1/foot/77.2295,28.6129;77.2167,28.6315?overview=false'"
else
  echo "  curl 'http://localhost:5002/route/v1/driving/77.2295,28.6129;77.2167,28.6315?overview=false'"
fi