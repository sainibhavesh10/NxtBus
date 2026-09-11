package com.nxtbus.backend.dto.realtime;

import java.time.Instant;
import java.util.List;

/**
 * The result of stitching three consecutive service days into one
 * midnight-safe timeline (see ContinuousScheduleBuilder).
 *
 * Deliberately has NO serviceDate field. A continuous schedule spans parts
 * of three calendar dates -- yesterday's overnight tail, all of today, and
 * tomorrow's early morning -- so a single LocalDate would misrepresent it.
 *
 * Trip ids here are already prefixed ("#prev_" / "#now_" / "#next_") and
 * stop times already shifted, so this type is safe to feed straight into
 * RaptorIndexBuilder with no further normalization.
 *
 * Kept as a distinct type from EffectiveScheduleSnapshot on purpose: it
 * stops a raw, un-shifted single-day snapshot from being handed to code
 * that assumes midnight-safe, deduplicated trip ids -- that mistake used to
 * compile just fine since both types held a List<TripSchedule>.
 */
public record ContinuousSchedule(List<TripSchedule> trips, Instant materializedAt) {}