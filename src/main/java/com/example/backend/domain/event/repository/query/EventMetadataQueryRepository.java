package com.example.backend.domain.event.repository.query;

import com.example.backend.domain.event.entity.EventMetadataEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventMetadataQueryRepository extends JpaRepository<EventMetadataEntity, Long> {
    EventMetadataEntity findByEventId(Long eventId);
}
