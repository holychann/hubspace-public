package com.example.backend.domain.event.repository.command;

import com.example.backend.domain.event.entity.QEventEntity;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
public class EventCommandRepositoryDslImpl implements EventCommandRepositoryDsl{

    private final JPAQueryFactory queryFactory;

    @Override
    public long updateIsActiveFalseByIds(List<Long> eventIds, LocalDateTime now) {

        QEventEntity event = QEventEntity.eventEntity;

        return queryFactory
                .update(event)
                .set(event.isActive, false)
                .where(
                        event.id.in(eventIds),
                        event.isActive.eq(true),
                        event.activeUntil.loe(now)
                )
                .execute();
    }
}
