---
layout: post
title: "Concurrence-22-Reentrance Lockout"
description: "翻译 梳理基础的东西"
category: 翻译 java 并发编程 多线程
tags: [翻译 并发编程 多线程]
---
#### Reentrance Lockout
<br/>
##### 重入锁死
<br/>

文章的地址：[翻译文章的源地址](http://tutorials.jenkov.com/java-concurrency/reentrance-lockout.html)
<br/>

重入锁死是类似 [deadlock](http://tutorials.jenkov.com/java-concurrency/deadlock.html) 和 
[nested monitor lockout](http://tutorials.jenkov.com/java-concurrency/nested-monitor-lockout.html)
的情况，重入锁死的情况在 [locks](http://tutorials.jenkov.com/java-concurrency/locks.html) 和 
[Read/Write Locks](http://tutorials.jenkov.com/java-concurrency/locks.html)        

当一个线程重入一个锁（Lock，ReadWriteLock，或者其他的不支持重入性的synchronized
同步代码块）的时候，有可能发生重入锁死的情况。重入，意味着一个线程已经拥有了一个
锁，再次的进入的情况。因为synchronized代码块是支持可重入的，因此下面的代码是没
有问题的。  

```
public class Reentrant{

  public synchronized outer(){
    inner();
  }

  public synchronized inner(){
    //do something
  }
}
```

现在 outer 和 inner 全部被声明了synchronized，在java中就相当于
synchronized(this)代码块。如果一个线程调用了outer方法，然后再去
调用inner，是没有问题的。因为两个方法都是同步在一个监测对象（this）
上。如果一个线程拥有一个监测对象上面的锁，那么它能够访问这个监测对象
上所有的同步代码块，这个就称之为：可重入性。线程能够重入它拥有的锁对应
的全部的同步代码块。    

下面的Lock的实现，就不是可重入性：    

```
public class Lock{

  private boolean isLocked = false;

  public synchronized void lock()
  throws InterruptedException{
    while(isLocked){
      wait();
    }
    isLocked = true;
  }

  public synchronized void unlock(){
    isLocked = false;
    notify();
  }
}
```
如果一个线程调用了lock两次，但是两次调用的中间并没有调用unlock，那么第二次
调用lock的时候，就会阻塞。重入锁死的情况就会发生。    

为了避免重入锁死的情况，我们有两个方法：   

1.避免写重入锁死的代码    
2.使用可重入的锁   

具体的那种策略适合你的工程，就取决于你工程的具体的情况。重入锁的执行
和非重入锁的执行过程不一样，实现起来也比较的困难，但是对你来说可能用
不到。是否实现，以及没有锁的可重入性，对你的代码是否有影响，取决于你
的具体的情况。





























