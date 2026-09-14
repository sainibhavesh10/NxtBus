-- (1min 4sec runtime) Merge stops that are exact duplicates (distance = 0, i.e. identical stop_lat/stop_lon)
-- and repoint stop_times to the surviving stop.
--
-- Why GROUP BY lat/lon instead of ST_Distance(geom_a, geom_b) = 0:
-- geom is GENERATED ALWAYS AS ST_SetSRID(ST_MakePoint(stop_lon, stop_lat), 4326),
-- so two points have zero distance iff their (stop_lat, stop_lon) pair is identical.
-- Grouping avoids an O(n^2) self-join over idx_stops_geom for the same result.
--
-- Canonical stop per duplicate group = MIN(stop_id). No other ranking logic.

-- ============================================================
-- 0. PREVIEW — run this first and eyeball it. If any group has
--    differing stop_name values, that pair might be two real,
--    distinct stops (e.g. separate gates/platforms) that just
--    happen to share a coordinate — don't blindly merge those.
-- ============================================================
SELECT
    stop_lat,
    stop_lon,
    array_agg(stop_id ORDER BY stop_id)   AS stop_ids,
    array_agg(DISTINCT stop_name)         AS distinct_names,
    count(*)                              AS group_size
FROM stops
GROUP BY stop_lat, stop_lon
HAVING count(*) > 1
ORDER BY group_size DESC;

-- ============================================================
-- 1. THE MERGE — wrapped in a transaction, safe to roll back.
-- ============================================================
BEGIN;

-- old_stop_id -> canonical_stop_id, for every stop that isn't
-- its own group's minimum
CREATE TEMP TABLE tmp_stop_merge AS
SELECT
    stop_id AS old_stop_id,
    MIN(stop_id) OVER (PARTITION BY stop_lat, stop_lon) AS canonical_stop_id
FROM stops;

DELETE FROM tmp_stop_merge WHERE old_stop_id = canonical_stop_id;

-- 2. Backfill any attribute the canonical stop is missing from
--    the duplicate that's about to be dropped
UPDATE stops canon
SET stop_code = COALESCE(canon.stop_code, dup.stop_code),
    zone_id   = COALESCE(canon.zone_id, dup.zone_id)
FROM tmp_stop_merge m
JOIN stops dup ON dup.stop_id = m.old_stop_id
WHERE canon.stop_id = m.canonical_stop_id;

-- 3. Repoint stop_times to the surviving stop_id.
--    Must happen BEFORE the delete — fk_stop_times_stop points at stops.stop_id.
--    Safe with respect to the stop_times PK (trip_id, stop_sequence): stop_id
--    isn't part of that key, so no conflicts even if a trip revisits the
--    merged stop at a different sequence.
UPDATE stop_times st
SET stop_id = m.canonical_stop_id
FROM tmp_stop_merge m
WHERE st.stop_id = m.old_stop_id;

-- 4. Drop the now-orphaned duplicate stop rows
DELETE FROM stops s
USING tmp_stop_merge m
WHERE s.stop_id = m.old_stop_id;

DROP TABLE tmp_stop_merge;

COMMIT;

-- ============================================================
-- 3. OPTIONAL FOLLOW-UP — only add this if you're confident
--    exact-coordinate duplicates should never legitimately
--    exist in your GTFS feed (i.e. you checked step 0 and it's
--    always the same physical stop, never separate platforms).
-- ============================================================
-- CREATE UNIQUE INDEX IF NOT EXISTS uq_stops_lat_lon ON stops (stop_lat, stop_lon);

-- Results of removing the stops with same lat,lon,name: Stops before:- 10,559 stops now:- 6880
-- Distance(in meters)                      300         500         750         1000
-- Before(stops within the distance)        38,538      76,711      1,41,057    2,24,373
-- After(stops within the distance)         14,826      30,411      56,618      90,626
-- I'll choose