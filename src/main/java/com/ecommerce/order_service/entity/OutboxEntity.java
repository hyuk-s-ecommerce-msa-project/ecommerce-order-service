package com.ecommerce.order_service.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "outbox")
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class OutboxEntity {
    @Id
    private Long id;

    @Column(name = "aggregateid")
    private String aggregateId;
    @Column(name = "aggregatetype")
    private String aggregateType;
    @Column(name = "type")
    private String eventType;

    @Column(columnDefinition = "json", nullable = false)
    private String payload;

    @CreatedDate
    private LocalDateTime createdAt;

    public static OutboxEntity create(Long id, String aggregateId, String aggregateType, String eventType, String jsonPayload) {
        OutboxEntity outboxEntity = new OutboxEntity();
        outboxEntity.id = id;
        outboxEntity.aggregateId = aggregateId;
        outboxEntity.aggregateType = aggregateType;
        outboxEntity.eventType = eventType;
        outboxEntity.payload = jsonPayload;
        outboxEntity.createdAt = LocalDateTime.now();
        return outboxEntity;
    }
}
