package com.xuecheng.test;

import com.alibaba.fastjson.JSON;
import com.xuecheng.media.model.po.MediaProcess;
import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import javax.annotation.Resource;

/**
 * @author zengweichuan
 * @description
 * @date 2024/4/12
 */
@SpringBootTest
public class RedisClientTest {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private RedissonClient redissonClient;
    /**
     * 设置键值对
     */
    @Test
    public void test(){
        //使用json传递对象
        MediaProcess mediaProcess = new MediaProcess();
        mediaProcess.setId(1L);
        mediaProcess.setStatus("1");
        mediaProcess.setFilename("我是傻逼");
        mediaProcess.setBucket("我是牛逼");

        //redisTemplate.opsForValue().set("user", JSON.toJSONString(mediaProcess));
        redisTemplate.opsForValue().set("user", mediaProcess);

        System.out.println(redisTemplate.opsForValue().get("user"));

    }

    /**
     * redisson分布式锁
     */
    @Test
    public void test01(){
        RLock lockName = redissonClient.getLock("lockName");
        try {
            lockName.lock();
            System.out.println("执行需要加锁的操作!");
            Thread.sleep(10000);
            System.out.println("执行操作完毕");
        }catch (Exception e){

        }finally {
            lockName.unlock();
        }
    }
}
