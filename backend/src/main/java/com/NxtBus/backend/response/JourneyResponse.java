package com.nxtbus.backend.response;

import com.nxtbus.routing.model.Journey;
import com.nxtbus.routing.model.JourneyLeg;

import java.util.List;

public record JourneyResponse(
        int departTimeSeconds,
        int arrivalTimeSeconds,
        int durationSeconds,
        int transferCount,
        List<LegResponse> legs
) {

    public static JourneyResponse from(Journey journey) {
        List<LegResponse> legResponses = journey.legs().stream()
                .map(LegResponse::from)
                .toList();

        long rideLegs = legResponses.stream()
                .filter(l -> l.type() == JourneyLeg.LegType.RIDE)
                .count();

        return new JourneyResponse(
                journey.departTimeSeconds(),
                journey.arrivalTimeSeconds(),
                journey.arrivalTimeSeconds() - journey.departTimeSeconds(),
                (int) Math.max(0, rideLegs - 1),
                legResponses
        );
    }

    public record LegResponse(
            JourneyLeg.LegType type,
            String routeDisplayName,   // null for WALK
            String tripId,             // null for WALK
            String fromStopId,
            String toStopId,
            Integer depTimeSeconds,
            Integer arrTimeSeconds
    ) {
        static LegResponse from(JourneyLeg leg) {
            return new LegResponse(
                    leg.type(),
                    leg.routeDisplayName(),
                    leg.tripId(),
                    leg.fromStopId(),
                    leg.toStopId(),
                    leg.depTimeSeconds(),
                    leg.arrTimeSeconds()
            );
        }
    }
}