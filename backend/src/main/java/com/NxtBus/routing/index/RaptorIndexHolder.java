package com.nxtbus.routing.index;

import org.springframework.stereotype.Component;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class RaptorIndexHolder {

    private final AtomicReference<RaptorSnapshot> current = new AtomicReference<>();

    public RaptorSnapshot get() { return current.get(); }

    /** Full rebuild always resets exclusions -- materialize() already
     *  re-derives CANCEL/UNDEFINED from the DB fresh. The overlay only
     *  bridges the gap until the next rebuild picks up a live change. */
    public void publishFullRebuild(RaptorIndex index) {
        current.set(new RaptorSnapshot(index, new boolean[index.totalTripCount()]));
    }

    public void excludeTrip(String tripId) { setExcluded(tripId, true); }
    public void includeTrip(String tripId) { setExcluded(tripId, false); }

    private void setExcluded(String tripId, boolean value) {
        RaptorSnapshot snapshot = current.get();
        if (snapshot == null) return;
        Integer pos = snapshot.index().tripPositionOf(tripId);
        if (pos == null) return; // not part of the current index

        boolean[] updated = snapshot.excludedTrips().clone();
        updated[pos] = value;
        current.compareAndSet(snapshot, new RaptorSnapshot(snapshot.index(), updated));
        // A failed CAS means a full rebuild just landed -- safe to drop,
        // since that rebuild already reflects current DB state.
    }
}