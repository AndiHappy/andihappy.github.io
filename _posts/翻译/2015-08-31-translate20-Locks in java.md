---
layout: post
title: "Concurrence-20-Locks in Java"
description: "翻译 梳理基础的东西"
category: 翻译 java 并发编程 多线程
tags: [翻译 并发编程 多线程]
---
#### Locks in Java
<br/>
##### java中的锁
<br/>

文章的地址：[翻译文章的源地址](http://tutorials.jenkov.com/java-concurrency/locks.html)
<br/>

一个锁是一个同步的机制，比java的关键字synchronized更加复杂的一个同步的机制，
锁也是使用synchronized关键字构成的，所以我们才能够使用synchronized关键字。    

自从java5以来，包java.util.concurrent.locks里面包含有几个锁的实现，所以我们
没有必要实现自己的锁。但是我们需要知道怎么使用这些锁，当然知道这些锁是如何实现的
也是比较有好处的，在以后，我们会说到这点。

#### 一个简单的锁

我们还是以一个synchronized代码块开始：   

```
public class Counter{

  private int count = 0;

  public int inc(){
    synchronized(this){
      return ++count;
    }
  }
}
```
现在synchronized(this)代码块在inc()方法里面，这个代码块保证只有在同一个时间
只有一个线程执行  return ++count 。同步块中的代码可以更先进，但简单++计数
即可完成任务。    

Counter类也可以像下面这样写，使用Lock替代synchronized代码块：   

```
public class Counter{

  private Lock lock = new Lock();
  private int count = 0;

  public int inc(){
    lock.lock();
    int newCount = ++count;
    lock.unlock();
    return newCount;
  }
}
``` 
lock方法会锁住lock实例，所以所有的调用lock方法都会被阻塞直到unlock方法的执行。   

lock方法的简单的实现：   

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

备注，while(isLocked)循环，这个叫做自旋锁。自旋锁和wait(),notify()方法在
文章 线程通信 中有详细的描述。当isLocked为true的时候，线程调用lock方法的时候
会在wait方法上等待着。在某种以为的情况下，线程将从wait等待中返回，并且并没有
接收到一个notify调用的唤醒信号。在这种情况下，线程应该重新检查isLocked状态，
看看是不是能够安全的运行，而不是直接假设线程被唤醒，能够安全的运行了。如果isLocked
为false，线程就会离开那个while(isLocked)循环，重新设置isLocked为true，为了
lock实例能够被其他调用lock的线程锁住。       

当一个线程执行完了临界区的代码后，线程就会调用unlock方法，执行unlock方法，就是
把isLocked重新置为false，唤醒一个等待着的线程。     


#### 锁的重入

java中的synchronized是可重入的，意思就是说：如果一个java线程进入了一个
synchronized代码块，就是占有了同步在监测对象上面的锁，这个线程就能够进入
在同一个监测器上面的synchronized代码块，下面就是例子：    

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

outer 和 inner 都声明了synchronized，在java语言中就相当于synchronized(this)
同步代码块，如果一个线程调用了outer，那么再去调用inner就没有问题，因为两个方法
都是同步在同一个监测对象上（this），如果一个线程获得了某一个监测对象上面的锁，
那么它可以访问这个锁上面的所有的同步代码块，这个就叫做可重入性。如果线程拥有了
锁，他就能够进入这个锁对应的所有的代码块。   

上面的Lock的实现（刚开始的那个Lock类），不是可重入的，如果我们重写Reentrant
类，线程将会阻塞在调用outer时，进入调用inner方法后的 lock.lock方法上。   

```
public class Reentrant2{

  Lock lock = new Lock();

  public outer(){
    lock.lock();
    inner();
    lock.unlock();
  }

  public synchronized inner(){
    lock.lock();
    //do something
    lock.unlock();
  }
}
```

一个线程首先调用outer方法的时候，会锁住lock实例，然后调用inner，在inner方法
中，线程再次的请求锁lock实例，这次会失败，也就是线程将会被阻塞，因为lock实例
已经被在outer中锁住过了。   

第二次调用lock被锁住的原因是，没有调用unlock的方法，下面是具体代码：   

```
public class Lock{

  boolean isLocked = false;

  public synchronized void lock()
  throws InterruptedException{
    while(isLocked){
      wait();
    }
    isLocked = true;
  }

  ...
}
```

决定线程是否能够离开lock方法的是while循环中的状态值，目前情况是，
不管什么线程锁定它，是否锁定必须设置为false。

为了让Lock类支持可重入，我们做一个小变动：   

```
public class Lock{

  boolean isLocked = false;
  Thread  lockedBy = null;
  int     lockedCount = 0;

  public synchronized void lock()
  throws InterruptedException{
    Thread callingThread = Thread.currentThread();
    while(isLocked && lockedBy != callingThread){
      wait();
    }
    isLocked = true;
    lockedCount++;
    lockedBy = callingThread;
  }


  public synchronized void unlock(){
    if(Thread.curentThread() == this.lockedBy){
      lockedCount--;

      if(lockedCount == 0){
        isLocked = false;
        notify();
      }
    }
  }

  ...
}

```

现在while循环(自旋锁)现在也需要考虑锁定锁的线程实例，如果锁是没有锁的或者调用
的线程是拥有Lock实例锁的线程，while循环将不会执行，线程调用lock将会被允许离开
这个方法。   


另外，我们需要计算这个锁被同一个线程锁住的次数。否则，无论这个锁被锁住了几次，
一个unlock调用就会释放这个锁。我们不希望锁被释放直到有线程锁住它，并且执行了
unlock的次数和lock的次数一样多。    

现在Lock类就是可重入的了。     

#### 锁的公平性

java的synchronized代码块不能够保证线程键入的顺序，因此，如果很多的线程频繁的
访问同一个代码块的时候，就会有一个或者多个线程从来没有得到访问的机会，访问的机会
都被其他的线程抢走了，这种情况叫做饥饿。为了避免这个问题，一个锁必须公平。因为
在这篇文章中，锁的实现都是采用的synchronized关键字，所以不保公平。饥饿和工程
在文章  Starvation and Fairness 中有详细的描述。  

#### 在finally-cause中调用unlock

当使用lock保护一个临界区的时候，这个临界区中可能抛出异常，那么在finally-cause
中调用unlock变得比较的重要。这样所的原因是在抛出异常后，锁就被解锁了，其他的线程
又能够重新的加锁。如下例：    

```
lock.lock();
try{
  //do critical section code, which may throw exception
} finally {
  lock.unlock();
}
```

这段的代码可以保证在临界区的抛出异常的时候，Lock能够被解锁。如果
unlock没有在finally-clause调用，当临界区抛出异常的时候，这个Lock
中就会被永远的锁住了，其他的线程再去Lock实例加锁，就会永远的阻塞。




























































