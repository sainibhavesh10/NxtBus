package com.nxtbus.backend.repository;

import com.nxtbus.backend.entity.Shape;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShapeRepository extends JpaRepository<Shape, String> {
}