package com.ecommerce.order_service.controller;

import com.ecommerce.order_service.dto.CartDto;
import com.ecommerce.order_service.service.CartService;
import com.ecommerce.order_service.vo.RequestCart;
import com.ecommerce.order_service.vo.ResponseCart;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/cart-service")
public class CartController {
    private final CartService cartService;
    private final ModelMapper modelMapper;

    @PostMapping("/cart/add")
    public ResponseEntity<ResponseCart> addCart(@RequestBody RequestCart request) {
        CartDto cartDto = modelMapper.map(request, CartDto.class);
        CartDto added = cartService.addToCart(cartDto);

        ResponseCart response = modelMapper.map(added, ResponseCart.class);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/cart/delete/{cartId}")
    public ResponseEntity<String> deleteCart(@PathVariable String cartId) {
        cartService.deleteCartItem(cartId);

        return ResponseEntity.status(HttpStatus.OK).body("Deleted Cart Id : " + cartId);
    }

    @GetMapping("/cart/{userId}")
    public ResponseEntity<ResponseCart> getCart(@PathVariable String userId) {
        CartDto cartDto = cartService.getCart(userId);
        ResponseCart response = modelMapper.map(cartDto, ResponseCart.class);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
 }
