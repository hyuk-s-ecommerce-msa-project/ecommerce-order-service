package com.ecommerce.order_service.service.connector;

import com.ecommerce.order_service.client.CatalogServiceClient;
import com.ecommerce.order_service.client.KeyInventoryClient;
import com.ecommerce.order_service.client.UserServiceClient;
import com.ecommerce.order_service.vo.RequestKey;
import com.ecommerce.order_service.vo.RequestPoint;
import com.ecommerce.order_service.vo.ResponseCatalog;
import com.ecommerce.order_service.vo.ResponseKey;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class InternalServiceConnector {
    private final CatalogServiceClient catalogServiceClient;
    private final KeyInventoryClient keyInventoryClient;
    private final UserServiceClient userServiceClient;

    @CircuitBreaker(name = "userService_restore_circuitBreaker", fallbackMethod = "fallbackUserPoints")
    public void restoreUserPoints(String userId, RequestPoint requestPoint) {
        userServiceClient.restorePoints(userId, requestPoint);
    }

    @CircuitBreaker(name = "keyInventory_revoke_circuitBreaker", fallbackMethod = "fallbackRevokeKeys")
    public List<ResponseKey> revokeGameKeys(RequestKey requestKey, String userId) {
        return keyInventoryClient.revokeKey(requestKey, userId);
    }

    @CircuitBreaker(name = "catalogService_get_circuitBreaker", fallbackMethod = "fallbackGetCatalogs")
    public List<ResponseCatalog> getCatalogList(List<String> productIds) {
        return catalogServiceClient.getCatalogList(productIds);
    }

    @CircuitBreaker(name = "keyInventory_assign_circuitBreaker", fallbackMethod = "fallbackAssignKeys")
    public List<ResponseKey> assignKeys(RequestKey requestKey, String userId) {
        List<ResponseKey> response = keyInventoryClient.assignKeys(requestKey, userId);

        if (response == null || response.size() != requestKey.getProductId().size()) {
            log.error("키 발급 개수 불일치, 요청 = {}, 응답 = {}", requestKey.getProductId().size(),
                    response != null ? response.size() : 0);

            throw new RuntimeException("요청한 상품의 키가 모두 발급되지 않았습니다.");
        }

        return response;
    }

    @CircuitBreaker(name = "userService_withdraw_circuitBreaker", fallbackMethod = "fallbackWithdrawPoints")
    public void withdrawPoint(String userId, RequestPoint requestPoint) {
        userServiceClient.usePoint(userId, requestPoint);
    }

    private void fallbackWithdrawPoints(String userId, RequestPoint requestPoint, Throwable throwable) {
        log.error("포인트 차감 실패 : userId={}, {}", userId, throwable.getMessage());

        throw new RuntimeException("포인트 차감 중 장애 발생, 나중에 다시 시도하세요.");
    }

    private List<ResponseKey> fallbackAssignKeys(RequestKey requestKey, String userId, Throwable throwable) {
        log.error("게임 키 발급 오류 : userId ={}, {}", userId, throwable.getMessage());

        throw new RuntimeException("게임 키 발급 중 장애가 발생했습니다. 나중에 다시 시도하세요.");
    }

    private List<ResponseCatalog> fallbackGetCatalogs(List<String> productIds, Throwable throwable) {
        log.error("Catalog 목록 가져오기 실패 : {}", throwable.getMessage());

        throw new RuntimeException("제품 목록을 가져오는 중 장애가 발생했습니다. 나중에 다시 시도하세요.");
    }

    private void fallbackUserPoints(String userId, RequestPoint requestPoint, Throwable throwable) {
        log.error("User service 포인트 복구 실패 : userId : {}, error : {}", userId, throwable.getMessage());

        throw new RuntimeException("포인트 복구 중 장애가 발생했습니다. 나중에 다시 시도하세요.");
    }

    private List<ResponseKey> fallbackRevokeKeys(RequestKey requestKey, String userId,  Throwable throwable) {
        log.error("Game Key 반환 실패 : userId: {}, error : {}", userId, throwable.getMessage());

        throw new RuntimeException("게임 키 회수가 실패했습니다. 나중에 다시 시도하세요");
    }
}
