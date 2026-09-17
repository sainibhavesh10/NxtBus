package com.nxtbus.backend.controller;

import com.nxtbus.backend.request.JourneyByLocationRequest;
import com.nxtbus.backend.request.JourneyByStopsRequest;
import com.nxtbus.backend.response.JourneyResponse;
import com.nxtbus.backend.service.JourneyService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/journey")
public class JourneyController {

    private final JourneyService journeyService;

    public JourneyController(JourneyService journeyService) {
        this.journeyService = journeyService;
    }

    @GetMapping("/plan/stops")
    public JourneyResponse planByStopIds(@Valid @ModelAttribute JourneyByStopsRequest request) {
        return JourneyResponse.from(journeyService.planJourney(request));
    }

    @GetMapping("/plan/coordinates")
    public JourneyResponse planByCoordinates(@Valid @ModelAttribute JourneyByLocationRequest request) {
        return JourneyResponse.from(journeyService.planJourney(request));
    }
}