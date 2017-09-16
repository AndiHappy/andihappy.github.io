---    
layout: post  
title: "JDK并发框架的描述（一）"  
subtitle: "重入锁，读写锁，还有一些扩展的锁的实现的基础（一）"  
date: 2017-08-26 09:00:00  
author: "zhailzh"  
header-img: "img/post-bg-2015.jpg"  
catalog: true  
tags:  
- 学习  
categories:  
- concurrent
---  
   梳理JDK中并发编程的实现的原理，感觉很多东西还是非常的有意思的，例如锁的实现。我们首先说：重入锁，读写锁，还有一些扩展的锁的实现的基础：同步器（java.util.concurrent.locks.AbstractQueuedSynchronizer）
<!--more-->

java.util.concurrent.locks.AbstractQueuedSynchronizer 为什么称之为实现锁的基础呢？    
这个我们需要从头说起。     

因为在java.util.concurrent包下面的JDK的锁的实现方式中，主要的设计模式就是模板方法。因为一个锁的基本行为是可以预期的：        

> 1. 分为了两个方法：抢锁，释放锁
> 
> 2. 抢到了锁，则可以返回true，抢不到锁则要等待。具体的怎么才能抢到锁的行为针对不同的锁，有不同的说法，但是针对大体的行为还是有规范的，就是抢不到锁要等待，一直等到锁再次可以抢了，就是一个循环了。
> 
> 3. 释放锁，如果释放成功，必须通知其他等待的线程，这个锁已经释放了，直接的返回true。如果失败，则会返回失败。

所以有了AbstractQueuedSynchronizer 定义模板方法，而不同的锁，只需要定义抽象出来的方法即可。        

1.首先我们看抢锁这个动作定义的模板，包括两个模板，首先是加锁的模板：     

~~~java
 public final void acquire(int arg) {
        if (!tryAcquire(arg) &&
            acquireQueued(addWaiter(Node.EXCLUSIVE), arg))
            selfInterrupt();
    }
~~~    
   

这里面有一个 int值，非常的重要的一个参数，或者说，整个锁的关键点就在于这个int值的理解。       

> 这个int值标识的锁的状态，对应的是AbstractQueuedSynchronizer的成员变量：      
   /**       
     * The synchronization state.       
     */      
    private volatile int state;           

从代码中，可以看到：acquire的第一个函数调用的就是：     
> tryAcquire(arg)   

代表是得到锁的状态值的函数。

整个if语句解释过来就是：
> !tryAcquire(arg)        

如果没有能够获得锁的状态，就去：acquireQueued(addWaiter(Node.EXCLUSIVE), arg) 把这个线程当做一个waiter，加入等待的队列。具体是怎么加入的等待队列以及等待队列的实现，我们后面再说。

如果获得了锁的状态，说明!tryAcquire(arg) 返回的是false，那么直接的返回了，就会继续执行临界区的代码。

这样的话，不同的锁只需要扩展自己对应的tryAcquire函数即可，所以我们可以看到AbstractQueuedSynchronizer 中tryAcquire是一个抽象函数。查了一下代码，竟然不是抽象的方法？奇怪。  但是也是继承类必须实现的一个方法了。  

~~~java
 protected boolean tryAcquire(int arg) {
        throw new UnsupportedOperationException();
    }
~~~

另外一个是释放锁的模板：        

~~~java
public final boolean release(int arg) {
        if (tryRelease(arg)) {
            Node h = head;
            if (h != null && h.waitStatus != 0)
                unparkSuccessor(h);
            return true;
        }
        return false;
    }
~~~    

这个我们就能够看到，释放锁的固定的流程，关于在于：
> tryRelease(arg)     

这个同样的是更新锁的状态，具体是否更新成功以及更新成为什么，想加锁的方法一样有具体的子类实现，如果更新成功，那么：   

~~~java
 Node h = head;
            if (h != null && h.waitStatus != 0)
                unparkSuccessor(h);
            return true;
