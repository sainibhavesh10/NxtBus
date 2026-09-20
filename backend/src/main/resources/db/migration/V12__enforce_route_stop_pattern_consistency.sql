-- trips.route_id immutability, plus a dedicated, immutable route_stop
-- table that defines each route's canonical stop pattern.
--
-- Ordering requirement going forward: a route's full stop pattern must be
-- inserted into route_stop BEFORE any trip on that route gets stop_times.
-- route_stop is authoritative and trips can only ever conform to it, never
-- define it — a stop_times row for a (route, seq) that route_stop doesn't
-- already have is rejected outright, not auto-established.
--
-- Why route_stop as its own table rather than comparing trips against
-- each other directly, or storing the pattern as an array column:
--   - stop_id gets a real FOREIGN KEY to stops(stop_id). An array column
--     cannot be FK-checked element-by-element in Postgres, so this closes
--     a gap that design would have left open.
--   - No dependency on picking some arbitrary "canonical" trip whose
--     identity could shift if that trip is ever deleted.
--   - The stop_times check is pure read-only validation against a fixed
--     table — no deferred/commit-time trigger needed, no concurrent-
--     insert race to worry about.

-- ============================================================
-- 1. trips.route_id immutability
-- ============================================================
CREATE OR REPLACE FUNCTION prevent_trip_route_change()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.route_id IS DISTINCT FROM NEW.route_id THEN
        RAISE EXCEPTION 'trips.route_id is immutable (trip %)', OLD.trip_id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_prevent_trip_route_change
    BEFORE UPDATE OF route_id ON trips
    FOR EACH ROW
    EXECUTE FUNCTION prevent_trip_route_change();

-- ============================================================
-- 2. route_stop: the canonical, immutable stop pattern per route
-- ============================================================
CREATE TABLE route_stop (
    route_id TEXT    NOT NULL REFERENCES routes(route_id) ON DELETE CASCADE,
    stop_seq INTEGER NOT NULL,
    stop_id  TEXT    NOT NULL REFERENCES stops(stop_id),
    PRIMARY KEY (route_id, stop_seq)
);

-- Once a (route_id, stop_seq) -> stop_id mapping is set, it can never be
-- changed. Rows can still be inserted (to define a seq that hasn't been
-- set yet, e.g. the initial population of a new route); only UPDATE is
-- blocked.
CREATE OR REPLACE FUNCTION prevent_route_stop_change()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'route_stop rows are immutable (route %, seq %)', OLD.route_id, OLD.stop_seq;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_prevent_route_stop_update
    BEFORE UPDATE ON route_stop
    FOR EACH ROW
    EXECUTE FUNCTION prevent_route_stop_change();

-- ============================================================
-- 3. Backfill — seed route_stop for EXISTING routes/trips from one
--    representative trip per route (the trip with the smallest
--    trip_id). This is a one-time bootstrap only: it exists because
--    these routes' patterns currently live implicitly in their trips'
--    stop_times, with nothing in route_stop yet. Going forward, a new
--    route's pattern must be inserted into route_stop directly (e.g.
--    during GTFS import) before any trip references it — this backfill
--    is not the ongoing population mechanism.
--    Safe to re-run: ON CONFLICT DO NOTHING means it will never try to
--    overwrite a row that's already there (which would fail anyway
--    thanks to the immutability trigger above).
-- ============================================================
INSERT INTO route_stop (route_id, stop_seq, stop_id)
SELECT rep.route_id, st.stop_sequence, st.stop_id
FROM (
    SELECT DISTINCT ON (route_id) route_id, trip_id
    FROM trips
    ORDER BY route_id, trip_id
) rep
JOIN stop_times st ON st.trip_id = rep.trip_id
ON CONFLICT (route_id, stop_seq) DO NOTHING;

-- ============================================================
-- 4. stop_times validation — every stop_times row must match an
--    ALREADY-EXISTING route_stop entry at its (route_id, stop_seq).
--    A missing entry is rejected, not auto-established.
-- ============================================================
CREATE OR REPLACE FUNCTION check_stop_time_matches_route_stop()
RETURNS TRIGGER AS $$
DECLARE
    route_id_val     TEXT;
    expected_stop_id TEXT;
BEGIN
    SELECT route_id INTO route_id_val FROM trips WHERE trip_id = NEW.trip_id;

    SELECT stop_id INTO expected_stop_id
    FROM route_stop
    WHERE route_id = route_id_val AND stop_seq = NEW.stop_sequence;

    IF expected_stop_id IS NULL THEN
        RAISE EXCEPTION 'Route % has no stop defined at seq % in route_stop — populate route_stop before adding trips',
            route_id_val, NEW.stop_sequence;
    ELSIF expected_stop_id <> NEW.stop_id THEN
        RAISE EXCEPTION 'Trip % stop_seq %: expected stop % per route %''s pattern, got %',
            NEW.trip_id, NEW.stop_sequence, expected_stop_id, route_id_val, NEW.stop_id;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_check_stop_time_matches_route_stop
    AFTER INSERT OR UPDATE ON stop_times
    FOR EACH ROW
    EXECUTE FUNCTION check_stop_time_matches_route_stop();