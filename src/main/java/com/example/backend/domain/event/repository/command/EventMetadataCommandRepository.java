package com.example.backend.domain.event.repository.command;

import com.example.backend.domain.event.entity.EventMetadataEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventMetadataCommandRepository extends JpaRepository<EventMetadataEntity, Long> {
    EventMetadataEntity findByEventId(Long eventId);
    void deleteByEventId(Long eventId);
}
