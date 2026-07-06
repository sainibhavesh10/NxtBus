-- ============================================================
-- NxtBus — GTFS data load (Flyway-compatible version)
--
-- FIX: the previous version used `\copy`, which is a psql CLIENT
-- meta-command — it is not SQL. It only exists inside the interactive
-- psql program, which parses it and does the file read itself before
-- ever talking to the server. Flyway (like any JDBC-based tool) just
-- sends this file's text as literal SQL statements over the wire —
-- it doesn't know what `\copy` means, so Postgres's parser saw a bare
-- `\` and rejected it: "syntax error at or near \".
--
-- The fix is `COPY` (no backslash) — a real SQL command that runs
-- entirely on the SERVER side. The server process itself opens the
-- file, which means the path below must be valid *inside the postgres
-- container*, not on your host machine or wherever Flyway runs from.
--
-- >>> VERIFY THIS PATH BEFORE RUNNING <<<
-- Earlier your docker-compose.yml had:
--     volumes:
--       - ./data/gtfs:/gtfs:ro
-- so the container-visible path is /gtfs/agency.txt, /gtfs/routes.txt,
-- etc. — NOT /data/gtfs/... as the previous script assumed. If you've
-- since changed the compose file, update every FROM path below to match
-- whatever's actually on the right-hand side of that volume mapping.
-- Quick check:  docker exec -it nxtbus_postgres ls /gtfs
--
-- Also note: server-side COPY requires the connecting role to be able
-- to read files as the OS postgres user — either superuser, or a
-- member of pg_read_server_files. The default POSTGRES_USER created by
-- the official postgres/postgis image is bootstrapped as a superuser,
-- so this normally just works. If you get "permission denied" instead
-- of a path error, run once as a superuser:
--     GRANT pg_read_server_files TO nxtbus_user;
--
-- Tested end-to-end (schema + this exact transform logic) against the
-- real Delhi files: agency(2), calendar(1), routes(2403), stops(10559),
-- trips(89393) all load cleanly with the column lists below — the CSV
-- header order does NOT match the table's column order for calendar.txt
-- and stops.txt, and routes.txt/trips.txt have fewer columns than the
-- schema (the rest are nullable), so every COPY uses an explicit column
-- list rather than relying on default table order.
-- ============================================================

-- ---------- AGENCY ----------
COPY gtfs_agency (agency_id, agency_name, agency_url, agency_timezone, agency_lang, agency_phone, agency_fare_url)
FROM '/gtfs/agency.txt' WITH (FORMAT csv, HEADER true, NULL '');

-- ---------- CALENDAR ----------
-- calendar.txt has service_id as the LAST column, not the first.
-- Postgres parses '20240101' as a DATE and '1'/'0' as BOOLEAN natively —
-- no casting or staging needed.
-- it bcz agency type is not null so we assigned a value initially
CREATE TEMP TABLE staging_calendar (
    start_date DATE,
    end_date DATE,
    monday BOOLEAN,
    tuesday BOOLEAN,
    wednesday BOOLEAN,
    thursday BOOLEAN,
    friday BOOLEAN,
    saturday BOOLEAN,
    sunday BOOLEAN,
    service_id TEXT
);

COPY staging_calendar (start_date, end_date, monday, tuesday, wednesday, thursday, friday, saturday, sunday, service_id)
FROM '/gtfs/calendar.txt' WITH (FORMAT csv, HEADER true, NULL '');

INSERT INTO gtfs_calendar (service_id,agency_id,start_date,end_date,monday,tuesday,wednesday,thursday,friday,saturday,sunday)
SELECT
    service_id,
    'DIMTS',
    start_date,
    end_date,
    monday,
    tuesday,
    wednesday,
    thursday,
    friday,
    saturday,
    sunday
FROM staging_calendar;

-- ---------- ROUTES ----------
-- routes.txt only has 5 of the 7 schema columns (no route_color/route_text_color) — they load as NULL.
COPY gtfs_routes (agency_id, route_id, route_long_name, route_short_name, route_type)
FROM '/gtfs/routes.txt' WITH (FORMAT csv, HEADER true, NULL '');

-- ---------- STOPS ----------
-- stops.txt column order differs from the table; geom is a GENERATED column
-- so it must NOT appear in the copy list (Postgres computes it from lat/lon).
COPY gtfs_stops (stop_code, stop_id, stop_lat, stop_lon, stop_name, zone_id)
FROM '/gtfs/stops.txt' WITH (FORMAT csv, HEADER true, NULL '');

-- ---------- TRIPS ----------
-- trips.txt only has 4 of the 7 schema columns (no trip_headsign/direction_id/block_id) — NULL.
COPY gtfs_trips (route_id, service_id, trip_id, shape_id)
FROM '/gtfs/trips.txt' WITH (FORMAT csv, HEADER true, NULL '');


-- ============================================================
-- STOP_TIMES
-- GTFS times can exceed 24:00:00 (a trip starting the previous
-- service-day, e.g. 25:10:00 = 1:10 AM next day). Postgres's TIME type
-- rejects hours > 23, so we can't COPY straight into it, and COPY has
-- no inline expression support to compute seconds during load either —
-- load raw text into a staging table, then convert with split_part,
-- which is just string splitting + arithmetic and has no hour ceiling.
-- ============================================================

CREATE TABLE IF NOT EXISTS stg_stop_times (
    trip_id        TEXT,
    arrival_time   TEXT,
    departure_time TEXT,
    stop_id        TEXT,
    stop_sequence  INTEGER
);
TRUNCATE stg_stop_times;

COPY stg_stop_times FROM '/gtfs/stop_times.txt' WITH (FORMAT csv, HEADER true, NULL '');

-- Sanity check before transforming: any row that didn't parse into all
-- 5 columns will show up here as a NULL trip_id (COPY itself would have
-- already errored on a genuinely short/long row, but this catches blank
-- or malformed fields that still matched the column count).
DO $$
DECLARE bad_rows INTEGER;
BEGIN
    SELECT count(*) INTO bad_rows FROM stg_stop_times WHERE trip_id IS NULL OR arrival_time IS NULL OR stop_id IS NULL;
    IF bad_rows > 0 THEN
        RAISE WARNING '% row(s) in stg_stop_times have a NULL trip_id/arrival_time/stop_id — inspect before trusting the transform.', bad_rows;
    END IF;
END $$;

INSERT INTO gtfs_stop_times (trip_id, stop_sequence, stop_id, arrival_time, departure_time)
SELECT
    trip_id,
    stop_sequence,
    stop_id,
    split_part(arrival_time, ':', 1)::int * 3600 + split_part(arrival_time, ':', 2)::int * 60 + split_part(arrival_time, ':', 3)::int,
    split_part(departure_time, ':', 1)::int * 3600 + split_part(departure_time, ':', 2)::int * 60 + split_part(departure_time, ':', 3)::int
FROM stg_stop_times;

TRUNCATE stg_stop_times;  -- drop the raw text copy now that it's transformed into gtfs_stop_times


-- ============================================================
-- POST-LOAD VERIFICATION
-- ============================================================
SELECT 'gtfs_agency'      AS table_name, count(*) FROM gtfs_agency
UNION ALL SELECT 'gtfs_calendar',   count(*) FROM gtfs_calendar
UNION ALL SELECT 'gtfs_routes',     count(*) FROM gtfs_routes
UNION ALL SELECT 'gtfs_stops',      count(*) FROM gtfs_stops
UNION ALL SELECT 'gtfs_trips',      count(*) FROM gtfs_trips
UNION ALL SELECT 'gtfs_stop_times', count(*) FROM gtfs_stop_times;

-- Should return 0 rows — any orphaned trip references in stop_times
SELECT st.trip_id, st.stop_sequence FROM gtfs_stop_times st
LEFT JOIN gtfs_trips t ON t.trip_id = st.trip_id
WHERE t.trip_id IS NULL
LIMIT 20;
