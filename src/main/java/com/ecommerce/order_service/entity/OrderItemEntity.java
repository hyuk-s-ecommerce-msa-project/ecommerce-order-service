package com.ecommerce.order_service.entity;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Getter
@Table(name = "order_items")
public class OrderItemEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String productId;

    @Column(nullable = false)
    private Integer unitPrice;

    @Column(nullable = false)
    private String deliveredKey;

    @Column(nullable = false)
    private Integer stock;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", referencedColumnName = "orderId")
    private OrderEntity order;

    void setOrder(OrderEntity order) {
        this.order = order;
    }

    public static OrderItemEntity create(String productId, Integer unitPrice, String deliveredKey, Integer stock) {
        OrderItemEntity item = new OrderItemEntity();
        item.productId = productId;
        item.unitPrice = unitPrice;
        item.deliveredKey = deliveredKey;
        item.stock = stock;
        return item;
    }
}
