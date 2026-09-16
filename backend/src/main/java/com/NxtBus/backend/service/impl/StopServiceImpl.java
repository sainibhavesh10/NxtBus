package com.nxtbus.backend.service.impl;

import com.nxtbus.backend.dto.NearbyStopDto;
import com.nxtbus.backend.dto.StopDto;
import com.nxtbus.backend.exception.StopNotFoundException;
import com.nxtbus.backend.repository.StopRepository;
import com.nxtbus.backend.repository.projection.NearbyStopView;
import com.nxtbus.backend.repository.projection.StopView;
import com.nxtbus.backend.request.SearchRequest;
import com.nxtbus.backend.request.StopProximityRequest;
import com.nxtbus.backend.request.SearchRequest;
import com.nxtbus.backend.service.StopService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StopServiceImpl implements StopService {

    private static final int MAX_SEARCH_RESULT_LIMIT = 50;

    private final StopRepository stopRepository;

    public StopServiceImpl(StopRepository stopRepository) {
        this.stopRepository = stopRepository;
    }

    @Override
    public void validateStopExists(String stopId) {
        if (!stopRepository.existsById(stopId)) {
            throw new StopNotFoundException(stopId);
        }
    }

    @Override
    public StopDto getStopById(String stopId) {
        var stop = stopRepository.findById(stopId)
                .orElseThrow(() -> new StopNotFoundException(stopId));
        return StopDto.from(stop);
    }

    @Override
    public List<StopDto> searchStopsByName(SearchRequest request) {
        int safeLimit = Math.min(request.limit(), MAX_SEARCH_RESULT_LIMIT);
        return stopRepository.searchByName(request.query().trim(), safeLimit).stream()
                .map(StopDto::from)
                .toList();
    }

    @Override
    public List<StopDto> getNearestStops(StopProximityRequest request) {
        int safeLimit = Math.max(1, Math.min(request.limit(), MAX_SEARCH_RESULT_LIMIT));
        List<StopView> views = stopRepository.findNearestStops(request.lat(), request.lon(), safeLimit);
        return views.stream()
                .map(StopDto::from)
                .toList();
    }

    @Override
    public List<NearbyStopDto> getNearbyStopsWithDistance(StopProximityRequest request) {
        int safeLimit = Math.min(request.limit(), MAX_SEARCH_RESULT_LIMIT);
        List<NearbyStopView> views = stopRepository.findNearbyStopsWithDistance(
                request.lat(), request.lon(), safeLimit * 4, safeLimit);
        return views.stream()
                .map(NearbyStopDto::from)
                .toList();
    }

}