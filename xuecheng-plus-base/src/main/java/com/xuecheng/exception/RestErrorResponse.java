package com.xuecheng.exception;

import lombok.Data;

import java.io.Serializable;

/**
 * @author zengweichuan
 * @description 错误相应参数包装
 * @date 2024/3/31
 */
@Data
public class RestErrorResponse implements Serializable {
    private String errMessage;

    public RestErrorResponse(String errMessage){
        this.errMessage = errMessage;
    }
}
