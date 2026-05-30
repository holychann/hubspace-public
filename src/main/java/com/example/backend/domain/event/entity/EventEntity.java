package com.example.backend.domain.event.entity;

import com.example.backend.domain.event.dto.EventRequestDto;
import com.example.backend.domain.user.entity.UserEntity;
import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "events")
public class EventEntity {

    @Id
    @Tsid
    private Long id;
    
    @JoinColumn(name = "user_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private UserEntity user;

    @Column(name = "event_title", nullable = false)
    private String eventTitle;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private EventType eventType;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "active_until")
    private LocalDateTime activeUntil;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "next_polling_at")
    private LocalDateTime nextPollingAt;

    @Column(name = "last_polling_at")
    private LocalDateTime lastPollingAt;

    @Column(name = "last_response_time")
    private LocalDateTime lastResponseTime;


    public void updateFormEvent(EventRequestDto.FormEvent dto) {
        if(dto.getEventTitle() != null) this.eventTitle = dto.getEventTitle();
    }

    public void updateEventTitle(String eventTitle){ this.eventTitle = eventTitle; }

    public void updateLastResponseTime(LocalDateTime lastResponseTime){
        this.lastResponseTime = lastResponseTime;
    }

    public void updateNextPollingAt(LocalDateTime nextPollingAt){
        this.nextPollingAt = nextPollingAt;
    }

    public void updateIsActive(Boolean isActive){
        this.isActive = isActive;
    }
}
