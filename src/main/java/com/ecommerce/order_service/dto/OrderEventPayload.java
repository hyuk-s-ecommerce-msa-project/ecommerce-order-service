package com.ecommerce.order_service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OrderEventPayload {

    private Long id;
    private String orderId;
    private String userId;

    private Integer totalAmount;
    private Integer usedPoint;
    private Integer payAmount;

    private String orderStatus;
    private String createdAt;
}
