package com.xuecheng.orders.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeWapPayRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.xuecheng.base.exception.XuechengPlusException;
import com.xuecheng.orders.config.AlipayConfig;
import com.xuecheng.orders.model.dto.PayStatusDto;
import com.xuecheng.orders.model.po.XcPayRecord;
import com.xuecheng.orders.service.OrderService;
import com.xuecheng.orders.service.PayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Service
@Slf4j
public class PayServiceImpl implements PayService {

    @Value("${pay.alipay.APP_ID}")
    String APP_ID;
    @Value("${pay.alipay.APP_PRIVATE_KEY}")
    String APP_PRIVATE_KEY;

    @Value("${pay.alipay.ALIPAY_PUBLIC_KEY}")
    String ALIPAY_PUBLIC_KEY;

    @Value("${pay.alipay.NOTIFY_URL}")
    String NOTIFY_URL;

    @Resource
    private OrderService orderService;

    @Override
    public void requestpay(String payNo, HttpServletResponse httpResponse) {
        //查询订单支付记录
        XcPayRecord payRecord = orderService.getPayRecordByPayno(payNo);

        //校验支付信息
        verifyBeforePay(payRecord);

        //支付
        pay(payRecord, httpResponse);
    }

    @Override
    public PayStatusDto queryPayResultFromAlipay(String payNo) {
        AlipayClient alipayClient = new DefaultAlipayClient(
                AlipayConfig.URL,
                APP_ID,
                APP_PRIVATE_KEY,
                AlipayConfig.FORMAT,
                AlipayConfig.CHARSET,
                ALIPAY_PUBLIC_KEY,
                AlipayConfig.SIGNTYPE);

        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", payNo);
        //bizContent.put("trade_no", "202210100010101001");

        request.setBizContent(bizContent.toString());
        AlipayTradeQueryResponse response = null;

        try {
            response = alipayClient.execute(request);
        } catch (AlipayApiException e) {
            log.error("支付宝api调用查询支付结果出错：{}", e.getMessage());
            throw new XuechengPlusException("支付宝api调用查询支付结果出错：" + e.getMessage());
        }

        //获取支付结果
        String resultJson = response.getBody();
        //转map
        Map resultMap = JSON.parseObject(resultJson, Map.class);
        Map alipay_trade_query_response = (Map) resultMap.get("alipay_trade_query_response");
        //支付结果
        String trade_status = (String) alipay_trade_query_response.get("trade_status");
        String total_amount = (String) alipay_trade_query_response.get("total_amount");
        String trade_no = (String) alipay_trade_query_response.get("trade_no");
        //保存支付结果
        PayStatusDto payStatusDto = new PayStatusDto();
        payStatusDto.setOut_trade_no(payNo);
        payStatusDto.setTrade_status(trade_status);
        payStatusDto.setApp_id(APP_ID);
        payStatusDto.setTrade_no(trade_no);
        payStatusDto.setTotal_amount(total_amount);
        return payStatusDto;
    }