~~~
就会把等待队列中的线程，unparkSuccessor(h),这个是激活某一个线程的方法。

如果没有更新成功，则会直接的返回false，也就是释放锁失败。


接着我们看具体的锁，是怎么实现的。最常见的一类锁：ReentrantLock，首先我们设想一下：可重入锁，针对模板方法需要实现的内容：      

>1. 要求只能一个线程进入，这个可以通过修改锁的状态值来实现，所以只需要把锁的状态设置为一个标志着：已经有一个线程占用的就可以了。如果有其他的线程看到了这个状态，直接的返回false
>2. 同一个线程，可以重复的进入，这个需要记录进入的次数，以便于在释放锁的时候，可以对应的出来几次才能够释放成功。

对应的代码是：    
         
~~~java     
abstract static class Sync extends AbstractQueuedSynchronizer {
        abstract void lock();
        final boolean nonfairTryAcquire(int acquires) {
            final Thread current = Thread.currentThread();
            int c = getState();
            if (c == 0) {
                if (compareAndSetState(0, acquires)) {
                    setExclusiveOwnerThread(current);
                    return true;
                }
            }
            else if (current == getExclusiveOwnerThread()) {
                int nextc = c + acquires;
                if (nextc < 0) // overflow
                    throw new Error("Maximum lock count exceeded");
                setState(nextc);
                return true;
            }
            return false;
        }
        
        
        static final class NonfairSync extends Sync {
        final void lock() {
            if (compareAndSetState(0, 1))
                setExclusiveOwnerThread(Thread.currentThread());
            else
                acquire(1);
        }

        protected final boolean tryAcquire(int acquires) {
            return nonfairTryAcquire(acquires);
        }
    }

~~~     
  
  
可以说实现的方式，大大出乎了我们的意料，仔细的观察代码，会发现我们想法的简陋，在代码的实现上面JDK的精致，优雅。虽然主题的意思也是按照我们所设想的那么个意思，但是代码的实现确实不愧为大师级别的代码:          

> 1. 首先是通过**静态内部类的方式来实现的**，为以后的可能的扩展，或者修改ReentrantLock的实现方法，打下了良好的基础。如果操作系统突然实现了一种新的控制锁状态的方法，替换模板方法的实现，丝毫不会影响ReentrantLock 对外提供的方法：lock（）
> 2. 考虑到了等待线程抢锁的时候的安排问题，如果等待时间比较长的先得到锁的公平锁，还是从新开始抢非公平锁。内部扩展了两种的实现方式：可重入公平锁，以及可重入的非公平锁。
> 3. ReentrantLock默认的是非公平锁，这个问题我们也可以以后讨论，我们先分析非公平锁的实现。首先就是去设置锁的状态，也就是抢锁，然后才是进入模板方法的：acquire（1），模板方法的acquire（1），调用tryAcquire（1），这个有ReentrantLock静态内部类：NonfairSync实现的tryAcquire方法，最后的实现的逻辑是抽象类AbstractQueuedSynchronizer的扩展者Sync：nonfairTryAcquire（1）
> 4. 在nonfairTryAcquire（1），我们找到了可重入的实现逻辑，如果当前的线程Thread.currentThread()是getExclusiveOwnerThread线程，那么锁的状态：    
> nextc = c + acquires;    
> 加一。
> 
> 5. 如果加锁不成功，就会触发模板方法中的： selfInterrupt()，所以lock才会是void的方法，需要在try-catch中调用


对应的释放锁的实现是：         

~~~java
abstract static class Sync extends AbstractQueuedSynchronizer {
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

你会发现 释放锁的逻辑，没有了公平和非公平一说，只有抢锁的时候才会出现。再一次感觉JDK代码的优雅。    

同样的首先是调用unlock逻辑，直接的调到：Sync的释放锁的方法。    
      
~~~java
public void unlock() {
        sync.release(1);
    }
~~~

释放锁实现判断：首先判断是不是当前的线程占有线程的状态，如果是，则会把线程的状态减一。减为零的时候，释放成功。


---------可重入锁的加锁逻辑分析完毕----------------      



---------CountDownLatch加锁--------------------       

CountDownLatch加锁逻辑和可重入锁的加锁逻辑不一样，主要是因为可重入锁是独占锁，只允许一个线程的进入，但是CountDownLatch，却是共享锁。

针对共享锁，java.util.concurrent.locks.AbstractQueuedSynchronizer 同样定义出了一套模板：

~~~java
public final void acquireShared(int arg) {
        if (tryAcquireShared(arg) < 0)
            doAcquireShared(arg);
    }
public final void acquireSharedInterruptibly(int arg)
            throws InterruptedException {
        if (Thread.interrupted())
            throw new InterruptedException();
        if (tryAcquireShared(arg) < 0)
            doAcquireSharedInterruptibly(arg);
    }
    
public final boolean releaseShared(int arg) {
        if (tryReleaseShared(arg)) {
            doReleaseShared();
            return true;
        }
        return false;
    }

~~~

我们来说一下共享锁的设计模板，共享式锁与独占式锁最大的不同就在于：同一个时间可能会有多个线程，拥有线程状态的访问权限。

就针对一个文件来说，拿到线程状态的读进程，可以有很多个，但是写进程就会被阻塞掉。

我们再来分析一下共享锁的模板方法：  
 
> 1. 加锁的过程，tryAcquireShared(arg)，还是有扩展的子类进行定义，如果拿到了锁，或者说tryAcquireShared(arg)> 0 说明拿到了共享锁，直接的返回，进入临界区的代码，执行。如果没有，即是：tryAcquireShared(arg) < 0,则会进入等待的队列    
> 2. 解锁的过程，基本是一样的，如果解锁成功，那么就要通知等待的队列再次的开始抢锁，如果解锁失败，直接的返回失败。


下面是针对CountDownLatch实现的加锁的分析，CountDownLatch是能够把几个动作拦截在同一时刻的基础的锁，提供的主要的方法是：   

~~~java
    public void countDown() {
        sync.releaseShared(1);
    }
    
