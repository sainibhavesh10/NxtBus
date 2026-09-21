package com.nxtbus.backend.request;

import com.nxtbus.backend.dto.RouteDto;

import java.util.List;

public record NewRouteRequest(RouteDto route, List<String> stopIds) {}