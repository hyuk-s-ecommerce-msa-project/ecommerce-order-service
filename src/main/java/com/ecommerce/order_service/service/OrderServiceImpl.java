package com.ecommerce.order_service.service;

import com.ecommerce.order_service.dto.OrderDto;
import com.ecommerce.order_service.entity.OrderEntity;
import com.ecommerce.order_service.entity.OrderItemEntity;
import com.ecommerce.order_service.entity.enums.OrderStatus;
import com.ecommerce.order_service.exception.OrderNotFoundException;
import com.ecommerce.order_service.repository.OrderRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final ModelMapper modelMapper;

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
            log.info("Starting point restoration process, user ID : {}, used point : {}", orderEntity.getUserId(), orderEntity.getUsedPoint());

            // TODO : 유저가 사용한 포인트 원복
        }

        return modelMapper.map(orderEntity, OrderDto.class);
    }

    @Override
    @Transactional
    public OrderDto completePayment(String orderId, String userId) {
        OrderEntity orderEntity = orderRepository.findByOrderId(orderId);

        if (orderEntity == null || !orderEntity.getUserId().equals(userId)) {
            throw new OrderNotFoundException("Cannot find order or You do not have permission for this order");
        }

        orderEntity.markAsPaid();

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

        return modelMapper.map(orderEntity, OrderDto.class);
    }

    @Override
    @Transactional
    public OrderDto createOrder(OrderDto orderDto, String userId) {
        String datePrefix = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomSuffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        String orderId = datePrefix + "-ORDER-" + randomSuffix;

        List<OrderItemEntity> items = orderDto.getOrderItems().stream()
                .map(itemDto -> {
                    // TODO : Key Inventory에서 나중에 받아올 예정
                    String mockKey = "GAME-" + UUID.randomUUID().toString().toUpperCase().substring(0, 12);

                    log.info("Key : {}", mockKey);

                    return OrderItemEntity.create(
                            itemDto.getProductId(),
                            itemDto.getUnitPrice(),
                            mockKey
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
