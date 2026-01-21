package com.ecommerce.order_service.service;

import com.ecommerce.order_service.dto.OrderDto;
import com.ecommerce.order_service.entity.OrderEntity;

import java.util.List;


public interface OrderService {
    OrderDto createOrder(OrderDto orderDto, String userId);
    OrderDto getOrderByOrderId(String orderId, String userId);
    List<OrderDto> getOrdersByUserId(String userId);
    OrderDto cancelOrder(String orderId, String userId);
    OrderDto completePayment(String orderId, String userId);
    OrderDto completeOrder(String orderId, String userId);
}
