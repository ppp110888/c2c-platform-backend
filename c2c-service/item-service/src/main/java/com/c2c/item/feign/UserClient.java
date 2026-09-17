package com.c2c.item.feign;

import com.c2c.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.Map;

// 呼叫用户服务
@FeignClient(name = "user-service")
public interface UserClient {

    @GetMapping("/user/info")
    Result<Map<String, Object>> getUserInfo(@RequestParam("userId") Long userId);
}