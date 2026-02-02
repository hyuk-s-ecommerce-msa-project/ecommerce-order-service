package com.ecommerce.order_service.entity;

import com.ecommerce.order_service.entity.enums.OrderStatus;
import com.ecommerce.order_service.exception.InvalidOrderException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Table(name = "orders")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String orderId;

    @Column(nullable = false, length = 255)
    private String userId;

    @Column(nullable = false)
    private Integer totalAmount; // 전체 상품 금액 합계

    @Column(nullable = false)
    private Integer usedPoint; // 사용한 포인트

    @Column(nullable = false)
    private Integer payAmount; // 실 결제 금액

    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItemEntity> orderItems = new ArrayList<>();

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static OrderEntity create(String orderId, String userId, Integer usedPoint, List<OrderItemEntity> items) {
        OrderEntity order = new OrderEntity();

        order.orderId = orderId;
        order.userId = userId;
        order.usedPoint = usedPoint;

        int totalAmount = 0;

        for (OrderItemEntity item : items) {
            order.addOrderItem(item);
            totalAmount += item.getUnitPrice();
        }

        order.totalAmount = totalAmount;

        if (totalAmount < usedPoint) {
            throw new InvalidOrderException("Used points cannot exceed the total amount");
        }
        order.payAmount = totalAmount - usedPoint;
        order.orderStatus = OrderStatus.CREATED;

        return order;
    }

    public void cancel() {
        if (this.orderStatus == OrderStatus.COMPLETED) {
            throw new IllegalStateException("Cannot cancel a completed order");
        }

        this.orderStatus = OrderStatus.CANCELED;
    }

    public void complete() {
        this.orderStatus = OrderStatus.COMPLETED;
    }

    private void addOrderItem(OrderItemEntity item) {
        this.orderItems.add(item);
        item.setOrder(this);
    }
}
