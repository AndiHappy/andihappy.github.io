---
layout: post
title: "并发知识梳理：2. Lock"
subtitle: ""
author: "zhailzh"  
header-img: "img/post-bg-2015.jpg"  
catalog: true
tags: Concurrent  
---

java.util.concurrent.locks.Lock 为一个接口，围绕着这个接口实现了一些列的锁。

<!--more-->

## Lock

Lock接口和Condition接口的均为锁相关的两个接口，具体的说明：
> Lock implementations provide more extensive locking operations than can be obtained using synchronized methods and statements. They allow more flexible structuring, may have quite different properties, and may support multiple associated Condition objects.

> A lock is a tool for controlling access to a shared resource by multiple threads. Commonly, a lock provides exclusive access to a shared resource: only one thread at a time can acquire the lock and all access to the shared resource requires that the lock be acquired first. However, some locks may allow concurrent access to a shared resource, such as the read lock of a ReadWriteLock.

其声明的接口为：

~~~
public interface Lock {

    void lock();
    void lockInterruptibly() throws InterruptedException;
    boolean tryLock();
    boolean tryLock(long time, TimeUnit unit) throws InterruptedException;
    void unlock();

    /**
     Returns a new Condition instance that is bound to this Lock instance.
     Before waiting on the condition the lock must be held by the current thread.
     A call to Condition.await() will atomically release the lock before waiting 
     and re-acquire the lock before the wait returns.
     */
    Condition newCondition();
}
~~~

Condition的实例绑定在Lock实例上，只有当前的线程拿到了锁以后，才能够调用Condition的wait方法。调用了condition的await()方法，会自动的释放锁。wait结束以后，会重新的获得这个锁。类似synchronized里面 object.wait()方法。

## 锁的实现的基础：AQS

说起来锁的实现就必须要说：AQS。AQS是用来构建锁和其他同步组件的基础框架，它也是Java三大并发工具类（CountDownLatch、CyclicBarrier、Semaphore）的基础。ReentrantLock，甚至BlockingQueue也是基于它的实现，可以说是非常重要了。

简单介绍一下，AQS其实就是一个类，全称是AbstractQueuedSynchronizer，队列同步器。

想要了解AQS，那就先仔细的阅读DOC。

~~~
Provides a framework for implementing blocking locks and related synchronizers (semaphores, events, etc) that rely on first-in-first-out (FIFO) wait queues. This class is designed to be a useful basis for most kinds of synchronizers that rely on a single atomic int value to represent state. Subclasses must define the protected methods that change this state, and which define what that state means in terms of this object being acquired or released. Given these, the other methods in this class carry out all queuing and blocking mechanics. Subclasses can maintain other state fields, but only the atomically updated int value manipulated using methods getState(), setState(int) and compareAndSetState(int, int) is tracked with respect to synchronization.
依赖FIFO的等待队列，实现了一个针对阻塞锁和同步器的框架。
使用一个 atomic int value 来标识状态。

Subclasses should be defined as non-public internal helper classes that are used to implement the synchronization properties of their enclosing class. Class AbstractQueuedSynchronizer does not implement any synchronization interface. Instead it defines methods such as acquireInterruptibly(int) that can be invoked as appropriate by concrete locks and related synchronizers to implement their public methods.
子类一般的采用非public的内部类的形式来实现需要满足同步特性的方法。

This class supports either or both a default exclusive mode and a shared mode. When acquired in exclusive mode, attempted acquires by other threads cannot succeed. Shared mode acquires by multiple threads may (but need not) succeed. This class does not "understand" these differences except in the mechanical sense that when a shared mode acquire succeeds, the next waiting thread (if one exists) must also determine whether it can acquire as well. Threads waiting in the different modes share the same FIFO queue. Usually, implementation subclasses support only one of these modes, but both can come into play for example in a ReadWriteLock. Subclasses that support only exclusive or only shared modes need not define the methods supporting the unused mode.

这个类提供了两种模式：独占和共享。一般来说子类只使用一种模式，
但是ReadWriteLock是两种模式共存的。

This class defines a nested AbstractQueuedSynchronizer.ConditionObject class that can be used as a Condition implementation by subclasses supporting exclusive mode for which method isHeldExclusively() reports whether synchronization is exclusively held with respect to the current thread, method release(int) invoked with the current getState() value fully releases this object, and acquire(int), given this saved state value, eventually restores this object to its previous acquired state. No AbstractQueuedSynchronizer method otherwise creates such a condition, so if this constraint cannot be met, do not use it. The behavior of AbstractQueuedSynchronizer.ConditionObject depends of course on the semantics of its synchronizer implementation.
这个类还提供了一个Condition类。

This class provides inspection, instrumentation, and monitoring methods for the internal queue, as well as similar methods for condition objects. These can be exported as desired into classes using an AbstractQueuedSynchronizer for their synchronization mechanics.

Serialization of this class stores only the underlying atomic integer maintaining state, so deserialized objects have empty thread queues. Typical subclasses requiring serializability will define a readObject method that restores this to a known initial state upon deserialization.
~~~

阅读了DOC有一个大致的了解，我们还是看源码比较的利索。AQS的成员变量有：
~~~
    private transient volatile Node head;

    private transient volatile Node tail;

    private volatile int state;
~~~

AQS中主要维护了state（锁状态的表示）和一个可阻塞的等待队列。
state是临界资源，也是锁的描述。表示有多少线程已经获取了锁。

