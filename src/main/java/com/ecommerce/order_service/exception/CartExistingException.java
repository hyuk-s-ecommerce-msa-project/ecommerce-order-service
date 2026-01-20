package com.ecommerce.order_service.exception;

public class CartExistingException extends RuntimeException {
    public CartExistingException(String message) {
        super(message);
    }
}
