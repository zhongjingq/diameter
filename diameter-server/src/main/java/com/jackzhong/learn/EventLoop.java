package com.jackzhong.learn;

import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

@Slf4j
public class EventLoop {
    public static void main(String[] args) {
        //1.创建事件循环组
        EventLoopGroup group = new NioEventLoopGroup(2);//io 事件，可以做普通任务、定时任务
        //EventLoopGroup defaultGroup = new DefaultEventLoopGroup();//不能做io事件，只能做普通任务、定时任务
        // 2. 获取一个事件循环对象
        log.debug(group.next().toString());
        // 3. 执行普通任务
        group.next().submit(()->{
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            log.debug("ok");
        });
        // 4. 执行定时任务
        group.next().scheduleAtFixedRate(()->{
            log.debug("schedule ok");
        },0,1, TimeUnit.SECONDS); //表示1秒执行一次
        log.debug("main");

    }
}
