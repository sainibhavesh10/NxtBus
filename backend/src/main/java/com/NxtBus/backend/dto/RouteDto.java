package com.nxtbus.backend.dto;

import com.nxtbus.backend.entity.Route;
import com.nxtbus.backend.repository.projection.RouteView;

public record RouteDto(
        String routeId,
        String agencyId,
        String routeShortName,
        String routeLongName,
        Integer routeType,
        String routeColor,
        String routeTextColor
) {

    public static RouteDto from(Route route) {
        return new RouteDto(
                route.getRouteId(),
                route.getAgencyId(),
                route.getRouteShortName(),
                route.getRouteLongName(),
                route.getRouteType(),
                route.getRouteColor(),
                route.getRouteTextColor()
        );
    }

    public static RouteDto from(RouteView route) {
        return new RouteDto(
                route.getRouteId(),
                route.getAgencyId(),
                route.getRouteShortName(),
                route.getRouteLongName(),
                route.getRouteType(),
                route.getRouteColor(),
                route.getRouteTextColor()
        );
    }
}