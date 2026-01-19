package com.ecommerce.order_service.repository;

import com.ecommerce.order_service.entity.CartEntity;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface CartRepository extends CrudRepository<CartEntity, Long> {
    CartEntity findByCartId(String cartId);
    CartEntity findByUserId(String userId);
}
