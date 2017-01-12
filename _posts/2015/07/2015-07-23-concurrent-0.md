---
layout: post
title: "并发编程梳理"
description: "梳理工作中遇到的并发的问题"
category: 多线程 并发编程
tags: [多线程 基础务实 问题记录 并发编程]
---

#### 并发编程-线程基础
   1.Thread和实现Rnunable接口的类并不是等同的概念。run接口只是表明，实现了该接口，可以实例化这个类调用Run()
方法，仅仅代表着一个任务而已，具体的调用需要Thread来“指挥”

   2.当一个程序大量分配线程的时候，会爆出：unable to create new native thread 的错误，说明了什么的问题？说明，线程使用的是
堆外的内存空间，也说明线程Thread本身所对应的实例仅仅是JVM内普通的一个java对象，是一个线程操作的外壳，并不是真正的线程。    
具体的说明：[](http://sesame.iteye.com/blog/622670)

   3.那么Thread的实例对象调用对象调用start() 方法到底是怎么启动线程的呢？

   4.线程的状态 new running blocked  waiting  