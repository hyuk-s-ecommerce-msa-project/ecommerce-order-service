package com.ecommerce.order_service.entity;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Getter
public class CartCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String categoryName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id")
    private CartEntity cart;

    public static CartCategory create(String categoryName, CartEntity cart) {
        CartCategory cartCategory = new CartCategory();

        cartCategory.categoryName = categoryName;
        cartCategory.cart = cart;

        return cartCategory;
    }
}
