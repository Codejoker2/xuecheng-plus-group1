package com.xuecheng.exception;

/**
 * @author zengweichuan
 * @description
 * @date 2024/3/31
 */
public class XuechengPlusException extends RuntimeException{

    private String errMessage;
    public XuechengPlusException(){
        super();
    }
    public XuechengPlusException(String errMessage){
        super(errMessage);
        this.errMessage = errMessage;
    }

    public String getErrMessage(){
        return errMessage;
    }

    public static void cast(CommonError commonError){
        throw new XuechengPlusException(commonError.getErrMessage());
    }

    public static void cast(String errMessage){
        throw new XuechengPlusException(errMessage);
    }
}
