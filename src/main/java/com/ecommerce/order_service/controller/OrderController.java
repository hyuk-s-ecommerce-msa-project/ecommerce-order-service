package com.ecommerce.order_service.controller;

import com.ecommerce.order_service.dto.OrderDto;
import com.ecommerce.order_service.entity.OrderEntity;
import com.ecommerce.order_service.service.OrderService;
import com.ecommerce.order_service.vo.RequestOrder;
import com.ecommerce.order_service.vo.ResponseOrder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/order-service")
@Slf4j
public class OrderController {
    private final OrderService orderService;
    private final Environment env;
    private final ModelMapper modelMapper;

    @GetMapping("/health-check")
    public String status() {
        return String.format("It's Working in Order Service on LOCAL PORT %s (SERVER PORT %s)",
                env.getProperty("local.server.port"),
                env.getProperty("server.port"));
    }

    @PostMapping("/orders/{userId}")
    public ResponseEntity<ResponseOrder> createOrder(@PathVariable String userId, @RequestBody RequestOrder requestOrder) {
        log.info("Before add orders data");

        OrderDto orderDto = modelMapper.map(requestOrder, OrderDto.class);
        orderDto.setUserId(userId);

        OrderDto createOrderDto = orderService.createOrder(orderDto);
        ResponseOrder responseOrder = modelMapper.map(createOrderDto, ResponseOrder.class);

        log.info("After added orders data");

        return ResponseEntity.status(HttpStatus.CREATED).body(responseOrder);
    }

    @GetMapping("/orders/{userId}")
    public ResponseEntity<List<ResponseOrder>> getOrder(@PathVariable String userId) throws Exception {
        List<OrderDto> orderList = orderService.getOrdersByUserId(userId);

        List<ResponseOrder> result = new ArrayList<>();
        orderList.forEach(order -> {
            result.add(modelMapper.map(order, ResponseOrder.class));
        });

        log.info("After retrieved orders data");

        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    @GetMapping("/orders/{orderId}/list")
    public ResponseEntity<ResponseOrder> getOrderByOrderId(@PathVariable String orderId) {
        OrderDto orderDto = orderService.getOrderByOrderId(orderId);
        ResponseOrder responseOrder = modelMapper.map(orderDto, ResponseOrder.class);

        return ResponseEntity.status(HttpStatus.OK).body(responseOrder);
    }

    @PutMapping("/orders/{orderId}/cancel")
    public ResponseEntity<ResponseOrder> cancelOrder(@PathVariable String orderId) {
        log.info("Cancelling order id {}", orderId);

        OrderDto canceledOrder = orderService.cancelOrder(orderId);

        ResponseOrder responseOrder = modelMapper.map(canceledOrder, ResponseOrder.class);

        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(responseOrder);
    }

    @PatchMapping("/orders/{orderId}/payment")
    public ResponseEntity<ResponseOrder> updatePaymentStatus(@PathVariable String orderId) {
        OrderDto updatedOrder = orderService.completePayment(orderId);

        ResponseOrder response = modelMapper.map(updatedOrder, ResponseOrder.class);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PatchMapping("/orders/{orderId}/complete")
    public ResponseEntity<ResponseOrder> updateCompletePaymentStatus(@PathVariable String orderId) {
        OrderDto updatedOrder = orderService.completeOrder(orderId);

        ResponseOrder response = modelMapper.map(updatedOrder, ResponseOrder.class);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
