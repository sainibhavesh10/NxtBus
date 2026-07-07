-- ============================================================
-- EXTENSIONS
-- ============================================================
CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS pg_trgm;


-- ============================================================
-- AGENCY
-- ============================================================
CREATE TABLE IF NOT EXISTS gtfs_agency (
    agency_id        TEXT PRIMARY KEY,
    agency_name      TEXT NOT NULL,
    agency_url       TEXT,
    agency_timezone  TEXT NOT NULL,
    agency_lang      TEXT,
    agency_phone     TEXT,
    agency_fare_url  TEXT
);


-- ============================================================
-- CALENDAR (weekly recurring service pattern)
-- ============================================================
CREATE TABLE IF NOT EXISTS gtfs_calendar (
    service_id  TEXT PRIMARY KEY,
    agency_id   TEXT NOT NULL,
    start_date  DATE NOT NULL,
    end_date    DATE NOT NULL,
    monday      BOOLEAN NOT NULL,
    tuesday     BOOLEAN NOT NULL,
    wednesday   BOOLEAN NOT NULL,
    thursday    BOOLEAN NOT NULL,
    friday      BOOLEAN NOT NULL,
    saturday    BOOLEAN NOT NULL,
    sunday      BOOLEAN NOT NULL,

    CONSTRAINT fk_gtfs_calendar_agency
            FOREIGN KEY (agency_id)
            REFERENCES gtfs_agency (agency_id),

    CONSTRAINT chk_calendar_dates CHECK (end_date >= start_date)
);

CREATE INDEX IF NOT EXISTS idx_gtfs_calendar_agency_id
ON gtfs_calendar (agency_id);


-- ============================================================
-- CALENDAR_DATES (service-level exceptions: holidays, special runs)
-- No FK on service_id — GTFS allows a service_id to exist ONLY here.
-- ============================================================
CREATE TABLE IF NOT EXISTS gtfs_calendar_dates (
    service_id      TEXT NOT NULL,
    date            DATE NOT NULL,
    exception_type  SMALLINT NOT NULL CHECK (exception_type IN (1, 2)), -- 1=added, 2=removed

    PRIMARY KEY (service_id, date)
);

CREATE INDEX IF NOT EXISTS idx_gtfs_calendar_dates_service
ON gtfs_calendar_dates (service_id);


-- ============================================================
-- ROUTES
-- ============================================================
CREATE TABLE IF NOT EXISTS gtfs_routes (
    route_id          TEXT PRIMARY KEY,
    agency_id         TEXT NOT NULL,
    route_short_name  TEXT,
    route_long_name   TEXT,
    route_type        INTEGER NOT NULL,
    route_color       TEXT,
    route_text_color  TEXT,

    CONSTRAINT fk_gtfs_routes_agency
        FOREIGN KEY (agency_id)
        REFERENCES gtfs_agency (agency_id),

    CONSTRAINT chk_route_name
        CHECK (route_short_name IS NOT NULL OR route_long_name IS NOT NULL),

    CONSTRAINT chk_route_type
        CHECK (route_type BETWEEN 0 AND 12)
);

CREATE INDEX IF NOT EXISTS idx_gtfs_routes_agency_id
ON gtfs_routes (agency_id);


-- ============================================================
-- STOPS
-- ============================================================
CREATE TABLE IF NOT EXISTS gtfs_stops (
    stop_id    TEXT PRIMARY KEY,
    stop_code  TEXT,
    stop_name  TEXT NOT NULL,
    stop_lat   DOUBLE PRECISION NOT NULL CHECK (stop_lat BETWEEN -90 AND 90),
    stop_lon   DOUBLE PRECISION NOT NULL CHECK (stop_lon BETWEEN -180 AND 180),
    zone_id    TEXT,

    geom geometry(Point, 4326)
        GENERATED ALWAYS AS (
            ST_SetSRID(ST_MakePoint(stop_lon, stop_lat), 4326)
        ) STORED
);

CREATE INDEX IF NOT EXISTS idx_gtfs_stops_geom
ON gtfs_stops USING GIST (geom);

