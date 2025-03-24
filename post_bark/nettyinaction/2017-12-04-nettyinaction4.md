---    
layout: post  
title: "netty in action 读书笔记"  
subtitle: "netty in action的第四,五章"  
date: 2017-12-04 09:00:00  
author: "zhailzh"  
header-img: "img/post-bg-2015.jpg"  
catalog: true  
tags:  
- 学习  
categories:  
- 读书笔记
---    
explain the transport implementations that come bundled with Netty and the use cases appropriate to each.       
the superior functionality and flexibility of ByteBuf as compared to the JDK’s ByteBuffer.      

<!--more-->   

a Channel has a ChannelPipeline and a ChannelConfig assigned to it. The ChannelConfig holds all of the configuration settings for the Channel and supports hot changes.     

The ChannelPipeline holds all of the ChannelHandler instances that will be applied to inbound and outbound data and events.     

Typical uses for ChannelHandlers include:     
■ Transforming data from one format to another     
■ Providing notification of exceptions     
■ Providing notification of a Channel becoming active or inactive    
■ Providing notification when a Channel is registered with or deregistered from an EventLoop     
■ Providing notification about user-defined events     

The ChannelPipeline implements a common design pattern, Intercepting Filter. UNIX pipes are another familiar example: com- mands are chained together, with the output of one command connecting to the input of the next in line.     

The ChannelPipeline implements a common design pattern, Intercepting Filter. UNIX pipes are another familiar example: com- mands are chained together, with the output of one command connecting to the input of the next in line.     

过滤链的设计模式，类似Unix的pipe

keep in mind that the broad range of functionality offered by Netty relies on a small number of interfaces. This means that you can make significant modifications to application logic without wholesale refactoring of your code base.     

netty设计过程中注重的一个点： a small number of interfaces。     

Netty’s Channel implementations are thread-safe, so you can store a reference to a Channel and use it whenever you need to write something to the remote peer, even when many threads are in use.     

Channel是线程安全的。    

~~~java
final Channel channel = ctx.channel();
final ByteBuf buf = Unpooled.copiedBuffer("your data",
    CharsetUtil.UTF_8).retain();
