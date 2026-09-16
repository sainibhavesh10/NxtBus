package com.nxtbus.routing.index;

import com.nxtbus.backend.dto.realtime.ContinuousSchedule;
import com.nxtbus.backend.dto.realtime.FootpathEdge;
import com.nxtbus.backend.service.realtime.ContinuousScheduleBuilder;
import com.nxtbus.backend.service.realtime.FootpathSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Rebuilds the RAPTOR index from scratch and publishes it to
 * {@link RaptorIndexHolder}. Runs once at startup, and again on a fixed
 * daily cadence to roll the 3-day continuous window forward.
 */
@Component
public class RaptorIndexRefreshService {

    private static final Logger log = LoggerFactory.getLogger(RaptorIndexRefreshService.class);

    private final ContinuousScheduleBuilder continuousScheduleBuilder;
    private final FootpathSource footpathSource;
    private final RaptorIndexBuilder raptorIndexBuilder;
    private final RaptorIndexHolder raptorIndexHolder;

    public RaptorIndexRefreshService(
            ContinuousScheduleBuilder continuousScheduleBuilder,
            FootpathSource footpathSource,
            RaptorIndexBuilder raptorIndexBuilder,
            RaptorIndexHolder raptorIndexHolder) {
        this.continuousScheduleBuilder = continuousScheduleBuilder;
        this.footpathSource = footpathSource;
        this.raptorIndexBuilder = raptorIndexBuilder;
        this.raptorIndexHolder = raptorIndexHolder;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        rebuild();
    }

    /** Rolls the 3-day continuous window forward. Runs just after midnight
     *  so "today" picks up the new service date before real traffic starts. */
    @Scheduled(cron = "0 5 0 * * *")
    public void scheduledRebuild() {
        rebuild();
    }

    public void rebuild() {
        LocalDate serviceDate = LocalDate.now();
        log.info("Rebuilding RAPTOR index for service date {}", serviceDate);

        ContinuousSchedule schedule = continuousScheduleBuilder.buildContinuousSchedule(serviceDate);
        Map<String, List<FootpathEdge>> footpaths = footpathSource.loadAll();

        RaptorIndex index = raptorIndexBuilder.build(schedule, footpaths);
        raptorIndexHolder.publishFullRebuild(index);

        log.info("RAPTOR index published: {} stops, {} routes, {} trips",
                index.numStops(), index.numRoutes(), index.totalTripCount());
    }
}