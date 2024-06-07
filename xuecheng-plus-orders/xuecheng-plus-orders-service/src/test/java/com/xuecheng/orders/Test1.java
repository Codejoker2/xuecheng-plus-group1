package com.xuecheng.orders;

import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @description TODO
 * @author Mr.M
 * @date 2022/10/2 10:32
 * @version 1.0
 */
 @SpringBootTest
public class Test1 {

     //测试查询支付宝订单接口

  @Test
 public void test() throws AlipayApiException {
      AlipayClient alipayClient = new DefaultAlipayClient(
              "https://openapi-sandbox.dl.alipaydev.com/gateway.do",
              "9021000137685443",
              "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQC7N3PSHpUhGvRo0gZofv843n9JDLmRdPYZS/qXT4IJ+pqoPWZSCtukK3ixffKY8TeDEVoDxyCU6Gztv7cLORjfwoq+ZnWLPpOGuKFMgF9BkTXGWw21OLP9JRb2D3+qro8rCzkau4IJ41bwoixPFgKM1oY4lxs4Xq2DZOd/igY/f+UKCAzENJRv8A28QfzcD3xFM6YGPeBZEcTaWNguLaJydIODtXsX9mYHwZVOY/HrCLYBniYcdOKUq3m2rYJFb5HjIvnpfgx70X+trVF5ZMtQoelWnuZ8Qa4ERbc8qs7fY1RmAHF+VPtfI7muJDz4e12mwAYjIyoaWvY00/A4wpXFAgMBAAECggEAezXccK2V4cq2jYGjnAPwfgHYbUAFpJgAGtfpHMnDwEOAozZ9b4Mb2CiP7uf/z+34Zbb2scPMSpPIlJN2qNkz6JdE7lfKBhDx0FkUNp/JkQI7DMcB94FQ20ocmWXL1I27RU3GkDRVOaK3pfhlBstqdDv+66Fu1aNhuDahMEurHwAueHdE7g8hqNwBRhDd0MulKH//pItS8tPIE3sz2AwRIuDiA/QU4ust+kVk38iPn8mmxEujEDiGQLfG4sl0bmOL4OmIaXKSaSJkwVE4Lpj3IotTobHQbn/ma/GSxMy5KEkBK6dpqUdwFPHe9Q8RMLN9OCTpt4APnw/xT/cAfYyo5QKBgQD9onNlkVXn7hcS9ehA5ynydmYIfEOMBHOPrp2YeUdCi1hu2bG0QaMi3frUQQf5lWx1xC0ejRuu4EeT1frKQdMQ8Vav9FUD/TUUUB6r+Wxnj14ghvLG3TrXnud+Jedy7lzziJGgR3Ev6LAZVmXVS+N42UlfEM2xjzwi51kAoIirJwKBgQC89m38zx54IhyJHIIPX/Tt2SXNhH/qKDeCCGPv5c7kn+0e05X0cF3PaIFnaiUWwFVQSKIgBUaJylPKp5RmQsCYZgXOlyG1sQQBN+c0qbgTQAAyaGxe3fU1tShid+Yon7oI074qcvqyhpoo2gCGH8uoKy1EoWVywXSpOJ8P4UW7MwKBgQDUWypQ9LE/YZnSFy+g/6qgRb9CBYCsf6GdwC5U1d3y+iZNkVj2uinzbHWxNs2NiyznIRxBwxVVgpSLU5IJu7kTK21QHrr7fwns+vPjOw8nWeMOSs6D8ABvPa1Gckfpc2dBw6thzc8XtLOUU0Epp8NJkGuc+72sl2dbNeXP35jt6wKBgCmyv6awFeswq9dd4R1/cU8jQgJOIOQNJ+Kb82G29qbg27SXwFmGik0ApL2rYK/alXj37j7eqygw67imrkq6pEF9Ef76vseXXmjeazjl0ub8ko6NGIz+seO+LEE+cB260ZStFSPM56GJrm8hXEg8r/ZWQNEKRJbENfg5tHAOqBKdAoGAFB/OIrxeGrGkw7/H17QXmatBgVByWpSFumoFTXXjF3MtY9LW5uRELNRK00NLiWWY5Nz/B5PnmN+tImbC9MYnfrUJt3X8liDbAUf5LTxrEwv6utrB8c1BcAe+qsJaygf9gI37ZAfdrGyDycYXkSEPymBW5Q5CaboxWdL6vdHUKGQ=",
              "json",
              "GBK",
              "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAuSsBtWAdET6E5/jR16t5NlLM6OErhg/u/iwG//hO1N0xGLqB2taDPTwc5YtsXTlHQ+s1UtoqFHVO/qcrsIqyiZQmDzVxfswZh2Bm80MaWyyH8G4jiKANOTHflon7ZvJPkJYxI4lZiNNNxI1Ahfe/pa/zjhx5xtwoabKjdnqFVPbXm1eOH8gDmmVo8pqccpQo3lxc87KMale4+Hy6Yoe2cMsrfXwnt36GbN/aQQj/AsN4JCOm/SfE005xchYXWFcJtQaJquxhEekluYKQ0MarPUBMzpss4Vx87G6APP2SI7Oaf9Oqwk94mX6bjopI0eFg1lPXgdRM6DYcxgREe1QdNQIDAQAB",
              "RSA2");
      AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
      JSONObject bizContent = new JSONObject();
      bizContent.put("out_trade_no", "202210100010101001");
//bizContent.put("trade_no", "202210100010101001");
      request.setBizContent(bizContent.toString());
      AlipayTradeQueryResponse response = alipayClient.execute(request);
      System.out.println(response.getTradeStatus());
      if(response.isSuccess()){
          System.out.println("调用成功");
      } else {
          System.out.println("调用失败");
      }
  }

}
