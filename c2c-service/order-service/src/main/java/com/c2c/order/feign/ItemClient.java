package com.c2c.order.feign;

import com.c2c.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient(name = "item-service", url = "http://127.0.0.1:8082")
public interface ItemClient {

    @PostMapping("/item/deductStock")
    Result<String> deductStock(@RequestParam("itemId") Long itemId);

    @PostMapping("/item/addStock")
    Result<String> addStock(@RequestParam("itemId") Long itemId);

    @GetMapping("/item/info")
    Result<Map<String, Object>> getItemInfo(@RequestParam("itemId") Long itemId);
}