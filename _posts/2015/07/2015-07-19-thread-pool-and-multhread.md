---
layout: post
title: "多线程和线程池"
description: ""
category: 多线程
tags: [多线程 基础务实 问题记录]
---
### 多线程[1]
1.在main方法中启动的线程 和 Juite启动的线程表现的不一样，具体的代码演示就是：

```
public class Test {
  public static void main(String[] args) {
    new Thread(new Runnable() {
      @Override
      public void run() {
        System.out.println("主线程在pao。。。。。。。。");
        for (int i = 0; i < 10; i++) {
          new Thread(new TestR()).start();
        }
        System.out.println("主线程结束");
      }
    }).start();
  }
}

class TestR implements Runnable {
  public void run() {
    System.out.println(Thread.currentThread().getName() + "线程被调用了。");
    while (true) {
      try {
        Thread.sleep(5000);
        System.out.println(Thread.currentThread().getName());
      }
      catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }
}
```
结果是：主线程结束了以后，子线程还在跑
但是在：juit里面，代码是：  

```
public class TestKK {

  @Test
  public void main() {
    new Thread(new Runnable() {
      @Override
      public void run() {
        System.out.println("主线程在pao。。。。。。。。");
        for (int i = 0; i < 10; i++) {
          new Thread(new TestR()).start();
        }
        System.out.println("主线程结束");
      }
    }).start();
  }
}

class TestR implements Runnable {
  public void run() {
    System.out.println(Thread.currentThread().getName() + "线程被调用了。");
    while (true) {
      try {
        Thread.sleep(5000);
        System.out.println(Thread.currentThread().getName());
      }
      catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }
}
```
在Juit的框架下，main方法对应的线程执行完毕后，main方法所启动的那10个线程，立马就会被“结束”，很像是被
强行结束的。为什么会这样？   

原因就是：JUIT的框架的原因了：因为每一个java类在执行的过程中都会寻找main方法，JUIT也一样，我们可以看一下JUIT调用的源代码：   

```
public static void main(String args[]) {
		TestRunner aTestRunner= new TestRunner();
		try {
			TestResult r= aTestRunner.start(args);
			if (!r.wasSuccessful()) 
				System.exit(FAILURE_EXIT);
			System.exit(SUCCESS_EXIT);
		} catch(Exception e) {
			System.err.println(e.getMessage());
			System.exit(EXCEPTION_EXIT);
		}
	}
```
我们可以看到：JUIT框架的@Test标注的方法，在最后调用了System.exit() 方法，这个即使还有其他的线程在运行，
main也会调用System.exit(0);System.exit()是系统调用，通知系统立即结束jvm的运行，
即使jvm中有线程在运行，jvm也会停止的。所以会出现之前的那种情况。
其中System.exit(0);的参数如果是0，表示系统正常退出，如果是非0，表示系统异常退出。所以在Juit中调用线程，如果想要其中
的线程完整的执行完毕，那么在JUIT的调用的方法中必须的等待，我们可以在Testkk的测试代码中加入：     

```
 Thread.sleep(100000);
```



