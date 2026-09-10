package com.nxtbus.backend.dto.realtime;

public record StopTimeEntry(String stopId, int stopSequence, int arrTimeSeconds, int depTimeSeconds) {}