package com.ecommerce.order_service.service;


import com.ecommerce.order_service.dto.CartDto;

import java.util.List;

public interface CartService {
    CartDto addToCart(CartDto cartDto, String userId);
    void deleteCartItem(String cartId, String userId);
    List<CartDto> getCart(String userId);
}
