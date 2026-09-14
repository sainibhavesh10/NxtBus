package com.nxtbus.backend.service.realtime;
import com.nxtbus.backend.dto.realtime.FootpathEdge;
import com.nxtbus.backend.entity.Footpath;
import com.nxtbus.backend.repository.FootpathRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
/**
 * Loads all footpaths into the Map<String, List<FootpathEdge>> shape
 * RaptorIndexBuilder expects, keyed by the walk's origin stop.
 *
 * This data has no correlated queries or per-row processing to justify raw
 * JDBC (compare StaticScheduleSource, which does) -- it's a plain "read
 * everything, group by origin" read, so a repository + a translation step
 * is enough. It's also not tied to a service date, so this is meant to be
 * called once (at startup, or behind a manually-invalidated cache) rather
 * than re-queried per routing request.
 */
@Component
public class FootpathSource {
    private final FootpathRepository footpathRepository;
    public FootpathSource(FootpathRepository footpathRepository) {
        this.footpathRepository = footpathRepository;
    }
    @Transactional(readOnly = true)
    public Map<String, List<FootpathEdge>> loadAll() {
        Map<String, List<FootpathEdge>> byOrigin = new LinkedHashMap<>();
        List<Footpath> all = footpathRepository.findAll(Sort.by("fromStopId"));
        for (Footpath footpath : all) {
            FootpathEdge edge = new FootpathEdge(footpath.getToStopId(), footpath.getDurationSeconds());
            byOrigin.computeIfAbsent(footpath.getFromStopId(), k -> new ArrayList<>()).add(edge);
        }
        return byOrigin;
    }
}