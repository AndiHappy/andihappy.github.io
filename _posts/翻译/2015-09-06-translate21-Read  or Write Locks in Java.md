---
layout: post
title: "Read/Write Locks in Java"
description: "翻译 梳理基础的东西"
category: 翻译 java 并发编程 多线程
tags: [翻译 并发编程 多线程]
---
#### Read / Write Locks in Java
<br/>
##### java中的读锁/写锁
<br/>

文章的地址：[翻译文章的源地址](http://tutorials.jenkov.com/java-concurrency/read-write-locks.html)
<br/>

读写锁比文章 Locks in java 中描述的Lock实现更加的复杂。设想你有一个应用，需要读
或者写某些资源，但是写的操作比读要少，两个线程读取相同的资源的时候，不会引发问题。
所以多个线程在同一个时间访问资源没有问题。但是一个线程想要写入某些资源，在相同的
时间内不能够有线程读或者写这个资源。为了解决这个允许多个线程读，单个线程写的问题，
我们需要读写锁。   

java5中在java.util.concurrent包中，有读写锁的实现，但是我们了解他们实现背后的
原理还是很有用的。   


#### 读写锁的java实现

首先我们总写一下，资源读写权限的状态：   

读权限： 如果没有线程在写，并且没有线程获得写的权限      
写权限： 如果没有线程在读或者写     

如果一个线程读取某一个资源，只要没有线程在写这个资源，或者没有线程拥有对这个资源
写的请求，就没有问题。这样可以通过上调写访问请求的优先级，表示写入请求比读请求
更重要。此外，如果读取是最常发生操作，我们就不提高写的优先级，因为可能
发生饥饿。请求写权限的线程将阻塞，直到所有的读权限的线程释放了ReadWriteLock锁
了。如果不断地授予新的读取访问线程，等待写入访问权限的线程将永远的阻止
，导致饥饿。如果没有当前已锁定读写锁的线程，或者为了写资源而请求读写锁的线程
，这个时候线程才能够被授予读取资源的权限。    

一个线程只有在没有线程读取资源，并且没有线程向这个资源写入，才能够授予写的权限。
除非我们关心请求写操作的线程之间的公平性，那么无论多少线程请求写的权限，都没有关
系(针对线程获得写权限)。    


带着上述简单的规则，我们实现一个如下的读写锁：    

```
public class ReadWriteLock{

  private int readers       = 0;
  private int writers       = 0;
  private int writeRequests = 0;

  public synchronized void lockRead() throws InterruptedException{
    while(writers > 0 || writeRequests > 0){
      wait();
    }
    readers++;
  }

  public synchronized void unlockRead(){
    readers--;
    notifyAll();
  }

  public synchronized void lockWrite() throws InterruptedException{
    writeRequests++;

    while(readers > 0 || writers > 0){
      wait();
    }
    writeRequests--;
    writers++;
  }

  public synchronized void unlockWrite() throws InterruptedException{
    writers--;
    notifyAll();
  }
}
```

这个读写锁拥有两个锁方法和两个释放锁的方法。一对锁与释放的方法是针对读权限的，
另一对是针对锁权限的。   

读权限的规则在lockRead方法中体现，所有的线程都能够得到读资源的权限，除非
这个时候已经有一个拥有写权限的线程，或者多个请求写权限的线程。    

写权限的规则在lockWrite方法中体现，一个线程能够得到写的权限，首先请求写的权限，
这个时候 writeRequests++ ，然后它会检查它能不能得到写的权限，如果这个时候
没有线程拥有读取资源的权限，并且也没有线程拥有写这个资源的权限，它就能够
得到写资源的权限。至于有多少个线程请求这个资源的写权限，没有关系。    

值得注意的是unlockRead 和 unlockWrite方法的调用，调用的是notifyAll方法
而不是notify方法，为了解释为什么，首先我们考虑下面的场景：   

在ReadWriteLock锁里面，现在有线程等待着读取资源的权限，还有线程等待着写资源
的权限，如果一个线程被notify方法唤醒，请求读取资源的权限，因为这个时候有写资源
的请求，然而，没有等待着写权限的线程被唤醒，所以什么也没有发生，也就是说这个
信号被“丢失”，什么也没有发生，没有哪一个线程获得写或者读取资源的权限。但是
调用notifyAll方法，所有等待着的线程都会被唤醒，检查状态看看能不能得到自己
希望的权限。    

调用notifyAll方法，还有其他的一个好处，当多个线程都在等待读取资源的时候，
而这个时候又没有线程读取写的权限，unlockWrite方法的调用，所有的线程都能够
得到读取资源的权限，而不是一个一个的唤醒。

#### 读写锁的重入性

上面实现的ReadWriteLock 类不是可重入的，如果一个线程已经获得写权限，在此请求
写权限，它将会阻塞，因为这个时候已经有一个获得写权限的线程了---它自己。另外考虑
下面的这种情况：    

1.线程1获得读资源的权限    
2.线程2请求写权限，但是被阻塞了，因为现在有一个Reader（拥有读取权限的线程）   
3.线程1再次请求读资源的权限，但是还是被阻塞了，因为现在已经有一个写的请求。   


这种情况下，先前的ReadWriteLock的就会直接的锁死了，和死锁的现象比较的相像。
无论是请求写还是读取资源的权限的线程，都不能够得到想要的权限。   

为了是ReadWriteLock，变为可重入的，我们需要做一些小的改动，另外，Reader和
Writer是分开处理的。   

#### 可重入的读

为了是ReadWriteLock对Reader变为是可重入的，我们首先明确对可重入读的规则：    

如果线程已经获得读取资源的权限（忽略写资源的请求），或者线程能够获得读取资源
的权限（没有Writer或者写资源的请求），线程就能够保证读取资源的可重入性。   

为了获得一个线程是否已经获得读取资源的权限了，我们保存了一个Map结构，保存
获得读取资源权限的线程和对应的获得对应锁的次数。当决定一个线程是否拥有读取
资源权限的时候，我们可以检查这个map结构，查看其中是否包含这个线程的引用，
下面就是lockRead和unlockRead方法的改动的结果：   

```
public class ReadWriteLock{

  private Map<Thread, Integer> readingThreads =
      new HashMap<Thread, Integer>();

  private int writers        = 0;
  private int writeRequests  = 0;

  public synchronized void lockRead() throws InterruptedException{
    Thread callingThread = Thread.currentThread();
    while(! canGrantReadAccess(callingThread)){
      wait();                                                                   
    }

    readingThreads.put(callingThread,
       (getAccessCount(callingThread) + 1));
  }


  public synchronized void unlockRead(){
    Thread callingThread = Thread.currentThread();
    int accessCount = getAccessCount(callingThread);
    if(accessCount == 1){ readingThreads.remove(callingThread); }
    else { readingThreads.put(callingThread, (accessCount -1)); }
    notifyAll();
  }


  private boolean canGrantReadAccess(Thread callingThread){
    if(writers > 0)            return false;
    if(isReader(callingThread) return true;
    if(writeRequests > 0)      return false;
    return true;
  }

  private int getReadAccessCount(Thread callingThread){
    Integer accessCount = readingThreads.get(callingThread);
    if(accessCount == null) return 0;
    return accessCount.intValue();
  }

  private boolean isReader(Thread callingThread){
    return readingThreads.get(callingThread) != null;
  }

} 

```   
像上面代码中显示：只有在没有线程频繁向资源写操作的时候，读的可重入性才能够得到
保证。另外，如果调用的线程已经获得了读取资源的权限，那么就不会管有多少的写资源
的请求。   

#### 写的可重入性

写的可重入性，只有在线程已经拥有写权限的情况下，才能够保证。下面就是
lockWrite和unlockWrite 的代码：

```
public class ReadWriteLock{

    private Map<Thread, Integer> readingThreads =
        new HashMap<Thread, Integer>();

    private int writeAccesses    = 0;
    private int writeRequests    = 0;
    private Thread writingThread = null;

  public synchronized void lockWrite() throws InterruptedException{
    writeRequests++;
    Thread callingThread = Thread.currentThread();
    while(! canGrantWriteAccess(callingThread)){
      wait();
    }
    writeRequests--;
    writeAccesses++;
    writingThread = callingThread;
  }

  public synchronized void unlockWrite() throws InterruptedException{
    writeAccesses--;
    if(writeAccesses == 0){
      writingThread = null;
    }
    notifyAll();
  }

  private boolean canGrantWriteAccess(Thread callingThread){
    if(hasReaders())             return false;
    if(writingThread == null)    return true;
    if(!isWriter(callingThread)) return false;
    return true;
  }

  private boolean hasReaders(){
    return readingThreads.size() > 0;
  }

  private boolean isWriter(Thread callingThread){
    return writingThread == callingThread;
  }
}
```   

注意：当决定线程能否得到写资源权限的时候，把当前的线程是否已经拥有写资源对应
的锁也考虑了进去。   

#### 读到写的可重入性

有时，对一个线程来说，不仅需要读资源的权限，还需要写的权限。这种情况下，可以
知道只能有一个Reader，为了支持这个，我们的writelock必须做出一点修改，如下
所示：   

```
public class ReadWriteLock{

    private Map<Thread, Integer> readingThreads =
        new HashMap<Thread, Integer>();

    private int writeAccesses    = 0;
    private int writeRequests    = 0;
    private Thread writingThread = null;

  public synchronized void lockWrite() throws InterruptedException{
    writeRequests++;
    Thread callingThread = Thread.currentThread();
    while(! canGrantWriteAccess(callingThread)){
      wait();
    }
    writeRequests--;
    writeAccesses++;
    writingThread = callingThread;
  }

  public synchronized void unlockWrite() throws InterruptedException{
    writeAccesses--;
    if(writeAccesses == 0){
      writingThread = null;
    }
    notifyAll();
  }

  private boolean canGrantWriteAccess(Thread callingThread){
    if(isOnlyReader(callingThread))    return true;
    if(hasReaders())                   return false;
    if(writingThread == null)          return true;
    if(!isWriter(callingThread))       return false;
    return true;
  }

  private boolean hasReaders(){
    return readingThreads.size() > 0;
  }

  private boolean isWriter(Thread callingThread){
    return writingThread == callingThread;
  }

  private boolean isOnlyReader(Thread thread){
      return readers == 1 && readingThreads.get(callingThread) != null;
      }
  
}
```    

上面的情况就能够支持 read-to-write 的可重入性支持。   

#### 写到读的可重入性

有时，一个线程不仅需要读取资源的权限，还需要写资源的权限。如果该线程已经拥有
写的权限，那么就会被保证对资源读取的权限。如果一个线程拥有对资源的写的权限，
那么这个时候没有读或者写的访问，这个时候就是安全的，没有危险性。下面就是
canGrantReadAccess方法：  

```
public class ReadWriteLock{

    private boolean canGrantReadAccess(Thread callingThread){
      if(isWriter(callingThread)) return true;
      if(writingThread != null)   return false;
      if(isReader(callingThread)  return true;
      if(writeRequests > 0)       return false;
      return true;
    }

}
```

#### ReadWriteLock全部的可重入性

下面是ReadWriteLock的全部的实现，与上面的对比，我们做了一些重构的工作。

```
public class ReadWriteLock{

  private Map<Thread, Integer> readingThreads =
       new HashMap<Thread, Integer>();

   private int writeAccesses    = 0;
   private int writeRequests    = 0;
   private Thread writingThread = null;


  public synchronized void lockRead() throws InterruptedException{
    Thread callingThread = Thread.currentThread();
    while(! canGrantReadAccess(callingThread)){
      wait();
    }

    readingThreads.put(callingThread,
     (getReadAccessCount(callingThread) + 1));
  }

  private boolean canGrantReadAccess(Thread callingThread){
    if( isWriter(callingThread) ) return true;
    if( hasWriter()             ) return false;
    if( isReader(callingThread) ) return true;
    if( hasWriteRequests()      ) return false;
    return true;
  }


  public synchronized void unlockRead(){
    Thread callingThread = Thread.currentThread();
    if(!isReader(callingThread)){
      throw new IllegalMonitorStateException("Calling Thread does not" +
        " hold a read lock on this ReadWriteLock");
    }
    int accessCount = getReadAccessCount(callingThread);
    if(accessCount == 1){ readingThreads.remove(callingThread); }
    else { readingThreads.put(callingThread, (accessCount -1)); }
    notifyAll();
  }

  public synchronized void lockWrite() throws InterruptedException{
    writeRequests++;
    Thread callingThread = Thread.currentThread();
    while(! canGrantWriteAccess(callingThread)){
      wait();
    }
    writeRequests--;
    writeAccesses++;
    writingThread = callingThread;
  }

  public synchronized void unlockWrite() throws InterruptedException{
    if(!isWriter(Thread.currentThread()){
      throw new IllegalMonitorStateException("Calling Thread does not" +
        " hold the write lock on this ReadWriteLock");
    }
    writeAccesses--;
    if(writeAccesses == 0){
      writingThread = null;
    }
    notifyAll();
  }

  private boolean canGrantWriteAccess(Thread callingThread){
    if(isOnlyReader(callingThread))    return true;
    if(hasReaders())                   return false;
    if(writingThread == null)          return true;
    if(!isWriter(callingThread))       return false;
    return true;
  }


  private int getReadAccessCount(Thread callingThread){
    Integer accessCount = readingThreads.get(callingThread);
    if(accessCount == null) return 0;
    return accessCount.intValue();
  }


  private boolean hasReaders(){
    return readingThreads.size() > 0;
  }

  private boolean isReader(Thread callingThread){
    return readingThreads.get(callingThread) != null;
  }

  private boolean isOnlyReader(Thread callingThread){
    return readingThreads.size() == 1 &&
           readingThreads.get(callingThread) != null;
  }

  private boolean hasWriter(){
    return writingThread != null;
  }

  private boolean isWriter(Thread callingThread){
    return writingThread == callingThread;
  }

  private boolean hasWriteRequests(){
      return this.writeRequests > 0;
  }

}
```











   























