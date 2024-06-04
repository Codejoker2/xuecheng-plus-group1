package com.xuecheng.auth.controller;

import com.xuecheng.ucenter.model.dto.RegistAndResetPassDto;
import com.xuecheng.ucenter.service.RegisteAndResetPasswordService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
public class RegisteAndResetPasswordController {

    @Resource
    private RegisteAndResetPasswordService registeAndResetPasswordService;

    @PostMapping("/findpassword")
    public void findpassword(@RequestBody RegistAndResetPassDto dto) {
        registeAndResetPasswordService.findpassword(dto);
    }

    @PostMapping("/register")
    public void register(@RequestBody RegistAndResetPassDto dto){
        registeAndResetPasswordService.register(dto);
    }
}
