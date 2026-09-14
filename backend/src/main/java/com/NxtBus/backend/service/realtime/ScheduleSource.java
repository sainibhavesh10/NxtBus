package com.nxtbus.backend.service.realtime;

import com.nxtbus.backend.dto.realtime.EffectiveScheduleSnapshot;

import java.time.LocalDate;

public interface ScheduleSource {
    EffectiveScheduleSnapshot loadSnapshot(LocalDate serviceDate);
}