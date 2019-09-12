---
layout: post
title: "并发知识梳理：3. 通过具体的应用展示锁的实现机制"
subtitle: "AQS和ReentrantLock"
author: "zhailzh"  
header-img: "img/post-bg-2015.jpg"  
catalog: true
tags: Concurrent  
---

上一节关于AQS的锁的架构实现，说的非常的抽象，看的非常的费劲，这次通过代码的注释和说明，来展示具体是如何实现的。

<!--more-->

## ReentrantLock

关于可重入锁带着两个问题，去分析的：
1. 怎么判断的重入，如何增加锁的次数的。
2. 重入次数的判断，释放锁的时候，如果消减锁的次数，释放以后，其他的等待的线程是如何响应的

### ReentrantLock lock 方法
~~~
public ReentrantLock() {
	sync = new NonfairSync();
}
public void lock() {
	sync.lock();
}
~~~

符合使用框架的时候，都采用内部类NonfairSync来实现实现的需要实现的接口，这点也符合DOC的说明。

NonfairSync 的实现逻辑如下：
~~~
 static final class NonfairSync extends Sync {
  /**
   * so beautiful ！ 整个操作如此的不见烟火气：
   *
   * ①首先是CAS(state,1) 确定是否已经有线程抢占到锁，然后设置：ExclusiveThread（这个是普通的成员变量）,返回true；
   *  
   * 然后是：Acquire(1),更新state为1，
   * 
   * 具体的逻辑： !tryAcquire(1) => acquireQueued(addWaiter(Node.EXCLUSIVE), 1)) =>selfInterrupt()
   * 
   * 根据getState的状态进行判断： 
   *    如果是0，说明原来的线程已经释放锁，重新的走① 
   *    如果大于零，当前的线程（Thread.concurrent）和 ExclusiveOwnerThread 进行比较 
   *              如果是独占线程为当前线程，则state+1，返回true; 如果不是，则返回false
   * 
   * 只有在返回false的前提下，才会执行：addWaiter(Node.EXCLUSIVE), 1)，
   */
  final void lock() {
   // 只是使用一个CAS的操作，就保证了线程抢占的唯一性
   if (compareAndSetState(0, 1))
    // private transient Thread exclusiveOwnerThread; 可以说只是一个普通的成员变量
    setExclusiveOwnerThread(Thread.currentThread());
   else
    // 如果是同一个线程，则去更新状态为1，如果不是当前的线程，会被阻塞的
    acquire(1);
  }

  protected final boolean tryAcquire(int acquires) {
   return nonfairTryAcquire(acquires);
  }
 }
~~~

ReentrantLock的lock方法，其实默认的调用的就是 NonfairSync的lock方法，也就是说默认的是非公屏锁。这里就产生了一个问题：
> 为什么是非公平的，和公平的差在什么地方？

然后具体的流程就是CAS后，设置独占线程，Note：这个独占线程为普通的成员变量。

再次就是acquire(1) ,这个方法其实调用的是AQS中的那个经典的方法：
~~~
//独占模式下的获取锁
 public final void acquire(int arg) {
  if (!tryAcquire(arg) && // tryAcquire 为子类必须实现的方法，提供扩展的逻辑
    acquireQueued(addWaiter(Node.EXCLUSIVE), arg)) // 获取锁失败以后
   selfInterrupt();
 }
~~~

其中的tryAcquire方法，就是在NonfairSync 继承的 Sync 中实习。
~~~
/**
   * 非公平获取锁 ！！！重要的框架！！！
   */
  final boolean nonfairTryAcquire(int acquires) {
   final Thread current = Thread.currentThread();
   int c = getState();
   if (c == 0) {
    if (compareAndSetState(0, acquires)) {
     setExclusiveOwnerThread(current);
     return true;
    }
   } else if (current == getExclusiveOwnerThread()) {
    int nextc = c + acquires;
    if (nextc < 0) // overflow
     throw new Error("Maximum lock count exceeded");
    setState(nextc);
    return true;
   }
   return false;
  }
~~~

首先没有判断是否是独占线程，而是首先判断的state的状态！根据state的状态，进行二次的CAS操作。
> 如果首先判断是否是独占线程，是否有问题？

然后再去判断是否是独占线程，如果是，则去更新state的状态。

返回以后，则进入AQS的框架中，如果没有抢到锁，则进入等待的队列，这里需要说一下这个等待的队列的加入函数：addWaiter
~~~
// 独占性的节点和共享性的节点，加入等待的链表中
 private Node addWaiter(Node mode) {
  Node node = new Node(Thread.currentThread(), mode);
  // Try the fast path of enq; backup to full enq on failure
  Node pred = tail;
  if (pred != null) {
   node.prev = pred;
   if (compareAndSetTail(pred, node)) {//相当于 tail=(pre==tail)?node:pre
    pred.next = node;
    return node;
   }

   /**
    *   head(new Node()) <----Thread[Thread-1] <---- Thread[Thread-2] <---- Thread[Thread-3](tail)
    *  相当于把node节点插入到队尾。
    *  Node node = new Node()
    *  Node pred = tail;
    *  node.prev = pred;
    *  tail = node;
    *  pred.next = node;
    * */
  }
  // 设置失败之后，进入enq，enq为for的无线循环的模式
  enq(node);
  return node;
 }
