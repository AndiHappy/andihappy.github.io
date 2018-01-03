---    
layout: post  
title: "netty in action 精简"  
subtitle: "netty in action的读书笔记+复习英语单词"  
date: 2017-12-01 09:00:00  
author: "zhailzh"  
header-img: "img/post-bg-2015.jpg"  
catalog: true  
tags:  
- 学习  
categories:  
- 读书笔记
---  
netty 异步事件驱动型IO框架，值得学习
<!--more-->
1. the system must scale up to 150,000 concurrent users with no loss of performance。      

scale up 负载       

2. If you can say with confidence, “Sure, no problem,” then hats off to you。    

hats off to you     

3. next steps were probably to browse the site, download the code, peruse the Javadocs and a few blogs, and start hacking. If you already had solid network programming experience, you probably made good progress；      

High-performance systems like the one in our example require more than first-class coding skills; they demand expertise in several complex areas: networking, multithreading, and concurrency.      

Java开始提供的Socket连接，只支持阻塞形式的IO。       

Socket server = new ServerSocket(8080).accept();      

accept的APIDOC中有一句：The method blocks until a connection is made.
accept() blocks until a connection is established on the ServerSocket b, then returns a new Socket for communication between the client and the server. The ServerSocket then resumes（重新恢复） listening for incoming connections.      


这种方式的利弊：        
First, at any point many threads could be dormant（休眠）, just waiting for input or output data to appear on the line.        
因为 new BufferedReader(new InputStreamReader(this.socket.getInputStream())).readerLine() 没有数据的时候，会被阻塞的        

Second,each thread requires an allocation of stack memory whose default size ranges from 64 KB to 1 MB, depending on the OS.             
如果每一个Client的连接是新建一个socket连接的话，那么需要给这个socket分配64kb到1MB的内存

Third, even if a Java virtual machine (JVM) can physically support a very large number of threads, the overhead of context-switching will begin to be troublesome long before that limit is reached, say by the time you reach 10,000 connections.          
JVM线程数的限制以及多开的线程造成的线程切换造成的花销。        
在这种通信的模型下面，基本上保持着每一个client对应一个线程，所以对大规模的同时连接，JVM的线程数是一种限制        


第二种的变形，只是在服务端提供线程池的方式来处理客户端的接入，这样的话，对JVM的线程限制数就有了一种解决方案。            
这种情况下，如果某些线程处理数据比较的慢，那么这些线程会夯在线程池中，可能就会影响其他客户端的接入和其他线程数据的处理           


JDK1.4 2002发布，支持NIO模型。           
Java support for non-blocking I/O was introduced in 2002, with the JDK 1.4 package java.nio，which provide considerably more control over the utilization of network resources.           

The class java.nio.channels.Selector is the linchpin（关键） of Java’s non-blocking I/O implementation.            
It uses the event notification API to indicate which, among a set of non-blocking sockets, are ready for I/O.              

利弊：
■ Many connections can be handled with fewer threads, and thus with far less overhead due to memory management and context-switching.            
较少的线程就能够处理接入和数据处理，然后线程切换和内存管理变得比较的简单        
■ Threads can be retargeted to other tasks when there is no I/O to handle.    
没有IO线程进行处理的时候，线程能够重定向处理其他的任务。          

这种通信模型下，主要的是基于：Selector，SocketChannel，Buffer       
这种通信模型，已经能够基本支持网络通信的需要，但是IO通信过程中可靠，重连，断网重链等问题仍需要自己进行开发。      

JDK1.7 升级了NIO，变为了NIO2，提供了异步io的通道           
NIO2.0 的异步套接字通道是真正的异步非阻塞I/O,它对应UNIX 网络编程中的事件驱动I/O (AIO)，它不需要通过多路复用器(Selector) 对注册的通道进行轮询操作即可实现异步读写，从而简化了NIO 的编程模型。           

In particular, processing and dispatching I/O reliably and efficiently under heavy load is a cumbersome（笨重的） and error-prone task best left to a high-performance networking expert—Netty             

We’ve learned from long and painful experience that the direct use of low-level APIs exposes complexity and introduces a critical dependency on skills that tend to be in short supply.Hence, a fundamental concept of object orientation: hide the complexity of underlying imple- mentations behind simpler abstractions.
This principle has stimulated the development of numerous frameworks that encapsulate solutions to common programming tasks, many of them germane to distributed systems development. It’s probably safe to assert that all professional Java developers are familiar with at least one of these.          

对于框架的封装，对于我们Developer，不知道是一件好事还是坏事？           


In essence, a system that is both asynchronous and event-driven exhibits a particular and, to us, extremely valuable kind of behavior: it can respond to events occurring at any time and in any order.          
异步的事件处理系统           
This capability is critical for achieving the highest levels of scalability    

Non-blocking network calls free us from having to wait for the completion of an operation.           
Fully asynchronous I/O builds on this feature and carries it a step further: an asynchronous method returns immediately and notifies the user when it is complete, directly or at a later time.      

Selectors allow us to monitor many connections for events with many fewer threads.           

Putting these elements together, with non-blocking I/O we can handle very large numbers of events much more rapidly and economically than would be possible with blocking I/O. From the point of view of networking, this is key to the kind of systems we want to build,and as you’ll see, it is also key to Netty’s design from the ground up.                

netty设计的关键在于非阻塞的网络通信和Selector感知连接等不同的事件             

netty的主要的模块：             
■ Channels：管道             
■ Callbacks：A callback is simply a method, a reference to which has been provided to another method.             
■ Futures:A Future provides another way to notify an application when an operation has com- pleted. This object acts as a placeholder for the result of an asynchronous operation; it will complete at some point in the future and provide access to the result.             
■ Events and handlers             

Channels：For now, think of a Channel as a vehicle for incoming (inbound) and outgoing (out- bound) data. As such, it can be open or closed, connected or disconnected.             
管道             
Callbacks are used in a broad range of programming situations and represent one of the most common ways to notify an interested party that an operation has completed.     
回调     
In fact, callbacks and Futures are complementary mecha- nisms; in combination they make up one of the key building blocks of Netty itself         
Future这个可以翻译为未来值？     

Netty is a networking framework,so events are categorized by their relevance to inbound or outbound data flow.    
Events that may be triggered by inbound data or an associated change of state include    
■ Active or inactive connections     
■ Data reads    
■ User events   
■ Error events   
An outbound event is the result of an operation that will trigger an action in the future, which may be    
■ Opening or closing a connection to a remote peer    
■ Writing or flushing data to a socket     

![envent](http://7xtrwx.com1.z0.glb.clouddn.com/82d4921691f30a71bea6f4b204353f8b.png)    

Netty’s asynchronous programming model is built on the concepts of Futures and callbacks, with the dispatching of events to handler methods happening at a deeper level. Taken together, these elements provide a processing environment that allows the logic of your application to evolve independently of any concerns with network operations. This is a key goal of Netty’s design approach   
