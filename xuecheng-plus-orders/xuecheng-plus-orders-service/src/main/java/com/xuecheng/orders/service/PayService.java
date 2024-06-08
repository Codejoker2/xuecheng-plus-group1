package com.xuecheng.orders.service;

import com.alipay.api.AlipayApiException;
import com.xuecheng.orders.model.dto.PayRecordDto;
import com.xuecheng.orders.model.dto.PayStatusDto;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 下单支付接口
 */
public interface PayService {
    public void requestpay(String payNo, HttpServletResponse httpResponse);

    /**
     * 查询订单支付结果
     * @param payNo 订单支付记录id
     * @return
     */
    public PayStatusDto queryPayResultFromAlipay(String payNo);

    public PayStatusDto paynotify(HttpServletRequest request, HttpServletResponse response) ;
}
