BEGIN;

WITH ordered_stops AS (
    SELECT
        st.trip_id,
        st.stop_id,
        COALESCE(s.stop_name, st.stop_id) AS stop_name,
        ROW_NUMBER() OVER (
            PARTITION BY st.trip_id
            ORDER BY st.stop_sequence
        ) AS rn,
        COUNT(*) OVER (
            PARTITION BY st.trip_id
        ) AS total_stops
    FROM gtfs_stop_times st
    JOIN gtfs_stops s ON s.stop_id = st.stop_id
),
trip_points AS (
    SELECT
        trip_id,

        MAX(stop_id) FILTER (WHERE rn = 1) AS first_stop_id,
        MAX(stop_name) FILTER (WHERE rn = 1) AS first_stop_name,

        MAX(stop_id) FILTER (WHERE rn = total_stops) AS last_stop_id,
        MAX(stop_name) FILTER (WHERE rn = total_stops) AS last_stop_name,

        MAX(stop_id) FILTER (WHERE rn = GREATEST(total_stops / 2, 1)) AS middle_stop_id,
        MAX(stop_name) FILTER (WHERE rn = GREATEST(total_stops / 2, 1)) AS middle_stop_name,

        MAX(total_stops) AS total_stops
    FROM ordered_stops
    GROUP BY trip_id
),
generated_headsigns AS (
    SELECT
        trip_id,
        CASE
            WHEN first_stop_id = last_stop_id THEN
                first_stop_name || ' to ' || last_stop_name || ' via ' || middle_stop_name
            ELSE
                first_stop_name || ' to ' || last_stop_name
        END AS new_trip_headsign
    FROM trip_points
)
UPDATE gtfs_trips t
SET trip_headsign = g.new_trip_headsign
FROM generated_headsigns g
WHERE t.trip_id = g.trip_id
  AND (
      t.trip_headsign IS NULL
      OR trim(t.trip_headsign) = ''
  );

-- Check result before saving
SELECT trip_id, trip_headsign
FROM gtfs_trips
ORDER BY trip_id
LIMIT 100;

-- If result is good:
COMMIT;

-- If result is bad, run instead of COMMIT:
-- ROLLBACK;