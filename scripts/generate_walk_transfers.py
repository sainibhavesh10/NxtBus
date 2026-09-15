#!/usr/bin/env python3
"""
generate_walk_transfers.py

Computes real walking transfer times and footpath geometries between nearby
NxtBus stops using a self-hosted OSRM 'foot' routing server, and persists
the results into the project's PostGIS database.

Expects to live at <project_root>/scripts/generate_walk_transfers.py and
reads DB credentials from <project_root>/.env (the same file used by
docker-compose.yml).

Pipeline
--------
1. Find candidate stop pairs within `radius` meters (straight-line /
   geodesic distance) using PostGIS ST_DWithin on geography.
2. Skip any (from, to) directions that already have a transfer row
   (idempotent re-runs -- important at this scale).
3. Route the remaining directions through OSRM concurrently (safe/fast
   because this hits your own local `osrm-foot` container, not a shared
   public server).
4. Store the route geometry in `shapes` and the duration in `transfers`,
   using ON CONFLICT DO NOTHING.

Setup
-----
    cd scripts
    pip install -r requirements.txt

    # One-time OSRM data prep (see scripts/setup_osrm.sh):
    ./setup_osrm.sh https://download.geofabrik.de/<region>-latest.osm.pbf
    docker compose up -d osrm-foot

    # Run:
    python generate_walk_transfers.py --limit 20 -v   # smoke test first
    python generate_walk_transfers.py                 # full run

Schema assumed
--------------
    stops(stop_id TEXT PK, stop_name TEXT, stop_lat DOUBLE PRECISION, stop_lon DOUBLE PRECISION)
    shapes(shape_id TEXT PK, geom geometry(LineString,4326), num_points INTEGER)
    transfers(from_stop_id TEXT, to_stop_id TEXT, min_transfer_time INTEGER, shape_id TEXT REFERENCES shapes(shape_id))

A UNIQUE constraint on transfers(from_stop_id, to_stop_id) is required for
ON CONFLICT DO NOTHING to dedupe correctly; this script creates it
automatically (best-effort, safe to re-run) unless --skip-schema-setup.
"""

import argparse
import json
import logging
import os
import sys
import time
from concurrent.futures import ThreadPoolExecutor, as_completed
from dataclasses import dataclass, field
from pathlib import Path
from typing import List, Optional, Set, Tuple

import psycopg2
import psycopg2.extras
import requests
from dotenv import load_dotenv
from requests.adapters import HTTPAdapter
from urllib3.util.retry import Retry

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
)
log = logging.getLogger("walk_transfers")

SCRIPT_DIR = Path(__file__).resolve().parent
PROJECT_ROOT = SCRIPT_DIR.parent


# --------------------------------------------------------------------------
# Config
# --------------------------------------------------------------------------

@dataclass
class Config:
    db: dict
    radius_m: float = 1000.0
    osrm_base_url: str = "http://localhost:5001/route/v1/foot/"
    concurrency: int = 8
    request_delay_s: float = 0.0       # only meaningful with concurrency=1 (e.g. public OSRM)
    request_timeout_s: float = 15.0
    max_retries: int = 4
    retry_backoff_s: float = 1.5
    commit_every: int = 200            # commit every N processed directions
    limit: int = 0                     # 0 = no limit; cap candidate pairs for testing
    dry_run: bool = False
    skip_schema_setup: bool = False
    max_transfer_seconds: float = 1200.0   # discard footpaths with a longer walking duration than this (default 20 min)


def load_db_config(cli_overrides: dict) -> dict:
    """
    Load DB connection info from the project's .env (same one used by
    docker-compose.yml), with CLI flags taking precedence.
    """
    env_path = PROJECT_ROOT / ".env"
    if env_path.exists():
        load_dotenv(dotenv_path=env_path)
    else:
        load_dotenv()  # fallback: search cwd/parents
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
        log.error(
            "Missing DB config %s. Checked %s for POSTGRES_DB/POSTGRES_USER/POSTGRES_PASSWORD.",
            missing, env_path,
        )
        sys.exit(1)

    return cfg


# --------------------------------------------------------------------------
# HTTP session with retry, sized for concurrent use
# --------------------------------------------------------------------------

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
    session.headers.update({"User-Agent": "nxtbus-walk-transfer-builder/1.0"})
    return session


# --------------------------------------------------------------------------
# Database helpers
# --------------------------------------------------------------------------

