---    
layout: post  
title: "JDK并发框架的描述（一）"  
subtitle: "重入锁，读写锁，还有一些扩展的锁的实现的基础（一）"  
date: 2017-08-26 09:00:00  
author: "zhailzh"  
header-img: "img/post-bg-2015.jpg"  
catalog: true  
tags:  
- 学习  
categories:  
- concurrent
---  
   梳理JDK中并发编程的实现的原理，感觉很多东西还是非常的有意思的，例如锁的实现。我们首先说：重入锁，读写锁，还有一些扩展的锁的实现的基础：同步器（java.util.concurrent.locks.AbstractQueuedSynchronizer）
<!--more-->

java.util.concurrent.locks.AbstractQueuedSynchronizer 为什么称之为实现锁的基础呢？    
这个我们需要从头说起。