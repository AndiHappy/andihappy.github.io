---
layout: post
title: "Concurrence-19-Slipped Conditions"
description: "翻译 梳理基础的东西"
category: 翻译 java 并发编程 多线程
tags: [翻译 并发编程 多线程]
---
#### Slipped Conditions
<br/>
##### 滑条件
<br/>

文章的地址：[翻译文章的源地址](http://tutorials.jenkov.com/java-concurrency/slipped-conditions.html)
<br/>

##### 滑条件是什么

滑条件：从某一个时刻起这个线程一直在检查一个状态知道它能够根据这个状态执行动作
，这个状态已经被另外的一个线程改变，所以对第一个线程来说，再去根据这个状态去执行
行为就是错误的。下面就是一个案例：

```
public class Lock {

    private boolean isLocked = true;

    public void lock(){
      synchronized(this){
        while(isLocked){
          try{
            this.wait();
          } catch(InterruptedException e){
            //do nothing, keep waiting
          }
        }
      }

      synchronized(this){
        isLocked = true;
      }
    }

    public synchronized void unlock(){
      isLocked = false;
      this.notify();
    }

}
```
注意：lock方法包含了两个同步的代码块，第一个代码块会阻塞线程直到isLocked变为
false。第二个代码块 设置isLocked为true，为其他的线程等待锁住的Lock实例。

设想一种情况，isLocked是false，两个线程同时的调用lock方法，第一个线程线程在
第一个代码块阻塞之前进入了第一个代码块，这个线程将会检查isLocked，结果是false，
然后第二个线程执行，因此也能够进入第一个代码块，这个线程检查isLocked也是false，
现在两个线程全部读到了isLocked状态全部是false，然后这两个线程都会进入第二个代码
块，把isLocked设置为true，继续运行。

这种情况就是滑条件的一个案例，这两个线程测试条件，然后退出synchronized块，
从而允许其他线程测试条件，在这两个线程之前更改状态条件。换句话说，这个状态
条件从状态被检查到线程为了接下来的线程再次的设置的这段时间，滑了。    

为了避免滑条件，检查和设置状态的动作必须被执行的线程原子的操作。这也就意味着
没有其他的线程能够在检查和设置状态的间隙中对装填进行检查和设置。      

可以针对上面例子做简单的修改，只需要把 isLocked = true 这行转移到 synchronized
代码块的里面去即可，既是在循环的下面，如下：

```
public class Lock {

    private boolean isLocked = true;

    public void lock(){
      synchronized(this){
        while(isLocked){
          try{
            this.wait();
          } catch(InterruptedException e){
            //do nothing, keep waiting
          }
        }
        isLocked = true;
      }
    }

    public synchronized void unlock(){
      isLocked = false;
      this.notify();
    }

}

```
现在检查和设置isLocked的变量全部在一个synchronized代码块里面的原子的操作了。

#### 一个更加现实的例子

我们可以永远也不会实现像上面的那个例子的锁，从而认为滑条件只不过是一种理论。
但是上面简单的例子仅仅是为了说明滑条件的概念。   

一个更加现实的例子，就是在公平锁的实现的过程中，这个我们已经在 饥饿和公平 这篇
中讨论过了，如果我们看到原来的在 嵌套管程锁死的那篇文章中的实现，并且试图解决
嵌套管程锁死的问题，就会很容易犯下滑条件的错误，例如下面的代码：

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

我们可以看到 带有QueueObject.wait()的synchronized(queueObject) 嵌套在
synchronized(this)代码块中，这也就导致可嵌套线程锁死的情况，为了避免这个
问题，我们把synchronized(queueObject) 挪出 synchronized(this)的代码块，
结果是：   

```
//Fair Lock implementation with slipped conditions problem

public class FairLock {
  private boolean           isLocked       = false;
  private Thread            lockingThread  = null;
  private List<QueueObject> waitingThreads =
            new ArrayList<QueueObject>();

  public void lock() throws InterruptedException{
    QueueObject queueObject = new QueueObject();

    synchronized(this){
      waitingThreads.add(queueObject);
    }

    boolean mustWait = true;
    while(mustWait){

      synchronized(this){
        mustWait = isLocked || waitingThreads.get(0) != queueObject;
      }

      synchronized(queueObject){
        if(mustWait){
          try{
            queueObject.wait();
          }catch(InterruptedException e){
            waitingThreads.remove(queueObject);
            throw e;
          }
        }
      }
    }

    synchronized(this){
      waitingThreads.remove(queueObject);
      isLocked = true;
      lockingThread = Thread.currentThread();
    }
  }
}
```

因为只有lock方法发生了变化，所以只展示了lock方法的代码。       


现在lock方法包含了3个synchronized代码块。     

第一个synchronized(this) 检查状态通过设置mustWait=isLocked || waitingThreads.get(0) != queueObject. 

第二个synchronized(queueObject)代码块，检查线程是否需要等待，在这个时候，
另外的一个线程可能已经释放了lock锁。让我们忽略时间上面的因素，假设这个锁已经
释放，这个线程会立刻的退出synchronized(queueObject)代码块。     


第三个代码块只有mustwait = false的时候才会执行，这个代码块会把isLocked
设置为true等等，离开lock方法。     

设想在同一时间上，锁没有锁的情况下，两个线程同时的调用lock方法，第一个线程1
将会检查isLocked状态，看到它是false，然后第二个线程会做同样的事情，然后这
两个线程都会等待，将会把isLocked设置为true，这就是滑条件的主要的一种情况。     

#### 解决滑条件

为了解决上面滑条件的问题，最后的一个synchronized(this)必须转移到第一个
同步代码块，代码就需要做一点小的调整，如下所示：      

```
//Fair Lock implementation without nested monitor lockout problem,
//but with missed signals problem.

public class FairLock {
  private boolean           isLocked       = false;
  private Thread            lockingThread  = null;
  private List<QueueObject> waitingThreads =
            new ArrayList<QueueObject>();

  public void lock() throws InterruptedException{
    QueueObject queueObject = new QueueObject();

    synchronized(this){
      waitingThreads.add(queueObject);
    }

    boolean mustWait = true;
    while(mustWait){

      
          synchronized(this){
          mustWait = isLocked || waitingThreads.get(0) != queueObject;
          if(!mustWait){
          waitingThreads.remove(queueObject);
          isLocked = true;
          lockingThread = Thread.currentThread();
          return;
          }
          }     

      synchronized(queueObject){
        if(mustWait){
          try{
            queueObject.wait();
          }catch(InterruptedException e){
            waitingThreads.remove(queueObject);
            throw e;
          }
        }
      }
    }
  }
}
```


现在局部变量mustwait 设置和检查都在同一个同步代码快里面了，即使mustwait
仍然在synchronized(this)外，while(mustwait)被检查，mustwait的值也没有
在synchronized(this)被改变。一个线程计算出来mustwait为false，会自动把
局部变量(isLocked)设置为true，这样其他的线程就检测到状态就会变为true。      

return;语句，在synchronized(this)同步块里面，是没有必要的。它只是一个小小
的优化，如果线程不必要等(mustwait == false),就没有必要进入synchronized(queueObject)
代码块，执行if(mustWait)判断。     

公平锁的实现如果仔细的说，会有信号丢失的问题。设想公平锁实例已经被一个线程
调用lock方法锁住了，所以在synchronized(this)里面，mustWait是true。设想
中线程是先调用lock的方法，然后锁住的线程在调用unlock的方法。我们以前的unlock
的方法的实现，你会发现我们调用了 queueObject.wait()，但是如果没有线程在lock
里面还没有调用queueObject.wait(),那么调用queueObject.notify()就会丢失，
信号就丢失了，当线程在调用queueObject.wait()后正确的调用lock方法，它就会
被阻塞直到其他的线程调用unlock方法，这个可能永远不会发生。     

丢失信号的问题，在饥饿和公平这篇中FairLock公平锁的实现中，把QueueObject类
转化为带有两个方法 doWait()和doNotify()方法的信号。这些方法会在QueueObject
类内部保存和重置信号量，这样的话即使doNotify()的调用在doWait()之前，信号也是
不会丢失的。













































