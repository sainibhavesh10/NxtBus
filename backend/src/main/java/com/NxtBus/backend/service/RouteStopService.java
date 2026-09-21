package com.nxtbus.backend.service;

import com.nxtbus.backend.dto.RouteDto;
import com.nxtbus.backend.entity.RouteStop;

import java.util.List;

//no delete on this, will only be deleted if route is deleted.
//also no single save
public interface RouteStopService {

    List<RouteStop> saveAllRouteStops(List<RouteStop> routeStops);

    List<RouteStop> getRouteStopsByRouteId(String routeId);

    RouteDto createRouteWithStops(RouteDto routeDto, List<String> stopIds);
}