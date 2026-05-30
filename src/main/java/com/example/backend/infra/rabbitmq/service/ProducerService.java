package com.example.backend.infra.rabbitmq.service;

import com.example.backend.domain.event.dto.EventWithMetadataDto;
import com.example.backend.infra.rabbitmq.dto.SendDto;


public interface ProducerService {

    /**
     * 메시지를 큐에 전송합니다.
     *
     * @param sendDto
     */
    void sendMessage(SendDto sendDto);
}
