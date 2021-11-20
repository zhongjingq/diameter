package com.jackzhong.learn;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.Charset;
import java.util.Scanner;

@Slf4j
public class NettyClientCloseFuture {
    public static void main(String[] args) throws InterruptedException {

        NioEventLoopGroup eventExecutors = new NioEventLoopGroup();

        ChannelFuture channelFuture = new Bootstrap()
                .group(eventExecutors)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel nioSocketChannel) throws Exception {
                        nioSocketChannel.pipeline().addLast(new LoggingHandler(LogLevel.DEBUG));
                        nioSocketChannel.pipeline().addLast(new StringEncoder(Charset.forName("utf-8")));
                        nioSocketChannel.pipeline().addLast(new ChannelInboundHandlerAdapter(){
                            @Override
                            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                ByteBuf buf = (ByteBuf) msg;
                                log.debug("接收到服务器消息：{}",new String(buf.toString(Charset.forName("utf-8"))));
                                super.channelRead(ctx, msg);
                            }
                        });
                    }
                }).connect("localhost", 8081);

        Channel channel = channelFuture.sync().channel();//同步等待
        log.debug("channel:{}",channel);
        new Thread(()->{
            Scanner scanner = new Scanner(System.in);
            while (true){
                String s = scanner.nextLine();
                if("q".equals(s)){
                    channel.close(); //close也是异步操作
                    break;
                }
                channel.writeAndFlush(s);
            }
        },"input").start();

        //获取ClosedFuture对象，1 同步处理关闭 2异步处理关闭
        ChannelFuture closeFuture = channel.closeFuture();
        log.debug("waiting close....");

//        closeFuture.sync();//同步等待关闭
//        log.debug("处理关闭-----");

        closeFuture.addListener(new ChannelFutureListener() { //异步关闭
            @Override
            public void operationComplete(ChannelFuture channelFuture) throws Exception {
                //可以处理异步关闭后，要在关闭后处理的事项
                log.debug("异步收到close关闭");
                eventExecutors.shutdownGracefully();//优雅关闭
            }
        });
    }
}
