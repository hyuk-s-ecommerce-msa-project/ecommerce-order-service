package com.ecommerce.order_service.repository;

import com.ecommerce.order_service.entity.OutboxEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxRepository extends JpaRepository<OutboxEntity, Long> {
}
