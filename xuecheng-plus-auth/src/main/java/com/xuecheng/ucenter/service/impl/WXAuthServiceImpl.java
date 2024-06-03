package com.xuecheng.ucenter.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.ucenter.mapper.XcUserMapper;
import com.xuecheng.ucenter.mapper.XcUserRoleMapper;
import com.xuecheng.ucenter.model.dto.AuthParamsDto;
import com.xuecheng.ucenter.model.dto.XcUserExt;
import com.xuecheng.ucenter.model.po.XcUser;
import com.xuecheng.ucenter.model.po.XcUserRole;
import com.xuecheng.ucenter.service.AuthService;
import com.xuecheng.ucenter.service.WxAuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * @author Mr.M
 * @version 1.0
 * @description 账号密码认证
 * @date 2022/9/29 12:12
 */
@Slf4j
@Service("wx_authservice")
public class WXAuthServiceImpl implements AuthService, WxAuthService {

    @Resource
    private XcUserMapper userMapper;

    @Resource
    private RestTemplate restTemplate;

    @Resource
    private XcUserRoleMapper userRoleMapper;

    @Resource
    private WXAuthServiceImpl currentProxy;

    @Value("${weixin.appid}")
    String appid;
    @Value("${weixin.secret}")
    String secret;

    @Override
    public XcUserExt execute(AuthParamsDto authParamsDto) {
        //账号
        String username = authParamsDto.getUsername();
        XcUser user = userMapper.selectOne(new LambdaQueryWrapper<XcUser>().eq(XcUser::getUsername, username));
        if (user == null) {
            //返回空表示用户不存在
            throw new RuntimeException("账号不存在");
        }
        XcUserExt xcUserExt = new XcUserExt();
        BeanUtils.copyProperties(user, xcUserExt);
        return xcUserExt;
    }

    @Override
    public XcUser wxAuth(String code) {
        //拿到授权码，根据授权码获取令牌
        Map<String, String> map = getAccess_token(code);

        //根据令牌获取用户信息
        String token = map.get("access_token");
        String openid = map.get("openid");
        Map<String, String> userInfoFromWX = getUserInfoByToken(token, openid);

        //将用户信息保存到数据库
        //非事务方法调用事务要先获取当前代理对象再调用
        XcUser xcUser = currentProxy.saveUser(userInfoFromWX);

        return xcUser;
    }

    /**
     * url: https://api.weixin.qq.com/sns/oauth2/access_token?appid=APPID&secret=SECRET&code=CODE&grant_type=authorization_code
     * 授权码，根据授权码获取令牌
     *
     * @param code 授权码
     * @return 微信返回的token信息
     * {
     * "access_token":"ACCESS_TOKEN",
     * "expires_in":7200,
     * "refresh_token":"REFRESH_TOKEN",
     * "openid":"OPENID",
     * "scope":"SCOPE",
     * "unionid": "o6_bmasdasdsad6_2sgVt7hMZOPfL"
     * }
     */
    private Map<String, String> getAccess_token(String code) {
        String wxUrl = "https://api.weixin.qq.com/sns/oauth2/access_token?"
                + "appid=" + appid +
                "&secret=" + secret +
                "&code=" + code +
                "&grant_type=authorization_code";
        ResponseEntity<String> exchange = restTemplate.exchange(wxUrl, HttpMethod.POST, null, String.class);
        log.info("调用微信接口申请access_token, url:{}", wxUrl);
        String json = exchange.getBody();
        Map<String, String> tokenInfo = JSON.parseObject(json, Map.class);

        return tokenInfo;
    }

    /**
     * https://api.weixin.qq.com/sns/userinfo?access_token=令牌&openid=openid
     *
     * @return
     */
    private Map<String, String> getUserInfoByToken(String token, String openId) {
        String wxUrl = "https://api.weixin.qq.com/sns/userinfo?" +
                "access_token=" + token +
                "&openid=" + openId;
        ResponseEntity<String> exchange = restTemplate.exchange(wxUrl, HttpMethod.POST, null, String.class);
        log.info("调用微信接口申请access_token, url:{}", wxUrl);

        //防止乱码进行转码
        String result = new String(exchange.getBody().getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
        Map<String, String> userInfo = JSON.parseObject(result, Map.class);

        return userInfo;
    }

    /**
     * {"openid":"o3_SC590unwqRf-pw09t8162k93o",
     * "nickname":"伟川",
     * "sex":0,
     * "language":"","city":"","province":"","country":"",
     * "headimgurl":"https:\/\/thirdwx.qlogo.cn\/mmopen\/vi_32\/MibmPQ6Aq9TOUjOjH9qibAwyQyY25JDB3IAjfvR8Yk9XHcsvpclP9ibica809GVWFRmOsDAicRNFhDpc9L8gYvQia5Gw\/132","
     * privilege":[],
     * "unionid":"oWgGz1JlEqXM9TSPCEXyr6xerC9Q"}
     * 查询用户信息是否存在，不存在则保存用户信息和用户角色信息
     * @param userInfo
     */
    @Transactional
    public XcUser saveUser(Map<String,String> userInfo){
        String nickname = userInfo.get("nickname");
        String headimgurl = userInfo.get("headimgurl");
        //将sex转换成string字符串
        String sex = String.valueOf(userInfo.get("sex"));
        String unionid = userInfo.get("unionid");

        //查询用户信息是否存在,存在直接返回即可,不存在则保存用户信息和用户角色信息
        XcUser userFromDB = userMapper.selectOne(new LambdaQueryWrapper<XcUser>().eq(XcUser::getWxUnionid, unionid));
        if (userFromDB != null){
            return userFromDB;
        }
        //保存到xc_user
        XcUser user = new XcUser();
        user.setUsername(unionid);
        user.setPassword(unionid);
        user.setName(nickname);
        user.setNickname(nickname);
        user.setUserpic(headimgurl);
        user.setSex(sex);
        user.setWxUnionid(unionid);
        user.setUtype("101001");//学生类型
        user.setStatus("1");//用户状态
        user.setCreateTime(LocalDateTime.now());
        userMapper.insert(user);

        //保存到xc_user_role
        XcUserRole userRole = new XcUserRole();
        userRole.setUserId(user.getId());
        userRole.setRoleId("17");
        userRole.setCreateTime(LocalDateTime.now());

        //当user插入后会返回user的id
        userRoleMapper.insert(userRole);
        return user;
    }
}
