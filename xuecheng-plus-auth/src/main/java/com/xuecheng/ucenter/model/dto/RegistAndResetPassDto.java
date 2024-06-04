package com.xuecheng.ucenter.model.dto;

import lombok.Data;

@Data
public class RegistAndResetPassDto {
    /*
    {"cellphone":"15231907203",
    "email":"3076368691@qq.com",
    "checkcodekey":"phone:15231907203",
    "checkcode":"",
    "confirmpwd":"",
    "password":""}
     */
    private String cellphone;
    private String email;
    private String checkcodekey;
    private String confirmpwd;
    private String password;
    private String checkcode;
}
