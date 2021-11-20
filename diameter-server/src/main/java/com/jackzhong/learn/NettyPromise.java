package com.jackzhong.learn;

import io.netty.channel.EventLoop;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.DefaultPromise;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutionException;

@Slf4j
public class NettyPromise {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        // 1.准备EventLoop
        EventLoop eventLoop = new NioEventLoopGroup(1).next();

        // 2.可以主动创建 promise
        DefaultPromise<Integer> promise = new DefaultPromise<>(eventLoop);

        // 3. 任意一个线程执行计算，计算结果向 promise 填充结果
        new Thread(() ->{
            log.debug("开始计算");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            try{
                int i = 1 /0 ;
            }catch (Exception ex){
                promise.setFailure(ex);
            }
        },"test").start();

        // 4.接收结果的线程
        log.debug("等待结果...");
        Integer integer = promise.get(); //阻塞线程，等待结果
        log.debug("计算结果：{}",integer);

    }
}
