---
layout: post
title: "并发知识梳理：4. 读写锁等其他的类型的锁的支持"
subtitle: "AQS和ReentrantReadWriteLock"
author: "zhailzh"  
header-img: "img/post-bg-2015.jpg"  
catalog: true
tags: Concurrent  
---

ReentrantReadWriteLock 是在重入锁的基础上，添加读写的控制。主要关注的是共享锁的实现和理解上。

<!--more-->

首先还是复习一下重入锁的伪代码：

~~~

ReentrantLock
public NonFairLock nonfairLock = new NonFairLock();
lock(){
	nonfairLock.lock()
}

// 获取锁的过程
nonfairLock.lock(){
	if(cas(state,0,1)){
		setExcludeThread(Thread.currentThread());
	}else{
		if(!tryAcqure(1) && acquireQueued(addWaiter(Node.excluse),arg))
			selfInterrupt();
	}
}

nonfairLock.tryAcquire(int acquire){
	int c = getState();
	if(c == 0) {
		if(cas(state,0,1){
			setExcludeThread(Thread.currentThread());
			return true;
		}
	}else if(Thread.currentThread() == getExclude){
		int nextc = c+acquire;
		if(nextc < 0) {
			throw new Error(lock too large);
		}
		setstate(nextc);
		return true;
	}
	return false;
}

Node addWaiter(){ // 把Thread放到一个Node节点中，组成等待的链条
	 Node node = new Node(Thread.currentThread(), mode);
	 addTail(node)
	 return node;
}
acquireQueued(Node node) {// 更新等待链条的状态，释放锁的时候，可以调用tryAcquire
	interrupted = false;
	for(;;){
		if(head == node.pre && tryAcquire()){
			 setHead(node); // head = node;
       p.next = null; // help GC
       failed = false;
       return interrupted;
		}
		
		if(shouldParkAfterAcqiureFailed(p,node) && parkAndChectInterrupt()){
			interrupted = true;
		}
	}
}

//释放锁的过程
unlock(){
	nonfairLock.release(1);
}

nonfairLock.release(1){
	if(tryRealse(1)){
		If(head != null && h.state != 0){
			unparkSuccessor(head) // unpark head的下一个节点，head中不包含thread
		}
	}
}

nonfairLock.tryRelease(1){
	int c = getState  - 1;
	if(Thread.concurrent != getExcluseThread) {
		throw Error( not state )
	}
	 boolean free = false;
	if(c == 0) {
		setExcluseThread(null)
		free = true
	}
	
	setState(c);
	return free;
}

~~~

读写锁的控制，更加的复杂，我们只是大致的梳理出来重要的逻辑框架，并不扣具体的细节。

~~~
class CachedData {
 Object data;
 volatile boolean cacheValid;
 final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();

 void processCachedData() {
  rwl.readLock().lock();
  if (!cacheValid) {
   // Must release read lock before acquiring write lock
   rwl.readLock().unlock();
   rwl.writeLock().lock();
   try {
    // Recheck state because another thread might have
    // acquired write lock and changed state before we did.
    if (!cacheValid) {
     data = "use data ";
     cacheValid = true;
    }
    // Downgrade by acquiring read lock before releasing write lock
    rwl.readLock().lock();
   } finally {
    rwl.writeLock().unlock(); // Unlock write, still hold read
   }
  }

  try {
   //       use(data);
  } finally {
   rwl.readLock().unlock();
  }
 }
}
~~~

从官方的示例代码中，我们可以知道ReentrantReadWriteLock（以下简写为RRW）有两个锁类型，读锁：ReadLock（RL）和写锁：WriteLock (WL)
从代码中也能初见端倪：

~~~
 public ReentrantReadWriteLock(boolean fair) {
        sync = fair ? new FairSync() : new NonfairSync();
        readerLock = new ReadLock(this);
        writerLock = new WriteLock(this);
    }
~~~ 

我们首先看读锁：RL

~~~
RL.lock(){
    sync.acquireShared(1);
}
~~~

还是原来的熟悉的配方，使用内部类：Sync来实现。Sync继承AQS的框架

~~~
AQS.acquireShared(int arg) {
        if (tryAcquireShared(arg) < 0)
            doAcquireShared(arg);
    }
~~~

首先就是共享锁定加锁的过程：

~~~
Sync.tryAcquireShared(int unused) {

            Thread current = Thread.currentThread();
            int c = getState();
            // 如果被独占锁占据 直接返回-1
            if (exclusiveCount(c) != 0 && // exclusiveCount(c) c的后16位标识独占锁
                getExclusiveOwnerThread() != current)
                return -1;

            int r = sharedCount(c); // sharedCount(c) c的前16位标识的是共享锁

            //readerShouldBlock()：第一个排队的线程是否为独占节点
            if (!readerShouldBlock() && r < MAX_COUNT && compareAndSetState(c, c + SHARED_UNIT)) {
                // 条件表示为：第一个等待的线程不是独占节点，并且CAS成功
                if (r == 0) {
                    firstReader = current;
                    firstReaderHoldCount = 1;
                } else if (firstReader == current) {
                    firstReaderHoldCount++;
                } else {
                    HoldCounter rh = cachedHoldCounter;
                    if (rh == null || rh.tid != getThreadId(current))
                        cachedHoldCounter = rh = readHolds.get();
                    else if (rh.count == 0)
                        readHolds.set(rh);
                    rh.count++;
                }
                //返回1
                return 1;
            }

            return fullTryAcquireShared(current); // 增加了无限的循环
        }
~~~

doAcquireShared 类似于重入锁里面的 addWaiter和 更新节点状态

~~~
 private void doAcquireShared(int arg) {
        final Node node = addWaiter(Node.SHARED);
        boolean failed = true;
        try {
            boolean interrupted = false;
            for (;;) {
                final Node p = node.predecessor();
                if (p == head) {
                    int r = tryAcquireShared(arg);
                    if (r >= 0) {
                        setHeadAndPropagate(node, r);
                        p.next = null; // help GC
                        if (interrupted)
                            selfInterrupt();
                        failed = false;
                        return;
                    }
                }
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

再来看解锁的过程,这次释放的是共享锁

~~~
RL.unlock() {
            sync.releaseShared(1);
        }
~~~

同样的Sync的实现AQS的框架：

~~~
AQS.releaseShared(int arg) {
        if (tryReleaseShared(arg)) {
            doReleaseShared();
            return true;
        }
        return false;
    }
~~~

熟悉的配方，但是不是熟悉的过程了。共享锁在释放的过程中，不再是线程安全的单个线程的调用，而是多个线程调用了。

~~~
Sync.tryReleaseShared(int unused) {
   Thread current = Thread.currentThread();
   // 首先是普通成员变量 firstReader 的更新操作
   if (firstReader == current) {
    // assert firstReaderHoldCount > 0;
    if (firstReaderHoldCount == 1)
     firstReader = null;
    else
     firstReaderHoldCount--;
   } else {
    HoldCounter rh = cachedHoldCounter;
    if (rh == null || rh.tid != getThreadId(current))
     rh = readHolds.get();
    int count = rh.count;
    if (count <= 1) {
     readHolds.remove();
     if (count <= 0)
      throw unmatchedUnlockException();
    }
    --rh.count;
   }
   // 再次就是无限循环的状态更新，最后的结果是 nextc 是否为0
   for (;;) {
    int c = getState();
    int nextc = c - SHARED_UNIT;
    if (compareAndSetState(c, nextc))
     // Releasing the read lock has no effect on readers,
     // but it may allow waiting writers to proceed if
     // both read and write locks are now free.
     return nextc == 0;
   }
  }
~~~

doReleaseShared 和重入锁的释放过程，基本类似。

~~~
private void doReleaseShared() {
  for (;;) {
   Node h = head;
   if (h != null && h != tail) {
    int ws = h.waitStatus;
    if (ws == Node.SIGNAL) {
     if (!compareAndSetWaitStatus(h, Node.SIGNAL, 0))
      continue; // loop to recheck cases
     unparkSuccessor(h);
    } else if (ws == 0 && !compareAndSetWaitStatus(h, 0, Node.PROPAGATE))
     continue; // loop on failed CAS
   }
   if (h == head) // loop if head changed
    break;
  }
 }
~~~

