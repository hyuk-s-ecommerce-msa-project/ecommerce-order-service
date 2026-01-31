package com.ecommerce.order_service.client;

import com.ecommerce.order_service.vo.ResponseCatalog;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "catalog-service")
public interface CatalogServiceClient {
    @PostMapping("catalog-service/catalogs/queries")
    List<ResponseCatalog> getCatalogList(@RequestBody List<String> productIds);

    @PostMapping("/catalogs/stock/increase")
    List<String> increaseStock(@RequestBody List<String> productIds);

    @PostMapping("/catalogs/stock/decrease")
    List<String> decreaseStock(@RequestBody List<String> productIds);
}
