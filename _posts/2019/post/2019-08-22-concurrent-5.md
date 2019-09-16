---
layout: post
title: "并发知识梳理：4. 读写锁等其他的类型的锁的支持"
subtitle: "AQS和ReentrantReadWriteLock"
author: "zhailzh"  
header-img: "img/post-bg-2015.jpg"  
catalog: true
tags: Concurrent  
---

ReentrantReadWriteLock 是在重入锁的基础上，添加读写的控制。主要是关注Condition的运用，底层的实现，更加清楚的明白重入锁的控制。

<!--more-->

