package com.ecommerce.order_service.service;

import com.ecommerce.order_service.dto.CartDto;
import com.ecommerce.order_service.entity.CartCategory;
import com.ecommerce.order_service.entity.CartEntity;
import com.ecommerce.order_service.entity.CartGenre;
import com.ecommerce.order_service.entity.CartRedisEntity;
import com.ecommerce.order_service.exception.CartExistingException;
import com.ecommerce.order_service.exception.CartNotFoundException;
import com.ecommerce.order_service.exception.UserNotFoundException;
import com.ecommerce.order_service.repository.CartRedisRepository;
import com.ecommerce.order_service.repository.CartRepository;
import com.ecommerce.snowflake.util.SnowflakeIdGenerator;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {
    private final CartRepository cartRepository;
    private final CartRedisRepository cartRedisRepository;
    private final ModelMapper modelMapper;
    private final SnowflakeIdGenerator snowflakeIdGenerator;

    private final Integer EXPIRED_DAYS = 30;

    @Override
    @Transactional
    public CartDto addToCart(CartDto cartDto, String userId) {
        CartEntity existingCartEntity = cartRepository.findByUserIdAndProductId(userId, cartDto.getProductId());

        if (existingCartEntity != null) {
            throw new CartExistingException("Already in cart");
        }

        String cartId = "Cart-" + UUID.randomUUID().toString();
        Long snowflakeId = snowflakeIdGenerator.nextId();

        // MYSQL 저장
        CartEntity cart = CartEntity.create(
                snowflakeId,
                cartId,
                cartDto.getProductName(),
                cartDto.getPrice(),
                cartDto.getThumbnailUrl(),
                userId,
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

        // Redis 저장
        CartRedisEntity cartRedisEntity = CartRedisEntity.create(
                cartId,
                cart.getUserId(),
                cart.getProductId(),
                cart.getProductName(),
                cart.getPrice(),
                cart.getThumbnailUrl(),
                cartDto.getCategories(),
                cartDto.getGenres()
        );

        cartRedisRepository.save(cartRedisEntity);

        CartDto resultDto = modelMapper.map(cart, CartDto.class);
        resultDto.setCategories(cartDto.getCategories());
        resultDto.setGenres(cartDto.getGenres());

        return resultDto;
    }

    @Override
    public List<CartDto> getCart(String userId) {
        // Redis 먼저 조회
        List<CartRedisEntity> redisList = cartRedisRepository.findAllByUserId(userId);

        if (!redisList.isEmpty()) {
            return redisList.stream()
                    .map(entity -> modelMapper.map(entity, CartDto.class))
                    .toList();
        }

        // MYSQL 조회 -> 30일 이내 데이터만
        LocalDateTime expirationTime = LocalDateTime.now().minusDays(EXPIRED_DAYS);
        List<CartEntity> mysqlList = cartRepository.findByUserIdAndCreatedAtAfter(userId, expirationTime);


        if (mysqlList == null || mysqlList.isEmpty()) {
            return new ArrayList<>();
        }

        mysqlList.forEach(cart -> {
            CartRedisEntity redisEntity = CartRedisEntity.create(
                    cart.getCartId(),
                    cart.getUserId(),
                    cart.getProductId(),
                    cart.getProductName(),
                    cart.getPrice(),
                    cart.getThumbnailUrl(),
                    cart.getCategories().stream().map(CartCategory::getCategoryName).toList(),
                    cart.getGenres().stream().map(CartGenre::getGenre).toList()
            );

            cartRedisRepository.save(redisEntity);
        });

        return convertEntityToCartDto(mysqlList);
    }

    @Override
    @Transactional
    public void deleteCartItem(String cartId, String userId) {
        CartEntity cartEntity = cartRepository.findByCartId(cartId);

        if (cartEntity == null || !cartEntity.getUserId().equals(userId)) {
            throw new CartNotFoundException("Cart Item not found or You don't have permission to delete this cart");
        }

        cartRepository.deleteById(cartEntity.getId());
        cartRedisRepository.deleteById(cartId);
    }

    private List<CartDto> convertEntityToCartDto(List<CartEntity> cartEntity) {
        return cartEntity.stream().map(entity -> {
                CartDto cartDto = modelMapper.map(entity, CartDto.class);

                cartDto.setCategories(entity.getCategories().stream().map(CartCategory::getCategoryName).toList());
                cartDto.setGenres(entity.getGenres().stream().map(CartGenre::getGenre).toList());

                return cartDto;
            }).toList();
    }
}
