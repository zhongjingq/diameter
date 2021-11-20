package com.jackzhong.learn;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;

@Slf4j
public class JdkFuture {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        // 1.创建线程池
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        // 2.提交任务
        log.debug("提交任务");
        Future<Integer> future = executorService.submit(() -> {
            log.debug("执行计算");
            Thread.sleep(2000);
            return 50;
        });
        log.debug("等待结果");
        // 3.同步等待结果
        Integer integer = future.get();
        log.debug("得到结果：{}",integer);


    }
}
