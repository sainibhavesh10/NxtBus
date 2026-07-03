CREATE TABLE IF NOT EXISTS gtfs_agency (
    agency_id TEXT PRIMARY KEY,
    agency_name TEXT NOT NULL,
    agency_url TEXT,
    agency_timezone TEXT NOT NULL,
    agency_lang TEXT,
    agency_phone TEXT,
    agency_fare_url TEXT
);

CREATE TABLE IF NOT EXISTS gtfs_calendar (
    service_id TEXT PRIMARY KEY,
    start_date CHAR(8) NOT NULL,
    end_date CHAR(8) NOT NULL,
    monday INTEGER NOT NULL,
    tuesday INTEGER NOT NULL,
    wednesday INTEGER NOT NULL,
    thursday INTEGER NOT NULL,
    friday INTEGER NOT NULL,
    saturday INTEGER NOT NULL,
    sunday INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS gtfs_routes (
    route_id TEXT PRIMARY KEY,
    agency_id TEXT NOT NULL,
    route_long_name TEXT,
    route_short_name TEXT,
    route_type INTEGER NOT NULL,

    CONSTRAINT fk_gtfs_routes_agency
        FOREIGN KEY (agency_id)
        REFERENCES gtfs_agency (agency_id)
);

CREATE TABLE IF NOT EXISTS gtfs_stops (
    stop_id TEXT PRIMARY KEY,
    stop_code TEXT,
    stop_lat DOUBLE PRECISION NOT NULL,
    stop_lon DOUBLE PRECISION NOT NULL,
    stop_name TEXT NOT NULL,
    zone_id TEXT,

    geom geometry(Point, 4326)
        GENERATED ALWAYS AS (
            ST_SetSRID(ST_MakePoint(stop_lon, stop_lat), 4326)
        ) STORED
);

CREATE INDEX IF NOT EXISTS idx_gtfs_stops_geom
ON gtfs_stops
USING GIST (geom);

CREATE INDEX IF NOT EXISTS idx_gtfs_stops_name
ON gtfs_stops (stop_name);

CREATE TABLE IF NOT EXISTS gtfs_trips (
    trip_id TEXT PRIMARY KEY,
    route_id TEXT NOT NULL,
    service_id TEXT NOT NULL,
    shape_id TEXT,

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

CREATE TABLE IF NOT EXISTS gtfs_stop_times (
    trip_id TEXT NOT NULL,
    arrival_time TEXT NOT NULL,
    departure_time TEXT NOT NULL,
    stop_id TEXT NOT NULL,
    stop_sequence INTEGER NOT NULL,

    PRIMARY KEY (trip_id, stop_sequence),

    CONSTRAINT fk_gtfs_stop_times_trip
        FOREIGN KEY (trip_id)
        REFERENCES gtfs_trips (trip_id),

    CONSTRAINT fk_gtfs_stop_times_stop
        FOREIGN KEY (stop_id)
        REFERENCES gtfs_stops (stop_id)
);

CREATE INDEX IF NOT EXISTS idx_gtfs_stop_times_trip_id
ON gtfs_stop_times (trip_id);

CREATE INDEX IF NOT EXISTS idx_gtfs_stop_times_stop_id
ON gtfs_stop_times (stop_id);