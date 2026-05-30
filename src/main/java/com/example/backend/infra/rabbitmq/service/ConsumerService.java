package com.example.backend.infra.rabbitmq.service;

import com.example.backend.domain.event.dto.EventWithMetadataDto;
import com.example.backend.infra.rabbitmq.dto.SendDto;

public interface ConsumerService {

    /**
     * 메시지를 수신하여 처리합니다.
     *
     * @param
     */
    void receive(SendDto sendDto);
}
