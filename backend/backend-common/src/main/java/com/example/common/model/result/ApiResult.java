package com.example.common.model.result;

import com.example.common.constant.CommonConstants;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一接口返回对象。
 *
 * @param <T> 返回数据类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResult<T> {

    private Integer code;
    private String message;
    private Boolean success;
    private T data;

    public static <T> ApiResult<T> success(T data) {
        return new ApiResult<>(CommonConstants.SUCCESS_CODE, "操作成功", Boolean.TRUE, data);
    }

    public static <T> ApiResult<T> success(String message, T data) {
        return new ApiResult<>(CommonConstants.SUCCESS_CODE, message, Boolean.TRUE, data);
    }

    public static <T> ApiResult<T> fail(String message) {
        return new ApiResult<>(CommonConstants.ERROR_CODE, message, Boolean.FALSE, null);
    }

    public static <T> ApiResult<T> fail(Integer code, String message) {
        return new ApiResult<>(code, message, Boolean.FALSE, null);
    }
}
