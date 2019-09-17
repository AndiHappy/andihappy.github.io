---
layout: post
title: "并发知识梳理：4. 阻塞队列"
subtitle: "AQS和ReentrantReadWriteLock"
author: "zhailzh"  
header-img: "img/post-bg-2015.jpg"  
catalog: true
tags: Concurrent  
---

阻塞队列的接口为： BlockingQueue<E> 这块是已经陌生但是以前比较梳理的内容，写出来在熟悉一下。

<!--more-->

### BlockingQueue 的操作方法

![](https://raw.githubusercontent.com/AndiHappy/blogimage/master/post/queue/q1.jpg)

如图中所示：
add(e),当队列满了以后，抛出IllegalStateException。
offer(e),当队列满了以后,返回false。
put(e),当队列满了以后,会被阻塞。
put(e,time,unit):false if the specified waiting time elapses before space is available

### BlockingQueue常用的四个实现类
   
ArrayBlockingQueue：规定大小的BlockingQueue,其构造函数必须带一个int参数来指明其大小.其所含的对象是以FIFO(先入先出)顺序排序的.

LinkedBlockingQueue：大小不定的BlockingQueue,若其构造函数带一个规定大小的参数,生成的
BlockingQueue有大小限制,若不带大小参数,所生成的BlockingQueue的大小由Integer.MAX_VALUE来决定.其所含的对象是以FIFO(先入先出)顺序排序的

PriorityBlockingQueue：类似于LinkedBlockQueue,但其所含对象的排序不是FIFO,而是依据对象的自然排序顺序或者是构造函数的Comparator决定的顺序.

SynchronousQueue：特殊的BlockingQueue,对其的操作必须是放和取交替完成的.

###  ArrayBlockingQueue<E> 主要实现的逻辑

我们还可以先回看一下ArrayQueue的实现：
~~~
// 成员变量

private int capacity;
private T[] queue;
private int head;
private int tail;

// 有点意思的是构造函数 capacity+1 
public ArrayQueue(int capacity) {
    this.capacity = capacity + 1;
    this.queue = newArray(capacity + 1);
    this.head = 0;
    this.tail = 0;
}
// 添加方法，采用的是tail+1 取余capacity 的方法，这也就是构建capacity+1的原因
 public boolean add(T o) {
  queue[tail] = o;
  int newtail = (tail + 1) % capacity;
  if (newtail == head)
   throw new IndexOutOfBoundsException("Queue full");
  tail = newtail;
  return true; // we did add something
 }

//删除的方法，采用的是 head+1 取余capacity 的方法，都是+1取余的操作
public T remove(int i) {
  if (i != 0)
   throw new IllegalArgumentException("Can only remove head of queue");
  if (head == tail)
   throw new IndexOutOfBoundsException("Queue empty");
  T removed = queue[head];
  queue[head] = null;
  head = (head + 1) % capacity;
  return removed;
 }

 public T get(int i) {
  int size = size();
  if (i < 0 || i >= size) {
   final String msg = "Index " + i + ", queue size " + size;
   throw new IndexOutOfBoundsException(msg);
  }
  int index = (head + i) % capacity;
  return queue[index];
 }

// 判定大小的时候，采用的是tail-head 的方法
 public int size() {
  // Can't use % here because it's not mod: -3 % 2 is -1, not +1.
  int diff = tail - head;
  if (diff < 0)
   diff += capacity;
  return diff;
 }
~~~

然后我们再看BlockingQueue的方法的实现：

~~~
 // ArrayBlockingQueue 的成员变量：

 /** The queued items */
 final Object[] items;

 /** items index for next take, poll, peek or remove */
 int takeIndex;

 /** items index for next put, offer, or add */
 int putIndex;

 /** Number of elements in the queue */
 int count;

 /** Main lock guarding all access */
 final ReentrantLock lock;

 /** Condition for waiting takes */
 private final Condition notEmpty;

 /** Condition for waiting puts */
 private final Condition notFull;

 public ArrayBlockingQueue(int capacity, boolean fair) {
  if (capacity <= 0)
   throw new IllegalArgumentException();
  this.items = new Object[capacity];
  lock = new ReentrantLock(fair);
  notEmpty = lock.newCondition();
  notFull = lock.newCondition();
 }

~~~

具体的方法的实现：

~~~
public boolean add(E e) {
  return super.add(e);
 }

/**
实现的方式：
public boolean add(E e) {
   if (offer(e))
        return true;
   else
       throw new IllegalStateException("Queue full");
} 
*/
 public boolean offer(E e) {
  checkNotNull(e);
  final ReentrantLock lock = this.lock;
  lock.lock();
  try {
   if (count == items.length)
    return false;
   else {
    // 保证lock的前提下，调用这个方法
    // 保证了队列不满，如果满了，直接在try方法中就已经返回
    enqueue(e);
    return true;
   }
  } finally {
   lock.unlock();
  }
 }
 /**
  * ① 已经拥有锁的前提下调用
  * ② 满的时候，putIndex直接设置为0，从头开始（这里面的putIndex为0，标识已经满了）
  * */
 private void enqueue(E x) {
  final Object[] items = this.items;
  items[putIndex] = x;
  if (++putIndex == items.length)
   putIndex = 0;
  count++;
  notEmpty.signal();
 }
 
 public void put(E e) throws InterruptedException {
  checkNotNull(e);
  final ReentrantLock lock = this.lock;
  lock.lockInterruptibly();
  try {
   while (count == items.length)
    notFull.await();
   enqueue(e);
  } finally {
   lock.unlock();
  }
 }
 
 //相对应的四个方法：remove() poll()
 public E remove() {
  E x = poll();
  if (x != null)
      return x;
  else
      throw new NoSuchElementException();
}
public E poll() {
  final ReentrantLock lock = this.lock;
  lock.lock();
  try {
   return (count == 0) ? null : dequeue();
  } finally {
   lock.unlock();
  }
 }
private E dequeue() {
 final Object[] items = this.items;
 @SuppressWarnings("unchecked")
 E x = (E) items[takeIndex];
 items[takeIndex] = null;
 if (++takeIndex == items.length)
  takeIndex = 0;
 count--;
 if (itrs != null)
  itrs.elementDequeued();
 notFull.signal();
 return x;
}
~~~

LinkedBlockingQueue 的实现的源码，对比ArrayBlockingQueue，实现的框架上面，基本相同。当时具体的数据处理是不一样的。

~~~
 public boolean add(E e) {
  addLast(e);
  return true;
 }

 public void addLast(E e) {
  if (!offerLast(e))
   throw new IllegalStateException("Deque full");
 }

 public boolean offerLast(E e) {
  if (e == null)
   throw new NullPointerException();
  Node<E> node = new Node<E>(e);
  final ReentrantLock lock = this.lock;
  lock.lock();
  try {
   return linkLast(node);
  } finally {
   lock.unlock();
  }
 }

 private boolean linkLast(Node<E> node) {
  // assert lock.isHeldByCurrentThread();
  if (count >= capacity)
   return false;

  Node<E> l = last;
  node.prev = l;
  last = node;
  if (first == null)
   first = node;
  else
   l.next = node;

  ++count;
  notEmpty.signal();
  return true;
 }
~~~















