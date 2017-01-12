---
layout: post
title: "Concurrence-8-Thread Safety and Shared Resources"
description: "翻译 梳理基础的东西"
category: 翻译 java 并发编程 多线程
tags: [翻译 并发编程 多线程]
---
#### Thread Safety and Shared Resources
<br/>
##### 线程安全和资源共享
<br/>

文章的地址：[翻译文章的源地址](http://tutorials.jenkov.com/java-concurrency/thread-safety.html)
<br/>

   能够安全的被多线程同时调用的代码就可以称之为线程安全，如果一段代码是线程安全的，那么其中必然没有竞争条件。竞争
条件发生在多个线程更新同一个共享资源的时候，因此知道多线程执行的时候共享了那些资源就是非常重要的啦。

   局部变量
   局部变量被存储在每一个线程自己的栈里面，这就意味着局部变量是绝不可能在不同的线程之间共享的。这也就意味着所有的
局部基本的变量都是线程安全的，下面是一个线程安全的局部基本变量的例子：

```
public void someMethod(){
  long threadSafeInt = 0;
  threadSafeInt++;
}
```

  局部变量的引用
  针对局部引用又有不同，引用本身是不能够被共享的，但是被引用的对象并没有被存储在各个线程的栈里面。所有的对象都被存储在共享的堆里面。
如果在本地创建的对象并且没有逃脱（escape）出创建的方法，它就是线程安全的。事实上，你也可以将真个对象传递给其他方法或者对象，只要这些方法或对象
，没有把这个对象可传递给其他线程，这个就是线程安全的。局部对象的一个线程安全的例子：

```
public void someMethod(){
  LocalObject localObject = new LocalObject();
  localObject.callMethod();
  method2(localObject);
}
public void method2(LocalObject localObject){
  localObject.setValue("value");
}
```

这个例子中的LocalObject实例，没有被创建的方法返回，也没有被传递到能够在someMethod方法之外能够访问的其他的对象
内。每个线程执行someMethod方法将创建其自己的LocalObject实例并将它分配给localObject引用。因此这里使用LocalObject是线程安全的。
事实上，整个方法都是线程安全的。即使LocalObject实例作为参数传递给同一个类的其他方法，或其他类，使用它是线程安全的。当然，
唯一的例外是，LocalObject作为一个参数被其他的方法调用，并且LocalObject存储的实例能够在其他的线程访问到。

对象成员
对象成员存储堆中，位于对象附近。因此如果两个线程调用同一个实例的同一个方法，这个方法更新对象的成员，那么这个方法就是线程不安全的，
下面为对象成员的线程不安全事例：  

```
public class NotThreadSafe{
    StringBuilder builder = new StringBuilder();
    public add(String text){
        this.builder.append(text);
    }
}
```

如果两个线程同时访问同一个NotThreadSafe 实例的add() 方法，那么将会导致竞争条件的出现，例如：

```
NotThreadSafe sharedInstance = new NotThreadSafe();
new Thread(new MyRunnable(sharedInstance)).start();
new Thread(new MyRunnable(sharedInstance)).start();
public class MyRunnable implements Runnable{
  NotThreadSafe instance = null;
  public MyRunnable(NotThreadSafe instance){
    this.instance = instance;
  }
  public void run(){
    this.instance.add("some text");
  }
}
```

注意到一点，两个MyRunnable实例共享的是同一个NotThreadSafe实例，因此当它们调用NotThreadSafe实例的add方法的时候，将会
导致竞争条件。

然而，如果两个线程同时调用add方法是基于不同的NotThreadSafe实例，那么就不会导致竞争条件，
下面是对上面示例的小小修改：    

```
new Thread(new MyRunnable(new NotThreadSafe())).start();
new Thread(new MyRunnable(new NotThreadSafe())).start();
```

现在两个线程每一个都拥有一个NotThreadSafe实例，所以他们同时调用add方法就不会相互影响，这段代码也就不会出现竞争条件。
所以，即使一个对象不是线程安全的，它也能够以一种没有竞争条件的方式使用。

线程控制逃脱规则
当需要确定你的代码访问的某一个资源是不是线程安全的时候，你需要使用线程控制逃脱规则    

	If a resource is created, used and disposed within the control of the same thread, 
	and never escapes the control of this thread, the use of that resource is thread safe.

共享资源包括对象，数组，文件，数据库连接，scoket连接等等，在java中，显式的释放对象并不常见，所以释放对象就可以
理解为丢失或者把对象的引用置null。

即使调用的对象是线程安全的，如果那个对象。指向一个共享的资源，比如：文件，数据库，你的应用从整体上
说可能不是线程安全的。例如，如果线程1和线程2每个都创建他们自己的数据库连接，连接1和连接2，连接本身的
使用时线程安全的。但是使用数据库连接可能导致线程的不安全，例如，如果两个线程执行的代码如下：     
 
```
check if record X exists
if not, insert record X
```
如果两个线程同时执行上面的伪代码，被检查的记录X恰好是同一个记录，那么就有可能两个线程全部的都插入了记录X
说明如下：   

```
Thread 1 checks if record X exists. Result = no
Thread 2 checks if record X exists. Result = no
Thread 1 inserts record X
Thread 2 inserts record X
```

这可能也发生在文件操作或其他共享资源的线程。因此，对于线程控制逃脱规则来说，区分由一个线程控制对象是资源，还是仅仅就是一个引用
是比较重要的。
