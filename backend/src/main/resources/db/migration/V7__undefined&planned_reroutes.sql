CREATE TABLE trip_out_of_path (
    trip_id       TEXT NOT NULL,
    service_date  DATE NOT NULL,
    PRIMARY KEY (trip_id, service_date)
);
-- row exists → UNDEFINED (deviation detected, exclude from RAPTOR)
-- no row → not deviated
-- inserted by the deviation detector, deleted the moment GPS confirms rejoin

CREATE TABLE rerouted_trips (
    trip_id        TEXT NOT NULL,
    service_date   DATE NOT NULL,
    stop_id        TEXT NOT NULL,
    stop_sequence  INT NOT NULL,
    arr_time       INT NOT NULL,
    dep_time       INT NOT NULL,
    PRIMARY KEY (trip_id, service_date, stop_sequence)
);

CREATE INDEX idx_trip_out_of_path_date ON trip_out_of_path (service_date, trip_id);
CREATE INDEX idx_rerouted_trips_date ON rerouted_trips (service_date, trip_id, stop_sequence);