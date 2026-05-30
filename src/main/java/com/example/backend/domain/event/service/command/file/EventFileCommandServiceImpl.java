package com.example.backend.domain.event.service.command.file;

import com.example.backend.domain.event.converter.EventRequestConverter;
import com.example.backend.domain.event.converter.EventResponseConverter;
import com.example.backend.domain.event.dto.EventRequestDto;
import com.example.backend.domain.event.entity.EventEntity;
import com.example.backend.domain.event.entity.EventMetadataEntity;
import com.example.backend.domain.event.repository.command.EventCommandRepository;
import com.example.backend.domain.event.repository.command.EventMetadataCommandRepository;
import com.example.backend.domain.user.entity.UserEntity;
import com.example.backend.global.error.BusinessException;
import com.example.backend.global.error.ErrorCode;
import com.example.backend.infra.redis.EventCountCache;
import com.example.backend.infra.redis.EventMetadataCache;
import com.example.backend.infra.redis.EventUserEventIdsCache;
import com.example.backend.infra.redis.EventViewCountCache;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static com.example.backend.domain.event.dto.EventResponseDto.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventFileCommandServiceImpl implements EventFileCommandService {

    private final EventCommandRepository eventCommandRepository;
    private final EventMetadataCommandRepository eventMetadataCommandRepository;
    private final EventRequestConverter eventRequestConverter;
    private final EventResponseConverter eventResponseConverter;
    private final EventMetadataCache eventMetadataCache;
    private final EventCountCache eventCountCache;
    private final EventViewCountCache eventViewCountCache;
    private final EventUserEventIdsCache eventUserEventIdsCache;

    /**
     * CSV, TSV 파일 기반 이벤트 생성용 메서드
     * @param userEntity - 유저 엔티티
     * @param eventRequestDto - 이벤트 생성 요청 DTO
     * @return 생성 후 응답 DTO
     */
    @Override
    @Transactional
    public CreatedFileEvent createFileEvent(UserEntity userEntity, EventRequestDto.FileEvent eventRequestDto) {

        EventEntity eventEntity = eventRequestConverter.fileDtoToEntity(eventRequestDto, userEntity);
        EventMetadataEntity eventMetadataEntity = eventRequestConverter.fileDtoToMetadataEntity(eventRequestDto, eventEntity);

        EventEntity event = eventCommandRepository.save(eventEntity);
        eventMetadataCommandRepository.save(eventMetadataEntity);

        log.info("💾[EVENT][SERVICE][CREATE][OK] CSV 파일 이벤트 생성 | eventId: {}", event.getId());

        // 캐시 처리
        eventUserEventIdsCache.evict(userEntity.getUsername());
        eventMetadataCache.evict(event.getId());
        eventCountCache.evict(event.getId());
        eventViewCountCache.evict(event.getId());

        return eventResponseConverter.entityToCreatedFileEvent(event);
    }


    /**
     * FILE 이벤트의 부분 수정 로직
     *
     * @param eventId
     * @param eventRequestDto
     * @return
     */
    @Override
    @Transactional
    public UpdateFileEvent simpleUpdateFileEvent(String username, Long eventId, EventRequestDto.UpdateFileEvent eventRequestDto){

        EventEntity eventEntity = eventCommandRepository.findById(eventId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EVENT_NOT_FOUND));

        // 예외 처리: 생성자 본인이 아닌 경우 예외 발생
        if(!eventEntity.getUser().getUsername().equals(username)) {
            throw new BusinessException(ErrorCode.EVENT_FORBIDDEN);
        }

        eventEntity.updateEventTitle(eventRequestDto.getEventTitle());

        // 캐시 처리
        eventUserEventIdsCache.evict(username);
        eventMetadataCache.evict(eventId);
        eventCountCache.evict(eventId);
        eventViewCountCache.evict(eventId);

        return eventResponseConverter.toUpdateFileEvent(eventId);
    }
}
