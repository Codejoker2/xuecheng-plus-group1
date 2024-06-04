package com.xuecheng.ucenter.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.xuecheng.ucenter.feignclient.CheckCodeClient;
import com.xuecheng.ucenter.mapper.XcUserMapper;
import com.xuecheng.ucenter.mapper.XcUserRoleMapper;
import com.xuecheng.ucenter.model.dto.RegistAndResetPassDto;
import com.xuecheng.ucenter.model.po.XcUser;
import com.xuecheng.ucenter.model.po.XcUserRole;
import com.xuecheng.ucenter.service.RegisteAndResetPasswordService;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;

@Service
public class RegisteAndResetPasswordServiceImpl implements RegisteAndResetPasswordService {

    @Resource
    private CheckCodeClient checkCodeClient;

    @Resource
    private XcUserMapper userMapper;

    @Resource
    private PasswordEncoder passwordEncoder;

    @Resource
    private XcUserRoleMapper userRoleMapper;

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

    @Override
    public void register(RegistAndResetPassDto dto) {
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

        XcUser xcUserFromDb = userMapper.selectOne(wrapper);
        if(xcUserFromDb != null)throw new RuntimeException("您已经通过手机或邮箱注册过了");

        //将用户保存到数据库
        saveUser(dto);

    }
    private void saveUser(RegistAndResetPassDto dto){

        //将密码做加密
        String password = passwordEncoder.encode(dto.getPassword());
        XcUser newUser = new XcUser();
        XcUser user = new XcUser();
        user.setUsername(dto.getUsername());
        user.setPassword(password);
        user.setName(dto.getNickname());
        user.setNickname(dto.getNickname());
        user.setEmail(dto.getEmail());
        user.setCellphone(dto.getCellphone());
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
