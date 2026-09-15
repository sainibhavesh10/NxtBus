#!/usr/bin/env python3
"""
generate_trip_shapes.py

Generates a real road-following geometry for every bus trip in the DB,
using a self-hosted OSRM 'driving' (car) instance as a road-network
approximation for buses, and gives each trip its own dedicated shape_id
(shape_id == trip_id) -- no sharing a shape across multiple trips.

This is the counterpart to generate_walk_transfers.py: that script
handles walk_* shapes between nearby stop pairs; this one handles every
bus trip's own shape.

Assumptions about your schema (standard GTFS; adjust the SQL below if
your table/column names differ):
    trips(trip_id, shape_id, ...)          -- shape_id nullable
    stop_times(trip_id, stop_id, stop_sequence, ...)
    stops(stop_id, stop_lat, stop_lon, ...)
    shapes(shape_id TEXT PK, geom geometry(LineString,4326), num_points INTEGER)

Model: trips.shape_id starts out NULL for every bus trip (walk_* shapes
live only in the `shapes`/`transfers` tables and are never referenced by
trips.shape_id). This script:

1. Selects every trip where shape_id IS NULL.
2. Pulls that trip's stops in stop_sequence order.
3. Sends all of those stop coordinates as ordered waypoints to OSRM's
   /route endpoint (driving profile) in a single request, so OSRM
   returns one continuous road-following path through all of them.
4. Inserts a `shapes` row keyed by shape_id = trip_id, then sets
   trips.shape_id = trip_id for that trip.

Because a trip's shape_id is only set after its shape is successfully
written, re-running the script after an interruption or failure just
picks up wherever it left off -- no separate "resume" flag needed.

--reset (optional): if you want to regenerate everything from scratch,
this deletes every non-walk_ row from `shapes` and nulls out shape_id on
every non-walk trip first -- i.e. exactly the manual reset you'd do
before a first per-trip run.

Setup
-----
    cd scripts
    pip install -r requirements.txt   # same deps as generate_walk_transfers.py

    # One-time OSRM car-profile data prep (separate from the foot profile):
    ./setup_osrm.sh https://download.geofabrik.de/<region>-latest.osm.pbf car
    docker compose up -d osrm-car

    python generate_trip_shapes.py --limit 5 -v   # smoke test first
    python generate_trip_shapes.py                # full run (safe to re-run/resume)

    # To wipe everything and regenerate from scratch:
    python generate_trip_shapes.py --reset
"""

import argparse
import json
import logging
import os
import sys
import time
from concurrent.futures import ThreadPoolExecutor, as_completed
from dataclasses import dataclass
from pathlib import Path
from typing import List, Optional

import psycopg2
import psycopg2.extras
import requests
from dotenv import load_dotenv
from requests.adapters import HTTPAdapter
from urllib3.util.retry import Retry

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
log = logging.getLogger("trip_shapes")

SCRIPT_DIR = Path(__file__).resolve().parent
PROJECT_ROOT = SCRIPT_DIR.parent


# --------------------------------------------------------------------------
# Config
# --------------------------------------------------------------------------

@dataclass
class Config:
    db: dict
    osrm_base_url: str = "http://localhost:5002/route/v1/driving/"
    concurrency: int = 8
    request_timeout_s: float = 30.0   # multi-waypoint routes can take longer than 2-point ones
    max_retries: int = 4
    retry_backoff_s: float = 1.5
    commit_every: int = 50
    limit: int = 0
    min_stops: int = 2
    dry_run: bool = False
    reset: bool = False


