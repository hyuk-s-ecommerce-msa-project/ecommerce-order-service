package com.ecommerce.order_service.repository;

import com.ecommerce.order_service.entity.OrderEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"orderItems"})
    OrderEntity findByOrderId(String orderId);

    @EntityGraph(attributePaths = {"orderItems"})
    List<OrderEntity> findByUserId(String userId);
}
