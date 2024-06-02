package com.xuecheng.ucenter.service.impl;


import com.alibaba.fastjson.JSON;
import com.xuecheng.ucenter.mapper.XcUserMapper;
import com.xuecheng.ucenter.model.dto.AuthParamsDto;
import com.xuecheng.ucenter.model.dto.XcUserExt;
import com.xuecheng.ucenter.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import javax.annotation.Resource;
import java.util.List;

@Slf4j
public class UserServiceImpl implements UserDetailsService {

    @Resource
    private XcUserMapper userMapper;

    @Resource
    private ApplicationContext applicationContext;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        AuthParamsDto authParamsDto = null;
        try {
            authParamsDto = JSON.parseObject(username,AuthParamsDto.class);
        } catch (Exception e) {
            log.error("认证请求数据格式不正确：{}",username);
            throw new RuntimeException("认证请求数据格式不正确！");
        }
        //使用策略模式来确认认证方法
        String authType = authParamsDto.getAuthType();
        //从spring容器中获取对象
        AuthService authService = applicationContext
                .getBean(authType + "_authservice", AuthService.class);

        //开始认证,执行这个策略具体的认证方法
        XcUserExt user = authService.execute(authParamsDto);

        //获取认证授权后的用户信息
        UserDetails userDetails = getUserPrincipal(user);

        return userDetails;
    }

    /**
     * @description 查询用户信息
     * @param user  用户id，主键
     * @return com.xuecheng.ucenter.model.po.XcUser 用户信息
     * @author Mr.M
     * @date 2022/9/29 12:19
     */
    public UserDetails getUserPrincipal(XcUserExt user){
        String password = user.getPassword();
        //去除密码信息
        user.setPassword(null);
        String userJson = JSON.toJSONString(user);
        List<GrantedAuthority> authorities = user.getAuthorities();
        //没有权限就创建一个
        if (authorities.isEmpty()){
            authorities.add(new SimpleGrantedAuthority("test"));
        }

        //创建UserDetails对象，权限信息待实现授权功能再向UserDetail中加入
        UserDetails userDetails = User.withUsername(userJson)
                .password(password)
                .authorities(authorities)
                .build();
        return userDetails;
    }
}
