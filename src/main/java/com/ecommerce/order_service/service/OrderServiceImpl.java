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

        if (orderEntity.getUsedPoint() > 0) {
            RequestPoint requestPoint = new RequestPoint();
            requestPoint.setPoint(orderEntity.getUsedPoint());

            userServiceClient.restorePoints(userId, requestPoint);

            log.info("Starting point restoration process, user ID : {}, used point : {}", orderEntity.getUserId(), orderEntity.getUsedPoint());
        }

        return modelMapper.map(orderEntity, OrderDto.class);
    }

    @Override
    @Transactional
    public OrderDto completeOrder(String orderId, String userId) {
        OrderEntity orderEntity = orderRepository.findByOrderId(orderId);

        if (orderEntity == null ||!orderEntity.getUserId().equals(userId)) {
            throw new OrderNotFoundException("Cannot find order or You do not have permission for this order");
        }

        orderEntity.complete();

        if (orderEntity.getUsedPoint() != null && orderEntity.getUsedPoint() > 0) {
            RequestPoint requestPoint = new RequestPoint();
            requestPoint.setPoint(orderEntity.getUsedPoint());

            try {
                log.info("used point : {}", requestPoint);
                userServiceClient.usePoint(userId, requestPoint);
                log.info("Point deduction successful for user: {}, points: {}", userId, orderEntity.getUsedPoint());
            } catch (Exception e) {
                log.error("Failed to deduct points for order: {}", orderId);
                throw new RuntimeException("Point service error, rolling back order completion");
            }
        }

        return modelMapper.map(orderEntity, OrderDto.class);
    }

    @Override
    @Transactional
    public OrderDto createOrder(OrderDto orderDto, String userId) {
        String datePrefix = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomSuffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        String orderId = datePrefix + "-ORDER-" + randomSuffix;

        List<String> productIds = orderDto.getOrderItems().stream().map(OrderItemsDto::getProductId).toList();

        List<ResponseCatalog> responseCatalogList = catalogServiceClient.getCatalogList(productIds);

        log.info("catalog list : {}", responseCatalogList);

        Map<String, ResponseCatalog> catalogMap = responseCatalogList.stream()
                .collect(Collectors.toMap(ResponseCatalog::getProductId, c -> c));

        RequestKey requestKey = new RequestKey(productIds, orderId);

        List<ResponseKey> assignedKeys = keyInventoryClient.assignKeys(requestKey, userId);

        Map<String, String> keyMap = assignedKeys.stream().collect(Collectors.toMap(ResponseKey::getProductId, ResponseKey::getGameKey));

        List<OrderItemEntity> items = orderDto.getOrderItems().stream()
                .map(itemDto -> {
                    String productId = itemDto.getProductId();

                    ResponseCatalog catalog = catalogMap.get(productId);

                    String key = keyMap.get(itemDto.getProductId());

                    if (catalog == null) {
                        throw new RuntimeException("Cannot find catalog for product id : " + itemDto.getProductId());
                    }

                    log.info("catalog : {}", catalog);
                    log.info("realPrice : {}", catalog.getUnitPrice());

                    log.info("Key : {}", key);

                    return OrderItemEntity.create(
                            itemDto.getProductId(),
                            catalog.getUnitPrice(),
                            key
                    );
                }).toList();

        OrderEntity orderEntity = OrderEntity.create(
                orderId,
                userId,
                orderDto.getUsedPoint() != null ? orderDto.getUsedPoint() : 0,
                items
        );

        orderRepository.save(orderEntity);

        OrderDto result = modelMapper.map(orderEntity, OrderDto.class);
        result.setUserId(userId);

        return result;
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
