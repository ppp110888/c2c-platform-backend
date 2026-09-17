package com.c2c.order.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "alipay")
public class AlipayProperties {
    private String appId;
    private String privateKey;
    private String publicKey;
    private String gatewayUrl;
    private String signType;
    private String charset;
    private String notifyUrl;
    private String returnUrl;
    private String frontendUrl;   // 前端地址，支付完成后重定向回去
    private boolean verifySign;   // 是否验签（沙箱可关，生产必须开）
}