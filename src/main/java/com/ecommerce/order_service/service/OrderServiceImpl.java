package com.ecommerce.order_service.service;

import com.ecommerce.order_service.config.ShardContextHolder;
import com.ecommerce.order_service.dto.*;
import com.ecommerce.order_service.entity.OrderEntity;
import com.ecommerce.order_service.entity.OrderItemEntity;
import com.ecommerce.order_service.entity.enums.OrderStatus;
import com.ecommerce.order_service.exception.OrderNotFoundException;
import com.ecommerce.order_service.messagequeue.OutboxProducer;
import com.ecommerce.order_service.repository.OrderRepository;
import com.ecommerce.order_service.service.connector.InternalServiceConnector;
import com.ecommerce.order_service.vo.RequestKey;
import com.ecommerce.order_service.vo.RequestPoint;
import com.ecommerce.order_service.vo.ResponseCatalog;
import com.ecommerce.order_service.vo.ResponseKey;
import com.ecommerce.snowflake.util.SnowflakeIdGenerator;
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
    private final InternalServiceConnector internalConnector;
    private final SnowflakeIdGenerator snowflakeIdGenerator;
    private final OutboxProducer outboxProducer;

    @Override
    @Transactional
    public OrderDto cancelOrder(String orderId, String userId) {
        setShardContext(userId);
        try {
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

            if (orderEntity.getUsedPoint() > 0) {
                RequestPoint requestPoint = new RequestPoint();
                requestPoint.setPoint(orderEntity.getUsedPoint());
                internalConnector.restoreUserPoints(userId, requestPoint);
            }

            RequestKey requestKey = new RequestKey(productIds, orderId);
            internalConnector.revokeGameKeys(requestKey, userId);

            OrderDto orderDto = modelMapper.map(orderEntity, OrderDto.class);
            outboxProducer.sendToOutbox(orderDto, "ORDER_CANCELED");

            return orderDto;
        } finally {
            ShardContextHolder.clear();
        }
    }

    @Override
    @Transactional
    public OrderDto completeOrder(String orderId, String userId) {
        setShardContext(userId);
        try {
            OrderEntity orderEntity = orderRepository.findByOrderId(orderId);

            if (orderEntity == null || !orderEntity.getUserId().equals(userId)) {
                throw new OrderNotFoundException("Cannot find order or You do not have permission for this order");
            }

            orderEntity.complete();
            OrderDto orderDto = modelMapper.map(orderEntity, OrderDto.class);
            outboxProducer.sendToOutbox(orderDto, "ORDER_COMPLETED");

            return orderDto;
        } finally {
            ShardContextHolder.clear();
        }
    }

    @Override
    public OrderDto createOrder(OrderDto orderDto, String userId) {
        setShardContext(userId);
        try {
            String datePrefix = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String randomSuffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            String orderId = datePrefix + "-ORDER-" + randomSuffix;
            Long snowflakeId = snowflakeIdGenerator.nextId();

            List<String> productIds = orderDto.getOrderItems().stream().map(OrderItemsDto::getProductId).toList();
            List<ResponseCatalog> responseCatalogList = internalConnector.getCatalogList(productIds);
            Map<String, ResponseCatalog> catalogMap = responseCatalogList.stream()
                    .collect(Collectors.toMap(ResponseCatalog::getProductId, c -> c));

            RequestKey requestKey = new RequestKey(productIds, orderId);
            List<ResponseKey> assignedKeys = internalConnector.assignKeys(requestKey, userId);
            Map<String, String> keyMap = assignedKeys.stream().collect(Collectors.toMap(ResponseKey::getProductId, ResponseKey::getGameKey));

            setupOrderDetails(orderDto, userId, orderId, snowflakeId, catalogMap, keyMap);

            List<OrderItemEntity> itemEntities = orderDto.getOrderItems().stream()
                    .map(itemDto -> OrderItemEntity.create(
                            itemDto.getId(),
                            itemDto.getProductId(),
                            itemDto.getUnitPrice(),
                            itemDto.getDeliveredKey(),
                            1
                    ))
                    .toList();

            OrderEntity orderEntity = OrderEntity.create(
                    orderDto.getId(),
                    orderDto.getOrderId(),
                    orderDto.getUserId(),
                    orderDto.getUsedPoint(),
                    itemEntities
            );
            orderRepository.save(orderEntity);

            if (orderDto.getUsedPoint() != null && orderDto.getUsedPoint() > 0) {
                RequestPoint requestPoint = new RequestPoint();
                requestPoint.setPoint(orderDto.getUsedPoint());
                internalConnector.withdrawPoint(userId, requestPoint);
            }

            return saveOrderToOutbox(orderDto);
        } finally {
            ShardContextHolder.clear();
        }
    }

    @Override
    @Transactional
    public OrderDto getOrderByOrderId(String orderId, String userId) {
        setShardContext(userId);
        try {
            OrderEntity orderEntity = orderRepository.findByOrderId(orderId);

            if (orderEntity == null || !orderEntity.getUserId().equals(userId)) {
                throw new OrderNotFoundException("Order not found or access denied");
            }

            return modelMapper.map(orderEntity, OrderDto.class);
        } finally {
            ShardContextHolder.clear();
        }
    }

    @Override
    public List<OrderDto> getOrdersByUserId(String userId) {
        setShardContext(userId);
        try {
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
        } finally {
            ShardContextHolder.clear();
        }
    }

    @Transactional
    public OrderDto saveOrderToOutbox(OrderDto orderDto) {
        outboxProducer.sendToOutbox(orderDto, "ORDER_CREATED");
        return orderDto;
    }

    private void setupOrderDetails(OrderDto orderDto, String userId, String orderId, Long snowflakeId,
                                   Map<String, ResponseCatalog> catalogMap, Map<String, String> keyMap) {
        orderDto.setId(snowflakeId);
        orderDto.setOrderId(orderId);
        orderDto.setUserId(userId);
        orderDto.setOrderStatus(OrderStatus.CREATED);
        orderDto.setCreatedAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        orderDto.getOrderItems().forEach(itemDto -> {
            ResponseCatalog catalog = catalogMap.get(itemDto.getProductId());
            Long itemSnowflakeId = snowflakeIdGenerator.nextId();
            itemDto.setId(itemSnowflakeId);
            itemDto.setUnitPrice(catalog.getUnitPrice());
            itemDto.setDeliveredKey(keyMap.get(itemDto.getProductId()));
            itemDto.setStock(1);
        });

        int total = orderDto.getOrderItems().stream().mapToInt(OrderItemsDto::getUnitPrice).sum();
        orderDto.setTotalAmount(total);
        orderDto.setPayAmount(total - (orderDto.getUsedPoint() != null ? orderDto.getUsedPoint() : 0));
    }

    private void setShardContext(String userId) {
        int shardIndex = Math.abs(userId.hashCode() % 2);
        ShardContextHolder.setShardIndex(shardIndex);
        log.info("Shard Index set to {} for userId: {}", shardIndex, userId);
    }
}
