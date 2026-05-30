package com.example.backend.domain.event.service.command;

import com.example.backend.domain.event.converter.EventRequestConverter;
import com.example.backend.domain.event.converter.EventResponseConverter;
import com.example.backend.domain.event.dto.EventRequestDto;
import com.example.backend.domain.event.dto.EventResponseDto;
import com.example.backend.domain.event.entity.EventEntity;
import com.example.backend.domain.event.entity.EventMetadataEntity;
import com.example.backend.domain.event.repository.command.EventCommandRepository;
import com.example.backend.domain.event.repository.command.EventMetadataCommandRepository;
import com.example.backend.domain.user.entity.UserEntity;
import com.example.backend.global.error.BusinessException;
import com.example.backend.global.error.ErrorCode;
import com.example.backend.infra.google.dto.GoogleFormCreateResponseDto;
import com.example.backend.infra.redis.EventCountCache;
import com.example.backend.infra.redis.EventMetadataCache;
import com.example.backend.infra.redis.EventUserEventIdsCache;
import com.example.backend.infra.redis.EventViewCountCache;
import com.example.backend.infra.redis.dto.EventMetadataCacheDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventCommandServiceImpl implements EventCommandService{

    private final EventCommandRepository eventCommandRepository;
    private final EventMetadataCommandRepository eventMetadataCommandRepository;
    private final EventRequestConverter eventRequestConverter;
    private final EventResponseConverter eventResponseConverter;
    private final EventMetadataCache eventMetadataCache;
    private final EventCountCache eventCountCache;
    private final EventViewCountCache eventViewCountCache;
    private final EventUserEventIdsCache eventUserEventIdsCache;

    @Override
    @Transactional
    public EventResponseDto.CreatedFormEvent createFormEvent(
            UserEntity userEntity, EventRequestDto.FormEvent eventRequestDto,
            GoogleFormCreateResponseDto googleFormCreateResponseDto
    ) {

        EventEntity eventEntity = eventRequestConverter.formDtoToEntity(eventRequestDto, userEntity);

        EventMetadataEntity eventMetadataEntity = eventRequestConverter.formDtoToMetadataEntity(
                eventRequestDto,
                eventEntity,
                googleFormCreateResponseDto);

        EventEntity event = eventCommandRepository.save(eventEntity);
        eventMetadataCommandRepository.save(eventMetadataEntity);

        log.info("💾[EVENT][SERVICE][CREATE][OK] 구글 폼 이벤트 생성 | eventId: {}, formId: {}", event.getId(), googleFormCreateResponseDto.getFormId());

        // 캐시 삭제
        eventUserEventIdsCache.evict(userEntity.getUsername());
        eventMetadataCache.evict(event.getId());
        eventCountCache.evict(event.getId());
        eventViewCountCache.evict(event.getId());

        return eventResponseConverter.toCreatedFormEventDto(event, googleFormCreateResponseDto);
    }

    /**
     * 다음 검색 시간 업데이트
     * @param eventId : 이벤트 정보
     * @param nextPollingAt : 다음 검색 시간
     */
    @Override
    @Transactional
    public void updatePollingData(Long eventId, LocalDateTime nextPollingAt, LocalDateTime lastResponseTime, Long count) {
        EventEntity eventEntity = eventCommandRepository.findById(eventId).orElseThrow(
                () -> new BusinessException(ErrorCode.EVENT_NOT_FOUND)
        );
        EventMetadataEntity metadata = eventMetadataCommandRepository.findByEventId(eventId);

        // 이벤트 메타데이터가 없을 경우(비정상)
        if(metadata == null) {
            throw new BusinessException(ErrorCode.EVENT_NOT_FOUND);
        }

        long delta = count - metadata.getCount();
        if (delta > 0) {
            metadata.updateCount(metadata.getCount() + delta);
        }
        eventEntity.updateNextPollingAt(nextPollingAt);
        eventEntity.updateLastResponseTime(lastResponseTime);

        log.info("🔄[EVENT][SERVICE][UPDATE][OK] 폴링 관련 데이터 수정 | eventId: {}", eventId);
    }

    /**
     * 이벤트 활성화 상태 업데이트
     * @param eventId : 이벤트 아디
     * @param isActive : 활성화 여부
     */
    @Override
    @Transactional
    public void updateEventStatus(Long eventId, Boolean isActive) {
        EventEntity eventEntity = eventCommandRepository.findById(eventId).orElseThrow(
                () -> new BusinessException(ErrorCode.EVENT_NOT_FOUND)
        );
        eventEntity.updateIsActive(isActive);
        log.info("🔄[EVENT][SERVICE][UPDATE][OK] 이벤트 상태 변경 | eventId: {}, now Status: {}", eventId, isActive);
    }

    /**
     * 다음 검색 시간 업데이트
     * @param eventId : 이벤트 정보
     * @param nextPollingAt : 다음 검색 시간
     */
    @Override
    @Transactional
    public void updateNextPollingAt(Long eventId, LocalDateTime nextPollingAt) {
        EventEntity eventEntity = eventCommandRepository.findById(eventId).orElseThrow(
                () -> new BusinessException(ErrorCode.EVENT_NOT_FOUND)
        );
        eventEntity.updateNextPollingAt(nextPollingAt);
        log.info("🔄[EVENT][SERVICE][UPDATE][OK] 다음 폴링 시간 업데이트 | eventId: {}, nextPolling: {}", eventId, nextPollingAt);
    }

    @Override
    @Transactional
    public void deleteEvent(String username, Long eventId) {
        EventEntity eventEntity = eventCommandRepository.findById(eventId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EVENT_NOT_FOUND));

        // 예외 처리: 같은 유저가 아니면 거부
        if(!eventEntity.getUser().getUsername().equals(username)) {
            throw new BusinessException(ErrorCode.EVENT_FORBIDDEN);
        }

        eventMetadataCommandRepository.deleteByEventId(eventId);
        eventCommandRepository.deleteById(eventId);

        // 캐시 삭제
        eventUserEventIdsCache.evict(username);
        eventMetadataCache.evict(eventId);
        eventCountCache.evict(eventId);
        eventViewCountCache.evict(eventId);
    }

    @Override
    @Transactional
    public EventResponseDto.Update updateFormEvent(
            String username, Long eventId, EventRequestDto.FormEvent eventRequestDto
    ) {

        EventEntity eventEntity = eventCommandRepository.findById(eventId).orElseThrow(
                () -> new BusinessException(ErrorCode.EVENT_NOT_FOUND)
        );

        // 예외 처리: 같은 유저가 아님
        if(!eventEntity.getUser().getUsername().equals(username)) {
            throw new BusinessException(ErrorCode.EVENT_FORBIDDEN);
        }

        eventEntity.updateFormEvent(eventRequestDto);

        // 캐시 처리
        eventUserEventIdsCache.evict(username);
        eventMetadataCache.evict(eventId);
        eventCountCache.evict(eventId);
        eventViewCountCache.evict(eventId);

        return eventResponseConverter.toUpdateSuccessDto(eventEntity);
    }

    @Override
    @Transactional
    public void closeEvent(String username, Long eventId) {
        EventEntity eventEntity = eventCommandRepository.findById(eventId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EVENT_NOT_FOUND));

        // 예외 처리: 같은 유저가 아니면 거부
        if(!eventEntity.getUser().getUsername().equals(username)) {
            throw new BusinessException(ErrorCode.EVENT_FORBIDDEN);
        }

        eventEntity.updateIsActive(false);

        // 캐시 처리
        eventUserEventIdsCache.evict(username);
        eventMetadataCache.evict(eventId);
        eventCountCache.evict(eventId);
        eventViewCountCache.evict(eventId);
    }

    /**
     * ViewCount 를 1 늘리는 메서드
     * @param eventId : 해당 이벤트의 아이디
     */
    @Override
    @Transactional
    public void addViewCount(Long eventId){

        EventMetadataEntity metadata = eventMetadataCommandRepository.findByEventId(eventId);

        metadata.addViewCount();
        eventViewCountCache.put(eventId, metadata.getViewCount());
    }

    /**
     * 만료된 이벤트들 비활성환
     * @param eventIds
     */
    @Override
    @Transactional
    public long expireEventIds(List<Long> eventIds) {
        LocalDateTime now = LocalDateTime.now();
        long updatedCount = eventCommandRepository.updateIsActiveFalseByIds(eventIds, now);

        // 개수가 맞지 않다면 로깅
        if(eventIds.size() != updatedCount) {
            log.error("❗InactiveEvents: eventIds.size() != updatedCount");
        }

        return updatedCount;
    }
}
