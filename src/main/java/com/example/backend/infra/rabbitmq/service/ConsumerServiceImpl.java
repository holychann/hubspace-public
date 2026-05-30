package com.example.backend.infra.rabbitmq.service;

import com.example.backend.domain.event.service.command.EventCommandService;
import com.example.backend.domain.event.service.query.EventQueryService;
import com.example.backend.domain.response.service.command.ResponseCommandService;
import com.example.backend.global.error.BusinessException;
import com.example.backend.infra.google.drive.GoogleDriveService;
import com.example.backend.infra.google.dto.GoogleFormResponseDto;
import com.example.backend.infra.rabbitmq.dto.SendDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConsumerServiceImpl implements ConsumerService{

    private final GoogleDriveService googleDriveService;
    private final ResponseCommandService responseCommandService;
    private final EventCommandService eventCommandService;

    @Override
    @RabbitListener(queues = "${rabbitmq.queue.name}")
    public void receive(SendDto sendDto) {

        long start = System.currentTimeMillis();
        log.info("🐇 [RABBIT_MQ][RECEIVE][START] 이벤트 응답 동기화 시작 | eventId: {}", sendDto.getEventId());

        try{

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime createdAt = sendDto.getCreatedAt();

            LocalDateTime activeUntil = createdAt
                    .toLocalDate()
                    .plusDays(31)
                    .atStartOfDay();

            Duration interval;
            if (!now.isAfter(createdAt.plusDays(11))) {
                interval = Duration.ofMinutes(1);
            } else if (!now.isAfter(createdAt.plusDays(16))) {
                interval = Duration.ofMinutes(30);
            } else if (now.isBefore(activeUntil)) {
                interval = Duration.ofHours(1);
            } else {
                eventCommandService.updateEventStatus(sendDto.getEventId(), false);
                return;
            }

            String validAccessToken = googleDriveService.getValidAccessToken(sendDto.getUsername());

            // drive에서 form 응답 가져오기
            List<GoogleFormResponseDto> formResponses = googleDriveService.getFormResponses(
                    sendDto.getFormId(), validAccessToken, sendDto.getSearchColumnIds(),
                    sendDto.getLastResponseTime()
            );

            // 저장 할 응답이 없다면 반환
            if(formResponses.isEmpty()){

                eventCommandService.updateNextPollingAt(sendDto.getEventId(), LocalDateTime.now().plus(interval));
                return;
            }


            // 핵심: 응답 저장
            responseCommandService.saveFormResponses(formResponses, sendDto.getEventId());


            // 폴링 메타 데이터 저장
            String createTimeStr = formResponses.get(formResponses.size() - 1).getCreateTime();
            LocalDateTime laseCreateTime = OffsetDateTime.parse(createTimeStr).toLocalDateTime();
            eventCommandService.updatePollingData(sendDto.getEventId(), LocalDateTime.now().plus(interval), laseCreateTime, (long) formResponses.size());


        } catch (BusinessException e){

            log.info("❌ [RABBIT_MQ][RECEIVE][ERROR] 응답 동기화 중 비즈니스 예외 발생. eventId: {} | 에러코드: {} | 메시지: {}", sendDto.getEventId(), e.getErrorCode(), e.getMessage());

            eventCommandService.updateNextPollingAt(sendDto.getEventId(), LocalDateTime.now().plusMinutes(10));

        } catch (IOException e) {

            log.info("❌ [RABBIT_MQ][RECEIVE][ERROR] 응답 동기화 중 비즈니스 예외 발생. eventId: {} | 메시지: {}", sendDto.getEventId(), e.getMessage());

            eventCommandService.updateNextPollingAt(sendDto.getEventId(), LocalDateTime.now().plusMinutes(10));

        } catch (Exception e){

            log.error("❌ [RABBIT_MQ][RECEIVE][ERROR] 응답 동기화 중 예상치 못한 시스템 오류 발생 eventId={} | 메시지: {}",
                    sendDto.getEventId(), e.getMessage(), e);

            eventCommandService.updateEventStatus(sendDto.getEventId(), false);

        }
    }
}
