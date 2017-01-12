---
layout: post
title: "Concurrence-24-Blocking Queues"
description: "翻译 梳理基础的东西"
category: 翻译 java 并发编程 多线程
tags: [翻译 并发编程 多线程]
---
#### Blocking Queues
<br/>
##### 阻塞队列
<br/>
[文章的源地址](http://tutorials.jenkov.com/java-concurrency/blocking-queues.html)
<br/>

一个阻塞队列在队列为空的时候，你如果出队，那么将会被阻塞。如果队满的时候，如果入队，也会被阻塞。
一个从空队列出队的线程会被阻塞到其他的线程在这个队列中插入一个元素。同样的一个向满队列插入元素的
线程会被阻塞一直到其他的线程出队或者清理掉所有的队列元素。    

下图表示两个线程对阻塞队列的操作：   

![](http://tutorials.jenkov.com/images/java-concurrency-utils/blocking-queue.png)

java5中实现阻塞队列的类位于java.util.concurrent中，我们来说一下阻塞队列的原理。    

##### 阻塞队列的实现

阻塞队列的实现看起来有点像 Bounded Semaphore 下面是一个简单的实现：   

```
public class BlockingQueue {

  private List queue = new LinkedList();
  private int  limit = 10;

  public BlockingQueue(int limit){
    this.limit = limit;
  }


  public synchronized void enqueue(Object item)
  throws InterruptedException  {
    while(this.queue.size() == this.limit) {
      wait();
    }
    if(this.queue.size() == 0) {
      notifyAll();
    }
    this.queue.add(item);
  }


  public synchronized Object dequeue()
  throws InterruptedException{
    while(this.queue.size() == 0){
      wait();
    }
    if(this.queue.size() == this.limit){
      notifyAll();
    }

    return this.queue.remove(0);
  }

}

```
备注： notifyAll 这个方法在dequeue和enqueue里面，只有当队列的大小到达了界限的时候（0或者最大值）的时候，才会被调用。
如果没有到达界限的时候，调用notifyAll,可能还没有线程在调用dequeue或者enqueue方法。


JDK的实现的简单的阻塞队列：   

```
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author zhailzh
 * 
 * @Date 2015年10月27日——下午1:57:15
 * 
 */
public class SimplyBlockingQueue {
	final Lock lock = new ReentrantLock();
	final Condition notFull = lock.newCondition();
	final Condition notEmpty = lock.newCondition();

	final Object[] items = new Object[10];
	int putptr, takeptr, count;

	public void put(Object x) throws InterruptedException {
		lock.lock();
		try {
			while (count == items.length) {
				System.out.println("buffer full, please wait");
				notFull.await();
			}
				
			items[putptr] = x;
			//比使用取摸运算要好，因为putptr一直递增的话，有可能超出界限
			if (++putptr == items.length){
				putptr = 0;
			}
				
			++count;
			notEmpty.signal();
		} finally {
			lock.unlock();
		}
	}




	public Object take() throws InterruptedException {
		lock.lock();
		try {
			while (count == 0) {
				System.out.println("no elements, please wait");
				notEmpty.await();
			}
			
			Object x = items[takeptr];
			//比使用取摸运算要好，因为takeptr一直递增的话，有可能超出界限
			if (++takeptr == items.length){
				takeptr = 0;
			}
			--count;
			notFull.signal();
			return x;
		} finally {
			lock.unlock();
		}
	}
	
	public static void main(String[] args) {
		final SimplyBlockingQueue boundedBuffer = new SimplyBlockingQueue();
		
		Thread t1 = new Thread(new Runnable() {
			@Override
			public void run() {
				System.out.println("t1 run");
				for (int i=0;i<1000;i++) {
					try {
						System.out.println("putting value ：" +i );
						boundedBuffer.put(Integer.valueOf(i));
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			
		}) ;
		
		Thread t2 = new Thread(new Runnable() {
			@Override
			public void run() {
				for (int i=0;i<1000;i++) {
					try {
						Object val = boundedBuffer.take();
						System.out.println("消耗了："+val);

						Thread.sleep(5000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			
		}) ;
		
		t1.start();
		t2.start();
	}
}

```

测试结果：     

```
no elements, please wait
t1 run
putting value ：0
消耗了：0
putting value ：1
putting value ：2
putting value ：3
putting value ：4
消耗了：1
putting value ：5
putting value ：6
putting value ：7
putting value ：8
putting value ：9
消耗了：2
putting value ：10
putting value ：11
putting value ：12
putting value ：13
buffer full, please wait
消耗了：3
```














