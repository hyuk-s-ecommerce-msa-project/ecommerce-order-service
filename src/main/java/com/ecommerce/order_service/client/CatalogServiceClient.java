package com.ecommerce.order_service.client;

import com.ecommerce.order_service.vo.ResponseCatalog;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(
        name = "catalog-service",
        url = "http://catalog-service:8082"
)
public interface CatalogServiceClient {
    @PostMapping("/catalog-service/catalogs/queries")
    List<ResponseCatalog> getCatalogList(@RequestBody List<String> productIds);

    @PostMapping("/catalog-service/catalogs/stock/increase")
    List<ResponseCatalog> increaseStock(@RequestBody List<String> productIds);

    @PostMapping("/catalog-service/catalogs/stock/decrease")
    List<ResponseCatalog> decreaseStock(@RequestBody List<String> productIds);
}