def get_connection(db_cfg: dict):
    conn = psycopg2.connect(**db_cfg)
    conn.autocommit = False
    return conn


def ensure_schema(conn, cfg: Config):
    """
    Best-effort, idempotent: ensure a UNIQUE constraint exists on
    transfers(from_stop_id, to_stop_id) so ON CONFLICT DO NOTHING works
    correctly on re-runs, plus a spatial index on stops for ST_DWithin.
    """
    if cfg.skip_schema_setup or cfg.dry_run:
        return

    constraint_ddl = """
    DO $$
    BEGIN
        IF NOT EXISTS (
            SELECT 1 FROM pg_constraint WHERE conname = 'uq_transfers_from_to'
        ) THEN
            ALTER TABLE transfers
                ADD CONSTRAINT uq_transfers_from_to UNIQUE (from_stop_id, to_stop_id);
        END IF;
    END$$;
    """
    index_ddl = """
    CREATE INDEX IF NOT EXISTS idx_stops_geog
        ON stops USING GIST (
            geography(ST_SetSRID(ST_MakePoint(stop_lon, stop_lat), 4326))
        );
    """
    for label, ddl in (("unique constraint on transfers", constraint_ddl),
                        ("spatial index on stops", index_ddl)):
        try:
            with conn.cursor() as cur:
                cur.execute(ddl)
            conn.commit()
            log.info("Ensured %s", label)
        except Exception as exc:
            conn.rollback()
            log.warning("Could not ensure %s (continuing): %s", label, exc)


def fetch_candidate_pairs(conn, cfg: Config) -> List[dict]:
    sql = """
        SELECT
            a.stop_id   AS from_stop_id,
            a.stop_lat  AS from_lat,
            a.stop_lon  AS from_lon,
            b.stop_id   AS to_stop_id,
            b.stop_lat  AS to_lat,
            b.stop_lon  AS to_lon
        FROM stops a
        JOIN stops b
          ON a.stop_id < b.stop_id   -- unordered pairs; no self-pairs, no duplicates
         AND ST_DWithin(
                geography(ST_SetSRID(ST_MakePoint(a.stop_lon, a.stop_lat), 4326)),
                geography(ST_SetSRID(ST_MakePoint(b.stop_lon, b.stop_lat), 4326)),
                %s
             )
        ORDER BY a.stop_id, b.stop_id
    """
    params: List = [cfg.radius_m]
    if cfg.limit and cfg.limit > 0:
        sql += " LIMIT %s"
        params.append(cfg.limit)

    with conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor) as cur:
        cur.execute(sql, params)
        rows = cur.fetchall()
    log.info("Found %d candidate stop pairs within %.0fm", len(rows), cfg.radius_m)
    return rows


def fetch_existing_transfer_keys(conn) -> Set[Tuple[str, str]]:
    """Bulk-load existing (from,to) pairs once, instead of a query per direction."""
    with conn.cursor() as cur:
        cur.execute("SELECT from_stop_id, to_stop_id FROM transfers")
        keys = {(row[0], row[1]) for row in cur.fetchall()}
    log.info("Loaded %d existing transfers (will be skipped)", len(keys))
    return keys


def insert_shape(conn, shape_id: str, geojson_str: str, num_points: int, dry_run: bool):
    if dry_run:
        log.debug("[dry-run] would insert shape %s (%d points)", shape_id, num_points)
        return
    with conn.cursor() as cur:
        cur.execute(
            """
            INSERT INTO shapes (shape_id, geom, num_points)
            VALUES (%s, ST_SetSRID(ST_GeomFromGeoJSON(%s), 4326), %s)
            ON CONFLICT (shape_id) DO NOTHING
            """,
            (shape_id, geojson_str, num_points),
        )


def insert_transfer(conn, from_stop_id: str, to_stop_id: str, min_transfer_time: int,
                     shape_id: str, dry_run: bool):
    if dry_run:
        log.debug("[dry-run] would insert transfer %s -> %s (%ds, shape=%s)",
                   from_stop_id, to_stop_id, min_transfer_time, shape_id)
        return
    with conn.cursor() as cur:
        cur.execute(
            """
            INSERT INTO transfers (from_stop_id, to_stop_id, min_transfer_time, shape_id)
            VALUES (%s, %s, %s, %s)
            ON CONFLICT (from_stop_id, to_stop_id) DO NOTHING
            """,
            (from_stop_id, to_stop_id, min_transfer_time, shape_id),
        )


