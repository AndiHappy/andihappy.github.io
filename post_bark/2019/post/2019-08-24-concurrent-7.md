---
layout: post
title: "并发知识梳理：5.Atomic 原子类，LockSupport支持类"
subtitle: "并发支持类"
author: "zhailzh"  
header-img: "img/post-bg-2015.jpg"  
catalog: true
tags: Concurrent  
---

在并发的场景下，除了常常使用的锁之类外，还有一些并发底层的支持类，例如：Atomic原子类和LockSupport，还有一个就是时常能见到，但是总感觉名字怪怪的：Unsafe等。

<!--more-->

首先我们来看Atomic原子类，自己把它分为三类的：
1. 基础类型：AtomicBoolean，AtomicInteger，AtomicLong，AtomicReference之类的。
2. 数组类型：AtomicIntegerArray，AtomicLongArray，AtomicReferenceArray之类的。
3. 成员变成之类的：AtomicIntegerFiledUpdate等 
  
我们先从基础类说明：AtomicBoolean有三个主要的成员变量：
~~~
private static final Unsafe unsafe = Unsafe.getUnsafe();
private static final long valueOffset;

static {
      try {
         valueOffset = unsafe.objectFieldOffset
            (AtomicBoolean.class.getDeclaredField("value"));
      } catch (Exception ex) { throw new Error(ex); }
  }

private volatile int value;

~~~

主要的方法：
~~~
public final boolean get() {
     return value != 0;
}

public final boolean compareAndSet(boolean expect, boolean update) {
    int e = expect ? 1 : 0;
    int u = update ? 1 : 0;
    return unsafe.compareAndSwapInt(this, valueOffset, e, u);
}
~~~

实现的原理就是，通过Unsafe，拿到Volatile修饰的成员变量value的内存地址，然后通过Unsafe的
compareAndSet方法来设置value的值。

其他的基础类，基本就是这种模式的实现。

AtomicIntegerArray 的实现的源码，也是操作内存地址，但是因为是数组，所以有了些许的变化：

~~~
AtomicIntegerArray：

private static final int base = unsafe.arrayBaseOffset(int[].class);
 private static final int shift;
 private final int[] array;

 static {
  int scale = unsafe.arrayIndexScale(int[].class);
  if ((scale & (scale - 1)) != 0)
   throw new Error("data type scale not a power of two");
  shift = 31 - Integer.numberOfLeadingZeros(scale);
 }

 private long checkedByteOffset(int i) {
  if (i < 0 || i >= array.length)
   throw new IndexOutOfBoundsException("index " + i);

  return byteOffset(i);
 }

 private static long byteOffset(int i) {
  return ((long) i << shift) + base;
 }
~~~

我们看一个具体的实现：

~~~
public final boolean compareAndSet(int i, int expect, int update) {
  return compareAndSetRaw(checkedByteOffset(i), expect, update);
 }

 private boolean compareAndSetRaw(long offset, int expect, int update) {
  return unsafe.compareAndSwapInt(array, offset, expect, update);
 }
~~~

checkedByteOffset 就是根据i，确实能够```array[i]``` 对应的内存地址： ```((long) i << shift) + base```
其中shift 为 ``` 31 - Integer.numberOfLeadingZeros(unsafe.arrayIndexScale(int[].class)) ```
就看计算公式，就让我们想起来数组的基础地址 + i* 每一个元素所占的步长的算法。

这也是其他的数组类原子类的基本操作了。

