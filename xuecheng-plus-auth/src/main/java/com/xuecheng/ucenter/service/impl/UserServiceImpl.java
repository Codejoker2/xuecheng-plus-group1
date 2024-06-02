package com.xuecheng.ucenter.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.ucenter.mapper.XcUserMapper;
import com.xuecheng.ucenter.model.po.XcUser;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import javax.annotation.Resource;

public class UserServiceImpl implements UserDetailsService {

    @Resource
    private XcUserMapper userMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        LambdaQueryWrapper<XcUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(XcUser::getUsername, username);

        XcUser user = userMapper.selectOne(wrapper);

        if (user == null) {
            //返回空表示用户不存在
            return null;
        }
        //取出数据库存储的正确密码
        String password = user.getPassword();

        //Todo 设置用户权限信息,现在设置硬编码
        String[] authorities = {"test"};

        //创建UserDetails对象，权限信息待实现授权功能再向UserDetail中加入
        UserDetails userDetails = User.withUsername(user.getUsername())
                .password(password)
                .authorities(authorities)
                .build();

        return userDetails;
    }
}
