---
layout: post
title: "并发知识梳理：2. Lock"
subtitle: "锁的实现架构AQS"
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
            // addWaiter 是采用CAS的操作，把独占的节点加入等待队列
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
                if (compareAndSetHead(new Node()))// tail=new Node()
                	 //初始化竟然是一个new Node()
                    tail = head;
            } else {
                node.prev = t;
                if (compareAndSetTail(t, node)) {//tail = node;
                    t.next = node; // t 这个时候还是原来的那个代表tail的节点
                    return t;
                }
            }
        }
    }
~~~
addWorker 分析完毕后，就是acquireQueued。

acquireQueued()：自旋方式获取资源并判断是否需要被挂起。
~~~
final boolean acquireQueued(final Node node, int arg) {
        boolean failed = true;
        try {
            boolean interrupted = false;
            //还是自旋尝试获取锁资源
            for (;;) {
                final Node p = node.predecessor();
                //如果节点的前驱是队列的头节点并且能拿到资源
                //成功后则返回中断位结束
                if (p == head && tryAcquire(arg)) {
                    setHead(node);
                    p.next = null; // help GC
                    failed = false;
                    return interrupted;
                }
                //shouldParkAfterFailedAcquire(Node, Node)检测当前节点是否应该park()
                //parkAndCheckInterrupt()用于中断当前节点中的线程
                if (shouldParkAfterFailedAcquire(p, node) &&
                    parkAndCheckInterrupt())
                    interrupted = true;
            }
        } finally {
            if (failed)
                cancelAcquire(node);
        }
    }
~~~

shouldParkAfterFailedAcquire()：判断当前节点是否应该被挂起。趁机梳理等待的节点，如果pre在初始状态下，则设置为SIGNAL状态，如果有CANCELED状态，则直接的删除，跳过。等等

~~~
    private static boolean shouldParkAfterFailedAcquire(Node pred, Node node) {
        int ws = pred.waitStatus;
        if (ws == Node.SIGNAL)
            //前驱节点的状态是SIGNAL，说明前驱节点释放资源后会通知自己
            //此时当前节点可以安全的park()，因此返回true
            return true;
        if (ws > 0) {
            //前驱节点的状态是CANCLLED，说明前置节点已经放弃获取资源了
            //此时一直往前找，直到找到最近的一个处于正常等待状态的节点
            //并排在它后面，返回false
            do {
                node.prev = pred = pred.prev;
            } while (pred.waitStatus > 0);
            pred.next = node;
        } else {
            //前驱节点的状态是0或PROPGATE，则利用CAS将前置节点的状态置
            //为SIGNAL，让它释放资源后通知自己
            //如果前置节点刚释放资源，状态就不是SIGNAL了，这时就会失败
            // 返回false
            compareAndSetWaitStatus(pred, ws, Node.SIGNAL);
        }
        return false;
    }
~~~

parkAndCheckInterrupt()：若确定有必要park，才会执行此方法。

~~~
    private final boolean parkAndCheckInterrupt() {
        //使用LockSupport，挂起当前线程
        LockSupport.park(this);
        return Thread.interrupted();
    }
~~~

selfInterrupt()：对当前线程产生一个中断请求。能走到这个方法，说明acquireQueued()返回true，就进行自我中断。

~~~
    static void selfInterrupt() {
        Thread.currentThread().interrupt();
    }
~~~

到这里，获取资源的流程就走完了，接下来总结一下。
aquire的步骤：
1）tryAcquire()尝试获取资源。

2）如果获取失败，则通过addWaiter(Node.EXCLUSIVE), arg)方法把当前线程添加到等待队列队尾，并标记为独占模式。

3）插入等待队列后，并没有放弃获取资源，acquireQueued()自旋尝试获取资源。根据前置节点状态状态判断是否应该继续获取资源。如果前驱是头结点，继续尝试获取资源；

4）在每一次自旋获取资源过程中，失败后调用shouldParkAfterFailedAcquire(Node, Node)检测当前节点是否应该park()。若返回true，则调用parkAndCheckInterrupt()中断当前节点中的线程。若返回false，则接着自旋获取资源。当acquireQueued(Node,int)返回true，则将当前线程中断；false则说明拿到资源了。

5）在进行是否需要挂起的判断中，如果前置节点是SIGNAL状态，就挂起，返回true。如果前置节点状态为CANCELLED，就一直往前找，直到找到最近的一个处于正常等待状态的节点，并排在它后面，返回false，acquireQueed()接着自旋尝试，回到3）。

