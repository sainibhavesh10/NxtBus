package com.nxtbus.routing.health;

import com.nxtbus.routing.index.RaptorIndexHolder;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
public class RaptorIndexHealthIndicator implements HealthIndicator {

    private static final Duration STALE_THRESHOLD = Duration.ofHours(26); // daily cadence + buffer

    private final RaptorIndexHolder raptorIndexHolder;

    public RaptorIndexHealthIndicator(RaptorIndexHolder raptorIndexHolder) {
        this.raptorIndexHolder = raptorIndexHolder;
    }

    @Override
    public Health health() {
        Instant lastRebuild = raptorIndexHolder.getLastSuccessfulRebuild();

        if (lastRebuild == null) {
            return Health.down()
                    .withDetail("reason", "No RAPTOR index has ever been built")
                    .build();
        }

        Duration age = Duration.between(lastRebuild, Instant.now());
        if (age.compareTo(STALE_THRESHOLD) > 0) {
            return Health.down()
                    .withDetail("reason", "RAPTOR index is stale")
                    .withDetail("lastSuccessfulRebuild", lastRebuild)
                    .withDetail("ageHours", age.toHours())
                    .build();
        }

        return Health.up()
                .withDetail("lastSuccessfulRebuild", lastRebuild)
                .withDetail("ageHours", age.toHours())
                .build();
    }
}
