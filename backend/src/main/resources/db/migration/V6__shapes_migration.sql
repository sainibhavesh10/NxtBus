-- ============================================================
-- SHAPES
-- Synthesized route geometry: one LineString per unique
-- ordered sequence of stop coordinates. Trips that visit the
-- same stops in the same order share a shape_id (mirrors how
-- real GTFS feeds reuse shape_id across trips on one pattern).
-- ============================================================
CREATE TABLE IF NOT EXISTS shapes (
    shape_id    TEXT PRIMARY KEY,
    geom        geometry(LineString, 4326) NOT NULL,
    num_points  INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_shapes_geom
ON shapes USING GIST (geom);


-- ============================================================
-- STEP 1: one row per trip = ordered stop sequence collapsed
-- into a LineString, with shape_id = md5 hash of the ordered
-- stop_id list (deterministic, and dedups identical patterns)
-- ============================================================
CREATE TEMP TABLE trip_shapes_tmp AS
SELECT
    trip_id,
    md5(string_agg(stop_id, '|' ORDER BY stop_sequence)) AS shape_id,
    ST_MakeLine(geom ORDER BY stop_sequence)              AS geom,
    count(*)                                              AS num_points
FROM (
    SELECT st.trip_id, st.stop_sequence, s.stop_id, s.geom
    FROM stop_times st
    JOIN stops s ON s.stop_id = st.stop_id
) sub
GROUP BY trip_id
HAVING count(*) >= 2;   -- can't make a line out of 1 point


-- ============================================================
-- STEP 2: insert distinct shapes
-- (multiple trips mapping to the same shape_id is the point --
-- it means they share a physical pattern)
-- ============================================================
INSERT INTO shapes (shape_id, geom, num_points)
SELECT DISTINCT ON (shape_id) shape_id, geom, num_points
FROM trip_shapes_tmp
ORDER BY shape_id
ON CONFLICT (shape_id) DO NOTHING;


-- ============================================================
-- STEP 3: point every trip at its shape
-- ============================================================
UPDATE trips t
SET shape_id = ts.shape_id
FROM trip_shapes_tmp ts
WHERE t.trip_id = ts.trip_id;

DROP TABLE trip_shapes_tmp;


-- ============================================================
-- OPTIONAL: enforce referential integrity going forward.
-- Skip / defer this if any trips have < 2 stop_times rows --
-- those are left with shape_id = NULL and would violate the FK.
-- Check first with:
--   SELECT trip_id FROM trips WHERE shape_id IS NULL;
-- ============================================================
-- ALTER TABLE trips
--     ADD CONSTRAINT fk_trips_shape
--     FOREIGN KEY (shape_id) REFERENCES shapes (shape_id);