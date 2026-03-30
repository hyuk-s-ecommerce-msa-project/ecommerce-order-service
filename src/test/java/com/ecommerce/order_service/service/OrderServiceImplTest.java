package com.ecommerce.order_service.service;

import com.ecommerce.order_service.dto.OrderDto;
import com.ecommerce.order_service.dto.OrderItemsDto;
import com.ecommerce.order_service.entity.OrderEntity;
import com.ecommerce.order_service.entity.OrderItemEntity;
import com.ecommerce.order_service.entity.enums.OrderStatus;
import com.ecommerce.order_service.exception.InvalidOrderException;
import com.ecommerce.order_service.exception.OrderNotFoundException;
import com.ecommerce.order_service.messagequeue.OutboxProducer;
import com.ecommerce.order_service.repository.OrderRepository;
import com.ecommerce.order_service.service.connector.InternalServiceConnector;
import com.ecommerce.order_service.vo.ResponseCatalog;
import com.ecommerce.order_service.vo.ResponseKey;
import com.ecommerce.snowflake.util.SnowflakeIdGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @InjectMocks
    private OrderServiceImpl orderService;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private InternalServiceConnector internalConnector;

    @Mock
    private SnowflakeIdGenerator snowflakeIdGenerator;

    @Mock
    private OutboxProducer outboxProducer;

    @Spy
    private ModelMapper modelMapper = new ModelMapper();

    private final String userId = "user-1234";
    private final String productId = "PROD-999";

    @Test
    @DisplayName("주문 생성: 정상적인 흐름으로 DB 저장 및 Outbox 발행 확인")
    void createOrder_Success() {
        OrderDto requestDto = new OrderDto();
        requestDto.setUsedPoint(5000);
        OrderItemsDto itemDto = new OrderItemsDto();
        itemDto.setProductId(productId);
        requestDto.setOrderItems(List.of(itemDto));

        when(snowflakeIdGenerator.nextId()).thenReturn(100L, 200L);

        ResponseCatalog catalog = new ResponseCatalog();
        catalog.setProductId(productId);
        catalog.setUnitPrice(15000);
        when(internalConnector.getCatalogList(anyList())).thenReturn(List.of(catalog));

        ResponseKey key = new ResponseKey();
        key.setProductId(productId);
        key.setGameKey("G-KEY-12345");
        when(internalConnector.assignKeys(any(), eq(userId))).thenReturn(List.of(key));

        OrderDto result = orderService.createOrder(requestDto, userId);

        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getTotalAmount()).isEqualTo(15000);
        assertThat(result.getPayAmount()).isEqualTo(10000); // 15000 - 5000
        assertThat(result.getOrderItems().get(0).getDeliveredKey()).isEqualTo("G-KEY-12345");

        verify(orderRepository, times(1)).save(any(OrderEntity.class));
        verify(internalConnector, times(1)).withdrawPoint(eq(userId), any());
        verify(outboxProducer, times(1)).sendToOutbox(any(OrderDto.class), eq("ORDER_CREATED"));
    }

    @Test
    @DisplayName("주문 취소: 포인트 복구 및 키 회수 로직 검증")
    void cancelOrder_Success() {
        String orderId = "20260330-ORDER-ABC";

        OrderItemEntity item = OrderItemEntity.create(1L, "PROD-001", 1000, "KEY-123", 1);
        List<OrderItemEntity> items = List.of(item);

        OrderEntity orderEntity = OrderEntity.create(100L, orderId, userId, 500, items);

        when(orderRepository.findByOrderId(orderId)).thenReturn(orderEntity);

        OrderDto result = orderService.cancelOrder(orderId, userId);

        assertThat(result.getOrderStatus()).isEqualTo(OrderStatus.CANCELED);
        verify(internalConnector, times(1)).restoreUserPoints(eq(userId), any());
        verify(internalConnector, times(1)).revokeGameKeys(any(), eq(userId));
        verify(outboxProducer, times(1)).sendToOutbox(any(), eq("ORDER_CANCELED"));
    }

    @Test
    @DisplayName("주문 완료: 상태 변경 및 아웃박스 발행")
    void completeOrder_Success() {
        String orderId = "20260330-ORDER-ABC";
        OrderEntity orderEntity = OrderEntity.create(100L, orderId, userId, 0, new ArrayList<>());
        when(orderRepository.findByOrderId(orderId)).thenReturn(orderEntity);

        OrderDto result = orderService.completeOrder(orderId, userId);

        assertThat(result.getOrderStatus()).isEqualTo(OrderStatus.COMPLETED);
        verify(outboxProducer, times(1)).sendToOutbox(any(), eq("ORDER_COMPLETED"));
    }

    @Test
    @DisplayName("예외 발생: 본인의 주문이 아닌 경우 조회 불가")
    void getOrder_Fail_WrongUser() {
        String orderId = "ORDER-XYZ";
        OrderEntity otherUserOrder = OrderEntity.create(100L, orderId, "wrong-user", 0, new ArrayList<>());
        when(orderRepository.findByOrderId(orderId)).thenReturn(otherUserOrder);

        assertThrows(OrderNotFoundException.class, () -> {
            orderService.getOrderByOrderId(orderId, userId);
        });
    }

    @Test
    @DisplayName("예외 발생: 이미 완료된 주문은 취소 불가")
    void cancelOrder_Fail_AlreadyCompleted() {
        String orderId = "ORDER-COMP";
        OrderEntity completedOrder = OrderEntity.create(100L, orderId, userId, 0, new ArrayList<>());
        completedOrder.complete();
        when(orderRepository.findByOrderId(orderId)).thenReturn(completedOrder);

        assertThrows(IllegalStateException.class, () -> {
            orderService.cancelOrder(orderId, userId);
        });
    }

    @Test
    @DisplayName("예외: 타인의 주문을 취소하려 하면 권한 에러가 발생해야 한다")
    void cancelOrder_Fail_Forbidden() {
        String testOrderId = "20260330-ORDER-FORBIDDEN";
        String otherUser = "other-user";

        OrderItemEntity item = OrderItemEntity.create(1L, "P-01", 1000, "K-1", 1);
        OrderEntity order = OrderEntity.create(100L, testOrderId, otherUser, 0, List.of(item));

        when(orderRepository.findByOrderId(testOrderId)).thenReturn(order);

        assertThrows(RuntimeException.class, () -> {
            orderService.cancelOrder(testOrderId, userId);
        });
    }

    @Test
    @DisplayName("예외: 포인트가 상품 총액을 초과하면 주문 생성에 실패해야 한다")
    void createOrder_Fail_PointExceeded() {
        OrderDto requestDto = new OrderDto();
        requestDto.setUsedPoint(2000);
        OrderItemsDto item = new OrderItemsDto();
        item.setProductId(productId);
        requestDto.setOrderItems(List.of(item));

        ResponseCatalog catalog = new ResponseCatalog();
        catalog.setProductId(productId);
        catalog.setUnitPrice(1000);
        when(internalConnector.getCatalogList(anyList())).thenReturn(List.of(catalog));

        ResponseKey key = new ResponseKey();
        key.setProductId(productId);
        key.setGameKey("MOCK-KEY");
        when(internalConnector.assignKeys(any(), anyString())).thenReturn(List.of(key));

        when(snowflakeIdGenerator.nextId()).thenReturn(100L, 200L);

        assertThrows(InvalidOrderException.class, () -> {
            orderService.createOrder(requestDto, userId);
        });
    }
}