package com.ecommerce.order_service.batch;

import com.ecommerce.order_service.repository.CartRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class CartScheduler {
    private final CartRepository cartRepository;

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void deleteOldCartItems() {
        LocalDateTime expirationDate = LocalDateTime.now().minusDays(30);

        int deletedCount = cartRepository.deleteByCreatedAtBefore(expirationDate);

        log.info("Deleted {} CartItems", deletedCount);
    }
}
