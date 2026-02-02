package com.ecommerce.order_service.messagequeue;

import com.ecommerce.order_service.dto.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderProducer {
    private final KafkaTemplate<String, String> kafkaTemplate;

    List<Field> fields = List.of(new Field("string", true, "order_id"),
            new Field("string", true, "user_id"),
            new Field("int32", true, "total_amount"),
            new Field("int32", true, "used_point"),
            new Field("int32", true, "pay_amount"),
            new Field("string", true, "order_status"),
            new Field("string", true, "created_at"));

    Schema schema = Schema.builder()
            .type("struct")
            .fields(fields)
            .optional(false)
            .name("orders")
            .build();

    List<Field> itemFields = List.of(
            new Field("string", true, "order_id"),
            new Field("string", true, "product_id"),
            new Field("int32", true, "unit_price"),
            new Field("string", true, "delivered_key"),
            new Field("int32", true, "stock")
    );

    Schema itemSchema = Schema.builder()
            .type("struct")
            .fields(itemFields)
            .optional(false)
            .name("order_items")
            .build();

    public OrderDto send(String topic, OrderDto orderDto) {
        Payload payload = Payload.builder()
                .orderId(orderDto.getOrderId())
                .userId(orderDto.getUserId())
                .totalAmount(orderDto.getTotalAmount())
                .usedPoint(orderDto.getUsedPoint())
                .payAmount(orderDto.getPayAmount())
                .orderStatus(String.valueOf(orderDto.getOrderStatus()))
                .createdAt(orderDto.getCreatedAt())
                .build();

        KafkaOrderDto kafkaOrderDto = new KafkaOrderDto(schema, payload);

        ObjectMapper objectMapper = new ObjectMapper();

        String jsonInString = "";

        try {
            jsonInString = objectMapper.writeValueAsString(kafkaOrderDto);
        } catch (JsonProcessingException e) {
            log.error("Json Parsing Error : {}", e.getMessage());
        }

        kafkaTemplate.send(topic, jsonInString);

        orderDto.getOrderItems().forEach(orderItem -> {
            OrderItemPayload itemPayload = OrderItemPayload.builder()
                    .orderId(orderDto.getOrderId())
                    .productId(orderItem.getProductId())
                    .unitPrice(orderItem.getUnitPrice())
                    .deliveredKey(orderItem.getDeliveredKey())
                    .stock(orderItem.getStock())
                    .build();

            KafkaOrderItemDto itemKafkaDto = new KafkaOrderItemDto(itemSchema, itemPayload);

            try {
                String itemJson = objectMapper.writeValueAsString(itemKafkaDto);
                kafkaTemplate.send("order_items", itemJson);
            } catch (JsonProcessingException e) {
                log.error("Order Item Json Error : {}", e.getMessage());
            }
        });

        log.info("Order Producer Sent Order Message : {}", kafkaOrderDto);

        return orderDto;
    }
}