6）前置节点处于其他状态，利用CAS将前置节点状态置为SIGNAL。当前置节点刚释放资源，状态就不是SIGNAL了，导致失败，返回false。但凡返回false，就导致acquireQueed()接着自旋尝试。

7）最终当tryAcquire(int)返回false，acquireQueued(Node,int)返回true，调用selfInterrupt()，中断当前线程。

上面的步骤说的不够清晰，需要一张图来进行表示，但是没有趁手的工具啊。

tryAcquire 比较好理解，addWorker也容易理解，关键在于acquireQueued，排队等待的worker，并不是一直等待着，而是无限的循环去抢锁：抢到了则退出，抢不到，需要更新状态，park，这个逻辑就是shouldParkAfterFailedAcquire(Node, Node)。


3.2 acquireShared()--共享模式获取资源
接下来简单说下共享模式下获取资源的流程。
acquireShared()：以共享模式获取对象，忽略中断。先是tryAcquireShared(int)尝试直接去获取资源，如果成功，acquireShared(int)就结束了；否则，调用doAcquireShared(Node)将线程加入等待队列，直到获取到资源为止。

    public final void acquireShared(int arg) {
        //模板方法模式，tryAcquireShared由子类实现
        //想看的话推荐读写锁的源码，这里就不细述了
        if (tryAcquireShared(arg) < 0)
            doAcquireShared(arg);
    }

doAcquireShared()：实现上和acquire()方法差不多，就是多判断了是否还有剩余资源，唤醒后继节点。

    private void doAcquireShared(int arg) {
        //将线程加入等待队列，设置为共享模式
        final Node node = addWaiter(Node.SHARED);
        boolean failed = true;
        try {
            boolean interrupted = false;
            //自旋尝试获取资源
            for (;;) {
                final Node p = node.predecessor();
                if (p == head) {
                    int r = tryAcquireShared(arg);
                    if (r >= 0) {
                        //设置头节点，且如果还有剩余资源，唤醒后继节点获取资源
                        setHeadAndPropagate(node, r);
                        p.next = null; // help GC
                        if (interrupted)
                            selfInterrupt();
                        failed = false;
                        return;
                    }
                }
                //是否需要被挂起
                if (shouldParkAfterFailedAcquire(p, node) &&
                    parkAndCheckInterrupt())
                    interrupted = true;
            }
        } finally {
            if (failed)
                cancelAcquire(node);
        }
    }

3.3 hasQueuedPredecessors()--公平锁在tryAqcuire()时调用
这里补充一个方法，ReentrantLock如果是公平锁的话，会调用AQS中的这个方法，算是后续文章的铺垫吧。

boolean hasQueuedPredecessors()：判断当前线程是否位于CLH同步队列中的第一个。如果是则返回flase，否则返回true。

    public final boolean hasQueuedPredecessors() {
        //判断当前节点在等待队列中是否为头节点的后继节点（头节点不存储数据），
        //如果不是，则说明有线程比当前线程更早的请求资源，
        //根据公平性，当前线程请求资源失败。
        //如果当前节点没有前驱节点的话，才有做后面的逻辑判断的必要性
        Node t = tail; // Read fields in reverse initialization order
        Node h = head;
        Node s;
        return h != t &&
            ((s = h.next) == null || s.thread != Thread.currentThread());
    }

3.4 doAcquireNanos()--独占模式下在规定时间内获取锁
这个方法在ReentrantLock.tryLock()过程中被调用。

doAcquireNanos()：这个方法只工作于独占模式，自旋获取资源超时后则返回false；如果有必要挂起且未超时则挂起。

    private boolean doAcquireNanos(int arg, long nanosTimeout)
            throws InterruptedException {
        if (nanosTimeout <= 0L)
            return false;
        //计算截至时间
        final long deadline = System.nanoTime() + nanosTimeout;
        final Node node = addWaiter(Node.EXCLUSIVE);
        boolean failed = true;
        try {
            for (;;) {
                final Node p = node.predecessor();
                //获取锁成功后，出队
                if (p == head && tryAcquire(arg)) {
                    setHead(node);
                    p.next = null; 
                    failed = false;
                    return true;
                }
                //重新计算超时时间
                nanosTimeout = deadline - System.nanoTime();
                //超时则返回false
                if (nanosTimeout <= 0L)
                    return false;
                //否则判断是否需要被阻塞，阻塞规定时间
                if (shouldParkAfterFailedAcquire(p, node) &&
                    nanosTimeout > spinForTimeoutThreshold)
                    LockSupport.parkNanos(this, nanosTimeout);
                if (Thread.interrupted())
                    throw new InterruptedException();
            }
        } finally {
            if (failed)
                cancelAcquire(node);
        }
    }

