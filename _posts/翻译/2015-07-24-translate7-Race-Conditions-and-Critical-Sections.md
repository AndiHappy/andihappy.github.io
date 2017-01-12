---
layout: post
title: "Concurrency-7-Race Conditions and Critical Sections"
description: "翻译 梳理基础的东西"
category: 翻译 java 并发编程 多线程
tags: [翻译 并发编程 多线程]
---
#### Race Conditions and Critical Sections 
<br/>
#####    竞争条件和临界区 
<br/>

文章的地址：[翻译文章的源地址](http://tutorials.jenkov.com/java-concurrency/race-conditions-and-critical-sections.html)
<br/>
一个应用中运行多个线程本身并不会出错，错误的产生在于多个线程访问相同的资源，例如相同的内存区域（变量，数组或者对象）
系统（数据库，web服务等等）或者是文件。事实上，问题只在多个线程同时向上面所说的资源中写的时候产生的。只要资源不发生变化，
多个线程读取同一份的资源是没有问题的。

下面就是一个多个线程同时运行的时候可能会失败的代码：  

```
  public class Counter {
     protected long count = 0;
     public void add(long value){
         this.count = this.count + value;
     }
  }
```

设想有两个线程，A和B，都在在执行相同的Count对象的实例的add方法，我们是没有办法知道操作系统是怎么在这两个线程之间切换的，
add方法的执行并不是一个简单的JVM的指令，相反的是，它的执行方式如下：   

```
   get this.count from memory into register
   add value to register
   write register to memory
```

观察下面A和B混合的执行的过程，会发生什么？   

```
   this.count = 0;
   A:  reads this.count into a register (0)
   B:  reads this.count into a register (0)
   B:  adds value 2 to register
   B:  writes register value (2) back to memory. this.count now equals 2
   A:  adds value 3 to register
   A:  writes register value (3) back to memory. this.count now equals 3
```

两个线程做的操作是给计数器添加值 2 和 3。因此两个线程完成执行完毕以后值应该是5的。然而，由于两个线程的执行交叉的
程从内存中读取的值为 0。然后他们把2或者3，添加到计数器，并将结果写回内存。那么计数器的值就取决于哪一个线程最后写入，
不只是正确的5. 在上述情况下，可能是线程 A，可能是线程 B最后写入。没有正确的线程同步机制就不会知道究竟如何交错线程执行。

##### 竞争条件和临界区

两个线程对同一块的资源进行竞争时，访问资源的顺序关系到最终的结果，这就可以称为竞争条件。临街区域就是导致竞争条件的那块
代码区。在先前的例子中，add方法就是临界区，导致了竞争条件。竞争条件可以通过在临界区采取恰当的线程同步机制避免。






























