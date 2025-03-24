---
layout: post
title: "并发知识梳理：1. synchronized，volatile"
subtitle: "JVM支持的关键字"
author: "zhailzh"  
header-img: "img/post-bg-2015.jpg"  
catalog: true
tags: Concurrent  
---

synchronized 关键字作为并发理念中，最最基础的部分，需要首先搞明白。

Volatile 关键字，又做了哪些工作和Synchronized 有什么不同？

<!--more-->

首先还是实例代码：

~~~
class Producer extends Thread {
	static final int MAXQUEUE = 5;
	private Vector<String> messages = new Vector<String>();

	@Override
	public void run() {
		try {
			while (true) {
				putMessage();
			}
		} catch (InterruptedException e) {
		}
	}

	private synchronized void putMessage() throws InterruptedException {
		while (messages.size() == MAXQUEUE) {
			wait();
		}
		messages.addElement(new java.util.Date().toString());
		System.out.println("put message");
		notify();
		//Later, when the necessary event happens, the thread that is running it calls notify() from a block synchronized on the same object.
	}

	// Called by Consumer
	public synchronized String getMessage() throws InterruptedException {
		notify();
		while (messages.size() == 0) {
			wait();//By executing wait() from a synchronized block, a thread gives up its hold on the lock and goes to sleep.
		}
		String message = (String) messages.firstElement();
		messages.removeElement(message);
		return message;
	}
}

class Consumer extends Thread {
	Producer producer;

