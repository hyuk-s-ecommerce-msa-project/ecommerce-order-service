package com.ecommerce.order_service.vo;

import lombok.Data;

@Data
public class RequestOrderItem {
    private String productId;
    private Integer unitPrice;
}
