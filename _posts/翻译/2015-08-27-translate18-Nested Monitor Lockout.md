---
layout: post
title: "Concurrence-18-Nested Monitor Lockout"
description: "翻译 梳理基础的东西"
category: 翻译 java 并发编程 多线程
tags: [翻译 并发编程 多线程]
---
#### Nested Monitor Lockout
<br/>
##### 嵌套管程锁死
<br/>

文章的地址：[翻译文章的源地址](http://tutorials.jenkov.com/java-concurrency/nested-monitor-lockout.html)
<br/>

##### 嵌套管程锁死是怎么发生的

嵌套管程锁死和死锁比较的像，像下面的这种情况就会发生嵌套管程锁死：  

```
Thread 1 synchronizes on A
Thread 1 synchronizes on B (while synchronized on A)
Thread 1 decides to wait for a signal from another thread before continuing
Thread 1 calls B.wait() thereby releasing the lock on B, but not A.

Thread 2 needs to lock both A and B (in that sequence)
        to send Thread 1 the signal.
Thread 2 cannot lock A, since Thread 1 still holds the lock on A.
Thread 2 remain blocked indefinately waiting for Thread1
        to release the lock on A

Thread 1 remain blocked indefinately waiting for the signal from
        Thread 2, thereby
        never releasing the lock on A, that must be released to make
        it possible for Thread 2 to send the signal to Thread 1, etc.
```


这可能听起来像一个漂亮的理论情况，但看下面的没有考虑到这种情况的锁实现: 

```
//lock implementation with nested monitor lockout problem

public class Lock{
  protected MonitorObject monitorObject = new MonitorObject();
  protected boolean isLocked = false;

  public void lock() throws InterruptedException{
    synchronized(this){
      while(isLocked){
        synchronized(this.monitorObject){
            this.monitorObject.wait();
        }
      }
      isLocked = true;
    }
  }

  public void unlock(){
    synchronized(this){
      this.isLocked = false;
      synchronized(this.monitorObject){
        this.monitorObject.notify();
      }
    }
  }
} 

```

注意，lock方法，第一个锁在this上面，第二个在monitorObject上面。如果isLocked是false的情况，这个写
法没有任何的问题，这个线程不会调用monitorObject.wait()方法，如果isLocked是true的时候，这个线程
就会调用lock()方法，暂停在调用monitor.wait()方法。    

问题是，调用了monitorObject.wait()方法只是释放了在monitorObject上面的锁，没有释放在this上面的
锁，也就是在this上面的监测还没有释放，换句话说，这个线程休眠了，但是还拥有这this上面的锁。     

当一个线程锁住了这个锁之后，试图去调用unlock方法来解锁的时候，它在试图进入unlock方法的synchronized
(this)的时候，会被阻塞。它将会被阻塞直到等待着lock方法那个线程释放synchronized锁，也就是说：离开
synchronized(this)的代码块，但是等在lock方法那个线程是不会离开的知道isLocked被设置为false，
monitoObject.notify()方法被调用，但是这个方法又在unlock方法内。      


简短的说，等在lock方法的线程，需要一个unlock调用，来退出lock方法，离开synchronized同步块。但是，
没有线程能够执行unlock方法，知道等待在lock方法中的那个线程离开同步代码快。    

结果就是任何调用lock或者unlock方法的线程都会被无限期的被阻塞，这种情况就被称之为嵌套管程死锁。

##### 一个更加现实的例子

你可能说你永远也不会实现像上面的一个锁，你可能不会在一个监测对象的内部调用wait或者notify，这也有可能。
但是某种情况下设计上面也会出现这种错误，例如，我们以前实现的公平锁，我们是每一个线程都在一个object上面
调用wait，这个object会被放在队列里面，这样我们能够在某一个时候唤醒某一个特定的线程。   

看一下下面这个FairLock的实现： 

```
//Fair Lock implementation with nested monitor lockout problem

public class FairLock {
  private boolean           isLocked       = false;
  private Thread            lockingThread  = null;
  private List<QueueObject> waitingThreads =
            new ArrayList<QueueObject>();

  public void lock() throws InterruptedException{
    QueueObject queueObject = new QueueObject();

    synchronized(this){
      waitingThreads.add(queueObject);

      while(isLocked || waitingThreads.get(0) != queueObject){

        synchronized(queueObject){
          try{
            queueObject.wait();
          }catch(InterruptedException e){
            waitingThreads.remove(queueObject);
            throw e;
          }
        }
      }
      waitingThreads.remove(queueObject);
      isLocked = true;
      lockingThread = Thread.currentThread();
    }
  }

  public synchronized void unlock(){
    if(this.lockingThread != Thread.currentThread()){
      throw new IllegalMonitorStateException(
        "Calling thread has not locked this lock");
    }
    isLocked      = false;
    lockingThread = null;
    if(waitingThreads.size() > 0){
      QueueObject queueObject = waitingThread.get(0);
      synchronized(queueObject){
        queueObject.notify();
      }
    }
  }
}
```

```
public class QueueObject {}
```

第一眼看过去，感觉没有什么问题，但是注意到lock方法中是怎么调用QueueObject.wait()方法的：穿过
了两个synchronized代码块。一个是同步在this上面，一个是嵌套在queueObject一个局部变量上，但是
当一个线程调用queueObject.wait()方法的时候，将会释放在QueueObject实例上面的锁，但是没有释放
this上面关联的锁。   

另外，在unlock方法中声明了synchronized方法，这也就等价于synchronized(this)。这也就意味着：
一个在lock方法内等待着的线程，拥有this上面的锁，其他所有的线程调用unlock的时候都会被无限期的
阻塞，因为要等待在lock里面的线程释放this上面的锁，这也是不可能放生的。因为释放this上面的锁，
必须有一个线程成功的释放一个信号到等待的线程，这个行为只能在执行unlock方法的时候才会释放这个信号。    

所以，上面的FairLock的实现方式，将会导致嵌套管程锁死，比较好像实现在 公平和饥饿中有描述。    

##### 嵌套管程锁死 和 死锁

嵌套管程锁死和死锁的结果比较的相像：线程都是无限期的等待着彼此。   

但是两种情况又不是完全的相同，就像以前在 死锁中解释道那样：死锁发生的时候是两个线程取得锁的顺序
不同。线程1取得锁A，等待锁B，线程2取得锁B，等待锁A。在死锁预防文中解释的一样：死锁是可以通过按照
相同的顺序请求锁来预防，然而，嵌套管程锁死发生的时候，两个线程请求锁的顺序是一样的，线程1占有了
锁A和锁B，释放了锁B，等待线程2的信号，线程2需要锁A和锁B，来推送信号给线程1.所以是一个线程等待着
信号，一个线程是等待锁的释放。   

两者之间的不同，总结如下：   

```
In deadlock, two threads are waiting for each other to release locks.

In nested monitor lockout, Thread 1 is holding a lock A, and waits
for a signal from Thread 2. Thread 2 needs the lock A to send the
signal to Thread 1.
```









































