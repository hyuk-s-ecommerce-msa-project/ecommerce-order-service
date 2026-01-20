package com.ecommerce.order_service.repository;

import com.ecommerce.order_service.entity.CartRedisEntity;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface CartRedisRepository extends CrudRepository<CartRedisEntity,String> {
    List<CartRedisEntity> findAllByUserId(String userId);
}
