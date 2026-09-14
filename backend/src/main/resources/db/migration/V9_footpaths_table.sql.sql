-- Static, geography-derived walking connections between stops.
-- Not tied to a service date -- only regenerated when stops or the
-- walkable network around them change, unlike trip_run_date,
-- trip_out_of_path, and rerouted_trips.
--
-- Both directions are stored as separate rows (A->B and B->A), even when
-- duration_seconds happens to be equal both ways, so lookups by origin
-- stop are a plain equality query with no self-join or OR logic needed.

CREATE TABLE footpaths (
    from_stop_id     VARCHAR(64) NOT NULL,
    to_stop_id       VARCHAR(64) NOT NULL,
    duration_seconds INT NOT NULL,
    PRIMARY KEY (from_stop_id, to_stop_id)
);

CREATE INDEX idx_footpaths_from_stop ON footpaths (from_stop_id);