    public void await() throws InterruptedException {
        sync.acquireSharedInterruptibly(1);
    }
~~~    

具体的实现的逻辑是：          

~~~java
    public CountDownLatch(int count) {
        if (count < 0) throw new IllegalArgumentException("count < 0");
        this.sync = new Sync(count);
    }
    
private static final class Sync extends AbstractQueuedSynchronizer {
        Sync(int count) {
            setState(count);
        }

        int getCount() {
            return getState();
        }

        protected int tryAcquireShared(int acquires) {
            return (getState() == 0) ? 1 : -1;
        }

        protected boolean tryReleaseShared(int releases) {
            // Decrement count; signal when transition to zero
            for (;;) {
                int c = getState();
                if (c == 0)
                    return false;
                int nextc = c-1;
                if (compareAndSetState(c, nextc))
                    return nextc == 0;
            }
        }
    }
    
~~~

实现的说明：
> 1. 还是静态的内部类，基本其他的锁也是都是这种形式
> 2. 开始的时候，就制定了“线程的状态” count ==》 setState(count),为后面的状态的加锁和释放锁做好了准备。
> 
> 3. 加锁的调用的是：sync.acquireSharedInterruptibly(1);不是模板方法acquire，不过该方法和acquire的逻辑比较的像，最终都会调用tryAcquireShared方法，如果不能成功，则会进入阻塞的等待队列。 CountDownLatch定义的tryAcquireShared，只有当状态为0的时候，才会返回1，所以在刚开始的时候count值大于1，直接的进入等待的队列。
> 
> 4. countDown，调用释放锁的逻辑，首先会进入模板方法：releaseShared，然后会调用CountDownLatch定义的tryReleaseShared，如果count不等于0，直接的返回是false，继续的执行相对应的代码，只有等到执行到最后一个时候，count为0的情况下，才会返回true，然后通知被阻塞的进程，激活调用await的线程。






