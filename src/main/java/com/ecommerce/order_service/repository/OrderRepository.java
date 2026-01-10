package com.ecommerce.order_service.repository;

import com.ecommerce.order_service.entity.OrderEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.CrudRepository;

public interface OrderRepository extends CrudRepository<OrderEntity, Long> {
    @EntityGraph(attributePaths = {"orderItems"})
    OrderEntity findByOrderId(String orderId);

    @EntityGraph(attributePaths = {"orderItems"})
    Iterable<OrderEntity> findByUserId(String userId);
}
