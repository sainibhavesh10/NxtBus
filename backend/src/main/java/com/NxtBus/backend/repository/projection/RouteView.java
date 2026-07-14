package com.nxtbus.backend.repository.projection;

public interface RouteView {

    String getRouteId();

    String getAgencyId();

    String getRouteShortName();

    String getRouteLongName();

    Integer getRouteType();

    String getRouteColor();

    String getRouteTextColor();
}