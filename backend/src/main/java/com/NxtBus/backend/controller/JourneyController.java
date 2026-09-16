package com.nxtbus.backend.controller;

import com.nxtbus.backend.response.JourneyResponse;
import com.nxtbus.backend.service.JourneyService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/journey")
public class JourneyController {

    private final JourneyService journeyService;

    public JourneyController(JourneyService journeyService){
        this.journeyService = journeyService;
    }

    @GetMapping("/plan/stops")
    public JourneyResponse planByStopIds(@RequestParam String from, @RequestParam String to,
                                @RequestParam int departTimeSeconds) {
        return JourneyResponse.from(journeyService.planJourney(from, to, departTimeSeconds));
    }

    @GetMapping("/plan/coordinates")
    public JourneyResponse planByCoordinates(
            @RequestParam double fromLat,
            @RequestParam double fromLon,
            @RequestParam double toLat,
            @RequestParam double toLon,
            @RequestParam int departTimeSeconds) {

        return JourneyResponse.from(
                journeyService.planJourney(fromLat, fromLon, toLat, toLon, departTimeSeconds));
    }
}
