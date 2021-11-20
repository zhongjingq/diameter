package com.jackzhong.learn;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.Charset;

@Slf4j
public class NettyServerPipeline {
    public static void main(String[] args) {
        new ServerBootstrap()
                .group(new NioEventLoopGroup())
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel nioSocketChannel) throws Exception {
                        // 1. 通过 channel 拿到 pipeline对象
                        ChannelPipeline pipeline = nioSocketChannel.pipeline();
                        pipeline.addLast(new LoggingHandler(LogLevel.DEBUG));
                        // 2. 添加处理器 pipeline 流水线工序如 head -> handler-1 -> handler-2 -> handler-3 -> tail
                        pipeline.addLast("handler-1",new ChannelInboundHandlerAdapter(){
                            @Override
                            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                log.debug("handler-1");
                                ByteBuf byteBuf = (ByteBuf) msg;
                                String name = byteBuf.toString(Charset.forName("utf-8"));
                                super.channelRead(ctx, name);
                            }
                        });
                        pipeline.addLast("handler-2",new ChannelInboundHandlerAdapter(){
                            @Override
                            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                log.debug("handler-2,name:{}",msg);
                                Student student = new Student(msg.toString() + "(先生)");
                                super.channelRead(ctx, student);
                            }
                        });
                        pipeline.addLast("handler-3",new ChannelInboundHandlerAdapter(){
                            @Override
                            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                Student student = (Student) msg;
                                log.debug("handler-3 student name:{}",student.getName());
                                /**
                                 * ctx.writeAndFlush()  -> 从当前的pipeline 向前找出站OutBound对象
                                 * channel.writeAndFlush()->从最后尾端，最后尾端是默认的tail添加的，向前找OutBound对象
                                 */
//                                ctx.writeAndFlush()
                                nioSocketChannel.writeAndFlush(ctx.alloc().buffer().writeBytes(("Server Response Student Name:" + student.getName()).getBytes(Charset.forName("utf-8"))));
                            }
                        });
                        pipeline.addLast("handler-4",new ChannelOutboundHandlerAdapter(){
                            @Override
                            public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
                                log.debug("handler-4");
                                super.write(ctx, msg, promise);
                            }
                        });
                        pipeline.addLast("handler-5",new ChannelOutboundHandlerAdapter(){
                            @Override
                            public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
                                log.debug("handler-5");
                                super.write(ctx, msg, promise);
                            }
                        });
                        pipeline.addLast("handler-6",new ChannelOutboundHandlerAdapter(){
                            @Override
                            public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
                                log.debug("handler-6");
                                super.write(ctx, msg, promise);
                            }
                        });
                    }
                })
                .bind(8081);
    }
}
