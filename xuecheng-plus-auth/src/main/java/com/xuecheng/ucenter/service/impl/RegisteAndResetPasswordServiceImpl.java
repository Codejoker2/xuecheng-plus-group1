package com.xuecheng.ucenter.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.xuecheng.ucenter.feignclient.CheckCodeClient;
import com.xuecheng.ucenter.mapper.XcUserMapper;
import com.xuecheng.ucenter.model.dto.RegistAndResetPassDto;
import com.xuecheng.ucenter.model.po.XcUser;
import com.xuecheng.ucenter.service.RegisteAndResetPasswordService;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Service
public class RegisteAndResetPasswordServiceImpl implements RegisteAndResetPasswordService {

    @Resource
    private CheckCodeClient checkCodeClient;

    @Resource
    private XcUserMapper userMapper;

    @Resource
    private PasswordEncoder passwordEncoder;

    @Transactional
    @Override
    public void findpassword(RegistAndResetPassDto dto) {
        //先校验验证码
        verifyCode(dto);

        //校验两次密码输入是否一致
        verifyPassword(dto);

        //根据用户提供的邮箱或手机号查询用户信息
        String email = dto.getEmail();
        String cellphone = dto.getCellphone();
        LambdaQueryWrapper<XcUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.checkValNotNull(email),XcUser::getEmail,email);
        wrapper.eq(StringUtils.checkValNotNull(cellphone),XcUser::getCellphone,cellphone);

        XcUser xcUser = userMapper.selectOne(wrapper);
        if(xcUser == null)throw new RuntimeException("这个用户还未进行注册！");

        //开始修改密码
        XcUser newUser = new XcUser();
        BeanUtils.copyProperties(xcUser,newUser);
        //给密码进行加密
        String newPassword = passwordEncoder.encode(dto.getPassword());
        newUser.setPassword(newPassword);

        userMapper.updateById(newUser);
    }

    private void verifyPassword(RegistAndResetPassDto dto){
        String password = dto.getPassword();
        String confirmpwd = dto.getConfirmpwd();
        if (!password.equals(confirmpwd)){
            throw new RuntimeException("两次密码输入不一致！");
        }
    }

    private void verifyCode(RegistAndResetPassDto dto){
        String key = dto.getCheckcodekey();
        String code = dto.getCheckcode();

        Boolean verify = checkCodeClient.verify(key, code);
        if (!verify){
            throw new RuntimeException("验证码错误！");
        }
    }
}
