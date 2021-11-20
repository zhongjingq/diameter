package com.jackzhong.learn;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.Charset;

@Slf4j
public class NettyServerEventLoop {
    public static void main(String[] args) {
        //创建一个独立的EventLoopGroup
        EventLoopGroup group = new DefaultEventLoopGroup(); //没有io操作，只做普通任务或定时任务

        new ServerBootstrap()
                // 第一参数：boss只负责channel accept 事件，第二参数 worker 只负责socketChannel上的读写
                .group(new NioEventLoopGroup(),new NioEventLoopGroup(2))
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel nioSocketChannel) throws Exception {
                        /**
                         * 解决某个handler 处理读事件，花费时间长，会影响整个handler处理的线程，可以根据实际的handler分给不同的事件循环组来管理,
                         */
                        nioSocketChannel.pipeline().addLast("handler1",new ChannelInboundHandlerAdapter(){
                            @Override
                            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                ByteBuf byteBuf = (ByteBuf) msg;
                                log.debug(byteBuf.toString(Charset.forName("utf-8")));
                                ctx.fireChannelRead(msg);//让消息传递给下一个handler
                            }
                        }).addLast(group,"handler2",new ChannelInboundHandlerAdapter(){
                            @Override
                            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                ByteBuf byteBuf = (ByteBuf) msg;
                                log.debug(byteBuf.toString(Charset.forName("utf-8")));
                            }
                        });
                    }
                })
                .bind(8081);
    }
}
