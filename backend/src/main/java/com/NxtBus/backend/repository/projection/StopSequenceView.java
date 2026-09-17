package com.nxtbus.backend.repository.projection;

public interface StopSequenceView {
    Integer getStopSequence();
    String getStopId();
    String getStopName();
    String getStopCode();
    Double getLat();
    Double getLon();
}

