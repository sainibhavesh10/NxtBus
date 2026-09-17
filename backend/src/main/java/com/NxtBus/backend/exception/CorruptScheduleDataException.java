package com.nxtbus.backend.exception;

import java.util.Map;

/**
 * Thrown when GTFS/schedule data is internally inconsistent in a way that
 * shouldn't be possible if the data pipeline is healthy — e.g. a trip with
 * no stop times, a trip whose stop count doesn't match its route signature,
 * a dangling foreign key, or any other referential/structural break in the
 * static or real-time schedule data. Distinct from generic INTERNAL_ERROR so
 * these can be alerted on and triaged separately from ordinary code bugs.
 */
public class CorruptScheduleDataException extends NxtBusException {

    private final String resourceType; // e.g. "trip", "route", "shape", "stop_time"
    private final String resourceId;
    private final String reason;

    public CorruptScheduleDataException(String resourceType, String resourceId, String reason) {
        super("Corrupt schedule data for " + resourceType + " " + resourceId + ": " + reason,
                ErrorCode.CORRUPT_SCHEDULE_DATA);
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.reason = reason;
    }

    public String getResourceType() {
        return resourceType;
    }

    public String getResourceId() {
        return resourceId;
    }

    public String getReason() {
        return reason;
    }

    @Override
    public Map<String, Object> getProperties() {
        return Map.of("resourceType", resourceType, "resourceId", resourceId, "reason", reason);
    }
}