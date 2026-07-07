
-- Returns TRUE if a trip runs on a date.
-- Priority: trip exception > service exception > weekly calendar.
CREATE OR REPLACE FUNCTION is_trip_running(p_trip_id TEXT, p_date DATE) RETURNS BOOLEAN AS $$
DECLARE
    v_service_id  TEXT;
    v_trip_exc    SMALLINT;
    v_service_exc SMALLINT;
    v_cal         RECORD;
BEGIN
    -- 1. Trip-level exception wins over everything
    SELECT exception_type INTO v_trip_exc
    FROM gtfs_trip_dates
    WHERE trip_id = p_trip_id AND date = p_date;

    IF v_trip_exc IS NOT NULL THEN
        RETURN v_trip_exc = 1;
    END IF;

    -- 2. Service-level exception
    SELECT service_id INTO v_service_id
    FROM gtfs_trips
    WHERE trip_id = p_trip_id;

    SELECT exception_type INTO v_service_exc
    FROM gtfs_calendar_dates
    WHERE service_id = v_service_id AND date = p_date;

    IF v_service_exc IS NOT NULL THEN
        RETURN v_service_exc = 1;
    END IF;

    -- 3. Weekly recurring pattern
    SELECT * INTO v_cal
    FROM gtfs_calendar
    WHERE service_id = v_service_id;

    IF v_cal IS NULL OR p_date < v_cal.start_date OR p_date > v_cal.end_date THEN
        RETURN FALSE;
    END IF;

    RETURN CASE EXTRACT(ISODOW FROM p_date)
        WHEN 1 THEN v_cal.monday
        WHEN 2 THEN v_cal.tuesday
        WHEN 3 THEN v_cal.wednesday
        WHEN 4 THEN v_cal.thursday
        WHEN 5 THEN v_cal.friday
        WHEN 6 THEN v_cal.saturday
        WHEN 7 THEN v_cal.sunday
    END;
END;
$$ LANGUAGE plpgsql STABLE;


-- =====================================================================
-- trip_run_date maintenance procedures
-- Rolling window: CURRENT_DATE - 1 .. CURRENT_DATE + 29 (31 days)
--
-- Converted from FUNCTION -> PROCEDURE (called via CALL, not SELECT),
-- so JDBC's PreparedStatement.executeUpdate() (jdbcTemplate.update(),
-- @Modifying) works without the "A result was returned when none was
-- expected" error you get calling a void FUNCTION via SELECT.
--
-- is_trip_running(...) is assumed to always return TRUE/FALSE (never
-- NULL), so its result is used directly without a COALESCE guard.
-- =====================================================================


-- Shared date-window helper. Single source of truth for the window
-- size -- change it here once instead of in five places.
CREATE OR REPLACE FUNCTION run_date_window()
RETURNS TABLE(gen_date date)
LANGUAGE sql
STABLE
AS $$
    SELECT generate_series(
        CURRENT_DATE - 1,
        CURRENT_DATE + 29,
        INTERVAL '1 day'
    )::date;
$$;


-- Full refresh for all trips for the 31-day window.
-- Use after big GTFS/calendar changes.
CREATE OR REPLACE PROCEDURE refresh_trip_run_date()
LANGUAGE plpgsql
AS $$
BEGIN
    DELETE FROM trip_run_date
    WHERE date < CURRENT_DATE - 1 OR date > CURRENT_DATE + 29;

    INSERT INTO trip_run_date (trip_id, date, running)
    SELECT
        t.trip_id,
        d.gen_date,
        is_trip_running(t.trip_id, d.gen_date)
    FROM gtfs_trips t
    CROSS JOIN run_date_window() d
    ON CONFLICT (trip_id, date)
    DO UPDATE SET running = EXCLUDED.running;
END;
$$;


-- Refresh all trips of one service_id for the full 31-day window.
-- Use after changing gtfs_calendar weekly pattern/start_date/end_date.
CREATE OR REPLACE PROCEDURE refresh_trip_run_date_for_calendar(p_service_id TEXT)
LANGUAGE plpgsql
AS $$
BEGIN
    INSERT INTO trip_run_date (trip_id, date, running)
    SELECT
        t.trip_id,
        d.gen_date,
        is_trip_running(t.trip_id, d.gen_date)
    FROM gtfs_trips t
    CROSS JOIN run_date_window() d
    WHERE t.service_id = p_service_id
    ON CONFLICT (trip_id, date)
    DO UPDATE SET running = EXCLUDED.running;
END;
$$;


-- Refresh all trips of one service_id for one date.
-- Use after changing gtfs_calendar_dates for a service/date.
CREATE OR REPLACE PROCEDURE refresh_trip_run_date_for_calendar_date(
    p_service_id TEXT,
    p_date DATE
)
LANGUAGE plpgsql
AS $$
BEGIN
    IF p_date < CURRENT_DATE - 1 OR p_date > CURRENT_DATE + 29 THEN
        RETURN;
    END IF;

    INSERT INTO trip_run_date (trip_id, date, running)
    SELECT
        t.trip_id,
        p_date,
        is_trip_running(t.trip_id, p_date)
    FROM gtfs_trips t
    WHERE t.service_id = p_service_id
    ON CONFLICT (trip_id, date)
    DO UPDATE SET running = EXCLUDED.running;
END;
$$;


-- Refresh one trip across the 31-day window.
-- Use after adding a new trip or changing a trip's service_id.
CREATE OR REPLACE PROCEDURE refresh_trip_run_date_for_trip(p_trip_id TEXT)
LANGUAGE plpgsql
AS $$
BEGIN
    DELETE FROM trip_run_date
    WHERE trip_id = p_trip_id;

    INSERT INTO trip_run_date (trip_id, date, running)
    SELECT
        t.trip_id,
        d.gen_date,
        is_trip_running(t.trip_id, d.gen_date)
    FROM gtfs_trips t
    CROSS JOIN run_date_window() d
    WHERE t.trip_id = p_trip_id;
END;
$$;


-- Refresh one trip on one date.
-- Use after changing gtfs_trip_dates for a trip/date.
CREATE OR REPLACE PROCEDURE refresh_trip_run_date_for_trip_date(
    p_trip_id TEXT,
    p_date DATE
)
LANGUAGE plpgsql
AS $$
BEGIN
    IF p_date < CURRENT_DATE - 1 OR p_date > CURRENT_DATE + 29 THEN
        RETURN;
    END IF;

    INSERT INTO trip_run_date (trip_id, date, running)
    SELECT
        t.trip_id,
        p_date,
        is_trip_running(t.trip_id, p_date)
    FROM gtfs_trips t
    WHERE t.trip_id = p_trip_id
    ON CONFLICT (trip_id, date)
    DO UPDATE SET running = EXCLUDED.running;
END;
$$;


-- Cheap nightly roll: drop the day that fell off the back of the
-- window, add the new day at the far edge. Schedule this daily
-- instead of running the full refresh_trip_run_date() every day --
-- reserve the full refresh for actual big-change events.
CREATE OR REPLACE PROCEDURE roll_trip_run_date_window()
LANGUAGE plpgsql
AS $$
BEGIN
    DELETE FROM trip_run_date
    WHERE date < CURRENT_DATE - 1;

    INSERT INTO trip_run_date (trip_id, date, running)
    SELECT
        t.trip_id,
        CURRENT_DATE + 29,
        is_trip_running(t.trip_id, CURRENT_DATE + 29)
    FROM gtfs_trips t
    ON CONFLICT (trip_id, date)
    DO UPDATE SET running = EXCLUDED.running;
END;
$$;
-- refreshing the whole TripRunDates
CALL refresh_trip_run_date();