3.5 doAcquireInterruptibly--获取锁时响应中断

这个方法在ReentrantLock.lockInterruptibly()过程中被调用。
doAcquireInterruptibly()：独占模式下在获取锁时会阻塞，但是能响应中断请求。

    private void doAcquireInterruptibly(int arg)
        throws InterruptedException {
        //添加到等待队列，包装成Node
        final Node node = addWaiter(Node.EXCLUSIVE);
        boolean failed = true;
        try {
            for (;;) {
                final Node p = node.predecessor();
                if (p == head && tryAcquire(arg)) {
                    setHead(node);
                    p.next = null; // help GC
                    failed = false;
                    return;
                }
                //自旋，直到前驱节点等待状态为SIGNAL，检查中断标志
                //符合条件则阻塞当前线程
                if (shouldParkAfterFailedAcquire(p, node) &&
                    parkAndCheckInterrupt())
                    //当前线程被阻塞后，会中断
                    throw new InterruptedException();
            }
        } finally {
            if (failed)
                cancelAcquire(node);
        }
    }

四、释放资源（锁）
同样的，释放资源也分为释放独占锁资源（release()）和共享锁(releaseShared())资源。
先来看对于独占锁的释放。

4.1 release()--独占模式释放资源

release()：工作于独占模式，首先调用子类的tryRelease()方法释放锁,然后唤醒后继节点,在唤醒的过程中,需要判断后继节点是否满足情况,如果后继节点不为空且不是作废状态,则唤醒这个后继节点,否则从tail节点向前寻找合适的节点,如果找到,则唤醒。

    public final boolean release(int arg) {
        if (tryRelease(arg)) {
            Node h = head;
            if (h != null && h.waitStatus != 0)
                unparkSuccessor(h);
            return true;
        }
        return false;
    }

unparkSuccessor()：尝试找到下一位继承人，就是确定下一个获取资源的线程，唤醒指定节点的后继节点。

    private void unparkSuccessor(Node node) {
        //如果状态为负说明是除CANCEL以外的状态，
        //尝试在等待信号时清除。
        int ws = node.waitStatus;
        if (ws < 0)
            compareAndSetWaitStatus(node, ws, 0);

        Node s = node.next;
        //下一个节点为空或CANCELLED状态
        //则从队尾往前找，找到正常状态的节点作为之后的继承人
        //也就是下一个能拿到资源的节点
        if (s == null || s.waitStatus > 0) {
            s = null;
            for (Node t = tail; t != null && t != node; t = t.prev)
                if (t.waitStatus <= 0)
                    s = t;
        }
        //此时找到继承人了，那么就唤醒它
        if (s != null)
            LockSupport.unpark(s.thread);
    }

4.2 releaseShared()--共享模式释放资源
releaseShared()：在释放一部分资源后就可以通知其他线程获取资源。

    public final boolean releaseShared(int arg) {
        if (tryReleaseShared(arg)) {
            doReleaseShared();
            return true;
        }
        return false;
    }

doReleaseShared()：把当前结点的状态由SIGNAL设置为PROPAGATE状态。

    private void doReleaseShared() {

        for (;;) {
            Node h = head;
            if (h != null && h != tail) {
                int ws = h.waitStatus;
                if (ws == Node.SIGNAL) {
                    if (!compareAndSetWaitStatus(h, Node.SIGNAL, 0))
                        //将自旋尝试修改等待状态为0
                        continue;
                    //唤醒下一个被阻塞的节点
                    unparkSuccessor(h);
                }
                else if (ws == 0 &&
                         !compareAndSetWaitStatus(h, 0, Node.PROPAGATE))
                    //设置为PROPAGATE状态
                    continue;               
            }
            //上面代码块执行过程中如果head变更了，就接着循环尝试
            if (h == head)
                break;
        }
    }

五、内部类-ConditionObject

ConditionObject实现了Condition接口。用于线程间的通信，能够把锁粒度减小。重点是await()和signal()。这个内部类还维护了一个condition队列，而且Node.nextWaiter就是用来将condition连接起来的。

        //condition队头
        private transient Node firstWaiter;
        //condition队尾
        private transient Node lastWaiter;
        //发生了中断，但在后续不抛出中断异常，而是“补上”这次中断
        private static final int REINTERRUPT =  1;
        //发生了中断，且在后续需要抛出中断异常
        private static final int THROW_IE    = -1;

5.1 await()--阻塞等待方法

5.1.1 await的流程

await()：当前线程处于阻塞状态，直到调用signal()或中断才能被唤醒。

