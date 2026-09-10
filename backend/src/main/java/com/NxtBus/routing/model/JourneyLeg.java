package com.nxtbus.routing.model;

public record JourneyLeg(
        LegType type, String routeDisplayName, String tripId,
        String fromStopId, String toStopId, int depTimeSeconds, int arrTimeSeconds
) {
    public enum LegType { RIDE, WALK }

    public static JourneyLeg ride(String routeName, String tripId, String from, String to, int dep, int arr) {
        return new JourneyLeg(LegType.RIDE, routeName, tripId, from, to, dep, arr);
    }
    public static JourneyLeg walk(String from, String to) {
        return new JourneyLeg(LegType.WALK, null, null, from, to, -1, -1);
    }
}