~~~

其中的注释可以比较清楚明白的说明，线程排队的数据结构。其中的enq也是tail为null的时候的，初始化操作。

然后就是更新线程队列的状态（我们现在只讨论比较普通的状况，特殊的等到遇见了在进行说明），以及怎么让线程“停下来”：

~~~
final boolean acquireQueued(final Node node, int arg) {
  boolean failed = true;
  try {
   boolean interrupted = false;
   for (;;) {
    //Node 的 prev 成员变量
    final Node p = node.predecessor();
    // 再次去检查，是否有机会获取锁
    if (p == head && tryAcquire(arg)) {
     setHead(node);
     p.next = null; // help GC
     failed = false;
     return interrupted;
    }
    if (shouldParkAfterFailedAcquire(p, node) && parkAndCheckInterrupt())
     interrupted = true;
   }
  } finally {
   if (failed)
    cancelAcquire(node);
  }
 }
~~~

再去更新等待队列线程节点Node的状态的时候，还是首先的去尝试获取锁，当然这个尝试是有条件的，那就是：```node.predecessor() == head``` 这个条件就是说明，node的前一个节点就是头结点，在等待队列初始化的时候，我们已经知道头结点只是一个“象征节点”，是没有对应线程的。所以如果满足条件，那么这个就应该是争抢锁的下一个节点。

如果不满足条件，则会调用：```shouldParkAfterFailedAcquire``` 这个就是线程对应的链表状态的更新：

~~~
private static boolean shouldParkAfterFailedAcquire(Node pred, Node node) {
  int ws = pred.waitStatus;
  if (ws == Node.SIGNAL)
   /*
    * This node has already set status asking a release
    * to signal it, so it can safely park.
    */
   return true;
  if (ws > 0) {
   /*
    * Predecessor was cancelled. Skip over predecessors and
    * indicate retry.
    */
   do {
    node.prev = pred = pred.prev;
   } while (pred.waitStatus > 0);
   pred.next = node;
  } else {
   /*
    * waitStatus must be 0 or PROPAGATE.  Indicate that we
    * need a signal, but don't park yet.  Caller will need to
    * retry to make sure it cannot acquire before parking.
    */
   /**
    * 这句是pre设置为了Node.SIGNAL状态:
    * 待唤醒后继状态，当前节点的线程处于此状态，后继节点会被挂起，当前节点释放锁或取消之后必须唤醒它的后继节点。
    * */
   compareAndSetWaitStatus(pred, ws, Node.SIGNAL);
  }
  return false;
 }
~~~

这里注意一下：
```acquireQueued``` 是一个无限的for循环结构，所以```shouldParkAfterFailedAcquire``` 可能会被调用很多次。所以一般的会被调用两次，第一次，因为新建的Node节点，waitStatus 就是默认值0，所以如果下载再有阻塞节点的时候，
```node.pre == 0```
都会先把前驱节点设置为：Node.SIGNAL，也就是调用：
```compareAndSetWaitStatus(pred, ws, Node.SIGNAL)```
然后下次acquireQueued的for循环里面，再次判断的时候   
 ```node.pre 就等于 Node.SIGNAL```
然后shouldParkAfterFailedAcquire，返回了true。

轮到了函数：```parkAndCheckInterrupt``` 这个方法很有意思：

~~~
private final boolean parkAndCheckInterrupt() {
  LockSupport.park(this);
  return Thread.interrupted();
 }

// LockSupport 的park方法
 public static void park(Object blocker) {
        Thread t = Thread.currentThread();
        setBlocker(t, blocker);
        UNSAFE.park(false, 0L);
        setBlocker(t, null);
    }
~~~

这里涉及到一个挺有意思的工具类：LockSupport，我们最基础的和工具类了，这个类的详细我们会面会说，只说一下其支持的两个方法的特点：

java.util.concurrent.locks.LockSupport.park(Object object)
暂停当前的线程,在缺乏凭证的前提下，会一直的:the current thread becomes disabled for thread scheduling

java.util.concurrent.locks.LockSupport.unpark(Thread thread)
给与参数中的Thread中的凭证，也就是立刻的唤醒对应的线程。

另外还有一点，unpark可以在 park之前调用，这样就不必担心漏掉“凭证”。具体的测试代码见：
~~~
public static void main(String[] args) throws InterruptedException {

  Thread test1 = new Thread(new Runnable() {
   @Override
   public void run() {

    try {
     Thread.sleep(1000);
    } catch (InterruptedException e) {
    }
    System.out.println("park before !");
    LockSupport.park("");
    System.out.println("park after !");
   }
  });
  
  test1.start();

  LockSupport.unpark(test1);
  System.out.println("Call unpark first");

  Thread.sleep(1000);
  System.out.println("Thread.state: " + test1.getState().name());

 }
~~~

然后回到```parkAndCheckInterrupt``` 方法，则等待的队列中的线程，算是真正的在等待着了。后面的只有等待别的线程释放锁以后，才能够运行了，我们接着看realease方法。


