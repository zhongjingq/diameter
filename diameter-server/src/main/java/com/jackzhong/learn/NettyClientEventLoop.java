package com.jackzhong.learn;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringEncoder;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;

@Slf4j
public class NettyClientEventLoop {
    public static void main(String[] args) throws InterruptedException {
        /**
         * 带有 Future、Promise 的类型都是和异步方法配套使用，用来处理结果
         */
        ChannelFuture channelFuture = new Bootstrap()
                .group(new NioEventLoopGroup())
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel nioSocketChannel) throws Exception {
                        nioSocketChannel.pipeline().addLast(new StringEncoder(Charset.forName("utf-8")));
                    }
                })
                // 1.连接到服务器
                /**
                 * connect 方法是异步非阻塞，主线程发起了调用，真正执行connect是 NioEventLoopGroup分配的nio线程，是异步建立服务器连器
                 */
                .connect(new InetSocketAddress("localhost", 8081));
        /**
         * 不加此方法，channelFuture.channel()获取不到channel的通道
         * 解决方法
         * 1：加上sync()方法，同步处理connect连接获取连接
         * 2：使用addListener 方法异步处理结果
          */
        // 解决方法 1
//        channelFuture.sync();//阻塞当前线程，直到NIO线程建立连接
//        Channel channel = channelFuture.channel();
//        channel.writeAndFlush("hello,world!");

        //解决方法 2
        channelFuture.addListener(new ChannelFutureListener() {
            //在connect nio线程连接建立好后，会调用operationComplete
            @Override
            public void operationComplete(ChannelFuture channelFuture) throws Exception {
                Channel channel = channelFuture.channel();
                log.debug("{}",channel);
                channel.writeAndFlush("hello,world!");
            }
        });
    }
}
