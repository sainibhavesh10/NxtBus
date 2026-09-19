-- Enforces that a trip's route and its service (calendar) always belong to
-- the same agency. Two parts:
--
--   1. A check at trip write-time (insert, or update of route_id/service_id)
--      that rejects a trip whose route and service point at different
--      agencies. This is the DB-level backstop for anything that writes to
--      trips outside the app's own validation in TripServiceImpl.saveTrip
--      (bulk GTFS imports, manual fixes, etc.) — the app-level check should
--      already catch this for normal API traffic before it reaches here.
--
--   2. Two immutability guards that make route/calendar agency_id
--      write-once. Without these, part 1's guarantee could still be
--      violated indirectly: a trip could be created consistent, then
--      drift out of consistency later if its route (or calendar) got
--      reassigned to a different agency afterwards. Locking agency_id
--      once it's set removes that path entirely, so there's no "later"
--      to re-validate against.
--
-- Net effect: a trip's route and service can never disagree on agency,
-- either at creation or at any point after.

-- ============================================================
-- 1. Trip-level consistency check (fires on INSERT, or on UPDATE
--    that changes route_id or service_id)
-- ============================================================
CREATE OR REPLACE FUNCTION check_trip_agency_consistency()
RETURNS TRIGGER AS $$
DECLARE
    route_agency    TEXT;
    calendar_agency TEXT;
BEGIN
    -- Both lookups are PK-indexed (route_id, service_id), so this is
    -- effectively two cheap point lookups per row, not a scan.
    SELECT agency_id INTO route_agency FROM routes WHERE route_id = NEW.route_id;
    SELECT agency_id INTO calendar_agency FROM calendar WHERE service_id = NEW.service_id;

    IF route_agency IS DISTINCT FROM calendar_agency THEN
        RAISE EXCEPTION 'Trip %: route % (agency %) and service % (agency %) belong to different agencies',
            NEW.trip_id, NEW.route_id, route_agency, NEW.service_id, calendar_agency;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_check_trip_agency_consistency
    BEFORE INSERT OR UPDATE OF route_id, service_id ON trips
    FOR EACH ROW
    EXECUTE FUNCTION check_trip_agency_consistency();

-- ============================================================
-- 2. Immutability guards — agency_id can be set once and never
--    changed again, on both routes and calendar. Each only fires
--    when agency_id itself is touched; every other column update
--    (route_short_name, monday, etc.) is untouched by this.
-- ============================================================
CREATE OR REPLACE FUNCTION prevent_route_agency_change()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.agency_id IS DISTINCT FROM NEW.agency_id THEN
        RAISE EXCEPTION 'routes.agency_id is immutable (route %)', OLD.route_id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_prevent_route_agency_change
    BEFORE UPDATE OF agency_id ON routes
    FOR EACH ROW
    EXECUTE FUNCTION prevent_route_agency_change();

CREATE OR REPLACE FUNCTION prevent_calendar_agency_change()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.agency_id IS DISTINCT FROM NEW.agency_id THEN
        RAISE EXCEPTION 'calendar.agency_id is immutable (service %)', OLD.service_id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_prevent_calendar_agency_change
    BEFORE UPDATE OF agency_id ON calendar
    FOR EACH ROW
    EXECUTE FUNCTION prevent_calendar_agency_change();