# --------------------------------------------------------------------------
# OSRM
# --------------------------------------------------------------------------

class OsrmError(Exception):
    pass


def osrm_foot_route(session: requests.Session, cfg: Config, direction: dict) -> dict:
    """
    Routes a single direction. Returns a result dict (always), never raises --
    errors are captured in the 'error' key so the thread pool never dies on
    a single bad pair.
    """
    from_stop_id = direction["from_stop_id"]
    to_stop_id = direction["to_stop_id"]
    url = f"{cfg.osrm_base_url}{direction['from_lon']},{direction['from_lat']};{direction['to_lon']},{direction['to_lat']}"
    params = {"overview": "full", "geometries": "geojson"}

    attempt = 0
    last_exc: Optional[Exception] = None

    while attempt <= cfg.max_retries:
        attempt += 1
        try:
            resp = session.get(url, params=params, timeout=cfg.request_timeout_s)

            if resp.status_code == 429:
                wait = cfg.retry_backoff_s * attempt
                time.sleep(wait)
                continue

            resp.raise_for_status()
            data = resp.json()

            if data.get("code") != "Ok":
                raise OsrmError(f"code={data.get('code')}: {data.get('message')}")

            route = data["routes"][0]
            if cfg.request_delay_s:
                time.sleep(cfg.request_delay_s)
            return {
                "from_stop_id": from_stop_id,
                "to_stop_id": to_stop_id,
                "geometry": route["geometry"],
                "duration": route["duration"],
                "error": None,
            }

        except (requests.RequestException, OsrmError, KeyError, IndexError, ValueError) as exc:
            last_exc = exc
            time.sleep(cfg.retry_backoff_s * attempt)

    return {
        "from_stop_id": from_stop_id,
        "to_stop_id": to_stop_id,
        "geometry": None,
        "duration": None,
        "error": str(last_exc),
    }


def persist_result(conn, cfg: Config, result: dict) -> str:
    """
    Writes one OSRM result to the DB.

    Returns one of:
        "ok"        - shape + transfer were (or would be, in --dry-run) written
        "too_long"  - walking duration exceeded cfg.max_transfer_seconds; not written
        "failed"    - OSRM error or a degenerate/unusable route; not written
    """
    if result["error"] is not None:
        log.error("OSRM failed for %s -> %s: %s", result["from_stop_id"], result["to_stop_id"], result["error"])
        return "failed"

    duration = result["duration"]
    if duration is not None and duration > cfg.max_transfer_seconds:
        log.info(
            "Skipping %s -> %s: walking duration %.0fs exceeds max_transfer_seconds=%.0fs",
            result["from_stop_id"], result["to_stop_id"], duration, cfg.max_transfer_seconds,
        )
        return "too_long"

    coords = result["geometry"].get("coordinates", [])
    num_points = len(coords)
    if num_points < 2:
        log.warning("Degenerate route %s -> %s (num_points=%d); skipping",
                    result["from_stop_id"], result["to_stop_id"], num_points)
        return "failed"

    shape_id = f"walk_{result['from_stop_id']}_{result['to_stop_id']}"
    geojson_str = json.dumps(result["geometry"])

    insert_shape(conn, shape_id, geojson_str, num_points, cfg.dry_run)
    insert_transfer(conn, result["from_stop_id"], result["to_stop_id"],
                     int(round(result["duration"])), shape_id, cfg.dry_run)
    return "ok"


# --------------------------------------------------------------------------
# Core pipeline
# --------------------------------------------------------------------------

def build_direction_list(pairs: List[dict], existing: Set[Tuple[str, str]]) -> List[dict]:
    directions = []
    for row in pairs:
        fwd = (row["from_stop_id"], row["to_stop_id"])
        bwd = (row["to_stop_id"], row["from_stop_id"])
        if fwd not in existing:
            directions.append({
                "from_stop_id": row["from_stop_id"], "from_lat": row["from_lat"], "from_lon": row["from_lon"],
                "to_stop_id": row["to_stop_id"], "to_lat": row["to_lat"], "to_lon": row["to_lon"],
            })
        if bwd not in existing:
            directions.append({
                "from_stop_id": row["to_stop_id"], "from_lat": row["to_lat"], "from_lon": row["to_lon"],
                "to_stop_id": row["from_stop_id"], "to_lat": row["from_lat"], "to_lon": row["from_lon"],
            })
    return directions


