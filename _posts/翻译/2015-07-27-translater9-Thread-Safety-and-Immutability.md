---
layout: post
title: "Concurrence-9-Thread-Safety-and-Immutability"
description: "翻译 梳理基础的东西"
category: 翻译 java 并发编程 多线程
tags: [翻译 并发编程 多线程]
---
#### Thread Safety and Immutability
<br/>
##### 线程安全和不可变性
<br/>

文章的地址：[翻译文章的源地址](http://tutorials.jenkov.com/java-concurrency/thread-safety-and-immutability.html)
<br/>
竞争条件只有在多个线程访问相同的资源，以及一个或多个线程写入资源时才会出现。
如果多个线程读取相同的资源竞争条件是不发生。我们可以确保线程之间共享对象
不更新以便使共享对象不变，从而线程安全的。下面是一个示例：    

```
public class ImmutableValue{
  private int value = 0;
  public ImmutableValue(int value){
    this.value = value;
  }
  public int getValue(){
    return this.value;
  }
}
```

备注：ImmutableValue实例的值传递给构造函数。注意到没有setter方法设置这个值。 也就是说
ImmutableValue实例创建后不能更改其值。它是不可变的。 然而你可以使用getValue()方法获取它的值。  

如果你需要操作ImmutableValue实例的值，你可以在操作运算后返回一个新的实例，下面就是add的方法：   
  
 
```
 public ImmutableValue add(int valueToAdd){
      return new ImmutableValue(this.value + valueToAdd);
      }
```

备注：add方法直接返回了一个实例，并且构造函数中接受的是加操作之后的值，而不是返回value所在的实例。   

引用不是线程安全的  
如果对象是不变的，那么它是线程安全的，但是对于它的引用可能不是线程安全的，见下例：   
   
```
public class Calculator{
  private ImmutableValue currentValue = null;
  public ImmutableValue getValue(){
    return currentValue;
  }
  public void setValue(ImmutableValue newValue){
    this.currentValue = newValue;
  }
  public void add(int newValue){
    this.currentValue = this.currentValue.add(newValue);
  }
}
```  

类Calculator 包含一个对ImmutableValue实例的引用，即使是使用了内部不可变的对象，现在也通过setValue方法和add方法你能够改变引用值， 
所以这个引用本身不是不可变的，所以它不是线程安全的。换句话说，就是不可变的类是线程安全的，但是对他的引用不是的。在通过不变形保证
线程安全时，需要记住这一点。

如果想要Calculator这个类线程安全，你可以声明getValue(), setValue(), and add()是同步方法。
