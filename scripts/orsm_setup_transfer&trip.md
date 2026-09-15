# OSRM & Trip/Transfer Shapes Setup

This project uses **OSRM** to calculate:

* Walking transfers between stops (distance, time, footpath shape) — via the **foot** profile
* Road-following shapes for every bus trip — via the **car** profile (used as a road-network approximation for buses)

Both are generated from OpenStreetMap data for the Northern Zone of India.

## Requirements

Before starting, make sure Docker has enough resources allocated.

### Docker Resources

* **RAM:** At least **7.5 GB**
* **Swap:** At least **4 GB**

If Docker does not have enough RAM or swap, the OSRM setup or generation scripts may fail.

---

## Setup

### 1. Remove Existing OSRM Data

From the project root, run:

```bash
rm -rf data/osrm
```

This removes all previously generated OSRM data (both profiles) so it can be rebuilt from the latest map data. Safe to run even if the folder doesn't exist yet.

### 2. Download and Set Up the Foot Profile

Run:

```bash
caffeinate -i ./scripts/setup_osrm.sh https://download.geofabrik.de/asia/india/northern-zone-latest.osm.pbf foot
```

On macOS, `caffeinate -i` prevents the system from going to sleep while the setup is running.

This downloads the `.osm.pbf` once into `data/osrm/`, then runs the extract/partition/customize pipeline for the **foot** profile, producing `data/osrm/foot/map.osrm*`.

> **Note:** This can take some time and requires a significant amount of RAM and disk space.

### 3. Start the Foot OSRM Server

Once the foot profile setup is complete:

```bash
docker compose up -d postgres osrm-foot
```

Check that the containers are running:

```bash
docker compose ps
```

---

## 4. Install Python Dependencies

Move into the `scripts` directory:

```bash
cd scripts
```

Install the required Python packages:

```bash
pip install -r requirements.txt
```

---

## 5. Generate Walk Transfers

The `generate_walk_transfers.py` script generates the transfer data, including:

* Walking distance
* Walking time
* Footpath shape

### Test with 20 Transfers

To test the setup first, run:

```bash
python generate_walk_transfers.py --limit 20 -v
```

This generates only 20 transfers and provides verbose output. It is useful for verifying that everything is working correctly.

### Generate All Transfers

Once the test succeeds, run:

```bash
python generate_walk_transfers.py
```

This generates the complete transfers table.

> **Note:** Generating all transfers can take a considerable amount of time depending on the amount of data and available system resources.

---

## 6. Generate Trip Shapes (Car Profile)

Trip shapes use OSRM's **driving** profile as a road-network approximation for buses. This reuses the same downloaded `.pbf` — no re-download needed — but still requires its own extract/partition/customize run, since the graph differs per profile.

### Download and Set Up the Car Profile

From the project root:

```bash
./scripts/setup_osrm.sh https://download.geofabrik.de/asia/india/northern-zone-latest.osm.pbf car
```

This produces `data/osrm/car/map.osrm*`.

### Start the Car OSRM Server

```bash
docker compose up -d osrm-car
```

### Test with 5 Trips

```bash
cd scripts
python generate_trip_shapes.py --limit 5 -v
```

### Generate All Trip Shapes

```bash
python generate_trip_shapes.py
```

This is safe to re-run/resume — a trip's `shape_id` is only set once its shape is successfully written, so an interrupted run just picks up where it left off.

To regenerate everything from scratch:

```bash
python generate_trip_shapes.py --reset
```

---

## Complete Setup Commands

If you have already configured Docker with the required resources, the complete setup is:

```bash
# clean slate (safe even if these don't exist)
rm -rf data/osrm

# prep the foot profile (downloads the .pbf once into data/osrm/, then extract/partition/customize)
caffeinate -i ./scripts/setup_osrm.sh https://download.geofabrik.de/asia/india/northern-zone-latest.osm.pbf foot

# start only the foot server for now
docker compose up -d postgres osrm-foot

cd scripts
pip install -r requirements.txt

# Test first
python generate_walk_transfers.py --limit 20 -v

# Generate all transfers
python generate_walk_transfers.py
```

Then, for trip shapes, prep and start the car profile too (reuses the same downloaded `.pbf`, no re-download):

```bash
cd ..
./scripts/setup_osrm.sh https://download.geofabrik.de/asia/india/northern-zone-latest.osm.pbf car
docker compose up -d osrm-car

cd scripts
python generate_trip_shapes.py --limit 5 -v
python generate_trip_shapes.py
```

## Troubleshooting

### Docker runs out of memory

Make sure Docker has at least:

```text
RAM:   7.5 GB
Swap:  4 GB
```

Then restart Docker and try the setup again.

### An OSRM container is not running

Check container status:

```bash
docker compose ps
```

View logs for a specific profile's container:

```bash
docker compose logs osrm-foot
docker compose logs osrm-car
```

### Start OSRM again

```bash
docker compose up -d osrm-foot
docker compose up -d osrm-car
```

### OSRM returns 400 Bad Request / NoRoute for a specific trip

This usually means a segment of that trip's route isn't drivable in OSRM's graph — commonly a river crossing where the bridge is tagged as foot/cycle-only, missing from the extract, or has an access restriction. Check the failing coordinate pair directly:

```bash
curl 'http://localhost:5002/route/v1/driving/<lon1>,<lat1>;<lon2>,<lat2>?overview=false'
```

The returned `message` field will identify the exact cause.

---

## Summary

The setup flow is:

```text
OpenStreetMap (.osm.pbf)
        ↓
setup_osrm.sh <url> foot          setup_osrm.sh <url> car
        ↓                                  ↓
data/osrm/foot/                   data/osrm/car/
        ↓                                  ↓
osrm-foot container                osrm-car container
        ↓                                  ↓
generate_walk_transfers.py         generate_trip_shapes.py
        ↓                                  ↓
Transfers table                    Trips get shape_id + shapes row
        ↓                                  ↓
Distance + Time + Footpath Shape   Road-following trip geometry
```

**Important:** Ensure Docker has **at least 7.5 GB RAM and 4 GB swap** before running either setup.