def load_db_config(cli_overrides: dict) -> dict:
    env_path = PROJECT_ROOT / ".env"
    if env_path.exists():
        load_dotenv(dotenv_path=env_path)
    else:
        load_dotenv()
        log.warning("No .env found at %s; falling back to default search", env_path)

    cfg = {
        "host": os.getenv("POSTGRES_HOST", "localhost"),
        "port": int(os.getenv("POSTGRES_PORT", "5432")),
        "dbname": os.getenv("POSTGRES_DB"),
        "user": os.getenv("POSTGRES_USER"),
        "password": os.getenv("POSTGRES_PASSWORD"),
    }
    cfg.update({k: v for k, v in cli_overrides.items() if v is not None})

    missing = [k for k in ("dbname", "user", "password") if not cfg.get(k)]
    if missing:
        log.error("Missing DB config %s (checked %s)", missing, env_path)
        sys.exit(1)
    return cfg


def build_session(cfg: Config) -> requests.Session:
    session = requests.Session()
    retry = Retry(
        total=cfg.max_retries,
        backoff_factor=cfg.retry_backoff_s,
        status_forcelist=(500, 502, 503, 504),
        allowed_methods=frozenset(["GET"]),
        raise_on_status=False,
    )
    pool_size = max(cfg.concurrency, 1)
    adapter = HTTPAdapter(max_retries=retry, pool_connections=pool_size, pool_maxsize=pool_size)
    session.mount("http://", adapter)
    session.mount("https://", adapter)
    session.headers.update({"User-Agent": "nxtbus-trip-shape-builder/1.0"})
    return session


def get_connection(db_cfg: dict):
    conn = psycopg2.connect(**db_cfg)
    conn.autocommit = False
    return conn


# --------------------------------------------------------------------------
# Database
# --------------------------------------------------------------------------

def reset_shapes(conn, cfg: Config):
    """
    Optional one-time reset (--reset): delete every non-walk row from
    `shapes` and null out shape_id on every non-walk trip. Equivalent to
    the manual reset before a first per-trip run -- lets you regenerate
    every trip shape from scratch.
    """
    with conn.cursor() as cur:
        cur.execute("DELETE FROM shapes WHERE shape_id NOT LIKE 'walk\\_%' ESCAPE '\\'")
        deleted = cur.rowcount

        cur.execute("""
            UPDATE trips
            SET shape_id = NULL
            WHERE shape_id IS NOT NULL
              AND shape_id NOT LIKE 'walk\\_%' ESCAPE '\\'
        """)
        updated = cur.rowcount

    if cfg.dry_run:
        log.info("[dry-run] would delete %d old shapes, null out shape_id on %d trips",
                  deleted, updated)
        conn.rollback()
    else:
        conn.commit()
        log.info("Reset done: deleted %d old shapes, nulled shape_id on %d trips",
                  deleted, updated)


def fetch_trips_needing_shapes(conn, cfg: Config) -> List[dict]:
    """
    Every trip with shape_id IS NULL still needs its shape generated.
    Trips already assigned a shape_id (from a prior successful run) are
    skipped automatically, so re-running the script resumes rather than
    redoing everything.
    """
    sql = """
        SELECT t.trip_id
        FROM trips t
        WHERE t.shape_id IS NULL
        ORDER BY t.trip_id
    """
    params: List = []
    if cfg.limit and cfg.limit > 0:
        sql += " LIMIT %s"
        params.append(cfg.limit)

    with conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor) as cur:
        cur.execute(sql, params)
        rows = cur.fetchall()
    log.info("Found %d trips still needing a shape", len(rows))
    return rows


def fetch_ordered_stops(conn, trip_id: str) -> List[dict]:
    sql = """
        SELECT s.stop_lat, s.stop_lon
        FROM stop_times st
        JOIN stops s ON s.stop_id = st.stop_id
        WHERE st.trip_id = %s
        ORDER BY st.stop_sequence
    """
    with conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor) as cur:
        cur.execute(sql, (trip_id,))
        return cur.fetchall()


