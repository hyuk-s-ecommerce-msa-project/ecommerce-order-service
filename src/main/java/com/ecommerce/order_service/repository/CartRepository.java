package com.ecommerce.order_service.repository;

import com.ecommerce.order_service.entity.CartEntity;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface CartRepository extends CrudRepository<CartEntity, Long> {
    CartEntity findByCartId(String cartId);
    List<CartEntity> findByUserId(String userId);
    CartEntity findByUserIdAndProductId(String userId, String productId);

    List<CartEntity> findByUserIdAndCreatedAtAfter(String userId, LocalDateTime createdAt);

    @Modifying
    @Query("delete from CartEntity c where c.createdAt < :createdAt")
    int deleteByCreatedAtBefore(@Param("createdAt") LocalDateTime createdAt);
}
