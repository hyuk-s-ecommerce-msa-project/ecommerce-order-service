package com.ecommerce.order_service.service;

import com.ecommerce.order_service.dto.OrderDto;
import com.ecommerce.order_service.entity.OrderEntity;
import com.ecommerce.order_service.entity.OrderItemEntity;
import com.ecommerce.order_service.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.stereotype.Service;

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
    public OrderDto createOrder(OrderDto orderDto) {
        String orderId = UUID.randomUUID().toString();

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
                orderDto.getUserId(),
                orderDto.getUsedPoint() != null ? orderDto.getUsedPoint() : 0,
                items
        );

        orderRepository.save(orderEntity);

        return modelMapper.map(orderEntity, OrderDto.class);
    }

    @Override
    public OrderDto getOrderByOrderId(String orderId) {
        OrderEntity orderEntity = orderRepository.findByOrderId(orderId);

        return modelMapper.map(orderEntity, OrderDto.class);
    }

    @Override
    public Iterable<OrderEntity> getOrdersByUserId(String userId) {
        return orderRepository.findByUserId(userId);
    }
}
