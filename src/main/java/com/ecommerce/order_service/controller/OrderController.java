package com.ecommerce.order_service.controller;

import com.ecommerce.order_service.dto.OrderDto;
import com.ecommerce.order_service.service.OrderService;
import com.ecommerce.order_service.vo.RequestOrder;
import com.ecommerce.order_service.vo.ResponseOrder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/orders")
    public ResponseEntity<OrderDto> createOrder(@RequestHeader("userId") String userId, @RequestBody RequestOrder requestOrder) {
        OrderDto orderDto = modelMapper.map(requestOrder, OrderDto.class);

        OrderDto dto = orderService.createOrder(orderDto, userId);

        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping("/orders")
    public ResponseEntity<List<ResponseOrder>> getOrder(@RequestHeader("userId") String userId) throws Exception {
        List<OrderDto> orderList = orderService.getOrdersByUserId(userId);

        List<ResponseOrder> result = orderList.stream()
                .map(order -> modelMapper.map(order, ResponseOrder.class))
                .toList();

        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    @GetMapping("/orders/{orderId}/list")
    public ResponseEntity<ResponseOrder> getOrderByOrderId(@PathVariable String orderId, @RequestHeader("userId") String userId) {
        OrderDto orderDto = orderService.getOrderByOrderId(orderId, userId);
        ResponseOrder responseOrder = modelMapper.map(orderDto, ResponseOrder.class);

        return ResponseEntity.status(HttpStatus.OK).body(responseOrder);
    }

    @PutMapping("/orders/{orderId}/cancel")
    public ResponseEntity<ResponseOrder> cancelOrder(@PathVariable String orderId, @RequestHeader("userId") String userId) {
        log.info("Cancelling order id {}", orderId);

        OrderDto canceledOrder = orderService.cancelOrder(orderId, userId);

        ResponseOrder responseOrder = modelMapper.map(canceledOrder, ResponseOrder.class);

        return ResponseEntity.status(HttpStatus.OK).body(responseOrder);
    }

    @PostMapping("/orders/{orderId}/complete")
    public ResponseEntity<ResponseOrder> updateCompletePaymentStatus(@PathVariable String orderId, @RequestHeader("userId") String userId) {
        OrderDto updatedOrder = orderService.completeOrder(orderId, userId);

        ResponseOrder response = modelMapper.map(updatedOrder, ResponseOrder.class);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
