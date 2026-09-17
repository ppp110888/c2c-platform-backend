package com.c2c.order.controller;

import cn.hutool.json.JSONUtil;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.c2c.order.config.AlipayProperties;
import com.c2c.order.entity.OrderInfo;
import com.c2c.order.service.OrderService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/order/alipay")
public class AlipayController {

    @Resource
    private AlipayProperties alipayProperties;

    @Resource
    private OrderService orderService;

    /**
     * 接口 1：呼出支付宝收银台
     */
    @GetMapping(value = "/pay", produces = "text/html;charset=utf-8")
    public String pay(@RequestParam("orderNo") String orderNo) throws AlipayApiException {

        QueryWrapper<OrderInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("order_no", orderNo);
        OrderInfo order = orderService.getOne(queryWrapper);

        if (order == null || order.getStatus() != 0) {
            return "<h1>订单不存在，或已被支付/取消</h1>";
        }

        AlipayClient alipayClient = new DefaultAlipayClient(
                alipayProperties.getGatewayUrl(),
                alipayProperties.getAppId(),
                alipayProperties.getPrivateKey(),
                "json",
                alipayProperties.getCharset(),
                alipayProperties.getPublicKey(),
                alipayProperties.getSignType()
        );

        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();

        // 异步回调地址
        request.setNotifyUrl(alipayProperties.getNotifyUrl());

        // 同步跳转地址：走网关，统一管理
        request.setReturnUrl(alipayProperties.getReturnUrl() + "api/order/alipay/return");

        Map<String, Object> bizContent = new HashMap<>();
        bizContent.put("out_trade_no", order.getOrderNo());
        bizContent.put("total_amount", order.getPayAmount());
        bizContent.put("subject", "C2C二手平台 - 宝贝担保支付");
        bizContent.put("product_code", "FAST_INSTANT_TRADE_PAY");

        request.setBizContent(JSONUtil.toJsonStr(bizContent));

        return alipayClient.pageExecute(request).getBody();
    }

    /**
     * 接口 2：支付宝同步跳转回应用
     */
    @GetMapping("/return")
    public void payReturn(HttpServletRequest request, HttpServletResponse response) throws Exception {
        // 同步返回不可信，只负责跳转；订单状态仅由验签后的异步通知更新。
        String frontendUrl = alipayProperties.getFrontendUrl();
        response.sendRedirect(frontendUrl + "/#/pages/order/list?type=bought");
    }

    /**
     * 接口 3：支付宝异步回调 (部署到公网服务器后使用)
     */
    @PostMapping("/notify")
    public String payNotify(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        Map<String, String[]> requestParams = request.getParameterMap();
        for (String name : requestParams.keySet()) {
            String[] values = requestParams.get(name);
            String valueStr = "";
            for (int i = 0; i < values.length; i++) {
                valueStr = (i == values.length - 1) ? valueStr + values[i] : valueStr + values[i] + ",";
            }
            params.put(name, valueStr);
        }

        boolean signVerified;
        try {
            signVerified = AlipaySignature.rsaCheckV1(
                        params,
                        alipayProperties.getPublicKey(),
                        alipayProperties.getCharset(),
                        alipayProperties.getSignType()
                );
        } catch (AlipayApiException e) {
            return "failure";
        }

        if (signVerified) {
            String tradeStatus = params.get("trade_status");
            String outTradeNo = params.get("out_trade_no");
            OrderInfo order = orderService.getOne(new QueryWrapper<OrderInfo>().eq("order_no", outTradeNo));
            String totalAmount = params.get("total_amount");
            String appId = params.get("app_id");
            boolean matchesOrder = order != null && totalAmount != null
                    && order.getPayAmount().compareTo(new java.math.BigDecimal(totalAmount)) == 0
                    && alipayProperties.getAppId().equals(appId);
            if ("TRADE_SUCCESS".equals(tradeStatus) && matchesOrder) {
                orderService.payOrderByCallback(outTradeNo);
            } else {
                return "failure";
            }
            return "success";
        } else {
            return "failure";
        }
    }
}
