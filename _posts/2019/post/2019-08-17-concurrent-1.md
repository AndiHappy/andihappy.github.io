---
layout: post
title: "并发知识梳理：概述"
subtitle: ""
author: "zhailzh"  
header-img: "img/post-bg-2015.jpg"  
catalog: true
tags: Concurrent  
---

梳理自己所掌握的，关于并发的内容。稳固而知道新的东西，想更加深入的方向，夯实自己薄弱基础。

<!--more-->

并发的东西，涉及的比较的多，自己只能从自己所知的角度来说明，来复习。

从是否设计到一个JVM，还是多个JVM的角度划分：

>    1. 单机版本的并发    

>    2. 多机版本的并发

我们首先说单机版本的并发支持，这个主要就是JDK支持的并发模型了。JDK的并发的模型：**java.util.concurrent** 首先列一下需要关注的内容，然后，一个一个的梳理。

### 0. synchronized,Volatile关键字 和其相关联的wait，notify机制。


### 1. java.util.concurrent.locks

Lock 锁，读写锁，可重入锁，Condition以及他们的实现的机制，具体的原理。

### 2. java.util.concurrent.atomic
   
AtomicInteger，AtomicLong，AtomicReference，AtomicIntegerArray，AtomicLongArray 等相关的内容，运用的方式，原理性的内容。


### 3. java.util.concurrent.BlockingQueue<E>

在其上的ArrayBlockingQueue，ConcurrentLinkedQueue Deque等等的实现的方式


### 4. java.util.concurrent.ConcurrentMap

ConcurrentHashMap，ConcurrentSkipListMap，等等二元的数据结构

### 5. CountDownLatch，CyclicBarrier，Exchanger

比较经典的并发场景


### 4. java.util.concurrent.AbstractExecutorService

在其上的线程池的原理：

java.util.concurrent.ForkJoinPool，  

java.util.concurrent.ScheduledExecutorService，  

java.util.concurrent.ThreadPoolExecutor  以及其关联的 Callable，Thread，CompletionService等






