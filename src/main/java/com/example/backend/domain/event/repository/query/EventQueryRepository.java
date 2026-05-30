package com.example.backend.domain.event.repository.query;

import com.example.backend.domain.event.entity.EventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EventQueryRepository extends JpaRepository<EventEntity, Long>, EventQueryRepositoryDsl {
    boolean existsById(Long id);
    Optional<EventEntity> findById(Long id);
}
