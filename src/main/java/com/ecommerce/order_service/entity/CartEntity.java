package com.ecommerce.order_service.entity;

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
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Table(name = "cart")
public class CartEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String cartId;
    @Column(nullable = false)
    private String productName;
    @Column(nullable = false)
    private Integer price;
    @Column(nullable = false)
    private String thumbnailUrl;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "cart", orphanRemoval = true)
    private List<CartCategory> categories = new ArrayList<>();
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "cart", orphanRemoval = true)
    private List<CartGenre> genres = new ArrayList<>();

    @Column(nullable = false)
    private String userId;
    @Column(nullable = false)
    private String productId;

    @CreatedDate
    private LocalDateTime createdAt;

    public static CartEntity create(String cartId, String productName, Integer price, String thumbnailUrl,
                                    String userId, String productId) {
        CartEntity cartEntity = new CartEntity();

        cartEntity.cartId = cartId;
        cartEntity.productName = productName;
        cartEntity.price = price;
        cartEntity.thumbnailUrl = thumbnailUrl;
        cartEntity.userId = userId;
        cartEntity.productId = productId;

        return cartEntity;
    }
}
