package com.example.backend.infra.scheduler;

import com.example.backend.domain.event.dto.EventWithMetadataDto;
import com.example.backend.domain.event.service.query.EventQueryService;
import com.example.backend.infra.rabbitmq.dto.SendDto;
import com.example.backend.infra.rabbitmq.service.ProducerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
@Service
public class Poller {

    private final EventQueryService eventQueryService;
    private final ProducerService producerService;

    @Scheduled(fixedRate = 60_000)
    public void scanAndPoll() {

        log.info("🤖[POLLING][START] 폴링 시작");

        long start = System.currentTimeMillis();

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threshold = now.plusMinutes(1);

        List<EventWithMetadataDto> targets = eventQueryService.getNextPollingEvents(threshold);

        for (EventWithMetadataDto event : targets) {

            try{
                producerService.sendMessage(SendDto.of(event));
            } catch (Exception e){
                log.error("❌[POLLING] 폴링 중 에러발생 | Message: {}", e.getMessage());
            }

        }

        long end = System.currentTimeMillis();
        double elapsed = (end - start) / 1000.0;

        log.info("🤖[POLLING][END] 폴링 완료 | 걸린시간: {}초", elapsed);
    }

}
