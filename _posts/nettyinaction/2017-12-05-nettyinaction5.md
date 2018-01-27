---    
layout: post  
title: "netty in action 读书笔记"  
subtitle: "netty in action的第六"  
date: 2017-12-05 09:00:00  
author: "zhailzh"  
header-img: "img/post-bg-2015.jpg"  
catalog: true  
tags:  
- 学习  
categories:  
- 读书笔记
---    
The ChannelHandler and ChannelPipeline      
<!--more-->   
The Channel lifecycle     
![Channel的状态](http://7xtrwx.com1.z0.glb.clouddn.com/faf428c08e7607718839a50f4173ae21.png)     
Channel的状态     

The ChannelHandler lifecycle     
![Channelhandler的生命周期](http://7xtrwx.com1.z0.glb.clouddn.com/e18807c3810151c1d02d1ccda5feb512.png)    

Channelhandler的生命周期    

When a ChannelInboundHandler implementation overrides channelRead(), it is respon- sible for explicitly releasing the memory associated with pooled ByteBuf instances. Netty provides a utility method for this purpose, ReferenceCountUtil.release()     

继承自ChannelInboundHandlerAdapter的扩展类，需要手动的释放ByteBuf的实例。       

Because SimpleChannelInboundHandler releases resources automatically, you shouldn’t store references to any messages for later use, as these will become invalid.     

继承自SimpleChannelInboundHandler的扩展类则不需要，因为SimpleChannelInboundHandler已经在内部进行了处理。      

A powerful capability of ChannelOutboundHandler is to defer an operation or event on demand, which allows for sophisticated approaches to request handling. If writing to the remote peer is suspended, for example, you can defer flush operations and resume them later.     

ChannelOutboundHandler的一个强大的能力是：根据需要推迟一个操作或者事件。？         

![Channelhandler的继承关系](http://7xtrwx.com1.z0.glb.clouddn.com/3164422cdad2d0dab5aba020aa21f479.png)     

---
Whenever you act on data by calling ChannelInboundHandler.channelRead() or ChannelOutboundHandler.write(), you need to ensure that there are no resource leaks. As you may remember from the previous chapter, Netty uses reference counting to handle pooled ByteBufs. So it’s important to adjust the reference count after you have finished using a ByteBuf.     

调用channelRead或者wirte的时候，需要注意内存的泄漏问题。    


To assist you in diagnosing potential problems, Netty provides class ResourceLeakDetector, which will sample about 1% of your application’s buffer allocations to check for memory leaks. The overhead involved is very small.     

netty提供了ResourceLeakDetector，只需要在运行的设置：     
java -Dio.netty.leakDetectionLevel=ADVANCED     

Every new Channel that’s created is assigned a new ChannelPipeline. This association is permanent; the Channel can neither attach another ChannelPipeline nor detach the current one.     

Channel一旦绑定到ChannelPipeline，就相当于定死了。    

![ChannelPipeline的handler上面的顺序](http://7xtrwx.com1.z0.glb.clouddn.com/b2580a8c978777aeb0e9cc5182927749.png)     

A ChannelHandler can modify the layout of a ChannelPipeline in real time by add- ing, removing, or replacing other ChannelHandlers. (It can remove itself from the ChannelPipeline as well.) This is one of the most important capabilities of the ChannelHandler。     

~~~java
		ChannelPipeline pipeline = ctx.pipeline();
		pipeline.addLast("handler1", firstHandler);
		pipeline.addFirst("handler2", new SecondHandler());
		pipeline.addLast("handler3", new ThirdHandler());
		pipeline.remove("handler3");
		pipeline.remove(firstHandler);
		pipeline.replace("handler2", "handler4", new FourthHandler())
~~~     

![channelContext的操作的event的流程](http://7xtrwx.com1.z0.glb.clouddn.com/a58c6c9f711778b569f8a419c5453730.png)     

inbound的异常操作：    
■ The default implementation of ChannelHandler.exceptionCaught() forwards the current exception to the next handler in the pipeline.      

■ If an exception reaches the end of the pipeline, it’s logged as unhandled.     

■ To define custom handling, you override exceptionCaught(). It’s then your decision whether to propagate the exception beyond that point.     

outbound的异常处理：     

■ Every outbound operation returns a ChannelFuture. The ChannelFuture- Listeners registered with a ChannelFuture are notified of success or error when the operation completes.      

■ Almost all methods of ChannelOutboundHandler are passed an instance of ChannelPromise. As a subclass of ChannelFuture, ChannelPromise can also be assigned listeners for asynchronous notification. But ChannelPromise also has writable methods that provide for immediate notification:
             ChannelPromise setSuccess();
             ChannelPromise setFailure(Throwable cause);     

Why choose one approach over the other? For detailed handling of an exception, you’ll probably find it more appropriate to add the ChannelFutureListener when calling the outbound operation, as shown in listing 6.13. For a less specialized approach to handling exceptions, you might find the custom ChannelOutboundHandler imple- mentation shown in listing 6.14 to be simpler.     

该如何选择异常，如果单个的处理，比较能够针对特殊的异常进行针对的额处理，增加ExceptionHandler的方式，便于统一的处理。     
