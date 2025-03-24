---    
layout: post  
title: "netty in action 读书笔记"  
subtitle: "netty in action的第七章"  
date: 2017-12-06 09:00:00  
author: "zhailzh"  
header-img: "img/post-bg-2015.jpg"  
catalog: true  
tags:  
- 学习  
categories:  
- 读书笔记
---    
■ Threading model overview    
■ Event loop concept and implementation    
■ Task scheduling    
■ Implementation details    
<!--more-->   
we must always guard against the possible side effects of concurrent execution, it’s important to understand the implications of the model being applied (there are single-thread models as well). Ignoring these matters and merely hoping for the best is tantamount to gambling—with the odds definitely against you.     

忽视这些事情，仅仅是希望得到最好的，就等于赌博，而你的胜算是绝对的。     

Java 5 then introduced the Executor API, whose thread pools greatly improved performance through Thread caching and reuse.    

java5才开始的Thread的缓存和重新利用的线程池应用。    

![线程池的Pattern](http://7xtrwx.com1.z0.glb.clouddn.com/89d7a27b645a2a7146c20fd61f0184ef.png)     

Netty’s EventLoop is part of a collaborative design that employs two fundamental APIs: concurrency and networking. First, the package io.netty.util.concurrent builds on the JDK package java.util.concurrent to provide thread executors. Second, the classes in the package io.netty.channel extend these in order to interface with Channel events.      

EventLoop的设计的基础：并发性和网络性。     

 event-handling logic must be generic and flexible enough to handle all possible use cases. Therefore, in Netty 4 all I/O operations and events are handled by the Thread that has been assigned to the EventLoop.    

事件处理逻辑需要足够的灵活能够处理所有的事件。     

The ScheduledExecutorService implementation has limitations, such as the fact that extra threads are created as part of pool management. This can become a bottleneck if many tasks are aggressively scheduled.     

netty的定时任务：    
~~~
ScheduledFuture<?> future = ch.eventLoop().scheduleAtFixedRate(...);
future.cancel(mayInterruptIfRunning)
~~~    

Netty’s EventLoop extends ScheduledExecutorService, so it provides all of the methods available with the JDK implementation, including schedule() and scheduleAtFixedRate(), used in the preceding examples. The complete list of all the operations can be found in the Javadocs for Scheduled- ExecutorService.3     

EventLoop扩展了ScheduledExecutorService接口，这个是比较的精彩的一个实现。所以EventLoop能够响应ScheduledExecutorService的所有的方法。     

The superior performance of Netty’s threading model hinges on determining the identity of the currently executing Thread; that is, whether or not it is the one assigned to the current Channel and its EventLoop. (Recall that the EventLoop is responsible for handling all events for a Channel during its lifetime.)    

当前执行的线程，当前的Channel，EventLoop 这三者是什么的关系？？     

~~~java    

		ScheduledFuture<?> futrueres = ctx.channel().eventLoop().scheduleAtFixedRate(thread, 1, 1, TimeUnit.SECONDS);    


    <V> ScheduledFuture<V> schedule(final ScheduledFutureTask<V> task) {
        if (inEventLoop()) {
            scheduledTaskQueue().add(task);
        } else {
            execute(new Runnable() {
                @Override
                public void run() {
                    scheduledTaskQueue().add(task);
                }
            });
        }

        return task;
    }     


 @Override
    public boolean inEventLoop(Thread thread) {
        return thread == this.thread;
    }
~~~
