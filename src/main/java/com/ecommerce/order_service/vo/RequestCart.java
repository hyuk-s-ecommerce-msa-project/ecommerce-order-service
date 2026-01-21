package com.ecommerce.order_service.vo;

import lombok.Data;

import java.util.List;

@Data
public class RequestCart {
    private String productId;
    private String productName;
    private Integer price;
    private String thumbnailUrl;
    private List<String> categories;
    private List<String> genres;
}
