package com.nxtbus.backend.repository;

import com.nxtbus.backend.entity.Footpath;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FootpathRepository extends JpaRepository<Footpath, Footpath.FootpathId> {

    List<Footpath> findByFromStopId(String fromStopId);
}