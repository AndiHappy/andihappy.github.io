---
layout: post
title: "Concurrence-14-Thread Signaling"
description: "翻译 梳理基础的东西"
category: 翻译 java 并发编程 多线程
tags: [翻译 并发编程 多线程]
---
#### Thread Signaling
<br/>
##### 线程通信
<br/>

文章的地址：[翻译文章的源地址](http://tutorials.jenkov.com/java-concurrency/thread-signaling.html)
<br/>

线程通信的目的是使线程能够发送信号给对方。此外，线程也能使线程结构其他线程发送的信号。
例如，一个线程B可能等待线程A的信号：数据已准备好处理。

##### 基于共享变量的通信

线程之间彼此之间发送信号的一个简单的方法是:把信号的值设置在在某些共享对象变量上。线程A设置成员布尔变量 
hasDataToProcess 为true，线程B可能会读这个成员变量 ，两者都在同步代码块中。下面就是一个简单的例子，说明
一个对象能够包含一个信号量，并且提供校验和设置的方法。   

```
public class MySignal{

  protected boolean hasDataToProcess = false;

  public synchronized boolean hasDataToProcess(){
    return this.hasDataToProcess;
  }

  public synchronized void setHasDataToProcess(boolean hasData){
    this.hasDataToProcess = hasData;  
  }

}
```

线程A和线程B为了能够相互通信协同工作，必须包含一个引用针对一个共享的MySignal实例的引用。如果线程A和B的
引用指向了不同的MySignal的实例，他们就不会发现彼此的信号。要处理的数据可以位于一个共享缓冲区，
与MySignal实例相隔离。     

##### 忙等待  

处理数据的线程B等待这数据准备好，可以处理数据。换句话，就是线程B等待线程A的信号：调用hasDataToProcess
方法返回true，下面就是线程B跑着的循环，等待着信号的到来。     

```
protected MySignal sharedSignal = ...

...

while(!sharedSignal.hasDataToProcess()){
  //do nothing... busy waiting
}
```   
循环会一直的运行，直到hasDataToProcess方法返回true，这个就叫做忙等待，线程当等待的时候，一直处于忙碌
的状态。      

###### wait(), notify() and notifyAll()   
忙等待对CPU的使用并不十分的高效，除非等待的时间比较的短。另外，如果在等待的时候直接的休眠然后在收到信号
以后再次变得活跃，这样会比忙等待要好。     

Java有一种内在的等待机制，可以使线程在等待的时候处于休眠的状态。Java的Object定义了三个方法 wait，notify，
notifyAll 来实现这个机制。    

一个线程可以调用任意一个对象的wait方法，变为休眠的状态知道另外的一个线程调用同样对象上面的notify方法，为了
调用对象上面的wait方法或者是notify方法，线程必须获得对象上的锁。换句话说，调用notify 或者wait方法的线程必须
在synchronized 关键字的代码块里面调用，下面就是一个修改了的MySignal的MyWaitNotify，使用了notify和wait方法。    

```

public class MonitorObject{
}

public class MyWaitNotify{

  MonitorObject myMonitorObject = new MonitorObject();

  public void doWait(){
    synchronized(myMonitorObject){
      try{
        myMonitorObject.wait();
      } catch(InterruptedException e){...}
    }
  }

  public void doNotify(){
    synchronized(myMonitorObject){
      myMonitorObject.notify();
    }
  }
}

```   
上面我们可以看到线程等待或者线程唤醒调用wait方法和notify方法都是在同一个synchronized代码块里面，并且这个
是强制的。一个线程是不能够在没有拿到调用wait，notify notifyAll方法的对象的锁的时候而去调用wait，notify，
notifyAll 这些方法的，不然的话，就会出现抛出 IllegalMonitorStateException 的错误。   

但是这又是怎么发生的呢？等待的线程会一直保持着监测对象上的锁只要它在synchronized代码块里面？等待线程会阻塞
唤醒线程进入synchronized代码块调用doNotify方法吗？答案是不会，一旦一个线程调用wait方法，它就会释放自己保留
在监测对象上的锁，这样的话，就允许其他的线程再次的调用wait或者notify方法，因此那些线程必须从一个synchronized
代码块里面调用。      

一旦一个线程被唤醒，它要等到其他的线程调用notify方法，离开它所在的synchronized代码块，它才能够退出wait方法。换
句话说就是：被唤醒的线程必须重新换的检测对象锁才能够退出wait方法，原因就是因为wait方法的调用时藏在synchronized
代码块里面。如果多个线程因为调用notifyAll被唤醒，在某一个时间只能有一个线程退出wait方法，因为每一个线程在退出
wait方法之前必须重新获得检测对象的锁。    

#### 信号丢失

当方法notify 和notifyAll调用的时候，并不保存是那些方法在调用他们，如果没有线程等待，那么notify信号就丢失了。
因此一个线一个线程在其他的线程调用wait方法前调用notify方法，这个唤醒的信号就会被等待的线程丢失。这或许会成为
一个问题，因为在某种的条件下，等待的线程会一直的等待下去，因为唤醒信号的丢失。   

为了避免唤醒信号的丢失，它们应该被保存在信号类里面。在MyWaitNotify 例子中 唤醒信号应该被保存在MyWaitNotify 的
一个实例的成员变量里。下面是修改的代码：   

```
public class MyWaitNotify2{

  MonitorObject myMonitorObject = new MonitorObject();
  boolean wasSignalled = false;

  public void doWait(){
    synchronized(myMonitorObject){
      if(!wasSignalled){
        try{
          myMonitorObject.wait();
         } catch(InterruptedException e){...}
      }
      //clear signal and continue running.
      wasSignalled = false;
    }
  }

  public void doNotify(){
    synchronized(myMonitorObject){
      wasSignalled = true;
      myMonitorObject.notify();
    }
  }
}
```

备注：doNotify方法现在在调用notify方法之前会设置wasSignalled 变量为true。dowait方法首先检查wasSignalled 
变量在调用wait方法之前，事实上只有在没有接到信号的时候才会调用wait方法。   

#### 假唤醒  

因为一些无法解释的原因，线程即使在没有调用notify或者notifyAll也会醒来，这个被称之为假唤醒。毫无理由的被唤醒了。    


如果在MyWaitNofity2类的doWait()方法中出现虚假唤醒，在没有没有得到适当的信号下线程可以继续处理运行！
这可能应用程序中出现严重的问题。  

为了应对假唤醒，信号成员变量在一个while循环中检查，而不是在一个if的条件判断中检查。这个while循环称之为自旋锁。
一直到自旋锁中的while循环为false的时候，线程被唤醒，自旋锁结束。下面是针对MyWaitNotify2 的修改：    

```
public class MyWaitNotify3{

  MonitorObject myMonitorObject = new MonitorObject();
  boolean wasSignalled = false;

  public void doWait(){
    synchronized(myMonitorObject){
      while(!wasSignalled){
        try{
          myMonitorObject.wait();
         } catch(InterruptedException e){...}
      }
      //clear signal and continue running.
      wasSignalled = false;
    }
  }

  public void doNotify(){
    synchronized(myMonitorObject){
      wasSignalled = true;
      myMonitorObject.notify();
    }
  }
}
```  

备注：现在wait调用被包藏在一个带有if判断的while循环中，如果等待的线程没有接受到一个信号就被唤醒，那么成员变量
wasSignalled 依然是false，那么while循环将会再次的执行，等待线程会再次的回到等待的状态。   

#### 多个线程等待同样的信号

如果有多个线程在等待，然后被notifyAll全部的唤醒，这种情况下，使用while循环也是一个比较好的解决办法，因为只有一个
线程线程能够继续的运行下去。在某一个时刻只有一个线程能够获得监测对象的锁，意味着只能有一个线程可以调用wait方法，清理
wasSignalled标志，一旦这个线程退出了synchronized代码块中的doWait方法，其他的线程可以退出wait方法，检查while循环
里面的wasSignalled，然而这个标志已经被第一个唤醒的线程清理了，所以剩下的线程继续的等待，直到下一个信号的到来。


#### 不要在常量字符串和全局的变量上调用wait方法

上一个版本的MyWaitNotify里的监测的对象是一个常量字符串（""），如下例：    

```
public class MyWaitNotify{

  String myMonitorObject = "";
  boolean wasSignalled = false;

  public void doWait(){
    synchronized(myMonitorObject){
      while(!wasSignalled){
        try{
          myMonitorObject.wait();
         } catch(InterruptedException e){...}
      }
      //clear signal and continue running.
      wasSignalled = false;
    }
  }

  public void doNotify(){
    synchronized(myMonitorObject){
      wasSignalled = true;
      myMonitorObject.notify();
    }
  }
}
```  
调用一个空字符串或者其他常量的字符串的wait，notify方法的问题是：JVM/编译器内部会把常量字符串放到一个常量池里面
也就是说，即使你调用两个不同的MyWaitNotify的实例，他们两个的引用都是指向同一个空的字符串实例，这也就说第二个
MySignalNotify实例上面的调用doNotify方法，可能会唤醒调用第一个MySignalNotify实例的dowait方法的线程，如下图
所示：    

![](http://tutorials.jenkov.com/images/java-concurrency/strings-wait-notify.png)

备注：即使这四个线程调用的notify和wait方法在相同的共享字符串上，dowait和doNotify方法调用的信号仍然单独的保存
在两个MySignalNotify实例中，一个在MySignalNotify 1上的doNotify的调用可能会唤醒等待在MySignalNotify 2上的
线程，但是信号只会保存在MySignalNotify 1内。   

初看，这似乎不是什么很大的问题，毕竟，如果在第二个MySignalNotify实例上面调用doNotify方法，线程A和线程B确实
都被唤醒，但是唤醒的线程会在while循环中检查，如果不是doNotify正确的调用，线程会再次的陷入等待的状态。这种情况
就是一种假唤醒，线程A或者线程B没有被信号通知，就醒了，但是代码会处理这种情况的，然后线程会再次的陷入睡眠中。    

问题是：因为doNotify调用的是notify方法，而不是notifyAll，即使四个线程线程同时等待在同一个实例上，也只有一个
被唤醒。 所以，如果A或者B唤醒，但是真正的信号是对应C或者D，被唤醒的线程会检查它的信号，看到没有信号接收到，直接
的再次陷入睡眠状态。C或者D就不会再检查实际收到的信号了，所以这个信号就丢失了，这种情况就相当于上面描述的丢信号的
情况，c和D都被发送一个信号，但是都没有响应。     

如果doNotify方法调用的是notifyAll方法，而不是notify方法，所有的线程都会唤醒，轮流的检查信号量。线程A和线程B可能会
继续返回睡眠状态，但是线程C或者D其中的一个可能会接收到这个信号，退出doWait方法，C或者D另外的一个继续陷入睡眠的状态
因为接受到信号的那个线程在离开dowait的时候清理了signal变量。    

你可能企图只调用notifyAll方法，而不去想调用notify，从性能上考虑，这个方法不好。没有理由吧全部的线程都唤醒，最后
只有一个线程在运行。   

所以，不要使用全局变量，字符串常量等的wait/notify机制。使用构建时候拥有的唯一的object。例如每一个MySignalNotify3
实例中都有一个MonitorObject实例，而不是空字符串实例用来调用wait和notify方法。
