    @Override
    public PayStatusDto paynotify(HttpServletRequest request, HttpServletResponse response) {
        Map<String, String> params = new HashMap<>();
        Map requestParams = request.getParameterMap();
        for (Iterator iter = requestParams.keySet().iterator(); iter.hasNext(); ) {
            String name = (String) iter.next();
            String[] values = (String[]) requestParams.get(name);
            String valueStr = "";
            for (int i = 0; i < values.length; i++) {
                valueStr = (i == values.length - 1) ? valueStr + values[i]
                        : valueStr + values[i] + ",";
            }
            params.put(name, valueStr);
        }
        try {
            //获取支付宝的通知返回参数，可参考技术文档中页面跳转同步通知参数列表(以上仅供参考)//
            //计算得出通知验证结果
            //boolean AlipaySignature.rsaCheckV1(Map<String, String> params, String publicKey, String charset, String sign_type)
            boolean verify_result = AlipaySignature.rsaCheckV1(params, ALIPAY_PUBLIC_KEY, AlipayConfig.CHARSET, "RSA2");

            if (verify_result) {//验证成功
                //商户订单号
                String out_trade_no = new String(request.getParameter("out_trade_no").getBytes("ISO-8859-1"), "UTF-8");
                //支付宝交易号
                String trade_no = new String(request.getParameter("trade_no").getBytes("ISO-8859-1"), "UTF-8");
                //支付金额
                String total_amount = new String(request.getParameter("total_amount").getBytes("ISO-8859-1"), "UTF-8");
                //交易状态
                String trade_status = new String(request.getParameter("trade_status").getBytes("ISO-8859-1"), "UTF-8");
                if (trade_status.equals("TRADE_SUCCESS")) {//交易成功
                    PayStatusDto payStatusDto = new PayStatusDto();
                    payStatusDto.setOut_trade_no(out_trade_no);
                    payStatusDto.setTrade_status(trade_status);
                    payStatusDto.setApp_id(APP_ID);
                    payStatusDto.setTrade_no(trade_no);
                    payStatusDto.setTotal_amount(total_amount);

                    //返回数据
                    return payStatusDto;
                }
                response.getWriter().write("success");
            } else {
                response.getWriter().write("fail");
            }
        } catch (Exception e) {
            throw new XuechengPlusException("获取订单支付信息出错：", e.getMessage());
        }
        return null;
    }

    private void verifyBeforePay(XcPayRecord payRecord) {
        //如果payNo不存在则提示重新发起支付
        if (payRecord == null) {
            XuechengPlusException.cast("请重新点击支付获取二维码");
        }
        //支付状态
        //Todo 使用乐观锁的方式，如果同时多个支付一个订单仍然会重复订单，
        // 使用乐观锁的话如果更新订单信息不成功就进行退款操作
        // 要么就是用分布式锁，抢锁失败就返回错误信息
        // 支付宝支付也在控制重复支付问题，没事了
        String status = payRecord.getStatus();
        if ("601002".equals(status)) {
            XuechengPlusException.cast("订单已支付，请勿重复支付。");
        }
    }

    //调用支付宝支付接口
    void pay(XcPayRecord payRecord, HttpServletResponse httpResponse) {
        Float price = payRecord.getTotalPrice();
        String orderName = payRecord.getOrderName();
        Long payNo = payRecord.getPayNo();

        AlipayClient alipayClient = new DefaultAlipayClient(
                AlipayConfig.URL,
                APP_ID,
                APP_PRIVATE_KEY,
                AlipayConfig.FORMAT,
                AlipayConfig.CHARSET,
                ALIPAY_PUBLIC_KEY,
                AlipayConfig.SIGNTYPE);
        //获得初始化的AlipayClient
        AlipayTradeWapPayRequest alipayRequest = new AlipayTradeWapPayRequest();//创建API对应的request

        alipayRequest.setNotifyUrl(NOTIFY_URL);//在公共参数中设置回跳和通知地址
        //准备支付所需数据
        HashMap<String, String> map = new HashMap<>();
        map.put("out_trade_no", payNo.toString());
        map.put("total_amount", price.toString());
        map.put("subject", orderName);
        map.put("product_code", "QUICK_WAP_WAY");
        String bizContent = JSON.toJSONString(map);

        alipayRequest.setBizContent(bizContent);
//        alipayRequest.setBizContent("{" +
//                "    \"out_trade_no\":\"" + payNo + "\"," +
//                "    \"total_amount\":" + price +
//                "    \"subject\":\"" + orderName + "\"," +
//                "    \"product_code\":\"QUICK_WAP_WAY\"" +
//                "  }");//填充业务参数

        try {
            String form = alipayClient.pageExecute(alipayRequest).getBody(); //调用SDK生成表单
            httpResponse.setContentType("text/html;charset=" + AlipayConfig.CHARSET);
            httpResponse.getWriter().write(form);//直接将完整的表单html输出到页面
            httpResponse.getWriter().flush();
        } catch (Exception e) {
            log.error("调用支付宝接口报错：{}", e.getMessage());
            throw new XuechengPlusException("调用支付宝接口报错：" + e.getMessage());
        }
    }
}
