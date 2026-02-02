package com.ecommerce.order_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
public class KafkaOrderItemDto implements Serializable {
    private Schema schema;
    private OrderItemPayload payload;
}