//新建的线程可以随便的调用
Runnable writer = new Runnable() {
    @Override
    public void run() {
        channel.write(buf.duplicate());
    }
~~~    

![transport](http://7xtrwx.com1.z0.glb.clouddn.com/5940f5d7453b6ab69893d714abefa445.png)   
netty支持的协议。    

The basic concept behind the selector is to serve as a registry where you request to be notified when the state of a Channel changes. The possible state changes are
■ A new Channel was accepted and is ready.
■ A Channel connection was completed.
■ A Channel has data that is ready for reading.
■ A Channel is available for writing data.
After the application reacts to the change of state, the selector is reset and the process repeats, running on a thread that checks for changes and responds to them accordingly.    

pipeline时间的变化，底层的实现还是基于：java.nio.channels.SelectionKey.      

![底层实现](http://7xtrwx.com1.z0.glb.clouddn.com/2fffdbdfb610aa63b43ce28b520ac893.png)      

Zero-copy      

Zero-copy is a feature currently available only with NIO and Epoll transport. It allows you to quickly and efficiently move data from a file system to the network without copying from kernel space to user space, which can significantly improve perfor- mance in protocols such as FTP or HTTP. **This feature is not supported by all OSes. Specifically it is not usable with file systems that implement data encryption or compression—only the raw content of a file can be transferred.** Conversely, transferring files that have already been encrypted isn’t a problem.      

零拷贝技术，是的netty的效率比较的高效。不支持文件系统中数据的压缩和解压缩。但是传输压缩的数据是没有问题的。      

**Netty’s NIO transport is based on the common abstraction for asynchronous/non-blocking networking provided by Java.** Although this ensures that Netty’s non-blocking API will be usable on any platform, it also entails limitations, because the JDK has to make compromises in order to deliver the same capabilities on all systems.

netty基于JDK提动的网路通信功能，保证了平台的普适性。但是也正因为是JDK为了平台的普适性，做出了妥协。     

Given this, you may wonder how Netty can support NIO with the same API used for asynchronous transports. The answer is that Netty makes use of the SO_TIMEOUT Socket flag, which specifies the maximum number of milliseconds to wait for an I/O opera- tion to complete. If the operation fails to complete within the specified interval, a SocketTimeoutException is thrown. Netty catches this exception and continues the processing loop. On the next EventLoop run, it will try again. This is the only way an asynchronous framework like Netty can support OIO    

OIO和NIO实现的差别。     

![channel的选择](http://7xtrwx.com1.z0.glb.clouddn.com/e5bb374823d0ad784f28e27327ac0a19.png)    



Netty’s API for data handling is exposed through two components—abstract class ByteBuf and interface ByteBufHolder.       
These are some of the advantages of the ByteBuf API:
■ It’s extensible to userdefined buffer types.    
■ Transparent zero-copy is achieved by a built-in composite buffer type.    
■ Capacity is expanded on demand (as with the JDK StringBuilder).     
■ Switching between reader and writer modes doesn’t require calling ByteBuffer’s flip() method.    
■ Reading and writing employ distinct indices.    
■ Method chaining is supported.    
■ Reference counting is supported.     
■ Pooling is supported.    

netty中使用bytebuffer的原因：
1.易于扩展用户自定义的数据类型    
2.新建类型的时候可以确定是否是Zero-Copy类型    
3.自动需求自动扩展容量    
4.读写之间的切换不用调用flip     
5.读写标志位的分离，方法链，引用计算以及池化的支持    

Because all network communications involve the movement of sequences of bytes, **an efficient and easy-to-use data structure is an obvious necessity.** Netty’s ByteBuf imple- mentation meets and exceeds these requirements.     

netty不用JDK自带的ByteBuffer，而是自行扩展了ByteBuf的原因。      


To understand the relationship between these indices, consider what would happen if you were to read bytes until the readerIndex reached the same value as the writerIndex. At that point, you would have reached the end of readable data. Attempting to read beyond that point would trigger an IndexOutOfBoundsException, just as when you attempt to access data beyond the end of an array.       

ByteBuf使用了两个index，readerIndex和writerIndex，为了操作的方便。netty封装的ByteBuf的实质：an array of bytes with distinct indices to control read and write access。       

另外ByteBuf提供的方法中，只有以read或者writer开头的方法名称，才会改变readindex或者writeindex的位置。    


The most frequently used ByteBuf pattern stores the data in the heap space of the JVM. Referred to as a backing array, this pattern provides fast allocation and dealloca- tion in situations where pooling isn’t in use.      

最常用的ByteBuf模式将数据存储在JVM的堆空间中。这一模式被称为backing array，在不使用池的情况下，该模式提供了快速分配和回收。这种分配的方式称为：heapBuffer    

NOTE Attempting to access a backing array when hasArray() returns false will trigger an UnsupportedOperation-  Exception. This pattern is similar to uses of the JDK’s ByteBuffer.      

Direct buffer is another ByteBuf pattern. We expect that memory allocated for object creation will always come from the heap, but it doesn’t have to—**the ByteBuffer class that was introduced in JDK 1.4 with NIO allows a JVM implementation to allocate memory via native calls.** This aims to avoid copying the buffer’s contents to (or from) an intermediate buffer before (or after) each invocation of a native I/O operation.The Javadoc for ByteBuffer states explicitly,**“The contents of direct buffers will reside outside of the normal garbage-collected heap.”**       

还有一种Buffer 称之为Direct buffer，这也是零拷贝的一个方面。      

The primary disadvantage of direct buffers is that they’re somewhat more expensive to allocate and release than are heap-based buffers. You may also encounter another drawback if you’re working with legacy code: because the data isn’t on the heap, you may have to make a copy, as shown next.     

direct buffers 分配和释放都比heap buffers 花费比较的高。并且direct buffers不能够make a copy。     

The third and final pattern uses a composite buffer, which presents an aggregated view of multiple ByteBufs. Here you can add and delete ByteBuf instances as needed, a fea- ture entirely absent from the JDK’s ByteBuffer      implementation.        

Netty implements this pattern with a subclass of ByteBuf, CompositeByteBuf, which provides a virtual representation of multiple buffers as a single, merged buffer     

第三中的类型是混合类型，实现的类型是：CompositeByteBuf。      

~~~java
	ByteBuf heapBuf = UnpooledByteBufAllocator.DEFAULT.heapBuffer();    

		UnpooledByteBufAllocator.DEFAULT.directBuffer();   

		CompositeByteBuf bytes = UnpooledByteBufAllocator.DEFAULT.compositeBuffer();
		heapBuf = PooledByteBufAllocator.DEFAULT.heapBuffer();
		bytes = PooledByteBufAllocator.DEFAULT.compositeBuffer() ;
		PooledByteBufAllocator.DEFAULT.directBuffer();
~~~    
![CompositeByteBuf](http://7xtrwx.com1.z0.glb.clouddn.com/c1598edefd37939bcb469ccac6bf11cc.png)    

CompositeByteBuf may not allow access to a backing array, so accessing the data in a CompositeByteBuf resembles the direct buffer pattern, as shown next.    

~~~java
CompositeByteBuf compBuf = Unpooled.compositeBuffer();
int length = compBuf.readableBytes();
byte[] array = new byte[length];
compBuf.getBytes(compBuf.readerIndex(), array);
handleArray(array, 0, array.length)
~~~   

Note that Netty optimizes socket I/O operations that employ CompositeByteBuf, elim- inating whenever possible the performance and memory usage penalties that are incurred with the JDK’s buffer implementation.2 This optimization takes place in Netty’s core code and is therefore not exposed, but you should be aware of its impact.     

Netty在Socket通信的时候，使用了已经优化的CompositeByteBuf，消除了使用JDK自带的Buffer，可能产生的性能和内存上的问题。     

While you may be tempted to call discardReadBytes() frequently to maximize the writable segment, please be aware that this will most likely cause memory copying because the readable bytes (marked CONTENT in the figures) have to be moved to the start of the buffer. We advise doing this only when it’s really needed; for example, when memory is at a premium.     

readindex之前的部分，就是已读的部分，已经没有什么用处了，调用discardReadBytes，可以提高buffer的利用率，但是可能会引发内存拷贝的发生，所以谨慎使用。      

The method writableBytes() is used here to determine whether there is sufficient space in the buffer.     

writableBytes 取得剩余的可写的字节。

The JDK’s InputStream defines the methods mark(int readlimit) and reset(). These are used to mark the current position in the stream to a specified value and to reset the stream to that position, respectively.       

mark和reset的作用，标记和恢复。       

Similarly, you can set and reposition the ByteBuf readerIndex and writerIndex by calling markReaderIndex(), markWriterIndex(), resetReaderIndex(), and reset- WriterIndex(). These are similar to the InputStream calls, except that there’s no readlimit parameter to specify when the mark becomes invalid.      
对应的ByteBuf中也有标记和恢复。     

You can also move the indices to specified positions by calling readerIndex(int) or writerIndex(int). Attempting to set either index to an invalid position will cause an IndexOutOfBoundsException.     

对特定的标识进行标记     

You can set both readerIndex and writerIndex to 0 by calling clear(). Note that this doesn’t clear the contents of memory.     

clear 不清理内存的内容。      

Calling clear() is much less expensive than discardReadBytes() because it resets the indices without copying any memory.   

能使用clear不使用discardReadbytes。     


■ duplicate()      
■ slice()    
■ slice(int, int)    
■ Unpooled.unmodifiableBuffer(...)    
■ order(ByteOrder)    
■ readSlice(int)    
 This makes a derived buffer inexpensive to create, but it also means that if you modify its contents you are modifying the source instance as well, so beware.    

使用上面的方法，能够得到一个副本的buf，但是需要注意，操作的时候是同一个buf。     

If you need a true copy of an existing buffer, use copy() or copy(int,int). Unlike a derived buffer, the ByteBuf returned by this call has an independent copy of the data.    

如果不想要副本，可以直接的调用copy，直接弄一个新的。    

To reduce the overhead of allocating and deallocating memory, Netty implements pooling with the interface ByteBufAllocator, which can be used to allocate instances of any of the ByteBuf varieties we’ve described.      
You can obtain a reference to a ByteBufAllocator either from a Channel (each of which can have a distinct instance) or through the ChannelHandlerContext that is bound to a ChannelHandler.      
使用ByteBufAllocator分配ByteBuf,ByteBufAllocator的引用可以通过Channel来进行获得。    

~~~java
ByteBufAllocator allocator = channel.alloc();
ByteBufAllocator allocator2 = ctx.alloc();
~~~    

Netty provides two implementations of ByteBufAllocator: PooledByteBufAllocator and UnpooledByteBufAllocator. The former pools ByteBuf instances to improve per- formance and minimize memory fragmentation. This implementation uses an effi- cient approach to memory allocation known as jemalloc4 that has been adopted by a number of modern OSes. The latter implementation doesn’t pool ByteBuf instances and returns a new instance every time it’s called.    

PooledByteBufAllocator和UnpooledByteBufAllocator，亦如类名所示。     

There may be situations where you don’t have a reference to a ByteBufAllocator. For this case, Netty provides a utility class called Unpooled, which provides static helper methods to create unpooled ByteBuf instances.    
Unpooled 工具类。    

Reference counting is a technique for optimizing memory use and performance by releasing the resources held by an object when it is no longer referenced by other objects.      
引用计算      

~~~java
Channel channel = ...;
ByteBufAllocator allocator = channel.alloc();
....
ByteBuf buffer = allocator.directBuffer();
assert buffer.refCnt() == 1;
...
~~~     
