package com.ecommerce.order_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Payload {
    private Long id;
    @JsonProperty("order_id")
    private String orderId;
    @JsonProperty("user_id")
    private String userId;
    @JsonProperty("total_amount")
    private Integer totalAmount;
    @JsonProperty("used_point")
    private Integer usedPoint;
    @JsonProperty("pay_amount")
    private Integer payAmount;
    @JsonProperty("order_status")
    private String orderStatus;
    @JsonProperty("created_at")
    private String createdAt;
}
