package com.ecommerce.order_service.service;

import com.ecommerce.order_service.client.CatalogServiceClient;
import com.ecommerce.order_service.client.KeyInventoryClient;
import com.ecommerce.order_service.client.UserServiceClient;
import com.ecommerce.order_service.dto.OrderDto;
import com.ecommerce.order_service.dto.OrderItemsDto;
import com.ecommerce.order_service.entity.OrderEntity;
import com.ecommerce.order_service.entity.OrderItemEntity;
import com.ecommerce.order_service.entity.enums.OrderStatus;
import com.ecommerce.order_service.exception.OrderNotFoundException;
import com.ecommerce.order_service.messagequeue.KafkaProducer;
import com.ecommerce.order_service.messagequeue.OrderProducer;
import com.ecommerce.order_service.repository.OrderRepository;
import com.ecommerce.order_service.vo.RequestKey;
import com.ecommerce.order_service.vo.RequestPoint;
import com.ecommerce.order_service.vo.ResponseCatalog;
import com.ecommerce.order_service.vo.ResponseKey;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final ModelMapper modelMapper;
    private final CatalogServiceClient catalogServiceClient;
    private final UserServiceClient userServiceClient;
    private final KeyInventoryClient keyInventoryClient;
    private final KafkaProducer kafkaProducer;
    private final OrderProducer orderProducer;

    @Override
    @Transactional
    public OrderDto cancelOrder(String orderId, String userId) {
        OrderEntity orderEntity = orderRepository.findByOrderId(orderId);

        if (orderEntity == null) {
            throw new OrderNotFoundException("Cannot find order");
        }

        if (!orderEntity.getUserId().equals(userId)) {
            throw new RuntimeException("You do not have permission for this order.");
        }

        if (orderEntity.getOrderStatus() == OrderStatus.CANCELED || orderEntity.getOrderStatus() == OrderStatus.COMPLETED) {
            throw new IllegalStateException("Cannot cancel order");
        }

        orderEntity.cancel();

        List<String> productIds = orderEntity.getOrderItems().stream().map(OrderItemEntity::getProductId).toList();

        try {
            if (orderEntity.getUsedPoint() > 0) {
                RequestPoint requestPoint = new RequestPoint();
                requestPoint.setPoint(orderEntity.getUsedPoint());

                userServiceClient.restorePoints(userId, requestPoint);

                log.info("Starting point restoration process, user ID : {}, used point : {}", orderEntity.getUserId(), orderEntity.getUsedPoint());
            }

            RequestKey requestKey = new RequestKey(productIds, orderId);
            List<ResponseKey> responseKey = keyInventoryClient.revokeKey(requestKey, userId);

            if (responseKey.size() != productIds.size()) {
                log.warn("Key revocation mismatch: Requested {}, but revoked {}",
                        productIds.size(), responseKey.size());
            }

            responseKey.forEach(key ->
                    log.info("Key revoked - OrderId: {}, ProductId: {}, Key: {}",
                            orderId, key.getProductId(), key.getGameKey())
            );

            OrderDto orderDto = modelMapper.map(orderEntity, OrderDto.class);

            List<OrderItemsDto> itemDtos = orderEntity.getOrderItems().stream()
                    .map(item -> modelMapper.map(item, OrderItemsDto.class))
                    .toList();

            orderDto.setOrderItems(itemDtos);

            log.info("orderdto : {}", orderDto);

            kafkaProducer.send("order-cancel-topic", orderDto);
            orderProducer.send("orders", orderDto);

            return modelMapper.map(orderEntity, OrderDto.class);
        } catch (Exception e) {
            log.error("Order cancellation failed. Starting compensation... : {}", e.getMessage());
            throw e;
        }
    }

    @Override
    @Transactional
    public OrderDto completeOrder(String orderId, String userId) {
        OrderEntity orderEntity = orderRepository.findByOrderId(orderId);

        if (orderEntity == null ||!orderEntity.getUserId().equals(userId)) {
            throw new OrderNotFoundException("Cannot find order or You do not have permission for this order");
        }

        orderEntity.complete();

        OrderDto orderDto = modelMapper.map(orderEntity, OrderDto.class);

        orderProducer.send("orders", orderDto);

        return orderDto;
    }

    @Override
    @Transactional
    public OrderDto createOrder(OrderDto orderDto, String userId) {
        String datePrefix = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomSuffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        String orderId = datePrefix + "-ORDER-" + randomSuffix;

        List<String> productIds = orderDto.getOrderItems().stream().map(OrderItemsDto::getProductId).toList();

        try {
            List<ResponseCatalog> responseCatalogList = catalogServiceClient.getCatalogList(productIds);

            Map<String, ResponseCatalog> catalogMap = responseCatalogList.stream()
                    .collect(Collectors.toMap(ResponseCatalog::getProductId, c -> c));

            RequestKey requestKey = new RequestKey(productIds, orderId);

            List<ResponseKey> assignedKeys = keyInventoryClient.assignKeys(requestKey, userId);

            Map<String, String> keyMap = assignedKeys.stream().collect(Collectors.toMap(ResponseKey::getProductId, ResponseKey::getGameKey));

            orderDto.setOrderId(orderId);
            orderDto.setUserId(userId);
            orderDto.setOrderStatus(OrderStatus.CREATED);
            orderDto.setCreatedAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

            orderDto.getOrderItems().forEach(orderItemDto -> {
                ResponseCatalog responseCatalog = catalogMap.get(orderItemDto.getProductId());

                orderItemDto.setProductId(responseCatalog.getProductId());
                orderItemDto.setUnitPrice(responseCatalog.getUnitPrice());
                orderItemDto.setDeliveredKey(keyMap.get(orderItemDto.getProductId()));
                orderItemDto.setStock(1);
            });

            int total = orderDto.getOrderItems().stream().mapToInt(OrderItemsDto::getUnitPrice).sum();
            int payAmount = total - orderDto.getUsedPoint();

            orderDto.setTotalAmount(total);
            orderDto.setPayAmount(payAmount);


            if (orderDto.getUsedPoint() != null && orderDto.getUsedPoint() > 0) {
                RequestPoint requestPoint = new RequestPoint();
                requestPoint.setPoint(orderDto.getUsedPoint());

                userServiceClient.usePoint(userId, requestPoint);
            }

            orderProducer.send("orders", orderDto);
            kafkaProducer.send("order-success-topic", orderDto);

            return orderDto;
        } catch (Exception e) {
            log.error("Order creation failed: {}", e.getMessage());
            throw e;
        }
    }

    @Override
    @Transactional
    public OrderDto getOrderByOrderId(String orderId, String userId) {
        OrderEntity orderEntity = orderRepository.findByOrderId(orderId);

        if (orderEntity == null || !orderEntity.getUserId().equals(userId)) {
            throw new OrderNotFoundException("Order not found or access denied");
        }

        return modelMapper.map(orderEntity, OrderDto.class);
    }

    @Override
    public List<OrderDto> getOrdersByUserId(String userId) {
        List<OrderEntity> orderEntities = orderRepository.findByUserId(userId);

        if (orderEntities == null || orderEntities.isEmpty()) {
            return new ArrayList<>();
        }

        return orderEntities.stream()
                .map(entity -> {
                    OrderDto dto = modelMapper.map(entity, OrderDto.class);
                    dto.setUserId(userId);
                    return dto;
                })
                .toList();
    }
}