1）将当前线程封装成node且等待状态为CONDITION。
2）释放当前线程持有的所有资源，让下一个线程能获取资源。
3）加入到条件队列后，则阻塞当前线程，等待被唤醒。
4）如果是因signal被唤醒，则节点会从条件队列转移到等待队列；如果是因中断被唤醒，则记录中断状态。两种情况都会跳出循环。
5）若是因signal被唤醒，就自旋获取资源；否则处理中断异常。

        public final void await() throws InterruptedException {
            //如果被中断，就处理中断异常
            if (Thread.interrupted())
                throw new InterruptedException();
            //初始化链表的功能，设置当前线程为链尾
            Node node = addConditionWaiter();
            //释放当前节点持有的所有资源
            int savedState = fullyRelease(node);
            int interruptMode = 0;
            //如果当前线程不在等待队列中，
            //说明此时一定在条件队列里，将其阻塞。
            while (!isOnSyncQueue(node)) {
                LockSupport.park(this);
                //说明中断状态发生变化
                if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
                    break;
            }
            //当前线程执行了signal方法会经过这个，也就是重新将当前线程加入同步队列中
            if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
                interruptMode = REINTERRUPT;
            //删除条件队列中不满足要求的元素
            if (node.nextWaiter != null) 
                unlinkCancelledWaiters();
            //处理被中断的情况
            if (interruptMode != 0)
                //这里是个难点，具体的实现我自己也有点不理解
                //就把知道的都写出来
                //如果是THROW_IE，说明signal之前发生中断
                //如果是REINTERRUPT，signal之后中断，
                //所以成功获取资源后会调用selfInterrupt()
                reportInterruptAfterWait(interruptMode);
        }

addConditionWaiter()：将当前线程封装成节点，添加到条件队列尾部，并返回当前节点。

        private Node addConditionWaiter() {
            Node t = lastWaiter;
            // 判断队尾元素，如果非条件等待状态则清理出去
            if (t != null && t.waitStatus != Node.CONDITION) {
                unlinkCancelledWaiters();
                //可能t之前引用的节点被删除了，所以要重新引用
                t = lastWaiter;
            }
            //这个节点就表示当前线程
            Node node = new Node(Thread.currentThread(), Node.CONDITION);
            //说明条件按队列中没有元素
            if (t == null)
                firstWaiter = node;
            else
                t.nextWaiter = node;
            lastWaiter = node;
            return node;
        }

unlinkCancelledWaiters()：遍历一遍条件队列，删除非CONDITION状态的节点。

        private void unlinkCancelledWaiters() {
            Node t = firstWaiter;
            //记录在循环中上一个waitStatus有效的节点
            Node trail = null;
            while (t != null) {
                Node next = t.nextWaiter;
                //再次判断等待状态，保证节点都是CONDITION状态
                //确保当前节点无效后删除引用
                if (t.waitStatus != Node.CONDITION) {
                    t.nextWaiter = null;
                    if (trail == null)
                        firstWaiter = next;
                    else
                        //否则就直接加到队尾的后面
                        trail.nextWaiter = next;
                    if (next == null)
                        lastWaiter = trail;
                }
                else
                    //记录有效的节点并向后遍历
                    trail = t;
                t = next;
            }
        }
5.1.2 await中关于中断的处理

通过对上面代码的观察，我们知道await()调用了checkInterruptWhileWaiting()。
关于中断这一块，我自己看的也比较迷，就把一些自己能理解的地方标注一下。
checkInterruptWhileWaiting()：判断在阻塞过程中是否被中断。如果返回THROW_IE，则表示线程在调用signal()之前中断的；如果返回REINTERRUPT，则表明线程在调用signal()之后中断；如果返回0则表示没有被中断。

        private int checkInterruptWhileWaiting(Node node) {
            return Thread.interrupted() ?
                (transferAfterCancelledWait(node) ? THROW_IE : REINTERRUPT) :
                0;
        }

transferAfterCancelledWait()：线程是否因为中断从park中唤醒。

    final boolean transferAfterCancelledWait(Node node) {
        //如果修改成功，暂且认为中断发生后，signal()被调用
        if (compareAndSetWaitStatus(node, Node.CONDITION, 0)) {
            enq(node);
            //true表示中断先于signal发生
            return true;
        }
        //~~不理解~~
        while (!isOnSyncQueue(node))
            Thread.yield();
        return false;
    }
5.2.1 signal()--唤醒condition队列中的线程

