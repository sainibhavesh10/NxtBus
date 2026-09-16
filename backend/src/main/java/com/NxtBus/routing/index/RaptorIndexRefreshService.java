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

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

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
        try {
            rebuild();
        } catch (Exception e) {
            log.error("Initial RAPTOR index build FAILED at startup for service date {} — " +
                            "no index is published yet, all routing requests will fail with IndexNotReadyException until the next rebuild",
                    LocalDate.now(), e);
        }
    }

    @Scheduled(cron = "0 5 0 * * *")
    public void scheduledRebuild() {
        try {
            rebuild();
        } catch (Exception e) {
            Instant last = raptorIndexHolder.getLastSuccessfulRebuild();
            log.error("Scheduled RAPTOR index rebuild FAILED for service date {} — " +
                            "serving stale index from last successful build at {}. " +
                            "Routing requests will continue to work but may reflect outdated schedule data.",
                    LocalDate.now(), last, e);
        }
    }

    private void rebuild() {
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