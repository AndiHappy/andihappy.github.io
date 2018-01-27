---    
layout: post  
title: "netty in action 读书笔记"  
subtitle: "netty in action的第八章"  
date: 2017-12-07 09:00:00  
author: "zhailzh"  
header-img: "img/post-bg-2015.jpg"  
catalog: true  
tags:  
- 学习  
categories:  
- 读书笔记
---    
■ Bootstrapping     
■ ChannelOptions and attributes    
<!--more-->   
bootstrapping an application is the process of configuring it to run—though the details of the process may not be as simple as its definition, especially in network applications.     

启动一个应用的门面模式的设计。     

![Bootstap](http://7xtrwx.com1.z0.glb.clouddn.com/0d1518359f078f2c0fcf6d9efaa3dca4.png)     

bootstrap的继承关系。     

![配置](http://7xtrwx.com1.z0.glb.clouddn.com/2202a24bd941507239db95c61330833b.png)  
![ddd](http://7xtrwx.com1.z0.glb.clouddn.com/6341f620bb98b62a7c060b93a86bede0.png)     

This compatibility must be maintained; you can’t mix components having different prefixes, such as NioEventLoopGroup and OioSocketChannel. The following listing shows an attempt to do just that.     
不同前缀的不能混用。      


![启动时候的配置](http://7xtrwx.com1.z0.glb.clouddn.com/4d93ea316f4dfdcbde120e1fc6eddc37.png)    
![client](http://7xtrwx.com1.z0.glb.clouddn.com/d2d5b9f8b5bf0e4936706c61207db0e4.png)    

![server](http://7xtrwx.com1.z0.glb.clouddn.com/ad1fe5a83bdacb60a65017e6364e9c4b.png)     

Suppose your server is processing a client request that requires it to act as a client to a third system. This can happen when an application, such as a proxy server, has to integrate with an organization’s existing systems, such as web services or databases. In such cases you’ll need to bootstrap a client Channel from a ServerChannel.     

在一个Channel中重新连接一个Serverd的NettyClient 这个也算是一个新的用法吧。就是传说中的RPC通信。    

**The topic we’ve discussed in this section and the solution presented reflect a general guideline in coding Netty applications: reuse EventLoops wherever possible to reduce the cost of thread creation.**      

重新利用EventLoops，把这个类EventLoops当做线程池来进行使用，避免线程池的消耗    

Manually configuring every channel when it’s created could become quite tedious. Fortunately, you don’t have to. Instead, you can use option() to apply ChannelOptions to a bootstrap. The values you provide will be applied automatically to all Channels created in the bootstrap. The ChannelOptions available include low-level connection details such as keep-alive or timeout properties and buffer settings.      

netty针对所有的链接设置超时等操作，option()操作针对的是全部的Channel。     

Netty offers the AttributeMap abstraction, a collection provided by the channel and boot- strap classes, and AttributeKey<T>, a generic class for inserting and retrieving attri- bute values. With these tools, you can safely associate any kind of data item with both client and server Channels.     

Channel上面绑定参数和参数的值。       

八个类一一的介绍完毕，需要写一个自己的总结出来，总结netty运行的的过程，包括扩展的协议之类的，只是把运行的的过程梳理一下。       
