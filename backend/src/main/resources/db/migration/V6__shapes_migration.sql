-- ============================================================
-- SHAPES TABLE
-- Stores geometric paths for routes and walking transfers.
-- ============================================================
CREATE TABLE IF NOT EXISTS shapes (
    shape_id    TEXT PRIMARY KEY,
    geom        geometry(LineString, 4326) NOT NULL,
    num_points  INTEGER NOT NULL
);

-- Spatial index for fast geographic queries
CREATE INDEX IF NOT EXISTS idx_shapes_geom
ON shapes USING GIST (geom);

-- ============================================================
-- TRIPS FOREIGN KEY
-- Ensures every shape_id in trips actually exists in the shapes table.
-- ============================================================
ALTER TABLE trips
    ADD CONSTRAINT fk_trips_shape
    FOREIGN KEY (shape_id)
    REFERENCES shapes (shape_id);