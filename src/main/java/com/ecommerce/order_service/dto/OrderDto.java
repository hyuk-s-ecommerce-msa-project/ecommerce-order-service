package com.ecommerce.order_service.dto;

import com.ecommerce.order_service.entity.enums.OrderStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderDto {
    private Long id;
    private Integer totalAmount;
    private Integer payAmount;
    private String orderId;
    private String userId;
    private Integer usedPoint;
    private OrderStatus orderStatus;
    private String createdAt;
    private List<OrderItemsDto> orderItems;
}
