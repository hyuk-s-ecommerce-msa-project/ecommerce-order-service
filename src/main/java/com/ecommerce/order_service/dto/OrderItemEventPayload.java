package com.ecommerce.order_service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OrderItemEventPayload {

    private Long id;
    private String orderId;

    private String productId;
    private Integer unitPrice;

    private String deliveredKey;
    private Integer stock;
}
