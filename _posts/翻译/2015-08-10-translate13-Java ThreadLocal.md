---
layout: post
title: "Concurrence-13-Java ThreadLocal"
description: "翻译 梳理基础的东西"
category: 翻译 java 并发编程 多线程
tags: [翻译 并发编程 多线程]
---
#### Java ThreadLocal
<br/>
##### java的线程局部变量
<br/>

文章的地址：[翻译文章的源地址](http://tutorials.jenkov.com/java-concurrency/threadlocal.html)
<br/>

java的类ThreadLocal可以使你创建的变量只会被同一个线程读或者写。因此即使是两个线程执行同一份代码，代码
中有一个指向ThreadLocal变量的引用，那么这两个线程是不能够看见对方的ThreadLocal变量的。     

#### 创建一个ThreadLocal
创建ThreadLocal 变量的代码：   

```
private ThreadLocal myThreadLocal = new ThreadLocal();
```

上例所示，我们创建了一个ThreadLocal对象，每一个线程只需创建一个ThreadLocal变量，即使两个不同的线程执行
相同的代码访问一个ThreadLocal，每一个线程只会看到它自己的ThreadLocal实例，即使两个线程为同一个ThreadLocal
对象设置不同的值，他们是互不可见的。   

####访问ThreadLocal

一个创建好的ThreadLocal对象，你可以这么进行进行设置存储值：   

```
myThreadLocal.set("A thread local value");
```

这样读取设置的值：   

```
String threadLocalValue = (String) myThreadLocal.get();
```

get()方法会返回一个Object类型的对象，而set方法则需要一个Object对象作为参数。  

####ThreadLocal泛型
如果不想在get方法做类型转化，我们可以使用泛型，如下例：   

```
private ThreadLocal myThreadLocal = new ThreadLocal<String>();
``` 

现在myThreadLocal实例可以保存String类型的数据，另外在从ThreadLocal实例中获得值的时候也不需要做类型
转化了：   

```
myThreadLocal.set("Hello ThreadLocal");
String threadLocalValue = myThreadLocal.get();
```

####初始线程局部变量值
因为设置在ThreadLocal对象的值只能够对设置这个值的线程是可见的，使用set方法设置一个初始值对所有的线程
都是可见的，是行不通的。    

但是你可以对一个ThreadL对象设置一个初始值通过创建ThreadLocal对象的子类，重载initialValue方法，如下：    

```
private ThreadLocal myThreadLocal = new ThreadLocal<String>() {
    @Override protected String initialValue() {
        return "This is the initial value";
    }
};  
```    

这样的话，所有的线程都能够看到这个初始化，在没有调用set方法之前，通过get获取到这个值。      

可运行的java ThreadLocal 示例：

```
public class ThreadLocalExample {
    public static class MyRunnable implements Runnable {
        private ThreadLocal<Integer> threadLocal =
               new ThreadLocal<Integer>();
        @Override
        public void run() {
            threadLocal.set( (int) (Math.random() * 100D) );
    
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
            }
    
            System.out.println(threadLocal.get());
        }
    }


    public static void main(String[] args) {
        MyRunnable sharedRunnableInstance = new MyRunnable();

        Thread thread1 = new Thread(sharedRunnableInstance);
        Thread thread2 = new Thread(sharedRunnableInstance);

        thread1.start();
        thread2.start();
    }

}
```  

这个例子创建了一个MyRunnable实例，传递给两个不同的线程，两个线程执行run方法的时候，会向ThreadLocal变量
中设置不同的值，如果访问set方法是同步的，也不是一个ThreadLocal对象，那么第二个线程会覆盖第一个线程设
置的值。   
然而，它是一个ThreadLocal对象，两个线程不能够见到对方的值，因此，它们会获得各自设置的值。     

####InheritableThreadLocal

InheritableThreadLocal 是 ThreadLocal的子类，不同是是ThreadLocal是每一个线程都有自己的副本，InheritableThreadLocal
保证的是不仅创建它的线程能访问到，它的子线程也能够访问到。















