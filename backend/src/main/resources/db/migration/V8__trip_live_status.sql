CREATE TABLE trip_live_status (
    trip_id        TEXT NOT NULL,
    service_date   DATE NOT NULL,
    driver_id      TEXT,
    vehicle_no     TEXT,
    last_seq       INT,
    delay_seconds  INT,
    PRIMARY KEY (trip_id, service_date)
);

CREATE INDEX idx_trip_live_status_date ON trip_live_status (service_date, trip_id);