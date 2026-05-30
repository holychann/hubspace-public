package com.example.backend.domain.event.service.command.file;

import com.example.backend.domain.event.dto.EventRequestDto;
import com.example.backend.domain.user.entity.UserEntity;

import static com.example.backend.domain.event.dto.EventResponseDto.*;

public interface EventFileCommandService {
    CreatedFileEvent createFileEvent(UserEntity userEntity, EventRequestDto.FileEvent eventRequestDto);
    UpdateFileEvent simpleUpdateFileEvent(String username, Long eventId, EventRequestDto.UpdateFileEvent eventRequestDto);
}
