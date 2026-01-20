package com.ecommerce.order_service.vo;

import lombok.Data;

import java.util.List;

@Data
public class ResponseCartList {
    private List<ResponseCart> cartList;
    private Integer totalPrice;
}
