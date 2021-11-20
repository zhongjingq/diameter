##Netty 组件
<h3>1. EvenLoop
<h4> EvenLoop（事件循环对象） 本质是一个单线程执行器（同时维护了一个Selector),里面有run方法处理channel上源源不断的IO事件
#####它的继承关系比较复杂
  1. 一条线是继承自j.u.c.ScheduledExecutorService因此包含了线程池中所有的方法
  2. 另一条线是继承自netty自己的OrderedEventExecutor
     + 提供了 boolean inEventLoop(Thread thread) 方法判断一个线程是否属于此EventLoop
     + 提供了 parent 方法来看看自己属于哪个EventLoop

<h4>EventLoopGroup(事件循环组)是一组EventLoop，Channel一般会调用EventLoopGroup的register方法来绑定其中一个EventLoop,后续这个Channel上的io事件都由此EventLoop来处理（保证了io事件处理的线程安全）
1. 继承自netty自己的EventExecutorGroup
   + 实现了Iterable接口提供遍历EventLoop能力
   + 另有next方法获取集合中下一个EventLoop
---
<h3>2. Channel
<h4> Channel 的主作用
+ close() 可以用来关闭
+ closeFuture()用来处理channel的关闭
    + sync方法作用是同步等待channel关闭
    + 而addListener方法是异步等channel关闭
+ pipeline() 方法是添加处理器
+ write() 方法是将数据写入
+ writeAndFlush() 方法将数据写入并刷出
---
<h3>3. Future & Promise
<h4> 在异步处理时，经常用到两这两个接口，首先要说明的netty中的 Future与jdk Future同时，两接口关系是：netty Future接口 extends Jdk Future，而netty promise又对jdk promise进行扩展
+ jdk Future 只能同步等待任务结束（或成功、或失败）才能得到结果
+ netty Future 可以同步等待任务结束得到结果，也可以异步方式得到结果，但都要等任务结束
+ nett promise 不仅有netty Future的功能，而且脱离了任务独立存在，只作为两个线程间传递结果的容器
---
<h3>4. Handler & Pipeline
<h4> ChannelHandler 用来处理Channel上的各种事件，分为入站（InBound）、出站（OutBound）两种，所有ChannelHandler被连成一串，就是Pipeline(流水线)
+ 入站（InBound）处理器通常是ChannelInBoundHandlerAdapter子类，主要用来读取客户端数据，写回结果
+ 出站（OutBound) 处理器通常是ChannelOutBoundHandlerAdapter子类，主要是对写回结果进行加工
####打个比喻，每个Channel是一个产品加工车间，Pipeline是车间中的流水线，ChannelHandler就是流水线上的各道工序，而后面要讲的ByteBuf就是原材料，经过很多工序的加工：先经过一道道入站工序，再经过一道道出站工序最终变成产品
---
<h3>5. ByteBuf
<h4> 5.1  直接内存 vs 堆内存
### 堆内存：可以使用下面的代码来创建池化基于堆的ByteBuf
```
ByteBuf buffer = ByteBufAllocator.DEFAULT.heapBuffer(10);
```
#### 直接内存 可以使用下面的代码来创建池化基于直接内存的ByteBuf
```
ByteBuf buffer = ByteBufAllocator.DEFAULT.directBuffer(10);
```
- 直接内存创建和销毁的代价昂贵，但读写性能高（少一次内存复制），适合配合池化功能一起使用
- 直接内存对GC压力小，因为这部分内存不受JVM垃圾回收管理，但也要注意及时主动释放

<h4> 5.2  池化 vs 非池化

###池化的最大意义在于可以复用ByteBuf 优点有：
+ 没有池化，则每次都得创建新的ByteBuf实例，这个操作对直接内存代价昂贵，就算是堆内存，也会增加GC压力
+ 有了池化，则可以重用池中ByteBuf实例，并且采用了与Jemalloc类似的内存分配算法提升分配效率
+ 高并发时，池化功能更节约内存，减少内存溢出的可能

###池化功能是否开启，可以通过下面系统环境变量来设置
```
-Dio.netty.allocator.type={unpooled|pooled}
```

+ 4.1 以后，非Android平台默认启用池化实现，Android平台启用非池化实现
+ 4.1 之前，池化功能还不成熟，默认非池化实现
