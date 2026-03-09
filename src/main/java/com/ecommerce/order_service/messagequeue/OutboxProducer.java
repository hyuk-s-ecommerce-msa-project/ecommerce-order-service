package com.ecommerce.order_service.messagequeue;

import com.ecommerce.order_service.dto.*;
import com.ecommerce.order_service.entity.OutboxEntity;
import com.ecommerce.order_service.repository.OutboxRepository;
import com.ecommerce.snowflake.util.SnowflakeIdGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
            Payload orderPayload = Payload.builder()
                    .id(orderDto.getId())
                    .orderId(orderDto.getOrderId())
                    .userId(orderDto.getUserId())
                    .totalAmount(orderDto.getTotalAmount())
                    .itemsJson(objectMapper.writeValueAsString(orderDto.getOrderItems()))
                    .orderStatus(orderDto.getOrderStatus().toString())
                    .createdAt(orderDto.getCreatedAt() != null ? orderDto.getCreatedAt().toString() : null)
                    .build();

            String orderJson = objectMapper.writeValueAsString(orderPayload);

            outboxRepository.save(OutboxEntity.create(
                    idGenerator.nextId(),
                    orderDto.getOrderId(),
                    "ORDER",
                    eventType,
                    orderJson
            ));
        } catch (Exception e) {
            log.error("Outbox 생성 실패", e);
            throw new RuntimeException(e);
        }
//        try {
//            List<OrderItemsDto> itemsList = orderDto.getOrderItems();
//
//            List<Field> orderFields = List.of(
//                    new Field("int64", false, "id"),
//                    new Field("string", false, "order_id"),
//                    new Field("string", false, "user_id"),
//                    new Field("int32", false, "total_amount"),
//                    new Field("string", true, "items_json"),
//                    new Field("string", false, "order_status"),
//                    new Field("string", true, "created_at")
//            );
//
//            Schema orderSchema = new Schema(
//                    idGenerator.nextId(),
//                    "struct",
//                    orderFields,
//                    false,
//                    "order_schema"
//            );
//
//            Payload orderPayload = Payload.builder()
//                    .id(orderDto.getId())
//                    .orderId(orderDto.getOrderId())
//                    .userId(orderDto.getUserId())
//                    .totalAmount(orderDto.getTotalAmount())
//                    .itemsJson(itemsList)
//                    .orderStatus(orderDto.getOrderStatus().toString())
//                    .createdAt(orderDto.getCreatedAt() != null ? orderDto.getCreatedAt().toString() : null)
//                    .build();
//
//            KafkaOrderDto kafkaOrderDto = new KafkaOrderDto(orderSchema, orderPayload);
//
//            String orderJson = objectMapper.writeValueAsString(kafkaOrderDto);
//
//            outboxRepository.save(OutboxEntity.create(
//                    idGenerator.nextId(),
//                    orderDto.getOrderId(),
//                    "ORDER",
//                    eventType,
//                    orderJson
//            ));
//        } catch (Exception e) {
//            log.error("Outbox 생성 실패", e);
//            throw new RuntimeException("Outbox creation failed", e);
//        }
    }
}