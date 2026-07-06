package com.nxtbus.backend.repository;

import com.nxtbus.backend.entity.Stop;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StopRepository   extends JpaRepository<Stop, String> {}
