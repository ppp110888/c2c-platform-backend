package com.c2c.message.feign;

import com.c2c.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.Map;

// 🚨 指向你的 user-service 微服务
@FeignClient(name = "user-service")
public interface UserClient {

    // 调用我们在 user-service 里写好的公开信息接口
    @GetMapping("/user/public/info")
    Result<Map<String, Object>> getPublicInfo(@RequestParam("userId") Long userId);
}