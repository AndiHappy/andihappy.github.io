---    
layout: post  
title: "netty in action 精简2"  
subtitle: "netty in action的读书笔记+复习英语单词"  
date: 2017-12-02 09:00:00  
author: "zhailzh"  
header-img: "img/post-bg-2015.jpg"  
catalog: true  
tags:  
- 学习  
categories:  
- 读书笔记
---    

这章主要的是介绍netty的基本的概念。

In this chapter we’ll show you how to build a Netty-based client and server。      
the exercise is important for two reasons.      
First, it will provide a test bed for setting up（设置） and verifying your development tools and environment, which is essential if you plan to work with the book’s sam- ple code in preparation for your own development efforts.      
Second, you’ll acquire hands-on experience with a key aspect of Netty, touched on in the previous chapter: building application logic with ChannelHandlers.      
<!--more-->
We’ll also assume that you’re going to want to tinker（修改） with the example code and soon start writing your own.   

Although we’ve spoken of the client, the figure shows multiple clients connected simultaneously to the server. The number of clients that can be supported is limited, in theory, only by the system resources available (and any constraints that might be imposed by the JDK version in use).     

客户端的连接数，和服务端的资源有关系，一般和文件打开数有关联。     

All Netty servers require the following:
■ At least one ChannelHandler—This component implements the server’s processing of data received from the client—its business logic.     

ChannelHandler 一个很重要的功能就是，处理客户端的发送的数据     

■ Bootstrapping—This is the startup code that configures the server. At a minimum, it binds the server to the port on which it will listen for connection requests.     

Bootstrap 和 ServerBootstrap 就是启动netty的辅助类

ChannelInboundHandlerAdapter has a straightforward（简单易懂的） API, and each of its methods can be overridden to hook into the event lifecycle at the appropriate point. **Because you need to handle all received data, you override channelRead().** In this server you simply echo the data to the remote peer.
Overriding exceptionCaught() allows you to react to any Throwable subtypes— here you log the exception and close the connection. **A more elaborate（复杂，精心制作的） application might try to recover from the exception,** but in this case simply closing the connection signals to the remote peer that an error has occurred.   

ChannelInboundHandlerAdapter 一般Handler需要继承的类，主要override的方法是：      
![ChannelInBoundHandlerAdapter](http://7xtrwx.com1.z0.glb.clouddn.com/1ab0e3db6783f4ca1cfc126d679065d8.png)    

override channelActive(), invoked when a connection has been established. This ensures that something is written to the server as soon as possible, which in this case is a byte buffer that encodes the string "Netty rocks!".    

这个方法可以用来做登录或者token的验证的逻辑！！    


What happens if an exception isn’t caught?   

Every Channel has an associated ChannelPipeline, which holds a chain of Channel- Handler instances. By default, a handler will forward the invocation of a handler method to the next one in the chain. Therefore, if exceptionCaught()is not imple- mented somewhere along the chain, exceptions received will travel to the end of the ChannelPipeline and will be logged. For this reason, your application should supply at least one ChannelHandler that implements exceptionCaught().    

每一个Handler属于一个 handler链中，每一个handler链绑在一个ChannelPipel中。如果Handler中对产生的异常没有处理，将会沿着handler链中传入到下一个handler，一直到最后一个。    

针对Handler的总结：
ChannelHandlers are invoked for different types of events.
■ Applications implement or extend ChannelHandlers to hook into the event lifecycle and provide custom application logic.      

■ Architecturally, ChannelHandlers help to keep your business logic decoupled from networking code. This simplifies development as the code evolves in response to changing requirements.  

ChannelHandlers处理的是不同类型的Event，与网路通信解耦。   

In this section you’ll encounter the term transport. In the standard, multilayered view of networking protocols, the transport layer is the one that provides services for end- to-end or host-to-host communications.     
传输层，四层网路模型中第二层，具体的是tcp udp等协议层。    

Internet communications are based on the TCP transport. NIO transport refers to a transport that’s mostly identical to TCP except for server-side performance enhance- ments provided by the Java NIO implementation。   

和TCP协议非常的像，只是增加了服务端性能的提升。     

~~~Java
 b.childHandler(new ChannelInitializer<SocketChannel>() { // 7
            @Override
            public void initChannel(SocketChannel ch) throws Exception {
              ch.pipeline().addLast(new EchoServerHandler());
            }
          });
~~~
you make use of a special class, ChannelInitializer. This is key. **When a new connection is accepted, a new child Channel will be created, and the Channel Initializer will add an instance of your EchoServerHandler to the Channel’s ChannelPipeline.** As we explained earlier, this handler will receive notifications about inbound messages.     

每一次新的连接的到来，就会创建一个新的Channel。      

NIO is used in this example because it’s currently the most widely used transport, thanks to its scalability and thoroughgoing asynchrony. But a different transport implementation could be used as well. If you wished to use the OIO transport in your server, you’d specify OioServerSocketChannel and OioEventLoopGroup.

OIO的协议是什么？？      

In the following chapters, you’ll see many more examples of how Netty simplifies scalability and concurrency.    
