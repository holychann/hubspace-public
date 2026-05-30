package com.example.backend.infra.scheduler;

import com.example.backend.domain.event.service.command.EventCommandService;
import com.example.backend.domain.event.service.query.EventQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventExpirationScheduler {

    private final EventQueryService eventQueryService;
    private final EventCommandService eventCommandService;

    private static final long ONE_HOUR = 60 * 60 * 1000L;

    @Scheduled(fixedDelay = ONE_HOUR)
    public void expireEvents() {

        // 만료 이벤트 조회
        List<Long> expiredEventIds = eventQueryService.getInactiveEventIds();

        if (expiredEventIds.isEmpty()) {
            return;
        }

        // 만료 처리
        long expiredCount = eventCommandService.expireEventIds(expiredEventIds);

        log.info("🕒[EVENT][SCHEDULER][EXPIRE] 만료 이벤트 비활성화 완료 | count={}", expiredCount);

    }
}