def run(cfg: Config):
    conn = get_connection(cfg.db)
    session = build_session(cfg)

    ensure_schema(conn, cfg)

    try:
        pairs = fetch_candidate_pairs(conn, cfg)
        existing = fetch_existing_transfer_keys(conn)
    except Exception:
        log.exception("Failed to fetch candidate pairs / existing transfers")
        conn.close()
        sys.exit(1)

    directions = build_direction_list(pairs, existing)
    total = len(directions)
    log.info("%d directions to route (%d already done, skipped)",
              total, len(pairs) * 2 - total)

    if total == 0:
        log.info("Nothing to do.")
        conn.close()
        return

    processed = 0
    succeeded = 0
    too_long = 0
    failed = 0

    try:
        with ThreadPoolExecutor(max_workers=cfg.concurrency) as pool:
            futures = {pool.submit(osrm_foot_route, session, cfg, d): d for d in directions}

            for future in as_completed(futures):
                result = future.result()
                try:
                    status = persist_result(conn, cfg, result)
                except Exception:
                    log.exception("DB error persisting %s -> %s",
                                  result["from_stop_id"], result["to_stop_id"])
                    conn.rollback()
                    status = "failed"

                processed += 1
                if status == "ok":
                    succeeded += 1
                elif status == "too_long":
                    too_long += 1
                else:
                    failed += 1

                if processed % cfg.commit_every == 0:
                    conn.commit()
                    log.info("Progress: %d/%d (%d ok, %d too long, %d failed)",
                              processed, total, succeeded, too_long, failed)

        conn.commit()

    except KeyboardInterrupt:
        log.warning("Interrupted; committing partial progress...")
        conn.commit()
        conn.close()
        sys.exit(1)

    log.info("Done. %d processed, %d succeeded, %d skipped (>%.0fs walk), %d failed.",
              processed, succeeded, too_long, cfg.max_transfer_seconds, failed)
    conn.close()
    session.close()


# --------------------------------------------------------------------------
# CLI
# --------------------------------------------------------------------------

def parse_args() -> Config:
    p = argparse.ArgumentParser(description="Generate walking transfers between nearby NxtBus stops via OSRM.")
    p.add_argument("--osrm-url", default="http://localhost:5001/route/v1/foot/", help="OSRM foot routing base URL")
    p.add_argument("--radius", type=float, default=1000.0, help="Search radius in meters (default 1000)")
    p.add_argument("--concurrency", type=int, default=8, help="Parallel OSRM requests (default 8; safe for local OSRM)")
    p.add_argument("--delay", type=float, default=0.0, help="Per-request delay (s); use ~1.0 if pointing at a shared/public OSRM server")
    p.add_argument("--timeout", type=float, default=15.0, help="OSRM request timeout (s)")
    p.add_argument("--max-retries", type=int, default=4, help="Max retries per OSRM request")
    p.add_argument("--backoff", type=float, default=1.5, help="Retry backoff base (s)")
    p.add_argument("--commit-every", type=int, default=200, help="Commit every N directions")
    p.add_argument("--max-transfer-seconds", type=float, default=1200.0,
                    help="Discard footpaths whose OSRM walking duration exceeds this many seconds (default 1200 = 20 min)")
    p.add_argument("--limit", type=int, default=0, help="Limit number of candidate pairs (0 = no limit; for testing)")
    p.add_argument("--dry-run", action="store_true", help="Don't write to DB, just log actions")
    p.add_argument("--skip-schema-setup", action="store_true", help="Don't auto-create the transfers unique constraint / spatial index")
    p.add_argument("-v", "--verbose", action="store_true", help="Verbose (DEBUG) logging")

    # DB overrides (optional; .env is used by default)
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
        radius_m=args.radius,
        osrm_base_url=osrm_url,
        concurrency=max(args.concurrency, 1),
        request_delay_s=args.delay,
        request_timeout_s=args.timeout,
        max_retries=args.max_retries,
        retry_backoff_s=args.backoff,
        commit_every=args.commit_every,
        limit=args.limit,
        dry_run=args.dry_run,
        skip_schema_setup=args.skip_schema_setup,
        max_transfer_seconds=args.max_transfer_seconds,
    )


if __name__ == "__main__":
    cfg = parse_args()
    run(cfg)