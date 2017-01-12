---
layout: post
title: "Concurrence-10-Java Memory Model"
description: "翻译 梳理基础的东西"
category: 翻译 java 并发编程 多线程
tags: [翻译 并发编程 多线程]
---
#### Java Memory Model
<br/>
##### Java 内存模型
<br/>

文章的地址：[翻译文章的源地址](http://tutorials.jenkov.com/java-concurrency/java-memory-model.html)
<br/>

Java内存模型说明了Java虚拟机是如何使用计算机内存(RAM)的。Java虚拟机是对整个电脑的模型化，
该模型自然的就包括内存模型-就是Java的内存模型。   

如果想要设计出来运行良好的多线程程序，理解java内存模型就是非常必要的。java内存模型说明了多个
线程是如何得到被其他线程写入的共享值，当必要的时候又是如何同步协作的访问共享变量。   

最初的Java内存模型是不完善的，所以Java 1.5中的Java内存模型进行了修订。此版本的Java内存模型
现在仍在使用Java 8中。    

java内存模型的内在
在JVM中，java内存模型内在的被分为了线程栈和内存堆，下面这张图可以从逻辑的角度说明java内存模型：

![](http://tutorials.jenkov.com/images/java-concurrency/java-memory-model-1.png)    

在JVM中运行的每一个线程都有自己的线程栈，线程栈中包含到达当前执行的代码线程必须调用的方法，我常
把这个称之为调用栈，随着线程执行代码，调用栈随之发生变化。    

线程栈中也会包含所有被执行方法（调用栈中的所有的方法）的局部变量，一个线程只能够访问它自己的线程
栈，局部变量只对创建它的那个线程可见，对其他的线程是不可见的。即使是两个执行同样的代码，这两个线程
针对代码中的局部变量也会在各自的线程栈里面创建各自对应的局部变量，因此，每一个线程对自己的局部变量
都有自己的版本。   

所有的基本类型（boolean, byte, short, char, int, long, float, double）的局部的变量都是
在线程栈里面存储的，因此对应其他的线程全部是不可见的。一个线程可能会传递一个基本类型的副本到
另外的一个线程，但是它不能够共享这个变量本身。   

内存堆存储了你的java应用中创建的所有对象，不管是线程创建的对象还是，还是基本类型对象的对象（例
如：Byte, Integer, Long 等等）。无论这个对象是创建后被赋值给一个局部的变量，还是作为另外一个对象
的局部的成员，这个对象都会被存储在堆中。

下面的这张图就表示 线程栈中的调用栈和局部变量以及堆中对象的存储方式：
![](http://tutorials.jenkov.com/images/java-concurrency/java-memory-model-2.png)   

一个局部变量可能是基本类型，这个时候它只能被保存在线程栈中。也有可能是对对象的一个引用，
在这种情况下，这个局部变量对象的引用仍然会被存储在线程栈中，但是引用对应的对象本身会保存
在堆中。  

一个对象可能包含方法，这些方法也可能包含局部的变量。这些局部的变量就保存在线程栈中，即使这个
这个方法归属的对象保存在堆中。   

一个对象的成员变量可能就存储在位于这个对象附件位置的堆中，无论这个成员变量是基本的类型或者是
对另外的一个对象的引用。   

静态类变量也会被存储在堆中，挨着类的定义的位置。  

堆中的对象，所有的线程通过一个指向该对象的引用都能够访问到。当一个线程访问某一个对象的时候，它
也能够访问这个对象的成员变量，如果两个线程同时访问同一个对象的方法的时候，那么这两个对象都能够
访问到这个对象的成员变量，但是每一个线程都有它们自己的拷本的副本的成员变量。  

下图可以说明这个问题：  

![](http://tutorials.jenkov.com/images/java-concurrency/java-memory-model-3.png)   

两个线程都有很多个局部变量，其中的一个局部变量（Local Variable 2）指向一个堆中的共享对象
（Object 3）两个线程每一个都拥有一个针对同一的对象的引用，他们的引用本身是一个成员变量所以
它们都被保存在线程栈中，尽管两个不同的引用指向的是同一个对象。  

备注：共享的变量3 还拥有对变量2和变量4的引用作为成员变量（图通通过箭头来进行表示），通过变量
3中成员变量的引用，两个线程就可以对变量2和变量4进行访问。   

这张图也表现出一个指向堆中两个不同对象的一个局部变量。在这个例子中，一个引用指向了两个不同的对
像，对象1和对象5，理论上，如果两个线程都含有对两个对象的引用，那么两个线程都能够访问对象1和对象5
但是在上图中，每一个线程只拥有针对其中一个对象的引用。  

然而，上图中对应的java代码是怎么样的呢？如下所示：     


```
public class MyRunnable implements Runnable() {
    public void run() {
        methodOne();
    }
    public void methodOne() {
        int localVariable1 = 45;
        MySharedObject localVariable2 =
            MySharedObject.sharedInstance;
        //... do more with local variables.
        methodTwo();
    }
    public void methodTwo() {
        Integer localVariable1 = new Integer(99);
        //... do more with local variable.
    }
}
```


```
public class MySharedObject {
    //static variable pointing to instance of MySharedObject
    public static final MySharedObject sharedInstance =
        new MySharedObject();
    //member variables pointing to two objects on the heap
    public Integer object2 = new Integer(22);
    public Integer object4 = new Integer(44);
    public long member1 = 12345;
    public long member1 = 67890;
}
```

如果两个线程都执行过run方法，那么上图的情景就会出现了，run方法调用了methodOne（）
methodOne方法调用了methodTwo方法。   

methodOne 声明了一个局部基础变量 int类型的localVariable1 ，还有局部变量localVariable2
是对一个对象的引用。     

每一个线程执行方法methodOne的时候，会在自己的线程栈里面复制localVariable1和localVariable2
的一个副本,其中，localVariable1会全部的隔离开，完全的都在各自的线程栈中保存，一个线程完全对
另外的一个线程中的变量localVariable1是不可见的。   

每一个线程在执行methodOne的时候，也会在自己的线程栈中，拷本一个localVariable2的副本，不过  
两个不同的副本都会指向堆中的同一个对象，代码中显示localVariable2指向了一个静态的变量，静态变量
只存储一份，这份会保存在堆中，因此两个关于localVariable2的副本指向同一个静态的MySharedObject
实例。MyShareObject实例也保存在堆中，它就是上图中的对象3。   

备注：类MyShareObject还包含了两个成员变量，这两个成员变量的本省保存在堆中，位于对象的附近。这两个
成员变量指向的是两个不同的Integer对象，在上图中是使用对象2和对象4来进行表示。   

备注：methodTwo方法创建了一个局部的变量localVariable1，这个局部变量是对一个Integer对象的引用。
这个方法，设置了localVariable1引用指向了一个新的Integer实例。两个Integer实例化的对象会被保存在
堆中，但是因为每次方法执行的时候，都会创建一个新的Integer的对象，两个线程指向这个方法的时候会分别
的创建Integer实例。在methodTwo方法内部创建的Integer对象对应上图中的对象1和对象5.  

备注：类MyShareObject中含有两个long类型的成员变量，属于基本类型，但是因为它们是成员变量，所以他们
会保存在堆中，靠近对象的位置。只有局部的变量才会被保存在线程栈中。

####硬件内存体系结构
现在的硬件内存体系结构是和java内存模型有些差别的，理解硬件内存体系结构也是比较重要的，这样能够
更好的理解java内存模型在其上是怎么工作的。这个部分描述常见的硬件内存模型，下个部分将会描述java
内存模型是如何在其上工作的。  

简单描述硬件内存模型的一张图：   
![](http://tutorials.jenkov.com/images/java-concurrency/java-memory-model-4.png)  

现在的一个计算机一般拥有2个或者2个以上的CPU，也就是说CPU可能会有多核，这就意味着在一个拥有2个
或者以上的CPU可能会用1个以上的线程同时在运行。每个CPU能够在任何特定时间运行一个线程。这意味着，
如果您的Java应用程序是多线程的，那么在您的Java应用程序内，一个线程可能并发或者并行运行
在每个CPU上。  

每个CPU都包含一组注册器，这些注册器其实就是CPU内存。CPU可以在寄存器上，比在内存中更快执行操作
。这是因为CPU访问寄存器的速度远远的超过访问内存的速度。    

每一个CPU可能会有一个CPU缓存层，事实上，大部分现代的CPU都会有一个不同大小的CPU缓存层，CPU
访问缓存层的速度要远超过访问内存的速度，但是常常没有访问他们的内存寄存器快，所以CPU缓存层的
访问速度就介于寄存器和内存之间，所以某些的CPU可以会有多个缓存层，例如缓存1层，缓存2层，但是
这个和理解java内存模型是如何和内存进行交互的关系不是很大，重要的是了解到CPU中会含有一个缓存层。   


计算机中一般还会包含一个主要内存区域（RAM），所有的CPU都能够访问这个内存区域，内存区域也要比
CPU的缓存大上很多很多。    

通常，当一个CPU需要访问主内存，它就会把这部分的的内存读入CPU缓存，甚至会把缓存的一部分的读入CPU
的内存寄存器，然后执行操作。当CPU的需要把结果写回内存的时候，CPU会先把CPU寄存器的值写回cpu的缓存
，并在某一个时刻写回内存。    

当CPU需要在CPU缓存中存储其他数据的时候，通常就把缓存中其他的数据刷回内存。cpu缓存可以一次将一块的
数据写入内存，这样CPU就没有必要每次缓存更新的时候就重新的读取或者写入一次。通常缓存是以一个小块的
模式更新的，这个小块就称之为告诉缓存块，一个或者多个缓存块会被从内存读入缓存区域，也可能一个或者多
个缓存块从缓存区域写入内存中。  

####java内存模型和硬件内存模型之间的联系

如前所述，Java内存模型和硬件内存模型是不同的。硬件内存架构并不区分线程堆栈和堆。
在硬件内存模型中、线程堆栈和堆都是位于主内存中。线程栈和堆的部分有时会出现在CPU缓存和
内部CPU寄存器。这是此图中所示：     

![](http://tutorials.jenkov.com/images/java-concurrency/java-memory-model-5.png)  

当对象和变量存储在计算机中的各种不同的内存区域时，可能会出现一些问题。比如，两个主要的问题是：   

1.对共享变量的线程更新的可见性   
2.当读取，检查，写入共享变量时出现的竞争条件  
下面我们就主要说明这两个问题。  

#### 共享变量的可见性
如果一个共享变量被两个或者多个线程使用，没有volatile声明和synchronization 恰当的声明和使用，
一个线程的对共享变量的更新可能对其他线程就是不可见的。

设想一开始的时候，共享的变量存储在内存中，一个在CPU上跑着的线程把这个共享变量读入了CPU缓存，然后
线程更新了这个共享变量，只要CPU缓存没有把这个共享变量写回内存，那么这个共享变量的变动对运行在其他
CPU上的线程来说就是不可见的，这种情况下每一个线程都会在CPU缓存中有一个与其他线程不同的副本。    

下图就说明了这种情况，一个线程运行在左边的那个CPU上，共享变量已经被读入CPU缓存中，然后把它的值变
为了2，这个变化对运行在右边的那个线程是不可见的，因为共享变量count值还没有被刷新回内存中。  
   
![](http://tutorials.jenkov.com/images/java-concurrency/java-memory-model-6.png)    

为了解决这个问题，我们可以使用java的 volatile 关键字，volatile关键字可以保证一个变量直接从内存
读取，并且当更新的时候直接写回内存中。   

####竞争条件

如果一个共享变量被两个或者多个线程使用，多个线程更新这个共享变量的时候，就有可能发生竞争条件。  

设想一下，如果线程A读取了count变量进入了CPU缓存中，再假设线程B也同样这样操作，但是在不同的cpu
缓存中，现在线程A把count变量加一，线程B也加了一，现在对于var1来说就被加了两次，每次都在不同的
cpu缓存中，如果是正确的逻辑的话，变量count应该增加2，原始的值应该被加了2以后的值写回内存。  

但是，在没有合适的同步机制的情况下，发生了两次自增运算，忽略掉究竟是线程A还是线程B最后把自己的
count更新的版本，最终内存更新的值只会比原来的值大1，而不是2.   
下图描述了上面竞争条件：  
 
![](http://tutorials.jenkov.com/images/java-concurrency/java-memory-model-7.png)   

为了解决这个问题，我们可以使用 synchronized 代码块，一个 synchronized代码块能够保证在任何时候
只有一个线程能够进入临界区中。synchronized代码块还保证代码块中能够访问的所有的变量都会从内存中读
取，当线程离开代码块的时候，所有的更新的变量全部的写回内存中，不管变量是不是被声明为volatile没有。























