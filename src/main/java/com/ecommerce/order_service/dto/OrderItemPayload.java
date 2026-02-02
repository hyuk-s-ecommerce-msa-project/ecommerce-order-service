package com.ecommerce.order_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderItemPayload {
    @JsonProperty("order_id")
    private String orderId;

    @JsonProperty("product_id")
    private String productId;

    @JsonProperty("unit_price")
    private Integer unitPrice;

    @JsonProperty("delivered_key")
    private String deliveredKey;
    private Integer stock;
}