CREATE INDEX IF NOT EXISTS idx_gtfs_stops_name_trgm
ON gtfs_stops USING GIN (stop_name gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_gtfs_stops_code
ON gtfs_stops (stop_code);


-- ============================================================
-- TRIPS
-- ============================================================
CREATE TABLE IF NOT EXISTS gtfs_trips (
    trip_id        TEXT PRIMARY KEY,
    route_id       TEXT NOT NULL,
    service_id     TEXT NOT NULL,
    shape_id       TEXT,
    trip_headsign  TEXT,
    direction_id   SMALLINT CHECK (direction_id IN (0, 1)),
    block_id       TEXT,

    CONSTRAINT fk_gtfs_trips_route
        FOREIGN KEY (route_id)
        REFERENCES gtfs_routes (route_id),

    CONSTRAINT fk_gtfs_trips_calendar
        FOREIGN KEY (service_id)
        REFERENCES gtfs_calendar (service_id)
);

CREATE INDEX IF NOT EXISTS idx_gtfs_trips_route_id
ON gtfs_trips (route_id);

CREATE INDEX IF NOT EXISTS idx_gtfs_trips_service_id
ON gtfs_trips (service_id);

CREATE INDEX IF NOT EXISTS idx_gtfs_trips_block_id
ON gtfs_trips (block_id);


-- ============================================================
-- STOP_TIMES
-- arrival_time / departure_time stored as INTEGER seconds-past-midnight
-- ============================================================
CREATE TABLE IF NOT EXISTS gtfs_stop_times (
    trip_id         TEXT NOT NULL,
    stop_sequence   INTEGER NOT NULL,
    stop_id         TEXT NOT NULL,
    arrival_time    INTEGER NOT NULL CHECK (arrival_time >= 0),
    departure_time  INTEGER NOT NULL CHECK (departure_time >= 0),

    PRIMARY KEY (trip_id, stop_sequence),

    CONSTRAINT fk_gtfs_stop_times_trip
        FOREIGN KEY (trip_id)
        REFERENCES gtfs_trips (trip_id),

    CONSTRAINT fk_gtfs_stop_times_stop
        FOREIGN KEY (stop_id)
        REFERENCES gtfs_stops (stop_id),

    CONSTRAINT chk_departure_after_arrival
        CHECK (departure_time >= arrival_time)
);

CREATE INDEX IF NOT EXISTS idx_gtfs_stop_times_stop_departure
ON gtfs_stop_times (stop_id, departure_time)
INCLUDE (trip_id, arrival_time);

CREATE INDEX IF NOT EXISTS idx_gtfs_stop_times_trip_id
ON gtfs_stop_times (trip_id);


-- ============================================================
-- TRIP-LEVEL CALENDAR EXCEPTIONS
-- Overrides gtfs_calendar_dates for one specific trip.
-- exception_type: 1 = added (runs), 2 = removed (cancelled)
-- ============================================================
CREATE TABLE IF NOT EXISTS gtfs_trip_dates (
    trip_id         TEXT NOT NULL REFERENCES gtfs_trips (trip_id),
    date            DATE NOT NULL,
    exception_type  SMALLINT NOT NULL CHECK (exception_type IN (1, 2)),

    PRIMARY KEY (trip_id, date)
);

CREATE INDEX IF NOT EXISTS idx_gtfs_trip_dates_date
ON gtfs_trip_dates (date);


-- ============================================================
-- PRECOMPUTED "IS THIS TRIP RUNNING ON THIS DATE" TABLE
-- Rolling ~30 day window. Your live "next buses" query filters
-- against THIS table — never gtfs_calendar / gtfs_calendar_dates /
-- gtfs_trip_dates directly at request time.
-- ============================================================
CREATE TABLE IF NOT EXISTS trip_run_date (
    trip_id  TEXT NOT NULL REFERENCES gtfs_trips (trip_id),
    date     DATE NOT NULL,
    running  BOOLEAN NOT NULL,

    PRIMARY KEY (trip_id, date)
);

CREATE INDEX IF NOT EXISTS idx_trip_run_date_date_running
ON trip_run_date (date, trip_id)
WHERE running = TRUE;
