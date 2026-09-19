package com.nxtbus.backend.exception;

import java.util.Map;

public class AgencyNotFoundException extends NxtBusException {

    private final String agencyId;

    public AgencyNotFoundException(String agencyId) {
        super("Agency not found: " + agencyId, ErrorCode.AGENCY_NOT_FOUND);
        this.agencyId = agencyId;
    }

    public String getAgencyId() {
        return agencyId;
    }

    @Override
    public Map<String, Object> getProperties() {
        return Map.of("agencyId", agencyId);
    }
}