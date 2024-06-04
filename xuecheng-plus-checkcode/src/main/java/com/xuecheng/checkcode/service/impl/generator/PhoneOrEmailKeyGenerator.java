package com.xuecheng.checkcode.service.impl.generator;

import com.xuecheng.checkcode.service.CheckCodeService;
import org.springframework.stereotype.Component;

@Component("PhoneOrEmailKeyGenerator")
public class PhoneOrEmailKeyGenerator implements CheckCodeService.KeyGenerator{
    @Override
    public String generate(String prefix) {
        return prefix;
    }
}
