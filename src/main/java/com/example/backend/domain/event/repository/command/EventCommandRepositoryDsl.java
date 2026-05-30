package com.example.backend.domain.event.repository.command;

import java.time.LocalDateTime;
import java.util.List;

public interface EventCommandRepositoryDsl {

    long updateIsActiveFalseByIds(List<Long> eventIds, LocalDateTime now);

}
