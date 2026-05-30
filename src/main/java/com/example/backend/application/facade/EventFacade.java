package com.example.backend.application.facade;

import com.example.backend.domain.event.dto.EventDetail;
import com.example.backend.domain.event.dto.EventRequestDto;
import com.example.backend.domain.event.dto.EventResponseDto;
import com.example.backend.domain.response.dto.ResponseSaveDto;
import org.springframework.web.multipart.MultipartFile;

public interface EventFacade {

    EventResponseDto.SearchList getEventList(String username);
    EventDetail getEventDetail(String username, Long eventId);
    EventResponseDto.CreatedFormEvent createFormEvent(String username, EventRequestDto.FormEvent eventRequestDto);
    ResponseSaveDto createFileEvent(String username, EventRequestDto.FileEvent eventRequestDto, MultipartFile file);
}
