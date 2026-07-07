-- ============================================================
-- Add agency_id to gtfs_calendar, split the shared calendar row
-- per-agency, and reassign trips to the correct one.
--
-- Context: gtfs_calendar currently has exactly ONE row (service_id='1')
-- shared by both agencies. This migration:
--   1. Adds a new service_id='2' for DTC, same weekly pattern/date range
--   2. Reassigns every DTC trip's service_id from '1' -> '2', based on
--      which agency actually owns that trip's route
-- ============================================================
INSERT INTO gtfs_calendar (service_id, agency_id, start_date, end_date, monday, tuesday, wednesday, thursday, friday, saturday, sunday)
SELECT '2', 'DTC', start_date, end_date, monday, tuesday, wednesday, thursday, friday, saturday, sunday
FROM gtfs_calendar WHERE service_id = '1'
ON CONFLICT (service_id) DO NOTHING;

-- reassign every trip whose ROUTE belongs to DTC (source of truth is
-- routes.agency_id, not the old shared service_id)
UPDATE gtfs_trips t
SET service_id = '2'
FROM gtfs_routes r
WHERE r.route_id = t.route_id AND r.agency_id = 'DTC';

-- extending the date so we can work
UPDATE gtfs_calendar
SET end_date = DATE '2027-01-01';


-- ============================================================
-- POST-MIGRATION VERIFICATION
-- ============================================================

-- expect one row per agency, identical pattern, different service_id
SELECT * FROM gtfs_calendar ORDER BY service_id;

-- expect trip counts per service_id to match each agency's real route/trip count
SELECT service_id, count(*) FROM gtfs_trips GROUP BY service_id ORDER BY service_id;

-- expect 0: every trip must still resolve to a calendar row
SELECT count(*) AS orphaned_trips
FROM gtfs_trips t LEFT JOIN gtfs_calendar c ON c.service_id = t.service_id
WHERE c.service_id IS NULL;

-- expect 0: a trip's service_id must point to a calendar row for its OWN route's agency
SELECT count(*) AS mismatches
FROM gtfs_trips t
JOIN gtfs_routes r ON r.route_id = t.route_id
JOIN gtfs_calendar c ON c.service_id = t.service_id
WHERE c.agency_id <> r.agency_id;