package com.ecommerce.order_service.messagequeue;

import com.ecommerce.order_service.dto.*;
import com.ecommerce.order_service.entity.OutboxEntity;
import com.ecommerce.order_service.repository.OutboxRepository;
import com.ecommerce.snowflake.util.SnowflakeIdGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class OutboxProducer {

    private final OutboxRepository outboxRepository;
    private final SnowflakeIdGenerator idGenerator;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public void sendToOutbox(OrderDto orderDto, String eventType) {
        try {
            List<Field> orderFields = List.of(
                    new Field("int64", false, "id"),
                    new Field("string", false, "order_id"),
                    new Field("string", false, "user_id"),
                    new Field("int32", false, "total_amount"),
                    new Field("int32", false, "used_point"),
                    new Field("int32", false, "pay_amount"),
                    new Field("string", false, "order_status"),
                    new Field("string", false, "created_at")
            );

            Schema orderSchema = Schema.builder()
                    .type("struct")
                    .fields(orderFields)
                    .optional(false)
                    .name("orders")
                    .build();

            Payload orderPayload = Payload.builder()
                    .id(orderDto.getId())
                    .orderId(orderDto.getOrderId())
                    .userId(orderDto.getUserId())
                    .totalAmount(orderDto.getTotalAmount())
                    .usedPoint(orderDto.getUsedPoint())
                    .payAmount(orderDto.getPayAmount())
                    .orderStatus(orderDto.getOrderStatus().name())
                    .createdAt(orderDto.getCreatedAt())
                    .build();

            KafkaOrderDto orderMsg = new KafkaOrderDto(orderSchema, orderPayload);
            String orderJson = objectMapper.writeValueAsString(orderMsg);

            outboxRepository.save(OutboxEntity.create(
                    idGenerator.nextId(),
                    orderDto.getOrderId(),
                    "ORDER",
                    eventType,
                    orderJson
            ));

            List<Field> itemFields = List.of(
                    new Field("int64", false, "id"),
                    new Field("string", false, "order_id"),
                    new Field("string", false, "product_id"),
                    new Field("int32", false, "unit_price"),
                    new Field("string", true, "delivered_key"),
                    new Field("int32", false, "stock")
            );

            Schema itemSchema = Schema.builder()
                    .type("struct")
                    .fields(itemFields)
                    .optional(false)
                    .name("order_items")
                    .build();

            for (OrderItemsDto item : orderDto.getOrderItems()) {
                OrderItemPayload itemPayload = OrderItemPayload.builder()
                        .id(item.getId())
                        .orderId(orderDto.getOrderId())
                        .productId(item.getProductId())
                        .unitPrice(item.getUnitPrice())
                        .deliveredKey(item.getDeliveredKey())
                        .stock(item.getStock())
                        .build();

                KafkaOrderItemDto itemMsg = new KafkaOrderItemDto(itemSchema, itemPayload);
                String itemJson = objectMapper.writeValueAsString(itemMsg);

                outboxRepository.save(OutboxEntity.create(
                        idGenerator.nextId(),
                        orderDto.getOrderId(),
                        "ORDER_ITEM",
                        eventType,
                        itemJson
                ));
            }

            log.info("Outbox events saved with full schema for orderId={}", orderDto.getOrderId());
        } catch (Exception e) {
            log.error("Failed to save outbox with schema", e);
            throw new RuntimeException("Outbox message creation failed", e);
        }
    }
}