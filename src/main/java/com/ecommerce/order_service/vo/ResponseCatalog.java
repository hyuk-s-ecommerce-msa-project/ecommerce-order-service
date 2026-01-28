package com.ecommerce.order_service.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class ResponseCatalog {
    private String productId;
    private String productName;
    @JsonProperty("price")
    private Integer unitPrice;
    private Integer stock;
    private String headerImage;
    private String detailDescription;
    private String releaseDate;

    private List<String> images;
    private List<String> genres;
    private List<String> categories;
}
