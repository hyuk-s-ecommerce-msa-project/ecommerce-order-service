package com.ecommerce.order_service.service;

import com.ecommerce.order_service.dto.CartDto;
import com.ecommerce.order_service.entity.CartCategory;
import com.ecommerce.order_service.entity.CartEntity;
import com.ecommerce.order_service.entity.CartGenre;
import com.ecommerce.order_service.exception.CartNotFoundException;
import com.ecommerce.order_service.exception.UserNotFoundException;
import com.ecommerce.order_service.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {
    private final CartRepository cartRepository;
    private final ModelMapper modelMapper;

    @Override
    public CartDto addToCart(CartDto cartDto) {
        String cartId = "Cart" + UUID.randomUUID().toString();

        CartEntity cart = CartEntity.create(
                cartId,
                cartDto.getProductName(),
                cartDto.getPrice(),
                cartDto.getThumbnailUrl(),
                cartDto.getUserId(),
                cartDto.getProductId()
        );

        if (cartDto.getCategories() != null) {
            cartDto.getCategories().stream()
                    .map(name -> CartCategory.create(name, cart))
                    .forEach(category -> cart.getCategories().add(category));
        }

        if (cartDto.getGenres() != null) {
            cartDto.getGenres().stream()
                    .map(name -> CartGenre.create(name, cart))
                    .forEach(genre -> cart.getGenres().add(genre));
        }

        cartRepository.save(cart);

        CartDto resultDto = modelMapper.map(cart, CartDto.class);
        resultDto.setCategories(cartDto.getCategories());
        resultDto.setGenres(cartDto.getGenres());

        return resultDto;
    }

    @Override
    public CartDto getCart(String userId) {
        CartEntity cartEntity = cartRepository.findByUserId(userId);

        if (cartEntity == null) {
            throw new UserNotFoundException("User does not exist");
        }

        return convertEntityToCartDto(cartEntity);
    }

    @Override
    public void deleteCartItem(String cartId) {
        CartEntity cartEntity = cartRepository.findByCartId(cartId);

        if (cartEntity == null) {
            throw new CartNotFoundException("Cart Item not found");
        }

        cartRepository.deleteById(cartEntity.getId());
    }

    private CartDto convertEntityToCartDto(CartEntity cartEntity) {
        CartDto cartDto = modelMapper.map(cartEntity, CartDto.class);

        cartDto.setCategories(cartEntity.getCategories().stream().map(CartCategory::getCategoryName).toList());
        cartDto.setGenres(cartEntity.getGenres().stream().map(CartGenre::getGenre).toList());

        return cartDto;
    }
}
