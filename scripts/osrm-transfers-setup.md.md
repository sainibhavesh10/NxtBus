# OSRM & Walk Transfers Setup

This project uses **OSRM** to calculate walking transfer information such as:

* Footpath distance
* Footpath shape
* Walking time

The transfer data is generated from OpenStreetMap data for the Northern Zone of India.

## Requirements

Before starting, make sure Docker has enough resources allocated.

### Docker Resources

* **RAM:** At least **7.5 GB**
* **Swap:** At least **4 GB**

If Docker does not have enough RAM or swap, the OSRM setup or transfer generation may fail.

---

## Setup

### 1. Remove Existing OSRM Data

From the project root, run:

```bash
rm -f data/osrm/northern-zone-latest.osrm*
```

This removes previously generated OSRM files so they can be created again from the latest map data.

### 2. Download and Setup OSRM Data

Run:

```bash
caffeinate -i ./scripts/setup_osrm.sh https://download.geofabrik.de/asia/india/northern-zone-latest.osm.pbf
```

On macOS, `caffeinate -i` prevents the system from going to sleep while the setup is running.

This step downloads the OpenStreetMap data and processes it for OSRM.

> **Note:** This can take some time and requires a significant amount of RAM and disk space.

### 3. Start the OSRM Docker Container

Once the OSRM setup is complete:

```bash
docker compose up -d osrm
```

Check that the container is running:

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

## Complete Setup Commands

If you have already configured Docker with the required resources, the complete setup is:

```bash
rm -f data/osrm/northern-zone-latest.osrm*

caffeinate -i ./scripts/setup_osrm.sh https://download.geofabrik.de/asia/india/northern-zone-latest.osm.pbf

docker compose up -d osrm

cd scripts
pip install -r requirements.txt

# Test first
python generate_walk_transfers.py --limit 20 -v

# Generate all transfers
python generate_walk_transfers.py
```

## Troubleshooting

### Docker runs out of memory

Make sure Docker has at least:

```text
RAM:   7.5 GB
Swap:  4 GB
```

Then restart Docker and try the setup again.

### OSRM container is not running

Check the container status:

```bash
docker compose ps
```

View OSRM logs with:

```bash
docker compose logs osrm
```

### Start OSRM again

```bash
docker compose up -d osrm
```

---

## Summary

The setup flow is:

```text
OpenStreetMap (.osm.pbf)
        ↓
setup_osrm.sh
        ↓
OSRM data
        ↓
OSRM Docker container
        ↓
generate_walk_transfers.py
        ↓
Transfers table
        ↓
Distance + Time + Footpath Shape
```

**Important:** Ensure Docker has **at least 7.5 GB RAM and 4 GB swap** before running the setup.
