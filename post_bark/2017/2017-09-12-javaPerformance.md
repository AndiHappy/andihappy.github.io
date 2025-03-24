---    
layout: post  
title: "JAVA性能优化（一）"  
subtitle: "梳理JAVA性能优化方面的内容，包括JVM的性能优化，编码，数据库相关的东西"  
date: 2017-09-12 09:00:00  
author: "zhailzh"  
header-img: "img/post-bg-2015.jpg"  
catalog: true  
tags:  
- 学习  
categories:  
- performance
---  
  JAVA的优化方面，设计的内容非常的多，有一本数是专门讲这个的，自己只是对JAVA应用程序所关心的几个部分，根据自己遇到的情况，来进行梳理，学习。
<!--more-->

1. 针对Tomcat 的JVM的优化：

~~~java

/opt/jdk/jre/bin/java 
-Djava.util.logging.config.file=/opt/C7910/conf/logging.properties 
-Djava.util.logging.manager=org.apache.juli.ClassLoaderLogManager 
-Xms2048m 
-Xmx2048m 
-XX:PermSize=128m 
-XX:MaxPermSize=256m 
-Djdk.tls.ephemeralDHKeySize=2048 
-Djava.protocol.handler.pkgs=org.apache.catalina.webresources  
-javaagent:/opt/oneapm.jar 
-classpath /opt/project/bin/bootstrap.jar:/opt/C7910/bin/tomcat-juli.jar 
-Dcatalina.base=/opt/project 
-Dcatalina.home=/opt/project 
-Djava.io.tmpdir=/opt/project/temp 
org.apache.catalina.startup.Bootstrap start
~~~ 

就JVM而言的优化，需要根据具体的业务进行分析的设置。
具体的规则：
1. 对vm分配尽可能多的memory；
2. 将Xms和Xmx设为一样的值，Xms和Xmx是设置堆内存的初始值和最大值。如果虚拟机启动时设置使用的内存比较小，这个时候又需要初始化很多对象，虚拟机就必须重复地增加内存；
3. 年轻代和年老代分配的选择
    
    让年轻代的尽量大，大到什么程度，需要根据 老年代来进行分配。老年代的大小和liveData相关联。
    一般来老年代的大小是liveData的1.1 到1.2倍，但是可以配置到1.5倍。
    具体怎么确定liveData的大小，我们可以通过检测工具，来进行确定。
    
    一个对于app流畅性运行影响的因素是young generation的大小,年轻代的大小。
    -XX:NewRatio反映的是young和tenured generation的大小比例。
    -XX:NewSize和-XX:MaxNewSize反映的是young generation大小的下限和上限，
    将这两个值设为一样就固定了young generation的大小（同Xms和Xmx设为一样）。
    
年轻代设置比较小的话，会引发的问题：
1.YGC次数更加频繁 2.可能导致YGC对象直接进入旧生代,如果此时旧生代满了,会触发FGC.






