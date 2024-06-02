package com.xuecheng.ucenter.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.ucenter.mapper.XcUserMapper;
import com.xuecheng.ucenter.model.dto.AuthParamsDto;
import com.xuecheng.ucenter.model.dto.XcUserExt;
import com.xuecheng.ucenter.model.po.XcUser;
import com.xuecheng.ucenter.service.AuthService;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;

/**
 * @description 账号密码认证
 * @author Mr.M
 * @date 2022/9/29 12:12
 * @version 1.0
 */
@Service("password_authservice")
public class PasswordAuthServiceImpl implements AuthService {

    @Resource
    private XcUserMapper userMapper;

    @Resource
    private PasswordEncoder passwordEncoder;
    @Override
    public XcUserExt execute(AuthParamsDto authParamsDto) {
        //从数据库中查询用户信息
        String username = authParamsDto.getUsername();
        XcUser userFromDb = userMapper
                .selectOne(new LambdaQueryWrapper<XcUser>().eq(XcUser::getUsername, username));

        if(userFromDb == null){
            throw new RuntimeException("用户不存在");
        }
        XcUserExt xcUserExt = new XcUserExt();
        BeanUtils.copyProperties(userFromDb,xcUserExt);
        //校验用户的密码输入是否正确
        boolean match = passwordEncoder.matches(authParamsDto.getPassword(), userFromDb.getPassword());
        if(!match){
            throw new RuntimeException("账号或密码错误");
        }
        xcUserExt.setPermissions(new ArrayList<>());
        return xcUserExt;
    }
}
