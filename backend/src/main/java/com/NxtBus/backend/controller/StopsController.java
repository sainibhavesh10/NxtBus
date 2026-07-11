package com.nxtbus.backend.controller;

import com.nxtbus.backend.dto.NearbyStopDto;
import com.nxtbus.backend.dto.StopDto;
import com.nxtbus.backend.service.StopService;
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
    public List<StopDto> searchStopsByName(
            @RequestParam String query,
            @RequestParam(defaultValue = "3") int limit) {
        return stopService.searchStopsByName(query, limit);
    }

    @GetMapping("/{stopId}")
    public StopDto getStop(@PathVariable String stopId) {
        return stopService.getStopById(stopId);
    }

    @GetMapping("/nearby")
    public List<NearbyStopDto> getNearbyStops(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam(defaultValue = "10") int limit
    ) {
        return stopService.getNearbyStops(lat, lon, limit);
    }

}