	Consumer(Producer p) {
		producer = p;
	}
	@Override
	public void run() {
		try {
			while (true) {
				String message = producer.getMessage();
				System.out.println("Got message: " + message);
				//sleep(200);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static void main(String args[]) {
		Producer producer = new Producer();
		producer.start();
		new Consumer(producer).start();
	}
}
~~~

Java虚拟机的运行时数据区中的堆和方法区是所有线程共享的区域，这个是加锁机制的基础：数据存储在一个公用的地方，这是为什么加锁的原因，也是可以加锁的基础。

Java虚拟机将锁与每个对象或类关联起来。锁就像一种特权，在任何时候只有一个线程可以“拥有”它。如果一个线程想要锁定一个特定的对象或类，它会请求JVM，在线程向JVM请求锁之后(如果锁未被持有可能很快，如果锁被持有也可能稍后，也可能永远不会)，JVM将锁提供给线程。当线程不再需要锁时，它将锁返回给JVM。

这个时候，就会有一个疑问，锁到底是什么？

对象锁：也就是类实例对象的锁。类锁实际上是作为对象锁实现的。当JVM加载类文件时，它会创建类java.lang.Class的实例。当锁定一个类时，实际上锁定了那个类的类对象。

在HotSpot虚拟机中，Java对象在内存中存储的布局分为3块区域：对象头、实例数据和对齐填充。对象头包含两部分，第一部分包含对象的HashCode、分代年龄、锁标志位、线程持有的锁、偏向线程ID等数据，这部分数据的长度在32位和64位虚拟机中分别为32bit和64bit，官方称为Mark World。考虑到虚拟机的空间效率，Mark World内部的数据结构是非固定的，也就是说对象头中存储的内容是不固定的，下图展示了不同状态下，对象头中存储的内容： 

![对象头存储的内容](https://7cmppq.ch.files.1drv.com/y4mH5bJLLrgVmBTfUJ_z3-ynrSM39knd9mcDHzpSE4DTGZIJdjeh48UFstIs1JD0Qat9LMxaRStoRoKtVMSw-qLqL0a-Dr4RLsjImgiP76b5so_lNdw-mpZLMR-3yR2iFfSDopIKyO_yyV5msy15_qstR5RGrln5VY-oyBEY1sQWfxMqChGvLaJM4amyaDlu_9PElfbphfFStlPpLQylV-LcQ?width=1376&height=372&cropmode=none)

当使用synchronized修饰方法或修饰语句块时(即获取对象锁或类锁时)，对象(类实例对象或类的类对象)的对象头中锁状态处于重量级锁，此时锁标志位为10，其余30bit用于存储指向互斥量(重量级锁)的指针，这里的指针，笔者理解为monitor对象的地址。


Java虚拟机中，synchronized支持的同步方法和同步语句都是使用monitor来实现的。**每个对象都与一个monitor相关联，当一个线程执行到一个monitor监视下的代码块中的第一个指令时，该线程必须在引用的对象上获得一个锁，这个锁是monitor实现的**。

在HotSpot虚拟机中，monitor是由ObjectMonitor实现，使用C++编写实现，具体代码在HotSpot虚拟机源码ObjectMonitor.hpp文件中。

~~~
ObjectMonitor() {
    _header       = NULL;
    _count        = 0;    // 记录该线程获取锁的次数
    _waiters      = 0,
    _recursions   = 0;    // 锁的重入次数
    _object       = NULL;
    _owner        = NULL; // 指向持有ObjectMonitor对象的线程
    _WaitSet      = NULL; // 处于wait状态的线程集合
    _WaitSetLock  = 0 ;
    _Responsible  = NULL ;
    _succ         = NULL ;
    _cxq          = NULL ;
    FreeNext      = NULL ;
    _EntryList    = NULL ; // 处于等待锁block状态的线程队列
    _SpinFreq     = 0 ;
    _SpinClock    = 0 ;
    OwnerIsThread = 0 ;
  }
~~~

当并发线程执行synchronized修饰的方法或语句块时，先进入_EntryList中，当某个线程获取到对象的monitor后，把monitor对象中的_owner变量设置为当前线程，同时monitor对象中的计数器_count加1，当前线程获取同步锁成功。

当synchronized修饰的方法或语句块中的线程调用wait()方法时，**当前线程将释放持有的monitor对象，monitor对象中的_owner变量赋值为null，同时，monitor对象中的_count值减1，然后当前线程进入_WaitSet集合中等待被唤醒。**


一个线程可以多次锁定同一个对象。对于每个对象，JVM维护对象被锁定的次数的计数。未加锁的对象的计数为零。当线程第一次获得锁时，计数将增加到1。每次线程获取同一个对象上的锁时，都会增加一个计数。每次线程释放锁时，计数将被递减。当计数达到0时，锁被释放，此时其它线程可以继续请求获取锁。

----

锁提供了两种主要特性：互斥（mutual exclusion） 和可见性（visibility）。互斥即一次只允许一个线程持有某个特定的锁，因此可使用该特性实现对共享数据的协调访问协议，这样，一次就只有一个线程能够使用该共享数据。

可见性要更加复杂一些，它必须确保释放锁之前对共享数据做出的更改对于随后获得该锁的另一个线程是可见的 —— 如果没有同步机制提供的这种可见性保证，线程看到的共享变量可能是修改前的值或不一致的值，这将会引发许多严重问题。

Volatile 变量具有 synchronized 的可见性特性，但是不具备原子特性。这就是说线程能够自动发现 volatile 变量的最新值。

一旦一个共享变量（类的成员变量、 类的静态成员变量） 被 volatile 修饰之后， 那么就具备了两层语义：
1. 保证了不同线程对这个变量进行读取时的可见性，即一个线程修改了某个变量的值，这新值对其他线程来说是立即可见的。 (volatile 解决了线程间共享变量的可见性问题)。  
2.  禁止进行指令重排序， 阻止编译器对代码的优化。

### 内存可见性

要说内存的可见性，就得知道JMM（JVM的内存模型）和硬件内存架构大致的样子。

![JVM的内存模型](https://7cocvg.ch.files.1drv.com/y4mYFPEo_uZaGWDzyWpvKyK1cUSkgtVrZucLl-Av_44-1w-HpwewW0YmxQvi689NZHYqZAe1lAj-Knv5RFl_v2kDpH9aKskfWJ8jxN74aAWOqbveBiKXkaAMklt9OQ2M_lt43LGbiMzOLbPcf2DJqae2Uj1iBE8AcLFCf5icVBuOCuB1QWTTRPkhBcZ912vHYb3gX9bYgMpnVztL1ruKbf-Ig?width=920&height=766&cropmode=none)

![内存模型](https://7cmdmw.ch.files.1drv.com/y4mHUqHQZ0hbKJDhqqpbWPeq7tSuN0mvwCEUDESuo4xDk0KAjSTvKMGDOD0kFoA-woavXsOasaKMwk3kW0OddjwYblSZ_YX8SK-CDX55xtdSOFluvjiJ8f-8ASPpc2y2nEDF2uvyxd1djcuYom2p5DUknCBUkrzNCS19Hi10dA68F2h9ETe1hCoE41Jw721mg44qvPdRfyrtxx5G41AmNzolQ?width=443&height=382&cropmode=none)

![映射](https://sjmzpw.ch.files.1drv.com/y4mBwlQrTVdfTSw8wqIo5KR9F9k33dsYh6vg2S3PbSHDmgx3MUCaD9CiaUmgfdUbd3UHO_s-6aDbiBSYTSwVK0jUQFrTNJcGZeDdnpGKO6qGAOZ0ORjmKT0LgF-SZ3siEr35GpyT0-zmAuL5F2E_XObf9618_o5_sEmv4rS89Mo8ZwMt6rzrxKG-qYmUFhCJzHaw7APSn_zqZF7iZRHVdUOig?width=1330&height=614&cropmode=none)

从抽象的角度来看，JMM定义了线程和主内存之间的抽象关系：

线程之间的共享变量存储在主内存（Main Memory）中
每个线程都有一个私有的本地内存（Local Memory），本地内存是JMM的一个抽象概念，并不真实存在，它涵盖了缓存、写缓冲区、寄存器以及其他的硬件和编译器优化。本地内存中存储了该线程以读/写共享变量的拷贝副本。

从更低的层次来说，主内存就是硬件的内存，而为了获取更好的运行速度，虚拟机及硬件系统可能会让工作内存优先存储于寄存器和高速缓存中。

Java内存模型中的线程的工作内存（working memory）是cpu的寄存器和高速缓存的抽象描述。而JVM的静态内存储模型（JVM内存模型）只是一种对内存的物理划分而已，它只局限在内存，而且只局限在JVM的内存。

说完了内存的模型以后，就必须的说一个协议：
![缓存一致性协议](https://sjo0gw.ch.files.1drv.com/y4muaDk9g7Gq1j1knYF4XPVM4hj37ZuZUikoecn7FIVgp6Uf8XTamzDK0961iu_1croUQaiikNAZHhzvcerDo-MfvVoCPObZyi5tJM0d6NX6tWq0xeYUnpNhLeTmH1QFIndiX7t5h7irWi2kEKbUYM0KY7YPoo2qSFHbCYwnhZAZYroabYzaaKGobjpRXhS52b6W8CvPT1JhsDk6XYjDwpgCQ?width=1412&height=976&cropmode=none)

说完了这么多的以后，Volatile是如何实现的内存可见性？
深入来说:通过加入内存屏障和禁止重排序优化来实现的

1. 对volatile变量执行写操作时,会在**写操作后加入一条store屏障指令**
   
2. 对volatile变量执行读操作时,会在**读操作前加入一条load屏障指令**

通俗地讲:volatile变量在每次被线程访问时,都强迫从主内存中重读该变量的值,而当该变量发生变化时,又会强迫将最新的值刷新到主内存,这样任何时刻,不同的线程总能看到该变量的最新值

volatile不能保证volatile变量复合操作的原子性,解决方案如下:
1. 使用synchronized关键字
2. 使用ReentrantLock关键字(java.util.concurrent.locks包下)
3. 使用AtomicInterger(java.util.concurrent.atomic包下)

但是Volatile的主要用法也不是符合操作，而是：

模式 #1：状态标志
模式 #2：一次性安全发布（one-time safe publication）
模式 #3：独立观察（independent observation）
模式 #4：“volatile bean” 模式
模式 #5：开销较低的读－写锁策略（存疑）



