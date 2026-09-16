package com.nxtbus.backend.controller;

import com.nxtbus.backend.dto.NearbyStopDto;
import com.nxtbus.backend.dto.StopDto;
import com.nxtbus.backend.request.StopProximityRequest;
import com.nxtbus.backend.request.StopSearchRequest;
import com.nxtbus.backend.service.StopService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stops")
public class StopsController {

    private final StopService stopService;

    @Autowired
    public StopsController(StopService stopService) {
        this.stopService = stopService;
    }

    @GetMapping("/search")
    public List<StopDto> searchStopsByName(@Valid @ModelAttribute StopSearchRequest request) {
        return stopService.searchStopsByName(request);
    }

    @GetMapping("/{stopId}")
    public StopDto getStop(@PathVariable String stopId) {
        return stopService.getStopById(stopId);
    }

    @GetMapping("/nearby")
    public List<NearbyStopDto> getNearbyStops(@Valid @ModelAttribute StopProximityRequest request) {
        return stopService.getNearbyStopsWithDistance(request);
    }

}
