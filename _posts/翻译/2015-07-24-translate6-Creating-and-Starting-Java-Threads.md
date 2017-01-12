---
layout: post
title: "Concurrence-6-Creating and Starting Java Threads"
description: "翻译 梳理基础的东西"
category: 翻译 java 并发编程 多线程
tags: [翻译 并发编程 多线程]
---
#### Creating and Starting Java Threads   
<br/>
##### 创建和启动java线程    
<br/>

文章的地址：[翻译文章的源地址](http://tutorials.jenkov.com/java-concurrency/creating-and-starting-threads.html)
<br/>
java线程和java的其他的对象一样，也是一个对象。线程是 类 java.lang.Thread的实例或者这个类的子类的实例。除了作为对象外，java线程也可以执行代码。
在java中创建一个线程的代码如下：   
```
  Thread thread = new Thread();
```

启动这个线程的时候，我们可以向下面一样调用它的start()方法：
```
  thread.start();
```

这里例子中线程的执行过程中不会执行任何的代码，线程启动后会马上停止。   
指定线程运行的过程执行的代码有两种的方法，第一种是创建一个Thread的子类，重载run()方法。第二种方法就是传一个实现Runnable接口的对象给Thread构建方法，
这两种方法，下面都有实例说明：    
第一种方式指定一个线程执行的代码，就是创建一个Thread的子类，重载run()方法。当你调用start()方法之后，跟着就是run()方法的运行，代码如下：

```
  public class MyThread extends Thread {
    public void run(){
       System.out.println("MyThread running");
    }
  }
```

对于上面定义的线程，你可以通过如下的代码创建和启动线程。 

  
```
  MyThread myThread = new MyThread();
  myTread.start();
```

start方法启动后，就会直接的返回，不会去等run方法调用完成之后再返回的。run方法就像是在另个的一个不同的CPU上面运行。在此理中，
run方法的运行，会打印出"MyThread running".    

你也可以通过创建匿名类来创建线程，如下所示：

```
  Thread thread = new Thread(){
    public void run(){
      System.out.println("Thread Running");
    }
  }
  thread.start();
```  

这里例子创建了一个新的线程，执行的时候，调用run方法，会打印出： "Thread running"     

Runnable 接口的实现   
第二种方式是指定线程执行代码的方式是：创建一个实现Runnable的接口的类，这个Runnable的对象可以被一个线程执行。
示例代码：  

```
  public class MyRunnable implements Runnable {
    public void run(){
       System.out.println("MyRunnable running");
    }
  }
```

为了能够使用线程执行这个对象，我们把MyRunnable的一个实例传给线程的构造函数，如下代码：   

```
   Thread thread = new Thread(new MyRunnable());
   thread.start();
```

当线程启动后，就会调用构造函数中的MyRunnable实例的run方法，上面的例子中就会打印："MyRunnable running".你也可以向下面一样
采用匿名实现Runnable的方式

```
Runnable myRunnable = new Runnable(){
     public void run(){
        System.out.println("Runnable running");
     }
   }
   Thread thread = new Thread(myRunnable);
   thread.start();
```

那我们是使用子类的方式还是接口实现的方式？
没有哪一个方式是最好的说法，个人认为，我比较喜欢实现接口Runnable的方式，使用一个线程调用实现接口的实例。因为当Runnable实现类被
线程池执行的时候，当线程池中一个线程位空闲的时候，Runnable实现类比较容易排成队（等候执行），这点比线程的子类来的简单。   

有些时候，你可能必须的使用线程的子类。例如，创建一个线程的子类，可以执行不止一个的Runnable实例，典型的场景就是线程池的实现。   

常见的陷阱：调用run方法而不是start方法
当创建和启动一个线程常见的一个错误就是调用run方法而不去调用start方法，例如：   

```
  Thread newThread = new Thread(MyRunnable());
  thread.run();  //should be start();
```

刚开始的时候你可能注意不到什么，因为调用run方法的执行结果和start一样，就如你所预料的。然而它并不是被你创建的线程执行的，它是被创建的线程的实例
执行的，换句话说，上面的thread执行run方法，就是直接的调用新建的MyRunnable()实例的run方法。如果想要线程调用，你必须执行start方法。   

线程名称   
当你常见一个线程的时候，你可以给它起一个名称，这个名称能够使你区分不同的线程。例如，当多个线程像System.out中写数据的时候，我们可以区分到底
是哪一个线程所写的文本，下面就是一个例子：    

```
   Thread thread = new Thread("New Thread") {
      public void run(){
        System.out.println("run by: " + getname());
      }
   };
   thread.start();
   System.out.println(thread.getName());
```

注意到 字符串"New Thread"作为参数被传入了Thread的构造函数，传入的这个字符串就是线程的名称，这个名称可以通过Thread的getName方法获得。
你也可以在实现Runnable接口的方式中，传入线程的名称，代码如下：   

```
   MyRunnable runnable = new MyRunnable();
   Thread thread = new Thread(runnable, "New Thread");
   thread.start();
   System.out.println(thread.getName());
```

一个取得当前执行线程名称的方式是：   

```
   String threadName = Thread.currentThread().getName();
```

java线程的例子：

```
public class ThreadExample {
  public static void main(String[] args){
    System.out.println(Thread.currentThread().getName());
    for(int i=0; i<10; i++){
      new Thread("" + i){//设置线程的名称
        public void run(){
          System.out.println("Thread: " + getName() + " running");
        }
      }.start();
    }
  }
}
```

注意：线程的启动是按照顺序进行启动的，但是她们的执行顺序可能不是顺序的。第一个线程可能并不是第一个执行的，并不是第一个打印
出自己名称的线程，线程的执行是并发的，执行的顺序是有JVM或者操作系统决定的，它们执行的顺序和启动的顺序并不是一致的。
