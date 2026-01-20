package com.ecommerce.order_service.entity;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

import java.util.List;

@Getter
@RedisHash(value = "cart", timeToLive = 2592000) // 30일
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CartRedisEntity {
    @Id
    private String cartId;

    @Indexed
    private String userId;
    private String productId;
    private String productName;
    private Integer price;
    private String thumbnailUrl;
    private List<String> categories;
    private List<String> genres;

    public static CartRedisEntity create(String cartId, String userId, String productId, String productName,
                                         Integer price, String thumbnailUrl, List<String> categories, List<String> genres) {
        CartRedisEntity cartRedisEntity = new CartRedisEntity();

        cartRedisEntity.cartId = cartId;
        cartRedisEntity.userId = userId;
        cartRedisEntity.productId = productId;
        cartRedisEntity.productName = productName;
        cartRedisEntity.price = price;
        cartRedisEntity.thumbnailUrl = thumbnailUrl;
        cartRedisEntity.categories = categories;
        cartRedisEntity.genres = genres;

        return cartRedisEntity;
    }
}
