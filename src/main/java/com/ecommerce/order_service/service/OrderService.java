package com.ecommerce.order_service.service;

import com.ecommerce.order_service.dto.OrderDto;
import com.ecommerce.order_service.entity.OrderEntity;


public interface OrderService {
    OrderDto createOrder(OrderDto orderDto);
    OrderDto getOrderByOrderId(String orderId);
    Iterable<OrderEntity> getOrdersByUserId(String userId);
}