def upsert_shape_and_link_trip(conn, trip_id: str, geojson_str: str, num_points: int, dry_run: bool):
    """
    Writes the shapes row (shape_id = trip_id) and points the trip at it
    in one transaction, so a trip's shape_id is only ever set once its
    shape actually exists.
    """
    if dry_run:
        log.debug("[dry-run] would upsert shape %s (%d points) and link trip %s", trip_id, num_points, trip_id)
        return
    with conn.cursor() as cur:
        cur.execute(
            """
            INSERT INTO shapes (shape_id, geom, num_points)
            VALUES (%s, ST_SetSRID(ST_GeomFromGeoJSON(%s), 4326), %s)
            ON CONFLICT (shape_id) DO UPDATE
                SET geom = EXCLUDED.geom,
                    num_points = EXCLUDED.num_points
            """,
            (trip_id, geojson_str, num_points),
        )
        cur.execute(
            "UPDATE trips SET shape_id = %s WHERE trip_id = %s",
            (trip_id, trip_id),
        )


# --------------------------------------------------------------------------
# OSRM
# --------------------------------------------------------------------------

class OsrmError(Exception):
    pass


def osrm_multi_waypoint_route(session: requests.Session, cfg: Config, job: dict) -> dict:
    """
    Routes a trip's full ordered stop list as waypoints through OSRM.
    Returns a result dict; never raises (errors captured in 'error').
    """
    trip_id = job["trip_id"]
    coords = job["coords"]  # list of (lat, lon) in visiting order

    waypoints = ";".join(f"{lon},{lat}" for lat, lon in coords)
    url = f"{cfg.osrm_base_url}{waypoints}"
    params = {"overview": "full", "geometries": "geojson"}

    attempt = 0
    last_exc: Optional[Exception] = None

    while attempt <= cfg.max_retries:
        attempt += 1
        try:
            resp = session.get(url, params=params, timeout=cfg.request_timeout_s)

            if resp.status_code == 429:
                time.sleep(cfg.retry_backoff_s * attempt)
                continue

            resp.raise_for_status()
            data = resp.json()

            if data.get("code") != "Ok":
                raise OsrmError(f"code={data.get('code')}: {data.get('message')}")

            route = data["routes"][0]
            return {
                "trip_id": trip_id,
                "geometry": route["geometry"],
                "error": None,
            }

        except (requests.RequestException, OsrmError, KeyError, IndexError, ValueError) as exc:
            last_exc = exc
            time.sleep(cfg.retry_backoff_s * attempt)

    return {"trip_id": trip_id, "geometry": None, "error": str(last_exc)}


def persist_result(conn, cfg: Config, result: dict) -> bool:
    if result["error"] is not None:
        log.error("OSRM failed for trip %s: %s", result["trip_id"], result["error"])
        return False

    coords = result["geometry"].get("coordinates", [])
    num_points = len(coords)
    if num_points < 2:
        log.warning("Degenerate route for trip %s (num_points=%d); skipping", result["trip_id"], num_points)
        return False

    upsert_shape_and_link_trip(conn, result["trip_id"], json.dumps(result["geometry"]), num_points, cfg.dry_run)
    return True


# --------------------------------------------------------------------------
# Core pipeline
# --------------------------------------------------------------------------

