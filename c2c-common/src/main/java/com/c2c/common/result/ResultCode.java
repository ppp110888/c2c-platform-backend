package com.c2c.common.result;

import lombok.Getter;

@Getter
public enum ResultCode {
    // --- 通用状态码 ---
    SUCCESS(200, "操作成功"),
    FAILED(500, "操作失败"),
    VALIDATE_FAILED(400, "参数检验失败"),
    UNAUTHORIZED(401, "暂未登录或Token已过期"),
    FORBIDDEN(403, "没有相关权限"),
    BUSINESS_ERROR(500, "业务执行异常"),

    // --- 用户模块特定状态码 (5001xx) ---
    USER_NOT_EXIST(500101, "用户不存在"),
    PASSWORD_ERROR(500102, "密码错误"),
    ACCOUNT_LOCKED(500103, "账号已被冻结"),

    // --- 商品模块特定状态码 (5002xx) ---
    ITEM_STOCK_LACK(500201, "商品不存在或库存不足"); // 🚨 新增：专门用于扣库存失败的专属错误码

    private final Integer code;
    private final String message;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}