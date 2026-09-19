package com.nxtbus.backend.service.impl;

import com.nxtbus.backend.entity.Agency;
import com.nxtbus.backend.exception.AgencyNotFoundException;
import com.nxtbus.backend.repository.AgencyRepository;
import com.nxtbus.backend.service.AgencyService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class AgencyServiceImpl implements AgencyService {

    private final AgencyRepository agencyRepository;

    public AgencyServiceImpl(AgencyRepository agencyRepository) {
        this.agencyRepository = agencyRepository;
    }

    @Override
    @Transactional
    public Agency saveAgency(Agency agency) {
        return agencyRepository.save(agency);
    }

    @Override
    public List<Agency> getAllAgencies() {
        return agencyRepository.findAll();
    }

    @Override
    public Optional<Agency> getAgencyById(String agencyId) {
        return agencyRepository.findById(agencyId);
    }

    @Override
    public void validateAgencyExists(String agencyId) {
        if(!agencyRepository.existsById(agencyId)){
            throw new AgencyNotFoundException(agencyId);
        }
    }
}