signal()：唤醒一个被阻塞的线程。

        public final void signal() {
            //判断当前线程是否为资源的持有者
            //这也是必须在lock()与unlock()代码中间执行的原因
            if (!isHeldExclusively())
                throw new IllegalMonitorStateException();
            //开始唤醒条件队列的第一个节点
            Node first = firstWaiter;
            if (first != null)
                doSignal(first);
        }

doSignal()：将条件队列的头节点从条件队列转移到等待队列，并且，将该节点从条件队列删除。

        private void doSignal(Node first) {
            do {
                //后续的等待条件为空，说明condition队列中只有一个节点
                if ( (firstWaiter = first.nextWaiter) == null)
                    lastWaiter = null;
                first.nextWaiter = null;
                //transferForSignal()是真正唤醒头节点的地方
            } while (!transferForSignal(first) &&
                     (first = firstWaiter) != null);
        }

transferForSignal()：将节点放入等待队列并唤醒。并不需要在条件队列中移除，因为条件队列每次插入时都会把状态不为CONDITION的节点清理出去。

    final boolean transferForSignal(Node node) {
        //当前节点等待状态改变失败，则说明当前节点不是CONDITION
        //状态，那就不能进行接下来的操作，返回false
        //0是正常状态
        if (!compareAndSetWaitStatus(node, Node.CONDITION, 0))
            return false;

        //放入等待队列队尾中，并返回之前队列的前一个节点
        Node p = enq(node);
        int ws = p.waitStatus;
        //如果节点没有被取消，或更改状态失败，则唤醒被阻塞的线程
        if (ws > 0 || !compareAndSetWaitStatus(p, ws, Node.SIGNAL))
            LockSupport.unpark(node.thread);
        return true;
    }

5.2.2 signalAll()--唤醒condition队列中所有线程
signalAll()本质上还是调用了doSignalAll()
doSignalAll()：遍历条件队列，插入到等待队列中。

        private void doSignalAll(Node first) {
            lastWaiter = firstWaiter = null;
            do {
                Node next = first.nextWaiter;
                first.nextWaiter = null;
                transferForSignal(first);
                first = next;
            } while (first != null);
        }
        
5.3 awaitNanos()--超时机制
这里补充一个方法awaitNanos()，是我看阻塞队列源码中遇到的。
awaitNanos()：轮询检查线程是否在同步线程上，如果在则退出自旋。否则检查是否已超过解除挂起时间，如果超过，则退出挂起，否则继续挂起线程到等待解除挂起。退出挂起之后，采用自旋的方式竞争锁。

        public final long awaitNanos(long nanosTimeout)
                throws InterruptedException {
            if (Thread.interrupted())
                throw new InterruptedException();
            Node node = addConditionWaiter();
            int savedState = fullyRelease(node);
            final long deadline = System.nanoTime() + nanosTimeout;
            int interruptMode = 0;
            //采用自旋的方式检查是否已在等待队列当中
            while (!isOnSyncQueue(node)) {
                //如果挂起超过一定的时间，则退出
                if (nanosTimeout <= 0L) {
                    transferAfterCancelledWait(node);
                    break;
                }
                //继续挂起线程
                if (nanosTimeout >= spinForTimeoutThreshold)
                    LockSupport.parkNanos(this, nanosTimeout);
                if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
                    break;
                nanosTimeout = deadline - System.nanoTime();
            }
            //采用自旋的方式竞争锁
            if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
                interruptMode = REINTERRUPT;
            if (node.nextWaiter != null)
                unlinkCancelledWaiters();
            if (interruptMode != 0)
                reportInterruptAfterWait(interruptMode);
            return deadline - System.nanoTime();
        }
总结
关于AQS我觉得比较重要的就是获取资源和释放资源的方法，里面用到了大量的CAS操作和自旋。AQS里面维护了两个队列，一个是等待队列（CHL），还有一个是条件队列（condition）。
acquire()尝试获取资源，如果获取失败，将线程插入等待队列。插入后，并没有放弃获取资源，而是根据前置节点状态状态判断是否应该继续获取资源。如果前置节点是头结点，继续尝试获取资源；如果前置节点是SIGNAL状态，就中断当前线程，否则继续尝试获取资源。直到当前线程被阻塞或者获取到资源，结束。
release()释放资源，需要唤醒后继节点,判断后继节点是否满足情况。如果后继节点不为空且不是作废状态,则唤醒这个后继节点；否则从尾部往前面找适合的节点，找到则唤醒。
调用await()，线程会进入条件队列，等待被唤醒，唤醒后以自旋方式获取资源或处理中断异常；调用signal()，线程会插入到等待队列，唤醒被阻塞的线程。

如有不当之处，欢迎指出~
