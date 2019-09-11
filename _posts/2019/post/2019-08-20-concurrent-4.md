---
layout: post
title: "并发知识梳理：3. 通过具体的应用展示锁的实现机制"
subtitle: "AQS和ReentrantLock"
author: "zhailzh"  
header-img: "img/post-bg-2015.jpg"  
catalog: true
tags: Concurrent  
---

上一节关于AQS的锁的架构实现，说的非常的抽象，看的非常的费劲，这次通过代码的注释和说明，来展示具体是如何实现的。

<!--more-->

## ReentrantLock

关于可重入锁带着两个问题，去分析的：
1. 怎么判断的重入，如何增加锁的次数的。
2. 重入次数的判断，释放锁的时候，如果消减锁的次数，释放以后，其他的等待的线程是如何响应的

