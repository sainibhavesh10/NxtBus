package com.nxtbus.backend.service;

import com.nxtbus.backend.entity.Agency;

import java.util.List;
import java.util.Optional;

public interface AgencyService {

    Agency saveAgency(Agency agency);

    List<Agency> getAllAgencies();

    Optional<Agency> getAgencyById(String agencyId);

    void validateAgencyExists(String agencyId);
}