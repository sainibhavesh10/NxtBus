# NxtBus Footpath Routing & Transfers

This module calculates real walking transfer times and footpath geometries between nearby bus stops (within a 1000m radius). To process hundreds of thousands of routes quickly, we use a self-hosted OSRM (Open Source Routing Machine) Docker container instead of public routing APIs.

## Prerequisites
* **Docker & Docker Compose** (for running OSRM and PostGIS)
* **Python 3.x**
* **macOS** (The setup script uses `caffeinate` to prevent the system from sleeping during data processing)

## Setup & Execution

### 1. Clean & Process Map Data
First, remove any stale OSRM data, then download and process the latest OpenStreetMap extract. We use the Northern Zone of India.

Because map processing is highly CPU-intensive and can take a while, we wrap the setup script in `caffeinate` to prevent the Mac from going to sleep midway through.

```bash
# From the project root
rm -f data/osrm/northern-zone-latest.osrm*
caffeinate -i ./scripts/setup_osrm.sh [https://download.geofabrik.de/asia/india/northern-zone-latest.osm.pbf](https://download.geofabrik.de/asia/india/northern-zone-latest.osm.pbf)