package com.ecommerce.order_service.controller;

import com.ecommerce.order_service.dto.CartDto;
import com.ecommerce.order_service.service.CartService;
import com.ecommerce.order_service.vo.RequestCart;
import com.ecommerce.order_service.vo.ResponseCart;
import com.ecommerce.order_service.vo.ResponseCartList;
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
    public ResponseEntity<ResponseCartList> getCart(@PathVariable String userId) {
        List<CartDto> cartDto = cartService.getCart(userId);

        List<ResponseCart> items = cartDto.stream().map(dto -> modelMapper.map(dto, ResponseCart.class)).toList();

        int totalPrice = items.stream().mapToInt(ResponseCart::getPrice).sum();

        ResponseCartList response = new ResponseCartList();
        response.setCartList(items);
        response.setTotalPrice(totalPrice);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
 }