关于state有一个更改的CAS操作：
~~~
    private static final Unsafe unsafe = Unsafe.getUnsafe();
    private static final long stateOffset;

    stateOffset = unsafe.objectFieldOffset
                (AbstractQueuedSynchronizer.class.getDeclaredField("state"));

    protected final boolean compareAndSetState(int expect, int update) {
        // See below for intrinsics setup to support this
        return unsafe.compareAndSwapInt(this, stateOffset, expect, update);
    }
~~~

成员变量的head ， tail 标识的是等待队列（也称之为 CHL，同步队列）的头结点和尾节点。
CHL队列主要有链表实现，以自旋的方式获取资源，是可阻塞的先进先出的双向队列。通过自旋才做来保证节点插入和移除的原子性。获取锁失败的线程，则会被添加到队尾。

### AQS的内部类：Node

该Node代表的是 Wait queue node class。 组织形式：
~~~
      +------+  prev +-----+       +-----+
 head |      | <---- |     | <---- |     |  tail
      +------+       +-----+       +-----+

~~~

Node有大段的注释，我们能够了解到：
We instead use them for blocking synchronizers, 
but use the same basic tactic of holding some of the control information about a thread in the predecessor of its node. 

还有其他的内容，我们结合着代码进行说明。

AQS的工作模式分为独占模式和共享模式，记录在节点的信息中。它还使用了模板方法设计模式，定义一个操作中算法的骨架，而将一些步骤的实现延迟到子类中。比如获取资源的方法就能很好的品味模板模式。一般地，它的实现类只实现一种模式，ReentrantLock就实现了独占模式；但也有例外，ReentrantReadAndWriteLock实现了独占模式和共享模式。下面来看Node相关源码。

~~~
        //当前节点处于共享模式的标记
        static final Node SHARED = new Node();
       
        //当前节点处于独占模式的标记
        static final Node EXCLUSIVE = null;

        //线程被取消了
        static final int CANCELLED =  1;
        //释放资源后需唤醒后继节点
        static final int SIGNAL    = -1;
        //等待condition唤醒
        static final int CONDITION = -2;
        //工作于共享锁状态，需要向后传播，
        //比如根据资源是否剩余，唤醒后继节点
        static final int PROPAGATE = -3;

        //等待状态，有1,0,-1,-2,-3五个值。分别对应上面的值
        volatile int waitStatus;

        //前驱节点
        volatile Node prev;

        //后继节点
        volatile Node next;

        //等待锁的线程
        volatile Thread thread;

        //等待条件的下一个节点，ConditonObject中用到
        Node nextWaiter;
~~~
状态说明：

CANCELLED
作废状态，该节点的线程由于超时，中断等原因而处于作废状态。是不可逆的，一旦处于这个状态，说明应该将该节点移除了。

SIGNAL
待唤醒后继状态，当前节点的线程处于此状态，后继节点会被挂起，当前节点释放锁或取消之后必须唤醒它的后继节点。

CONDITION
等待状态，表明节点对应的线程因为不满足一个条件（Condition）而被阻塞。


### 获取资源（锁）

获取释放资源其实都是对state变量的修改，有的文章会管他叫锁，笔者更喜欢叫资源。
获取资源的方法有acquire()，acquiredShared()。先来看acquire()，该方法只工作于独占模式。

####  acquire()--独占模式获取资源

aquire()：以独占模式获取资源，忽略中断（ReentrantLock.lock()中调用了这个方法）

~~~
//Acquires in exclusive mode, ignoring interrupts. 
//Implemented by invoking at least once tryAcquire, returning on success. Otherwise the thread is queued, possibly repeatedly blocking and unblocking, invoking tryAcquire until success. 
public final void acquire(int arg) {
    if (!tryAcquire(arg) &&

            acquireQueued(addWaiter(Node.EXCLUSIVE), arg))
            selfInterrupt();
    }
~~~

之前也提到了AQS使用了模板方法模式，其实tryAcuire()方法就是一个钩子方法。在AQS中，此方法会抛出UnsupportedOperationException，所以需要子类去实现。tryAcquire(arg)返回false，其实就是获取锁失败的情况。这时候就调用：acquireQueued(addWaiter(Node.EXCLUSIVE), arg)

addWaiter()：将当前线程插入至队尾，返回在等待队列中的节点（就是处理了它的前驱后继）。
~~~
private Node addWaiter(Node mode) {
        //把当前线程封装为node,指定资源访问模式
        Node node = new Node(Thread.currentThread(), mode);

        // Try the fast path of enq; backup to full enq on failure
        Node pred = tail;
        //如果tail不为空,把node插入末尾
        if (pred != null) {
            node.prev = pred;
            //此时可能有其他线程插入,所以使用CAS重新判断tail
            if (compareAndSetTail(pred, node)) {
                pred.next = node;
                return node;
            }
        }
        //如果tail为空，说明队列还没有初始化，执行enq()
        enq(node);
        return node;
    }
~~~


CAS设置队尾：
~~~
private static final Unsafe unsafe = Unsafe.getUnsafe();
private static final long tailOffset;
 tailOffset = unsafe.objectFieldOffset
                (AbstractQueuedSynchronizer.class.getDeclaredField("tail"));

    private final boolean compareAndSetTail(Node expect, Node update) {
        return unsafe.compareAndSwapObject(this, tailOffset, expect, update);
    }
~~~

enq()：将节点插入队尾，失败则自旋，直到成功。
~~~
private Node enq(final Node node) {
        for (;;) {
            Node t = tail;
            if (t == null) { // Must initialize
                if (compareAndSetHead(new Node()))
                    tail = head;
            } else {
                node.prev = t;
                if (compareAndSetTail(t, node)) {
                    t.next = node;
                    return t;
                }
            }
        }
    }
~~~




