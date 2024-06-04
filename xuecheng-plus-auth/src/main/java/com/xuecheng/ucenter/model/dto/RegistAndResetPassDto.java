package com.xuecheng.ucenter.model.dto;

import lombok.Data;

@Data
public class RegistAndResetPassDto {

    /*
{"cellphone":"15231907203",
"username":"zwc",
"email":"3076368691@qq.com",
"nickname":"zwc",
"password":"111111",
"confirmpwd":"111111",
"checkcodekey":"phone:15231907203",
"checkcode":"CRWCHE"}


     */
    private String cellphone;
    private String username;
    private String email;
    private String nickname;
    private String checkcodekey;
    private String confirmpwd;
    private String password;
    private String checkcode;
}
