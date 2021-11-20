package com.jackzhong.learn;

import io.netty.channel.EventLoop;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.Future;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutionException;

@Slf4j
public class NettyFuture {

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        // 1. 创建
        NioEventLoopGroup nioEventLoop = new NioEventLoopGroup(1);
        EventLoop eventLoop = nioEventLoop.next();
        log.debug("提交任务");

        Future<Integer> future = eventLoop.submit(() -> {
            log.debug("计算处理");
            Thread.sleep(1000);
            return 30;
        });

//        log.debug("等待结果");
//        Integer integer = future.get();//同步获取
//        log.debug("得到结果：{}",integer);

        future.addListener(future1 -> { //异步获取
            Integer now = (Integer) future1.getNow();
            log.debug("得到结果：{}",now);
        });

    }
}
