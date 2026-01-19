package com.ecommerce.order_service.entity;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Getter
public class CartGenre {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String genre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id")
    private CartEntity cart;

    public static CartGenre create(String genre, CartEntity cart) {
        CartGenre cartGenre = new CartGenre();

        cartGenre.genre = genre;
        cartGenre.cart = cart;

        return cartGenre;
    }
}
