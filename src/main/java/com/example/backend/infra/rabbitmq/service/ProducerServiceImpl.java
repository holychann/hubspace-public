package com.example.backend.infra.rabbitmq.service;

import com.example.backend.domain.event.dto.EventWithMetadataDto;
import com.example.backend.infra.rabbitmq.dto.SendDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProducerServiceImpl implements ProducerService{

    private final RabbitTemplate rabbitTemplate;

    @Value( "${rabbitmq.exchange.name}")
    private String EXCHANGE;
    @Value( "${rabbitmq.routing.key}")
    private String ROUTING_KEY;

    @Override
    public void sendMessage(SendDto sendDto) {
        try {
            rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, sendDto);
        } catch (Exception e) {
            log.error("❌ [RABBITMQ][PRODUCER][SERVICE] 프로듀서 에러 | eventId: {}", sendDto.getEventId());
            log.error(e.getMessage());
        }

    }
}
