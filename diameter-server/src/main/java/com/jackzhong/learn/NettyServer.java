package com.jackzhong.learn;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;

/**
 * 一开始需要树立正确的观念
 * <ul>
 *     <li>把 channel 理解数据通道</li>
 *     <li>把 msg 理解为流动的数据，最开始输入是ByteBuf,但经过pipeline的加工，会变成其它类型对象，最后输出又变成ByteBuf对象</li>
 *     <li>把 handler 理解为数据的处理工序
 *      <ul>
 *          <ol>工序有多道，合在一起就是 pipeline（流水线）,pipeline负责发布事件（读、读取完成...) 传播给每个 handler, handler 对自己感兴趣的事件进行处理 (重写了相应事件处理方法） </ol>
 *          <ol>handler 分为 Inbound（入站） 和 Outbound（出站） 两类</ol>
 *      </ul>
 *     </li>
 *     <li>把 EvenLoop 理解为处理数据的工人
 *      <ul>
 *          <ol>工人可以管理多个channel的io操作，并且一旦工人负责了某个channel，就会负责到底（绑定）</ol>
 *          <ol>工人即可以执行io操作，也可以进行任务处理，每位工人有任务队列，队列里可以堆放多个 channel 的待处理任务，任务分为普通任务、定时任务</ol>
 *          <ol>工人按照pipeline（流水线）顺序，依次按照handler的规划（代码）处理数据，可以为每道工序指定不同的工人</ol>
 *      </ul>
 *     </li>
 * </ul>
 */

public class NettyServer {

    public static void main(String[] args) {
        // 1.启动器，负责组装netty组件，启动服务器
       new ServerBootstrap()
               // 2.BossEventLoop ,WorkerEvenLoop(selector,thread),group 组
               .group(new NioEventLoopGroup()) //accept、read 事件
               // 3. 选择 服务器 ServerSocketChannel 实现
               .channel(NioServerSocketChannel.class) //OIO BIO
               // 4. boss 负责处理连接 work(child) 负责处理读写，决定了work(child)能执行哪些操作（handler)
               .childHandler(
                       // 5. channel 代表和客户端进行数据读写通道 Initializer 初始化，负责添加别的 handler
                       new ChannelInitializer<NioSocketChannel>() {
                   @Override //连接建设后，调用初始化方法
                   protected void initChannel(NioSocketChannel nioSocketChannel) throws Exception {
                       // 6. 添加具体 handler
                       nioSocketChannel.pipeline().addLast(new StringDecoder()); // 将 ByteBuf 转为字符串
                       nioSocketChannel.pipeline().addLast(new ChannelInboundHandlerAdapter(){ // 自定义 handler
                           @Override // 读事件
                           public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                               // 打印上一步转换好的字符串
                               System.out.println(msg);
                           }
                       });
                   }
               })
               // 7. 监听端口 8080
               .bind(8080);
    }
}
