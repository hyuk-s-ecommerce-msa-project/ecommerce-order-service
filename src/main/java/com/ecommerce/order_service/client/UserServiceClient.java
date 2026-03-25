package com.ecommerce.order_service.client;

import com.ecommerce.order_service.vo.RequestPoint;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(
        name = "user-service",
        url = "http://user-service:8081"
)
public interface UserServiceClient {
    @PostMapping("/point/increase")
    void restorePoints(@RequestHeader("userId") String userId, @RequestBody RequestPoint requestPoint);

    @PostMapping("/point/withdraw")
    void usePoint(@RequestHeader("userId") String userId, @RequestBody RequestPoint requestPoint);
}