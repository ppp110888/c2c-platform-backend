package com.c2c.order.feign;

import com.c2c.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

@FeignClient(name = "user-service", url = "http://127.0.0.1:8081")
public interface UserClient {

    @GetMapping("/user/address/detail/{id}")
    Result<Map<String, Object>> getAddressDetail(@PathVariable("id") Long id,
                                                 @RequestHeader("X-User-Id") Long userId);

    @GetMapping("/user/info")
    Result<Map<String, Object>> getUserInfo(@RequestParam("userId") Long userId);
}
