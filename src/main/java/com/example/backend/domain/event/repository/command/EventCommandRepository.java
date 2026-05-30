package com.example.backend.domain.event.repository.command;

import com.example.backend.domain.event.entity.EventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventCommandRepository extends JpaRepository<EventEntity, Long>, EventCommandRepositoryDsl {

}
