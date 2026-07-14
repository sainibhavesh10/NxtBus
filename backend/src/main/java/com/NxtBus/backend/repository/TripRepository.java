package com.nxtbus.backend.repository;

import com.nxtbus.backend.entity.Trip;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TripRepository   extends JpaRepository<Trip, String> {}

