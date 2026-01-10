package com.ecommerce.order_service.vo;

import lombok.Data;

import java.util.List;

@Data
public class RequestOrder {
    private Integer usedPoint;
    private List<RequestOrderItem> orderItems;
}