### ReentrantLock unlock 方法

unlock 方法和lock的方法，又有很大的不同，因为lock方法一开始调用的时候，也就是一开始执行CAS代码的时候，是可能多个线程同时执行，但是unlock方法，在一开始的时候，只有拥有锁的方法，才去调用。

~~~
public void unlock() {
  sync.release(1);
 }

 //sync.release方法，直接进入AQS的框架

 /**
  * 释放锁的框架：tryRelease(int arg) 有子类进行实现
  * 如果释放成功，则去通知等待的线程
  * */
 public final boolean release(int arg) {
  if (tryRelease(arg)) {
   Node h = head;
   //如果存在等待的线程：Wait queue中有值，根据我们先前的AddWaiter和shouldParkAfterFailedAcquire 逻辑
   // head 应该是new Node(),并且 waitStatus 为 Signal状态
   if (h != null && h.waitStatus != 0)
    unparkSuccessor(h);
   return true;
  }
  return false;
 }
~~~

我们还是按照原来的流程，先去关注Sync实现的子类方法tryRelease：

~~~
 /**
   * 释放的时候，肯定是获得锁的线程进行调用，所以不需要CAS之类的操作，只需要判定状态即可
   */
  protected final boolean tryRelease(int releases) {
   int c = getState() - releases;
   if (Thread.currentThread() != getExclusiveOwnerThread())
    throw new IllegalMonitorStateException();
   boolean free = false;
   if (c == 0) {
    free = true;
    setExclusiveOwnerThread(null);
   }
   setState(c);
   return free;
  }
~~~

首先是状态检查，然后根据state的值，判断锁释放是否成功，可以说这段的代码就是单线程在执行，不用担心线程安全的问题。

然后就是 release方法中的 再次争抢锁的过程了，这里面有一个比较重要的看点

~~~
private void unparkSuccessor(Node node) {
  // 把当Node节点（head节点），设置为了状态0
  int ws = node.waitStatus;
  if (ws < 0)
   compareAndSetWaitStatus(node, ws, 0);

  //根据状态，获得最近的可以被唤醒的线程节点
  Node s = node.next;
  if (s == null || s.waitStatus > 0) {
   s = null;
   for (Node t = tail; t != null && t != node; t = t.prev)
    if (t.waitStatus <= 0)
     s = t;
  }
  if (s != null)
   LockSupport.unpark(s.thread);
 }
~~~

这个方法有意思的一点就是，是和shouldParkAfterFailedAcquire 相对应的，shouldParkAfterFailedAcquire 把自己的pre Node 设置为Node.SIGNAL 状态，而unparkSuccessor 把Node 设置为原来的0 状态。

然后调用：
 ```LockSupport.unpark(s.thread)``` 
唤醒队列中最先等待的线程，让它去抢锁。

> 这不看起来挺公平的吗？为什么叫做：NonfairSync，难道有锁的时候，大家一起抢，才算作是公平吗？

我们在来看一下，公平锁是怎么实现的，公平又是如何体现的：

~~~
 static final class FairSync extends Sync {
  private static final long serialVersionUID = -3000897897090466540L;

  final void lock() {
   acquire(1);
  }

  /**
   * Fair version of tryAcquire.  Don't grant access unless
   * recursive call or no waiters or is first.
   */
  protected final boolean tryAcquire(int acquires) {
   final Thread current = Thread.currentThread();
   int c = getState();
   if (c == 0) {
    if (!hasQueuedPredecessors() && compareAndSetState(0, acquires)) {
     setExclusiveOwnerThread(current);
     return true;
    }
   } else if (current == getExclusiveOwnerThread()) {
    int nextc = c + acquires;
    if (nextc < 0)
     throw new Error("Maximum lock count exceeded");
    setState(nextc);
    return true;
   }
   return false;
  }
 }
~~~

两点比较重要：
> 1. 没有单独的实现realease方法，说明在释放锁的逻辑上和非公平锁的逻辑是一样的，都是释放锁成功后，唤醒等待链表中，等待时间最长的那个。

> 2. lock 方法上面有差别，公平锁上来就是acquire(1),然后在tryAcquire方法中，增加了一个!hasQueuedPredecessors 的判断。

其中hasQueuedPredecessors代码如下：
~~~
public final boolean hasQueuedPredecessors() {
  Node t = tail; // Read fields in reverse initialization order
  Node h = head;
  Node s;
  return h != t && ((s = h.next) == null || s.thread != Thread.currentThread());
 }
~~~

tail != head 好理解，意思是有线程在排队
(s = h.next) == null || s.thread != Thread.currentThread() 排队的线程不是当前线程

到了这里公平锁的逻辑就算理清楚了，它是在抢锁的时候，保证公平，如果有等待队列的时候，新进来的线程，首先会进行：!hasQueuedPredecessors，立马回返回false，因为存在等待的线程，并且当前的等待激活的线程不是当前的线程，就会立即的进入等待线程中等待。

但是被唤醒的线程，就会因为当前的等待激活的线程就是当前的线程
，而去执行```compareAndSetState(0, acquires)``` 才有可能抢到锁。




