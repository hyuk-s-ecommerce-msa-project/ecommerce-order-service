package com.ecommerce.order_service.client;

import com.ecommerce.order_service.vo.RequestKey;
import com.ecommerce.order_service.vo.ResponseKey;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

@FeignClient("key-inventory-service")
public interface KeyInventoryClient {
    @PostMapping("key-inventory/assign")
    List<ResponseKey> assignKeys(@RequestBody RequestKey requestKey, @RequestHeader("userId") String userId);
}
