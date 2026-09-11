package com.nxtbus.backend.service.realtime;

import com.nxtbus.backend.dto.realtime.ContinuousSchedule;
import com.nxtbus.backend.dto.realtime.EffectiveScheduleSnapshot;
import com.nxtbus.backend.dto.realtime.StopTimeEntry;
import com.nxtbus.backend.dto.realtime.TripSchedule;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Stitches three consecutive service-day snapshots into one continuous
 * timeline centered on {@code serviceDate}, so downstream code never has to
 * reason about midnight wraparound.
 *
 * Rules:
 *   1. YESTERDAY — drop any trip that doesn't cross into today (i.e. has no
 *      stop time >= 24h). Keep the ones that do, and shift EVERY stop time
 *      on those trips back by 24h, so a stop originally at 25:00 (1am, in
 *      yesterday's raw encoding) becomes 1:00 on today's clock, and a stop
 *      at 23:00 becomes -1:00 (an hour before today's midnight). Prefixed
 *      "#prev_".
 *   2. TODAY — kept in full, untouched times. Prefixed "#now_".
 *   3. TOMORROW — drop any trip that starts at or after noon. Keep the
 *      early-morning ones, and shift EVERY stop time on those trips forward
 *      by 24h, so they land after today's times on the same continuous
 *      clock. Prefixed "#next_".
 *
 * Only tripId is prefixed — routeId is left as-is since it's the display
 * label, not an identity key, and doesn't need disambiguating.
 *
 * Returns a ContinuousSchedule, not an EffectiveScheduleSnapshot -- there's
 * no single LocalDate that correctly describes a window spanning three
 * calendar dates, and using a distinct type also stops a raw, un-shifted
 * single-day snapshot from being handed to RaptorIndexBuilder by mistake.
 *
 * ASSUMPTIONS (adjust here if these don't match the intended semantics):
 *   - "runs till next day" = ANY stop time (arrival OR departure) on the
 *     trip is >= SECONDS_PER_DAY.
 *   - "starts after 12PM" = the departure time of the trip's FIRST stop
 *     (lowest stopSequence) is >= NOON_SECONDS.
 */
@Component
public class ContinuousScheduleBuilder {

    private static final int SECONDS_PER_DAY = 24 * 60 * 60;
    private static final int NOON_SECONDS = 12 * 60 * 60;

    private static final String PREV_PREFIX = "#prev_";
    private static final String NOW_PREFIX = "#now_";
    private static final String NEXT_PREFIX = "#next_";

    private final ScheduleSource scheduleSource;

    public ContinuousScheduleBuilder(ScheduleSource scheduleSource) {
        this.scheduleSource = scheduleSource;
    }

    public ContinuousSchedule buildContinuousSchedule(LocalDate serviceDate) {
        EffectiveScheduleSnapshot yesterday = scheduleSource.loadSnapshot(serviceDate.minusDays(1));
        EffectiveScheduleSnapshot today = scheduleSource.loadSnapshot(serviceDate);
        EffectiveScheduleSnapshot tomorrow = scheduleSource.loadSnapshot(serviceDate.plusDays(1));

        List<TripSchedule> combined = new ArrayList<>();

        yesterday.trips().stream()
                .filter(ContinuousScheduleBuilder::runsPastMidnight)
                .map(trip -> shiftAndPrefix(trip, PREV_PREFIX, -SECONDS_PER_DAY))
                .forEach(combined::add);

        today.trips().stream()
                .map(trip -> shiftAndPrefix(trip, NOW_PREFIX, 0))
                .forEach(combined::add);

        tomorrow.trips().stream()
                .filter(ContinuousScheduleBuilder::startsBeforeNoon)
                .map(trip -> shiftAndPrefix(trip, NEXT_PREFIX, SECONDS_PER_DAY))
                .forEach(combined::add);

        return new ContinuousSchedule(combined, Instant.now());
    }

    private static boolean runsPastMidnight(TripSchedule trip) {
        return trip.stopTimes().stream()
                .anyMatch(st -> st.arrTimeSeconds() >= SECONDS_PER_DAY || st.depTimeSeconds() >= SECONDS_PER_DAY);
    }

    private static boolean startsBeforeNoon(TripSchedule trip) {
        return firstStop(trip).depTimeSeconds() < NOON_SECONDS;
    }

    private static StopTimeEntry firstStop(TripSchedule trip) {
        return trip.stopTimes().stream()
                .min(Comparator.comparingInt(StopTimeEntry::stopSequence))
                .orElseThrow(() -> new IllegalStateException(
                        "Trip " + trip.tripId() + " has no stop times"));
    }

    private static TripSchedule shiftAndPrefix(TripSchedule trip, String prefix, int offsetSeconds) {
        List<StopTimeEntry> shiftedStops = trip.stopTimes().stream()
                .map(st -> new StopTimeEntry(
                        st.stopId(),
                        st.stopSequence(),
                        st.arrTimeSeconds() + offsetSeconds,
                        st.depTimeSeconds() + offsetSeconds))
                .toList();

        return new TripSchedule(prefix + trip.tripId(), trip.routeId(), shiftedStops);
    }
}