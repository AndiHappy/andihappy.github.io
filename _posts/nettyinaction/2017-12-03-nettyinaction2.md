---    
layout: post  
title: "netty in action 读书笔记"  
subtitle: "netty in action的读书笔记+复习英语单词"  
date: 2017-12-03 09:00:00  
author: "zhailzh"  
header-img: "img/post-bg-2015.jpg"  
catalog: true  
tags:  
- 学习  
categories:  
- 读书笔记
---    

这章主要的是介绍netty的架构，主要的模块。       
verifying your development tools and environment,     
 building application logic with ChannelHandlers.      

<!--more-->

From a high-level perspective, Netty addresses two corresponding areas of concern, which we might label broadly as technical and architectural. First, its asynchronous and event-driven implementation, built on Java NIO, guarantees maximum application performance and scalability under heavy load. Second, Netty embodies a set of design patterns that decouple application logic from the network layer, simplify ing development while maximizing the testability, modularity, and reusability of code.     

netty的两个方面，技术层次的异步和事件处理机制，架构层面的很多的设计模式。     

***The following sections will add detail to our discussion of the Channel, EventLoop, and ChannelFuture classes which, taken together, can be thought of as representing Netty’s networking abstraction: ***      
■ Channel—Sockets      
■ EventLoop—Control flow, multithreading, concurrency    
■ ChannelFuture—Asynchronous notification     

netty的框架抽象的内容：Channel，socket连接的抽象，EventLoop，并发，多线程事件控制流。ChannelFuture，异步的通知。      

Basic I/O operations (bind(), connect(), read(), and write()) depend on primitives supplied by the underlying network transport. In Java-based networking, the funda- mental construct is class Socket. Netty’s Channel interface provides an API that greatly reduces the complexity of working directly with Sockets. Additionally, Channel is the root of an extensive class hierarchy having many predefined, specialized implementations。example：
NioSocketChannel     

netty针对socket操作的抽象出来的是Channel，具体的集成关系：      

~~~java  
public abstract class AbstractChannel extends DefaultAttributeMap implements Channel
public abstract class AbstractNioChannel extends AbstractChannel
public abstract class AbstractNioByteChannel extends AbstractNioChannel
public interface SocketChannel extends Channel
public class NioSocketChannel extends AbstractNioByteChannel implements io.netty.channel.socket.SocketChannel
~~~       

The EventLoop defines Netty’s core abstraction for handling events that occur during the lifetime of a connection。    


![Channel，EventLoop之间的关系](http://7xtrwx.com1.z0.glb.clouddn.com/26aa162535f06260ad225a84bf19f3a8.png)   

From the application developer’s standpoint, the primary component of Netty is the ChannelHandler, which serves as the container for all application logic that applies to handling inbound and outbound data. This is possible because ChannelHandler methods are triggered by network events (where the term “event” is used very broadly). In fact, a ChannelHandler can be dedicated to almost any kind of action, such as converting data from one format to another or handling exceptions thrown during processing.    

对开发者具体使用来说，netty最主要就是ChannelHandler了，可以专业处理各种各样的Event事件。   

A ChannelPipeline provides a container for a chain of ChannelHandlers and defines an API for propagating the flow of inbound and outbound events along the chain. When a Channel is created, it is automatically assigned its own ChannelPipeline.      
ChannelHandlers are installed in the ChannelPipeline as follows:       
■ A ChannelInitializer implementation is registered with a ServerBootstrap.    
■ When ChannelInitializer.initChannel() is called, the ChannelInitializer installs a custom set of ChannelHandlers in the pipeline.     
■ The ChannelInitializer removes itself from the ChannelPipeline.    

ChannelPipeline 可以说是包含了ChannelHandler的容器，一个container。   

The movement of an event through the pipeline is the work of the ChannelHandlers that have been installed during the initialization, or bootstrapping phase of the application. These objects receive events, execute the processing logic for which they have been implemented, and pass the data to the next handler in the chain. The order in which they are executed is determined by the order in which they were added. **For all practical purposes, it’s this ordered arrangement of ChannelHandlers that we refer to as the ChannelPipeline.**     

ChannelHandler 处理事件，然后把处理事件后数据传给下一个ChannelHandler，安排这个顺序就是ChannelPipeline。     

![pipeline和handler](http://7xtrwx.com1.z0.glb.clouddn.com/5983e2d96f71af4c1ffa9050f46e533a.png)    

Figure 3.3 also shows that **both inbound and outbound handlers can be installed in the same pipeline.**  If a message or any other inbound event is read, it will start from the head of the pipeline and be passed to the first ChannelInboundHandler. This handler may or may not actually modify the data, depending on its specific function, after which the data will be passed to the next ChannelInboundHandler in the chain. Finally, the data will reach the tail of the pipeline, at which point all processing is terminated.     

一个Pipeline中如果有 inbound 和 outbound 的handler，一个事件的处理顺序？？     
按照inbound的顺序，是按照pipeline中增加的顺序，然后在最后一个的inbound的handler中调用 ctx.write(object) 就会再次的调用outbound的handler去处理了。     
When a ChannelHandler is added to a ChannelPipeline, it’s assigned a Channel- HandlerContext, which represents the binding between a ChannelHandler and the ChannelPipeline. Although this object can be used to obtain the underlying Channel, it’s mostly utilized to write outbound data.     

ChannelHandler都会绑定一个 ChannelHandlerContext，这个上下文比较的有用。   

Netty provides a number of default handler implementations in the form of adapter classes, which are intended to simplify the development of an application’s processing logic. You’ve seen that each ChannelHandler in a pipeline is responsible for forwarding events to the next handler in the chain. These adapter classes (and their subclasses) do this automatically, so you can override only the methods and events you want to specialize.    

netty 提供的默认的handler：      

ByteToMessageDecoder      
MessageToByteEncoder      

字节到特殊类型的JAVA对象和JAVA对应到自己的转换的对象。
The pattern for outbound messages is the reverse: an encoder converts the mes- sage to bytes and **forwards them to the next ChannelOutboundHandler.**     

SimpleChannelInboundHandler<T>   
 a handler that receives a decoded mes- sage and applies business logic to the data.where T is the Java type of the message you want to process.     

Please keep in mind that strictly speak- ing the term “connection” applies only to connection-oriented protocols such as TCP, which guarantee ordered delivery of messages between the connected endpoints.

connection 应该对应的是 “链接”    

Netty的启动类区分了Client和Server，分别是bootStrap 和 ServerBootStrap，其中ServerBootStrap绑定到本地的一个端口上，因为server需要监听这个端口上面的数据，BootStrap却是可以连接到一个remote peer。      

第二种不同的点，就是BootStrap只需要一个EventLoopGroup，但是ServerBootStrap 需要两个。    

A server needs two distinct sets of Channels. The first set will contain a single ServerChannel representing the server’s own listening socket, bound to a local port. The second set will contain all of the Channels that have been created to handle incom- ing client connections—one for each connection the server has accepted. Figure 3.4 illustrates this model, and shows why two distinct EventLoopGroups are required.    

![server的EventLoopCroup](http://7xtrwx.com1.z0.glb.clouddn.com/c4c708ecd1b21e36cc553764267db07c.png)    

server端的两个EventLoopGroup，一个是处理Connection接入，监听接口数据的，一个是处理接入以后的业务逻辑的。  
