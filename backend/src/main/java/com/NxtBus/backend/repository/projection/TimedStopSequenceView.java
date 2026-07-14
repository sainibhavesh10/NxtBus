package com.nxtbus.backend.repository.projection;

public interface TimedStopSequenceView {

    Integer getStopSequence();

    String getStopId();

    String getStopName();

    String getStopCode();

    Integer getArrivalTime();

    Integer getDepartureTime();

    Double getLat();

    Double getLon();
}