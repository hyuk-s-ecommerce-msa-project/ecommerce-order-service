package com.ecommerce.order_service.dto;

import lombok.Data;

import java.util.List;

@Data
public class OrderDto {
    private String productId;
    private Integer totalAmount;
    private Integer payAmount;
    private String orderId;
    private String userId;
    private Integer usedPoint;
    private List<OrderItemsDto> orderItems;
}
