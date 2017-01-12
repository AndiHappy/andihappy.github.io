---
layout: post
title: "Concurrence-23-Semaphores"
description: "翻译 梳理基础的东西"
category: 翻译 java 并发编程 多线程
tags: [翻译 并发编程 多线程]
---
#### Semaphores
<br/>
##### 信号量
<br/>
[文章的源地址](http://tutorials.jenkov.com/java-concurrency/semaphores.html)
<br/>

大意翻译，逐字逐句的翻译太累了，并且只为了翻译而翻译，不是看这篇文章的本意，
故采取大意翻译，当然首先还是得一句一句得看懂得，毕竟追求得就是看懂文章。   

信号量，也是一种并发的机制，可以被用来在线程之间发送信号，避免信号丢失；也
可以像锁一样保卫临界区的代码。java5以后，在java.util.concurrent包中有
Semaphore的实现，我们看一下它的实现原理。   

#### 简单的信号量
代码如下：   

```
public class Semaphore {
  private boolean signal = false;

  public synchronized void take() {
    this.signal = true;
    this.notify();
  }

  public synchronized void release() throws InterruptedException{
    while(!this.signal) wait();
    this.signal = false;
  }

}
```

take方法中发送一个信号，被储存在Semaphore上。  
release方法等待着一个信号。当接收到一个信号的时候，信号的标志位会被重新的
清洗，然后release方法会退出。     

使用信号量，可以避免信号的丢失。你可以使用take方法代替notify，使用release
代替wait，如果调用take方法在release方法之前，那么线程在调用release的时候
将会知道take已经被调用了，因为信号被存储在signal变量里面。wait和notify方
法不能够做到这一点。     

take 和 release 方法的成名比较的古怪。    

#### 使用信号量通信

两个线程之间使用Semaphore通信：

```
Semaphore semaphore = new Semaphore();

SendingThread sender = new SendingThread(semaphore);

ReceivingThread receiver = new ReceivingThread(semaphore);

receiver.start();
sender.start();
```

```
public class SendingThread {
  Semaphore semaphore = null;

  public SendingThread(Semaphore semaphore){
    this.semaphore = semaphore;
  }

  public void run(){
    while(true){
      //do something, then signal
      this.semaphore.take();

    }
  }
}
```

```
public class RecevingThread {
  Semaphore semaphore = null;

  public ReceivingThread(Semaphore semaphore){
    this.semaphore = semaphore;
  }

  public void run(){
    while(true){
      this.semaphore.release();
      //receive signal, then do something...
    }
  }
}
```
#### 计数信号量

上面Semaphore类的实现不能够统计出take调用的次数，也就是信号发送的次数。
变换一下实现，我们可以改写成为一个计数的信号量，如下所示：  

```
public class CountingSemaphore {
  private int signals = 0;

  public synchronized void take() {
    this.signals++;
    this.notify();
  }

  public synchronized void release() throws InterruptedException{
    while(this.signals == 0) wait();
    this.signals--;
  }

}
```

#### 边界信号量

CountingSemaphore没有设置它储存的信号量的上线，我们可以改变一下信号的
实现的方式,如下：  

```
public class BoundedSemaphore {
  private int signals = 0;
  private int bound   = 0;

  public BoundedSemaphore(int upperBound){
    this.bound = upperBound;
  }

  public synchronized void take() throws InterruptedException{
    while(this.signals == bound) wait();
    this.signals++;
    this.notify();
  }

  public synchronized void release() throws InterruptedException{
    while(this.signals == 0) wait();
    this.signals--;
    this.notify();
  }
}
```

现在take方法，只有到信号到达了上线的时候，线程才会被阻塞。
> 感觉 while循环里面的条件应该变为 this.signals >= bound   
仔细想想感觉还是应该是原来的条件。   

#### 把信号量当做锁来使用

如果把边界信号量的边界设置为1，使用take和release方法就能够保护临界区，
代码如下：   

```
BoundedSemaphore semaphore = new BoundedSemaphore(1);

...

semaphore.take();

try{
  //critical section
} finally {
  semaphore.release();
}
```

take 和 release 方法就像锁的lock 和 unlock 一样。    

如果我们把BoundSemaphore的上线设置为5，那么在同一时间内，可能就会有
五个线程进入临界区里面。当然，我们需要保证临界区有五个线程不会出现冲突，
否则应用就会出现错误。



















