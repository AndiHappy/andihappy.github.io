---    
layout: post  
title: "netty in action 读书笔记"  
subtitle: "netty in action的第四章，"  
date: 2017-12-02 09:00:00  
author: "zhailzh"  
header-img: "img/post-bg-2015.jpg"  
catalog: true  
tags:  
- 学习  
categories:  
- 读书笔记
---    
explain the transport implementations that come bundled with Netty and the use cases appropriate to each. With this informa- tion in hand, you should find it straightforward to choose the best option for your application.    
<!--more-->   

a Channel has a ChannelPipeline and a ChannelConfig assigned to it. The ChannelConfig holds all of the configuration settings for the Channel and supports hot changes.     

The ChannelPipeline holds all of the ChannelHandler instances that will be applied to inbound and outbound data and events.     

Typical uses for ChannelHandlers include:
■ Transforming data from one format to another
■ Providing notification of exceptions
■ Providing notification of a Channel becoming active or inactive
■ Providing notification when a Channel is registered with or deregistered from an EventLoop
■ Providing notification about user-defined events     

The ChannelPipeline implements a common design pattern, Intercepting Filter. UNIX pipes are another familiar example: com- mands are chained together, with the output of one command connecting to the input of the next in line.     

The ChannelPipeline implements a common design pattern, Intercepting Filter. UNIX pipes are another familiar example: com- mands are chained together, with the output of one command connecting to the input of the next in line.     

过滤链的设计模式，类似Unix的pipe

keep in mind that the broad range of functionality offered by Netty relies on a small number of interfaces. This means that you can make significant modifications to application logic without wholesale refactoring of your code base.     

netty设计过程中注重的一个点： a small number of interfaces。     

Netty’s Channel implementations are thread-safe, so you can store a reference to a Channel and use it whenever you need to write something to the remote peer, even when many threads are in use.     

Channel是线程安全的。    

~~~java
final Channel channel = ctx.channel();
final ByteBuf buf = Unpooled.copiedBuffer("your data",
    CharsetUtil.UTF_8).retain();
//新建的线程可以随便的调用
Runnable writer = new Runnable() {
    @Override
    public void run() {
        channel.write(buf.duplicate());
    }
~~~    

![transport](http://7xtrwx.com1.z0.glb.clouddn.com/5940f5d7453b6ab69893d714abefa445.png)   
netty支持的协议。    

The basic concept behind the selector is to serve as a registry where you request to be notified when the state of a Channel changes. The possible state changes are
■ A new Channel was accepted and is ready.
■ A Channel connection was completed.
■ A Channel has data that is ready for reading.
■ A Channel is available for writing data.
After the application reacts to the change of state, the selector is reset and the process repeats, running on a thread that checks for changes and responds to them accordingly.    

pipeline时间的变化，底层的实现还是基于：java.nio.channels.SelectionKey.      

![底层实现](http://7xtrwx.com1.z0.glb.clouddn.com/2fffdbdfb610aa63b43ce28b520ac893.png)      

Zero-copy      

Zero-copy is a feature currently available only with NIO and Epoll transport. It allows you to quickly and efficiently move data from a file system to the network without copying from kernel space to user space, which can significantly improve perfor- mance in protocols such as FTP or HTTP. **This feature is not supported by all OSes. Specifically it is not usable with file systems that implement data encryption or compression—only the raw content of a file can be transferred.** Conversely, transferring files that have already been encrypted isn’t a problem.      

零拷贝技术，是的netty的效率比较的高效。不支持文件系统中数据的压缩和解压缩。但是传输压缩的数据是没有问题的。      
