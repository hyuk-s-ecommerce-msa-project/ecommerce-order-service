package com.ecommerce.order_service.vo;

import lombok.Data;

import java.util.List;

@Data
public class ResponseCart {
    private String cartId;
    private String userId;
    private String productId;
    private String productName;
    private Integer price;
    private List<String> categories;
    private List<String> genres;
}
