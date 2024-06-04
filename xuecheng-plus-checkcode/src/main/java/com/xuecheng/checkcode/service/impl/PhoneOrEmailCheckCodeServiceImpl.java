package com.xuecheng.checkcode.service.impl;

import com.xuecheng.checkcode.model.CheckCodeParamsDto;
import com.xuecheng.checkcode.model.CheckCodeResultDto;
import com.xuecheng.checkcode.service.AbstractCheckCodeService;
import com.xuecheng.checkcode.service.CheckCodeService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.regex.Pattern;

/**
 * @author Mr.M
 * @version 1.0
 * @description 手机号验证码生成器
 * @date 2022/9/29 16:16
 */
@Service("PhoneOrEmailCheckCodeService")
public class PhoneOrEmailCheckCodeServiceImpl extends AbstractCheckCodeService implements CheckCodeService {

    @Resource(name = "NumberLetterCheckCodeGenerator")
    @Override
    public void setCheckCodeGenerator(CheckCodeGenerator checkCodeGenerator) {
        this.checkCodeGenerator = checkCodeGenerator;
    }

    @Resource(name="PhoneOrEmailKeyGenerator")
    @Override
    public void setKeyGenerator(KeyGenerator keyGenerator) {
        this.keyGenerator = keyGenerator;
    }

    @Resource(name="RedisCheckCodeStore")
    @Override
    public void setCheckCodeStore(CheckCodeStore checkCodeStore) {
        this.checkCodeStore = checkCodeStore;
    }

    @Override
    public CheckCodeResultDto generate(CheckCodeParamsDto checkCodeParamsDto) {
        //确定验证模式
        String prefix = getPrefix(checkCodeParamsDto.getParam1());
        GenerateResult generate = generate(checkCodeParamsDto, 6, prefix, 300);



        String key = generate.getKey();
        String code = generate.getCode();
        //Todo 可以使用策略模式来将code发送短信或者邮箱,现在只需要从redis查询即可

        CheckCodeResultDto checkCodeResultDto = new CheckCodeResultDto();
        checkCodeResultDto.setKey(key);
        return checkCodeResultDto;
    }

    //确定验证模式
    private String getPrefix(String param){
        String prefix = "";
        if(checkPhoneRegex(param)){
            prefix = "phone:" + param;
        }
        if(checkEmailRegex(param)){
            prefix = "email:" + param;
        }
        return prefix;
    }

    //校验手机格式
    private boolean checkPhoneRegex(String param){
        String phoneRegex = "^1[3-9]\\d{9}$";
        Pattern pattern = Pattern.compile(phoneRegex, Pattern.CASE_INSENSITIVE);
        return pattern.matcher(param).matches();
    }
    //校验是否是邮箱格式
    private boolean checkEmailRegex(String param){
        String phoneRegex = "^[\\w-\\.]+@[\\w-]+\\.[a-z]{2,4}$";
        Pattern pattern = Pattern.compile(phoneRegex, Pattern.CASE_INSENSITIVE);
        return pattern.matcher(param).matches();
    }
}