def run(cfg: Config):
    conn = get_connection(cfg.db)
    session = build_session(cfg)

    if cfg.reset:
        try:
            reset_shapes(conn, cfg)
        except Exception:
            log.exception("Reset step failed -- check that 'shapes' and 'trips' "
                           "tables exist with the expected columns")
            conn.rollback()
            conn.close()
            sys.exit(1)

    try:
        trips = fetch_trips_needing_shapes(conn, cfg)
    except Exception:
        log.exception("Failed to fetch trips -- check that 'trips' and 'stop_times' "
                       "tables exist with the expected columns")
        conn.close()
        sys.exit(1)

    jobs = []
    skipped_too_few = 0
    for row in trips:
        stops = fetch_ordered_stops(conn, row["trip_id"])
        if len(stops) < cfg.min_stops:
            skipped_too_few += 1
            continue
        jobs.append({
            "trip_id": row["trip_id"],
            "coords": [(s["stop_lat"], s["stop_lon"]) for s in stops],
        })

    total = len(jobs)
    log.info("%d trip shapes to route (%d skipped: fewer than %d stops)",
              total, skipped_too_few, cfg.min_stops)

    if total == 0:
        log.info("Nothing to do.")
        conn.close()
        return

    processed = 0
    succeeded = 0
    failed = 0

    try:
        with ThreadPoolExecutor(max_workers=cfg.concurrency) as pool:
            futures = {pool.submit(osrm_multi_waypoint_route, session, cfg, j): j for j in jobs}

            for future in as_completed(futures):
                result = future.result()
                try:
                    ok = persist_result(conn, cfg, result)
                except Exception:
                    log.exception("DB error persisting trip %s", result["trip_id"])
                    conn.rollback()
                    ok = False

                processed += 1
                succeeded += 1 if ok else 0
                failed += 0 if ok else 1

                if processed % cfg.commit_every == 0:
                    conn.commit()
                    log.info("Progress: %d/%d (%d ok, %d failed)", processed, total, succeeded, failed)

        conn.commit()

    except KeyboardInterrupt:
        log.warning("Interrupted; committing partial progress...")
        conn.commit()
        conn.close()
        sys.exit(1)

    log.info("Done. %d processed, %d succeeded, %d failed.", processed, succeeded, failed)
    conn.close()
    session.close()


# --------------------------------------------------------------------------
# CLI
# --------------------------------------------------------------------------

def parse_args() -> Config:
    p = argparse.ArgumentParser(description="Generate real road-following geometries for GTFS trip shapes via OSRM.")
    p.add_argument("--osrm-url", default="http://localhost:5002/route/v1/driving/", help="OSRM driving routing base URL")
    p.add_argument("--concurrency", type=int, default=8, help="Parallel OSRM requests (default 8)")
    p.add_argument("--timeout", type=float, default=30.0, help="OSRM request timeout (s); multi-waypoint routes are slower")
    p.add_argument("--max-retries", type=int, default=4)
    p.add_argument("--backoff", type=float, default=1.5)
    p.add_argument("--commit-every", type=int, default=50)
    p.add_argument("--limit", type=int, default=0, help="Limit number of trips processed (0 = no limit; for testing)")
    p.add_argument("--min-stops", type=int, default=2, help="Skip trips with fewer than this many stops")
    p.add_argument("--reset", action="store_true",
                   help="Delete all non-walk shapes and null out shape_id on every non-walk "
                        "trip before routing, to regenerate everything from scratch")
    p.add_argument("--dry-run", action="store_true")
    p.add_argument("-v", "--verbose", action="store_true")

    p.add_argument("--db-host", default=None)
    p.add_argument("--db-port", type=int, default=None)
    p.add_argument("--db-name", default=None)
    p.add_argument("--db-user", default=None)
    p.add_argument("--db-password", default=None)

    args = p.parse_args()
    if args.verbose:
        log.setLevel(logging.DEBUG)

    db_overrides = {
        "host": args.db_host, "port": args.db_port, "dbname": args.db_name,
        "user": args.db_user, "password": args.db_password,
    }
    db_cfg = load_db_config(db_overrides)

    osrm_url = args.osrm_url if args.osrm_url.endswith("/") else args.osrm_url + "/"

    return Config(
        db=db_cfg,
        osrm_base_url=osrm_url,
        concurrency=max(args.concurrency, 1),
        request_timeout_s=args.timeout,
        max_retries=args.max_retries,
        retry_backoff_s=args.backoff,
        commit_every=args.commit_every,
        limit=args.limit,
        min_stops=args.min_stops,
        dry_run=args.dry_run,
        reset=args.reset,
    )


if __name__ == "__main__":
    cfg = parse_args()
    run(cfg)