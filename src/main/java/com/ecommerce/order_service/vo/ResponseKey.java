package com.ecommerce.order_service.vo;

import lombok.Data;

@Data
public class ResponseKey {
    private String productId;
    private String orderId;
    private String userId;
    private String gameKey;
}
