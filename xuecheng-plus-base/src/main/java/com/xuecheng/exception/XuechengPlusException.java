package com.xuecheng.exception;

import lombok.Data;

/**
 * @author zengweichuan
 * @description
 * @date 2024/3/31
 */
@Data
public class XuechengPlusException extends RuntimeException{

    private String errCode;
    private String errMessage;
    public XuechengPlusException(){
        super();
    }
    public XuechengPlusException(String errMessage){
        super(errMessage);
        this.errMessage = errMessage;
    }
    public XuechengPlusException(String errCode,String errMessage){
        super(errMessage);
        this.errMessage = errMessage;
        this.errCode = errCode;
    }

    public static void cast(CommonError commonError){
        throw new XuechengPlusException(commonError.getErrMessage());
    }

    public static void cast(String errMessage){
        throw new XuechengPlusException(errMessage);
    }
}
