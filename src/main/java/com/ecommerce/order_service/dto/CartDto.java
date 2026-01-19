package com.ecommerce.order_service.dto;

import lombok.Data;

import java.util.List;

@Data
public class CartDto {
    private String cartId;
    private String userId;
    private String productId;
    private String productName;
    private Integer price;
    private String thumbnailUrl;
    private List<String> categories;
    private List<String> genres;
}
