package com.ecommerce.order_service.messagequeue;

import com.ecommerce.order_service.service.OrderService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumer {
    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "payment-topic", groupId = "order-service-group")
    public void consumePaymentEvent(String message) {
        try {
            log.info("결제 완료 이벤트 수신: {}", message);

            JsonNode jsonNode = objectMapper.readTree(message);
            String orderId = jsonNode.get("orderId").asText();
            String userId = jsonNode.get("userId").asText();

            orderService.completeOrder(orderId, userId);

            log.info("주문 완료 처리 성공 - OrderId: {}, UserId: {}", orderId, userId);

        } catch (Exception e) {
            log.error("주문 처리 중 에러 발생: {}", e.getMessage());
            throw new RuntimeException("Consumer failed", e);
        }
    }

    @KafkaListener(topics = "pay-failed-topic", groupId = "order-service-group")
    public void consumePaymentFailedEvent(String message) {
        try {
            log.info("결제 실패 이벤트 수신: {}", message);

            JsonNode jsonNode = objectMapper.readTree(message);
            String orderId = jsonNode.get("orderId").asText();
            String userId = jsonNode.get("userId").asText();

            orderService.cancelOrder(orderId, userId);

            log.info("결제 실패에 따른 주문 취소 완료 - OrderId: {}", orderId);
        } catch (Exception e) {
            log.error("결제 실패 처리 중 에러 발생: {}", e.getMessage());
        }
    }
}
