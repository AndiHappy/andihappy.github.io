---    
layout: post  
title: "JDK并发框架的描述（二）"  
subtitle: "读写锁实现的说明"  
date: 2017-08-27 09:00:00  
author: "zhailzh"  
header-img: "img/post-bg-2015.jpg"  
catalog: true  
tags:  
- 学习  
categories:  
- concurrent
---  
   读写锁也是在同步器（java.util.concurrent.locks.AbstractQueuedSynchronizer）的基础上面实现的，不过就是复杂了点。

<!--more-->

  读写锁的主要的要求就是：     
> 1. 读线程占有线程锁状态的情况下，其余的读线程都能进入临界区，执行代码。但是写线程会被阻塞。
> 2. 写线程占有线程锁的情况下，其他的读线程，写线程都会被阻塞

待续。。。。。。。。。
