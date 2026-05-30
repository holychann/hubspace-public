package com.example.backend.domain.event.repository.query;

import com.example.backend.domain.event.dto.EventWithMetadataDto;
import com.example.backend.domain.event.entity.QEventEntity;
import com.example.backend.domain.event.entity.QEventMetadataEntity;
import com.example.backend.domain.user.entity.UserEntity;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class EventQueryRepositoryDslImpl implements EventQueryRepositoryDsl {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<EventWithMetadataDto> findByUserIdAndIsActive(String username, Boolean isActive) {

        QEventMetadataEntity metadata = QEventMetadataEntity.eventMetadataEntity;
        QEventEntity event = QEventEntity.eventEntity;

        return queryFactory
                .select(Projections.constructor(
                        EventWithMetadataDto.class,
                        event.id,
                        event.user.username,
                        event.eventTitle,
                        event.eventType,
                        event.isActive,
                        event.createdAt,
                        event.updatedAt,
                        event.nextPollingAt,
                        event.lastPollingAt,
                        event.lastResponseTime,

                        metadata.id,
                        metadata.count,
                        metadata.viewCount,
                        metadata.searchColumns,
                        metadata.searchColumnsIds,
                        metadata.formId,
                        metadata.formUrl
                ))
                .from(metadata)
                .join(metadata.event, event)
                .where(event.user.username.eq(username).and(event.isActive.eq(isActive)))
                .orderBy(event.createdAt.desc())
                .fetch();
    }

    @Override
    public EventWithMetadataDto findByUserIdAndEventIdAndIsActive(String username, Long eventId, Boolean isActive) {

        QEventMetadataEntity metadata = QEventMetadataEntity.eventMetadataEntity;
        QEventEntity event = QEventEntity.eventEntity;

        return queryFactory
                .select(Projections.constructor(
                        EventWithMetadataDto.class,
                        event.id,
                        event.user.username,
                        event.eventTitle,
                        event.eventType,
                        event.isActive,
                        event.createdAt,
                        event.updatedAt,
                        event.nextPollingAt,
                        event.lastPollingAt,
                        event.lastResponseTime,

                        metadata.id,
                        metadata.count,
                        metadata.viewCount,
                        metadata.searchColumns,
                        metadata.searchColumnsIds,
                        metadata.formId,
                        metadata.formUrl
                ))
                .from(metadata)
                .join(metadata.event, event)
                .where(event.user.username.eq(username).and(event.id.eq(eventId).and(event.isActive.eq(isActive))))
                .fetchOne();
    }

    @Override
    public Optional<EventWithMetadataDto> findByEventIdAndIsActive(Long eventId, Boolean isActive) {

        QEventMetadataEntity metadata = QEventMetadataEntity.eventMetadataEntity;
        QEventEntity event = QEventEntity.eventEntity;

        return Optional.ofNullable(queryFactory
                .select(Projections.constructor(
                        EventWithMetadataDto.class,
                        event.id,
                        event.user.username,
                        event.eventTitle,
                        event.eventType,
                        event.isActive,
                        event.createdAt,
                        event.updatedAt,
                        event.nextPollingAt,
                        event.lastPollingAt,
                        event.lastResponseTime,

                        metadata.id,
                        metadata.count,
                        metadata.viewCount,
                        metadata.searchColumns,
                        metadata.searchColumnsIds,
                        metadata.formId,
                        metadata.formUrl
                ))
                .from(metadata)
                .join(metadata.event, event)
                .where(event.id.eq(eventId).and(event.isActive.eq(isActive)))
                .fetchOne()
        );
    }

    @Override
    public List<EventWithMetadataDto> findByNextPollingAtBefore(LocalDateTime threshold) {

        QEventMetadataEntity metadata = QEventMetadataEntity.eventMetadataEntity;
        QEventEntity event = QEventEntity.eventEntity;

        return queryFactory
                .select(Projections.constructor(
                        EventWithMetadataDto.class,
                        event.id,
                        event.user.username,
                        event.eventTitle,
                        event.eventType,
                        event.isActive,
                        event.createdAt,
                        event.updatedAt,
                        event.nextPollingAt,
                        event.lastPollingAt,
                        event.lastResponseTime,

                        metadata.id,
                        metadata.count,
                        metadata.viewCount,
                        metadata.searchColumns,
                        metadata.searchColumnsIds,
                        metadata.formId,
                        metadata.formUrl
                ))
                .from(metadata)
                .join(metadata.event, event)
                .where(event.nextPollingAt.before(threshold).and(event.isActive.eq(true)))
                .fetch();
    }

    @Override
    public List<Long> findInactiveEventIds(LocalDateTime now) {
        QEventEntity event = QEventEntity.eventEntity;
        return queryFactory
                .select(event.id)
                .from(event)
                .where(event.isActive.eq(true).and(event.activeUntil.before(now)))
                .fetch();
    }
}
