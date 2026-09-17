package com.c2c.common.result;

import lombok.Data;

@Data
public class Result<T> {
    private Integer code;
    private String message;
    private T data;

    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(ResultCode.SUCCESS.getCode());
        result.setMessage(ResultCode.SUCCESS.getMessage());
        result.setData(data);
        return result;
    }

    public static <T> Result<T> failed(ResultCode resultCode) {
        Result<T> result = new Result<>();
        result.setCode(resultCode.getCode());
        result.setMessage(resultCode.getMessage());
        return result;
    }

    public static <T> Result<T> failed(Integer code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }

    /**
     * 专门用于业务抛出自定义报错信息的重载方法
     */
    public static <T> Result<T> failed(String message) {
        // 具体写法取决于您的 Result 类的属性名，通常长这样：
        Result<T> result = new Result<>();
        result.setCode(500); // 或者用您定义好的默认失败状态码，如 ResultCode.FAILED.getCode()
        result.setMessage(message);
        return result;
    }
}