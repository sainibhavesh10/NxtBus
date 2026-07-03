package com.nxtbus.backend.health;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class DbTestController {

    private final JdbcTemplate jdbcTemplate;

    public DbTestController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/api/db-test")
    public Map<String, Object> testDatabaseConnection() {

        Long stopCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM gtfs_stops",
                Long.class
        );

        List<Map<String, Object>> sampleStops = jdbcTemplate.queryForList(
                """
                SELECT stop_id, stop_name, stop_lat, stop_lon
                FROM gtfs_stops
                LIMIT 5
                """
        );

        return Map.of(
                "status", "DB connected successfully",
                "totalStops", stopCount,
                "sampleStops", sampleStops
        );
    }
}