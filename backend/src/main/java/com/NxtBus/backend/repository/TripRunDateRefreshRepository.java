package com.nxtbus.backend.repository;

import com.nxtbus.backend.entity.TripRunDate;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

public interface TripRunDateRefreshRepository extends Repository<TripRunDate, String> {

    @Transactional
    @Procedure(procedureName = "refresh_trip_run_date")
    void refreshAll();

    @Transactional
    @Procedure(procedureName = "refresh_trip_run_date_for_calendar")
    void refreshForCalendar(@Param("p_service_id") String serviceId);

    @Transactional
    @Procedure(procedureName = "refresh_trip_run_date_for_calendar_date")
    void refreshForCalendarDate(@Param("p_service_id") String serviceId,
                                @Param("p_date") LocalDate date);

    @Transactional
    @Procedure(procedureName = "refresh_trip_run_date_for_trip")
    void refreshForTrip(@Param("p_trip_id") String tripId);

    @Transactional
    @Procedure(procedureName = "refresh_trip_run_date_for_trip_date")
    void refreshForTripDate(@Param("p_trip_id") String tripId,
                            @Param("p_date") LocalDate date);

    @Transactional
    @Procedure(procedureName = "roll_trip_run_date_window")
    void rollWindow();
}