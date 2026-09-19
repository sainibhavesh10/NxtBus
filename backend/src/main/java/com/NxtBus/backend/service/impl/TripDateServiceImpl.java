package com.nxtbus.backend.service.impl;

import com.nxtbus.backend.entity.TripDate;
import com.nxtbus.backend.exception.TripDateNotFoundException;
import com.nxtbus.backend.exception.TripNotFoundException;
import com.nxtbus.backend.repository.TripDateRepository;
import com.nxtbus.backend.repository.TripRepository;
import com.nxtbus.backend.service.TripDateService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class TripDateServiceImpl implements TripDateService {

    private final TripDateRepository tripDateRepository;
    private final TripRepository tripRepository;

    public TripDateServiceImpl(TripDateRepository tripDateRepository, TripRepository tripRepository) {
        this.tripDateRepository = tripDateRepository;
        this.tripRepository = tripRepository;
    }

    @Override
    @Transactional
    public TripDate saveTripDate(TripDate tripDate) {
        if (!tripRepository.existsById(tripDate.getTripId())) {
            throw new TripNotFoundException(tripDate.getTripId());
        }
        return tripDateRepository.save(tripDate);
    }

    @Override
    public List<TripDate> getTripDatesByTripId(String tripId) {
        return tripDateRepository.findByTripId(tripId);
    }

    @Override
    public Optional<TripDate> getTripDateByTripIdAndDate(String tripId, LocalDate date) {
        return tripDateRepository.findByTripIdAndDate(tripId, date);
    }

    @Override
    @Transactional
    public void deleteTripDate(TripDate.TripDateId id) {
        if (!tripDateRepository.existsById(id)) {
            throw new TripDateNotFoundException(id);
        }
        tripDateRepository.deleteById(id);
    }
}