package com.xuecheng.orders.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.base.exception.XuechengPlusException;
import com.xuecheng.base.utils.IdWorkerUtils;
import com.xuecheng.base.utils.QRCodeUtil;
import com.xuecheng.messagesdk.model.po.MqMessage;
import com.xuecheng.messagesdk.service.MqMessageService;
import com.xuecheng.orders.config.PayNotifyConfig;
import com.xuecheng.orders.mapper.XcOrdersGoodsMapper;
import com.xuecheng.orders.mapper.XcOrdersMapper;
import com.xuecheng.orders.mapper.XcPayRecordMapper;
import com.xuecheng.orders.model.dto.AddOrderDto;
import com.xuecheng.orders.model.dto.PayRecordDto;
import com.xuecheng.orders.model.dto.PayStatusDto;
import com.xuecheng.orders.model.po.XcOrders;
import com.xuecheng.orders.model.po.XcOrdersGoods;
import com.xuecheng.orders.model.po.XcPayRecord;
import com.xuecheng.orders.service.OrderService;
import com.xuecheng.orders.service.PayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.concurrent.FailureCallback;
import org.springframework.util.concurrent.SuccessCallback;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    @Resource
    private XcOrdersGoodsMapper ordersGoodsMapper;

    @Resource
    private XcOrdersMapper ordersMapper;

    @Value("${pay.qrcodeurl}")
    String qrcodeurl;

    @Value("${pay.alipay.APP_ID}")
    String APP_ID;

    @Resource
    private OrderServiceImpl currentProxy;

    @Resource
    private PayService payService;

    @Resource
    private XcPayRecordMapper payRecordMapper;

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Resource
    private MqMessageService mqMessageService;

    @Override
    public PayRecordDto createOrder(String userId, AddOrderDto addOrderDto) {
        //1.生成订单信息，将信息保存到订单表，订单详情表
        XcOrders xcOrders = saveXcOrders(userId, addOrderDto);
        //2.生成订单记录信息
        XcPayRecord payRecord = createPayRecord(xcOrders);
        //3.生成二维码
        String qrCode = createQRCode(qrcodeurl, payRecord.getPayNo());
        //4.准备返回数据
        PayRecordDto payRecordDto = new PayRecordDto();
        BeanUtils.copyProperties(payRecord, payRecordDto);
        payRecordDto.setQrcode(qrCode);
        return payRecordDto;
    }

    @Override
    public XcPayRecord getPayRecordByPayno(String payNo) {
        return payRecordMapper.selectOne(
                new LambdaQueryWrapper<XcPayRecord>()
                        .eq(XcPayRecord::getPayNo, payNo));
    }


    @Override
    public PayRecordDto queryPayResult(String payNo) {
        //查询订单支付记录
        XcPayRecord payRecord = getPayRecordByPayno(payNo);
        if (payRecord == null) {
            XuechengPlusException.cast("请重新点击支付获取二维码");
        }
        //支付状态
        String status = payRecord.getStatus();
        //如果支付成功直接返回
        if ("601002".equals(status)) {
            PayRecordDto payRecordDto = new PayRecordDto();
            BeanUtils.copyProperties(payRecord, payRecordDto);
            return payRecordDto;
        }
        //没有支付成功就查询支付宝接口
        PayStatusDto payStatus = payService.queryPayResultFromAlipay(payNo);
        //保存支付结果
        currentProxy.saveAliPayStatus(payStatus);
        //重新查询支付记录
        payRecord = getPayRecordByPayno(payNo);
        PayRecordDto payRecordDto = new PayRecordDto();
        BeanUtils.copyProperties(payRecord, payRecordDto);
        return payRecordDto;
    }

    //Todo 保存订单支付结果接口
    @Transactional
    @Override
    public void saveAliPayStatus(PayStatusDto payStatus) {
        //校验支付结果信息
        verifyPayResult(payStatus);
        //保存支付成功状态到订单支付记录表
        XcPayRecord xcPayRecordNew = savePayResult2OrdersReCord(payStatus);
        //保存支付成功状态到订单表
        XcOrders xcOrders = savePayResult2Orders(xcPayRecordNew);

        //把订单支付成功的消息发送给mq,让学习中心更新用户的课程表
        //1.先将消息保存到数据库
        MqMessage mqMessage = mqMessageService.addMessage(
                "payresult_notify",
                xcOrders.getOutBusinessId(),
                xcOrders.getOrderType(),
                null);
        //2.发送消息到mq
        notifyPayResult(mqMessage);
    }

    @Transactional
    @Override
    public void paynotify(HttpServletRequest request, HttpServletResponse response) {
        //调用支付服务查询支付宝返回的支付信息，并提取出所需要的信息
        PayStatusDto payStatusDto = payService.paynotify(request, response);
        currentProxy.saveAliPayStatus(payStatusDto);
    }

    @Override
    public void notifyPayResult(MqMessage message) {
        //1.消息体，转json
        String msg = JSON.toJSONString(message);
        //设置消息持久化
        Message msgObj = MessageBuilder.withBody(msg.getBytes(StandardCharsets.UTF_8))
                .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                .build();
        //2.全局唯一的消息ID,需要封装到CorrelationData中
        CorrelationData correlationData = new CorrelationData(message.getId().toString());
        //3.添加callback
        correlationData.getFuture().addCallback(
                result -> {
                    if (result.isAck()) {
                        // 3.1.ack，消息成功
                        log.debug("通知支付结果消息发送成功, ID:{}", correlationData.getId());
                        //删除消息表中的记录
                        mqMessageService.completed(message.getId());
                    } else {
                        // 3.2.nack，消息失败
                        log.error("通知支付结果消息发送失败, ID:{}, 原因{}", correlationData.getId(), result.getReason());
                    }
                },
                ex -> log.error("消息发送异常, ID:{}, 原因{}", correlationData.getId(), ex.getMessage())
        );
        //4.发送消息
        rabbitTemplate.convertAndSend(PayNotifyConfig.PAYNOTIFY_EXCHANGE_FANOUT, "", msgObj,correlationData);
    }

    private void verifyPayResult(PayStatusDto payStatus) {
        //支付流水号
        String payNo = payStatus.getOut_trade_no();
        XcPayRecord payRecord = getPayRecordByPayno(payNo);
        if (payRecord == null) {
            XuechengPlusException.cast("支付记录找不到");
        }
        log.debug("收到支付结果:{},支付记录:{}}", payStatus.toString(), payRecord.toString());

        //支付不成功直接返回
        if (!payStatus.getTrade_status().equals("TRADE_SUCCESS")) {
            throw new XuechengPlusException("支付失败，请重试！");
        }
        //校验
        //支付金额变为分
        Float totalPrice = payRecord.getTotalPrice() * 100;
        Float total_amount = Float.parseFloat(payStatus.getTotal_amount()) * 100;

        //校验是否一致,不一致直接返回
        if (!payStatus.getApp_id().equals(APP_ID)
                || totalPrice.intValue() != total_amount.intValue()) {
            throw new XuechengPlusException("校验支付结果失败");
        }
    }

    private XcPayRecord savePayResult2OrdersReCord(PayStatusDto payStatus) {
        String payNo = payStatus.getOut_trade_no(); //商户订单号
        log.debug("更新支付结果,支付交易流水号:{},支付结果:{}", payNo, payStatus.getTrade_status());

        XcPayRecord xcPayRecordNew = new XcPayRecord();
        xcPayRecordNew.setStatus("601002");//支付成功
        xcPayRecordNew.setOutPayChannel("Alipay");
        xcPayRecordNew.setOutPayNo(payStatus.getTrade_no());//支付宝交易号
        xcPayRecordNew.setPaySuccessTime(LocalDateTime.now());//通知时间

        //保存支付结果
        int update = payRecordMapper.update(xcPayRecordNew,
                new LambdaQueryWrapper<XcPayRecord>().eq(XcPayRecord::getPayNo, payNo));
        if (update > 0) {
            log.info("更新支付记录状态成功:{}", xcPayRecordNew.toString());
        } else {
            log.info("更新支付记录状态失败:{}", xcPayRecordNew.toString());
        }
        //将完整的订单支付记录返回
        XcPayRecord result = payRecordMapper.selectOne(new LambdaQueryWrapper<XcPayRecord>().eq(XcPayRecord::getPayNo, payNo));
        return result;
    }

    private XcOrders savePayResult2Orders(XcPayRecord xcPayRecordNew) {
        Long orderId = xcPayRecordNew.getOrderId();
        //查询订单信息
        XcOrders xcOrder = ordersMapper.selectOne(new LambdaQueryWrapper<XcOrders>()
                .eq(XcOrders::getId, orderId));
        if (xcOrder == null) {
            log.info("根据支付记录[{}}]找不到订单", xcPayRecordNew.toString());
            XuechengPlusException.cast("根据支付记录找不到订单");
        }
        XcOrders newOrder = new XcOrders();
        newOrder.setStatus("601002");//支付成功
        int update = ordersMapper.update(newOrder, new LambdaQueryWrapper<XcOrders>().eq(XcOrders::getId, orderId));
        if (update > 0) {
            log.info("更新订单表状态成功,订单号:{}", orderId);
        } else {
            log.info("更新订单表状态失败,订单号:{}", orderId);
            XuechengPlusException.cast("更新订单表状态失败");
        }
        return xcOrder;
    }

    private XcOrders saveXcOrders(String userId, AddOrderDto dto) {
        //如果是重新下单，就可以直接返回，然后生成新的支付记录
        XcOrders order = getOrderByBusinessId(dto.getOutBusinessId());
        if (order != null) {
            return order;
        }

        //新订单生成
        //Todo 从前端获取订单金额不是很合理，先和教程一致的写
        String desc = dto.getOrderDescrip();
        String orderType = dto.getOrderType();
        String detail = dto.getOrderDetail();
        String orderName = dto.getOrderName();
        Float price = dto.getTotalPrice();
        String businessId = dto.getOutBusinessId();

        //给订单赋值
        XcOrders xcOrders = new XcOrders();
        xcOrders.setTotalPrice(price);
        xcOrders.setCreateDate(LocalDateTime.now());
        xcOrders.setStatus("601001");//未支付
        xcOrders.setUserId(userId);
        xcOrders.setOrderType(orderType);
        xcOrders.setOrderName(orderName);
        xcOrders.setOrderDescrip(desc);
        xcOrders.setOrderDetail(detail);//保存到订单详情表
        xcOrders.setOutBusinessId(businessId);//选课记录id
        //插入到订单表
        ordersMapper.insert(xcOrders);
        //插入到订单详情表
        saveOrderDetail(xcOrders.getId(), xcOrders.getOrderDetail());
        //将订单信息返回
        return xcOrders;
    }

    //保存到订单详情表
    private void saveOrderDetail(Long orderId, String orderDetailJson) {
        //将json数据转化成object
        List<XcOrdersGoods> goods = JSON.parseArray(orderDetailJson, XcOrdersGoods.class);
        //将信息保存到订单详情表中
        for (XcOrdersGoods good : goods) {
            good.setOrderId(orderId);
            ordersGoodsMapper.insert(good);
        }
    }

    //创建支付记录的原因是可以让用户在支付失败后可以进行重新下单
    //所以用户在支付记录中一个订单可以有多个支付记录
    public XcPayRecord createPayRecord(XcOrders orders) {
        if (orders == null) {
            XuechengPlusException.cast("订单不存在");
        }

        //防止重复支付问题
        if (orders.getStatus().equals("601002")) {
            throw new XuechengPlusException("订单已支付");
        }
        Long ordersId = orders.getId();
        String orderName = orders.getOrderName();
        Float price = orders.getTotalPrice();
        String userId = orders.getUserId();

        XcPayRecord payRecord = new XcPayRecord();
        //利用雪花算法生成id
        long payNo = IdWorkerUtils.getInstance().nextId();
        payRecord.setPayNo(payNo);
        payRecord.setOrderId(ordersId);
        payRecord.setOrderName(orderName);
        payRecord.setTotalPrice(price);
        payRecord.setCurrency("CNY");//币种
        payRecord.setCreateDate(LocalDateTime.now());
        payRecord.setStatus("601001");//未支付
        payRecord.setUserId(userId);

        payRecordMapper.insert(payRecord);
        return payRecord;
    }

    //根据业务id查询订单，这个业务id是课程记录表的id
    public XcOrders getOrderByBusinessId(String businessId) {
        XcOrders orders = ordersMapper.selectOne(new LambdaQueryWrapper<XcOrders>().eq(XcOrders::getOutBusinessId, businessId));
        return orders;
    }

    //生成二维码，把订单记录表id存到到二维码中
    private String createQRCode(String qrcodeurl, long payNo) {
        //url要可以被访问，url为下单接口
        String url = String.format(qrcodeurl, payNo);
        String qrCode = null;
        try {
            qrCode = QRCodeUtil.createQRCode(url, 200, 200);
        } catch (IOException e) {
            throw new XuechengPlusException("生成二维码出错");
        }
        return qrCode;
    }
}
