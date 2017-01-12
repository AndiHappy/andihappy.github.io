---
layout: post
title: "Concurrence-17-Starvation and Fairness"
description: "翻译 梳理基础的东西"
category: 翻译 java 并发编程 多线程
tags: [翻译 并发编程 多线程]
---
#### Starvation and Fairness
<br/>
##### 饥饿和公平
<br/>

文章的地址：[翻译文章的源地址](http://tutorials.jenkov.com/java-concurrency/starvation-and-fairness.html)
<br/>

如果一个线程因为其他的线程一直占用的CPU时间而没有被分配cpu时间，这种情况称之为饥饿。因为其他的线程一直的占有
CPU的时间，这个线程就会饿死。这个情况的解决办法：公平，所有的线程保证公平获得执行的机会。  

##### java中引起饥饿的原因 

主要有下面三个原因引起饥饿：
1.高优先级的线程吞掉了低优先级的CPU时间      
2.因为一直有其他的线程进入同步款，线程无限期的等待进入synchronized块    
3.其他的线程常被唤醒，这个线程一直等待着

##### 优先级高的线程吞掉优先级低的CPU时间

你可以单独的设置每一个线程的优先级。优先级比较高的线程肯定会比优先级低的获得更加多的时间，线程的优先级可以设置为1
到10，具体是怎么解释这10个优先级取决于你的应用运行在的操作系统。对大多数应用来说，优先级最好是固定的。   

##### 线程在进入synchronized同步块时阻塞

java的synchronized代码块阻塞，也是线程饥饿的一个原因。java的synchronized代码块不能够保证等待在同步代码外的线程
按照一定的顺序进入代码块内。这就意味着有一种可能：某一个线程一直被阻塞在代码块外面，永远的都在尝试着进入代码块内
因为总有其他的线程能够在它之前进入代码块内。这个问题就称为“饥饿”，一个线程可能会因为一直被其他线程占有着CPU，自己
因为分配不到CPU而被饿死。    

##### 线程在一个对象上无限期的等待下去

如果多个线程都调用了一个对象的wait方法，notify方法的调用不能够保证那一个的线程就能够被唤醒。其他的任何线程都在等待
，这个就有一个情况，某一个线程永远的被等待着，因为每次唤醒的都是其他的线程。

#### java中公平的实现

虽然不能够保证100%的公平，但是我们可以提高我们的同步实现机机制来提高线程之间的公平性。
首先，我们先看一段同步代码块的代码：  

```
public class Synchronizer{

  public synchronized void doSynchronized(){
    //do a lot of work which takes a long time
  }

}
```

如果多个线程调用doSynchronized()方法，其中的一些线程就会阻塞知道第一个线程得到访问的权限并且执行
结束。如果多个线程都等待着，那么这里就没有任何的保证下次哪一个线程获得访问的权限。    

#### 使用锁而不是synchronized代码块

为了实现等待线程之间的公平性，我们首先通过一个锁来保证代码块的线程安全，替换掉原来的synchronized
代码块：   

```
public class Synchronizer{
  Lock lock = new Lock();

  public void doSynchronized() throws InterruptedException{
    this.lock.lock();
      //critical section, do a lot of work which takes a long time
    this.lock.unlock();
  }

}
```

代码中，doSynchronized方法不再声明为synchronized，替代它的是使用lock.lock() 和
lock.unlock()方法保护着竞争区域。

锁的简单的实现，如下代码：

```
public class Lock{
  private boolean isLocked      = false;
  private Thread  lockingThread = null;

  public synchronized void lock() throws InterruptedException{
    while(isLocked){
      wait();
    }
    isLocked      = true;
    lockingThread = Thread.currentThread();
  }

  public synchronized void unlock(){
    if(this.lockingThread != Thread.currentThread()){
      throw new IllegalMonitorStateException(
        "Calling thread has not locked this lock");
    }
    isLocked      = false;
    lockingThread = null;
    notify();
  }
}

```

如果你看看上面的同步类，看着这把锁执行，你会发现，如果多个线程同时调用lock()，线程试图访问lock()
方法被阻塞。第二，如果锁被锁定，线程被阻止在lock方法的while循环里wait()方法。以前说过：调用了
wait方法，就释放了Lock实例上的锁，所以其他的线程就能够进入lock方法，这样所有的线程都能够进入
lock方法，调用了wait。          

doSynchronized方法中，在lock()和unLock()方法中间的评论：两者之间的代码需要很长一段时间才能够执行
结束。我们首先假设：这段代码执行的时间会比进入lock方法和调用wait方法要长很多，因为锁已经被锁定。
这就意味着，等待锁着锁和进入临界区所花的时间，大多数时间花在在wait()方法的调用上，而不是阻塞在
试图进入lock()方法。      

像先前提到的，多个线程等在在synchronized同步代码块之外的，不能够保证哪一个线程能够进入代码块，
现在调用wait()的方法等待的线程也不能够决定在调用了notify()方法后那一个线程被唤醒。所以现在这个
版本的Lock类，与synchronized相比较的话，也不能够保证线程之间的公平。但是我们可以修改Lock类。

现在版本的Lock类，调用它自己的wait()方法，如果每一个线程调用一个分开对象的wait()方法，这样的话
每一个线程都会调用一个对象的wait()方法，Lock类决定那一个对象的notify()方法会被调用，这样就能够
精确的确定哪一个线程会被唤醒。       

#### 一个公平锁

下面就是在原来Lock上面修改的公平锁，叫做FairLock，对比着先前的Lock类，在实现上面 和synchronized，wait()/notify()
有一点区别。      

到达设计一个公平锁，前面几篇已经说明了几个问题：嵌套管程锁死，条件滑丢，信号丢失，这些都会在这个翻译
中涉及到，现在我们先看这个公平锁，每一个线程调用lock都要排队，只有第一个线程能够获得FairLock实例
上面的锁，如果它解锁，其他的线程会一直阻塞直到他们到达队列的顶部。       

```
public class FairLock {
    private boolean           isLocked       = false;
    private Thread            lockingThread  = null;
    private List<QueueObject> waitingThreads =
            new ArrayList<QueueObject>();

  public void lock() throws InterruptedException{
    QueueObject queueObject           = new QueueObject();
    boolean     isLockedForThisThread = true;
    synchronized(this){
        waitingThreads.add(queueObject);
    }

    while(isLockedForThisThread){
      synchronized(this){
        isLockedForThisThread =
            isLocked || waitingThreads.get(0) != queueObject;
        if(!isLockedForThisThread){
          isLocked = true;
           waitingThreads.remove(queueObject);
           lockingThread = Thread.currentThread();
           return;
         }
      }
      try{
        queueObject.doWait();
      }catch(InterruptedException e){
        synchronized(this) { waitingThreads.remove(queueObject); }
        throw e;
      }
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
      waitingThreads.get(0).doNotify();
    }
  }
}
```
  
```
public class QueueObject {

  private boolean isNotified = false;

  public synchronized void doWait() throws InterruptedException {
    while(!isNotified){
        this.wait();
    }
    this.isNotified = false;
  }

  public synchronized void doNotify() {
    this.isNotified = true;
    this.notify();
  }

  public boolean equals(Object o) {
    return this == o;
  }
}
```
首先，lock方法已经不再声明为synchronized，替代的是需要同步的内容放在了synchronized代码块里面。        

在FairLock中，当每一个线程在调用lock的时候，创建了一个新的QueueObject实例，并且加入了一个队列。
线程在调用unlock的时候，将会取队列顶部的QueueObject实例，调用上面的doNotify()方法，唤醒等待这个
类上面的线程。这种方式在某一个时刻只有一个线程被唤醒，而不是所有的全部被唤醒，这一部分作为FairLock公平性
的一部分，。    

在同一个synchronized代码块里面校验和设置锁的状态，避免条件丢失。   

另外QueueObject 是一个真的信号量。dowait方法和doNotify方法把信号保存在QueueObject的内部，这样能够避免信号的丢失
的情况发生，例如：另外一个线程刚刚调用了unLock方法，就是QueueObject.doNotify()方法，这个线程就去调用QueueObject
的doWait方法。 QueueObject.doWait调用在synchronized(this),避免嵌套管程死锁。所以当没有线程在
synchronized(this)代码块的lock方法内另外的线程可以调用unlock方法。

最终，queueObject.doWait()的调用是在一个try-catch里面，如果一个InterruptedException 异常抛出的时候，
我们需要把它移除队列。    

性能

如果你比较Lock和fairLock的代码，你会发现在lock和unlock方法中，FairLock总多了一些东西，额外多出的代码会使FairLock执行的时候比Lock慢一点。
这点对你的应用的影响，取决于被公平锁保护的竞争区内代码的执行时间，竞争区内的代码执行时间越长，影响就会越小，当然它也和这段代码执行的频率有关。







































































