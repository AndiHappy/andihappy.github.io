---
layout:     post
title:      "定时任务的说明"
subtitle:   "Java语言中采用定时任务的实现的方法"
date:       2017-12-01 13:00:00
author:     "zhailzh"
header-img: "img/post-bg-2015.jpg"
catalog:     true
tags:  
- 定时任务
categories:  
- 工作总结
---   

定时任务是一个使用概率不高，但是很大几率会使用到的功能，记录之。   
<!--more-->
~~~java    


/**
 * 任务的执行，依赖于Timer，可以特定的时间执行或者按照某种规律循环的执行
 * A facility for threads to schedule tasks for future execution in a background thread.
 * Tasks may be scheduled for one-time execution, or for repeated execution at regular intervals.
 *
 * 弊端：
 * 1. 不能针对某一个定时任务进行取消
 * 2. Timer 的设计核心是一个 TaskList 和一个 TaskThread。Timer 将接收到的任务丢到自己的 TaskList 中，
 * TaskList 按照 Task 的最初执行时间进行排序。TimerThread 在创建 Timer 时会启动成为一个守护线程。
 * 这个线程会轮询所有任务，找到一个最近要执行的任务，然后休眠，当到达最近要执行任务的开始时间点，TimerThread 被唤醒并执行该任务。
 * 之后 TimerThread 更新最近一个要执行的任务，继续休眠。但由于所有任务都是由同一个线程来调度，因此所有任务都是串行执行的，
 * 同一时间只能有一个任务在执行，前一个任务的延迟或异常都将会影响到之后的任务。
 * */
public class TimerTest extends TimerTask {

	private String jobName = "";

	public TimerTest(String jobName) {
		super();
		this.jobName = jobName;
	}

	@Override
	public void run() {
		try {
			//如果一个线程执行的过程中休眠会影响其他的线程的执行
			Thread.sleep(100000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("execute " + jobName);
	}

	public static void main(String[] args) {
		Timer timer = new Timer();
		long delay1 = 1 * 1000;
		long period1 = 1000;
		// 从现在开始 1 秒钟之后，每隔 1 秒钟执行一次 job1
		timer.schedule(new TimerTest("job1"), delay1, period1);

		long delay2 = 2 * 1000;
		long period2 = 2000;
		// 从现在开始 2 秒钟之后，每隔 2 秒钟执行一次 job2
		timer.schedule(new TimerTask() {

			@Override
			public void run() {
				System.out.println("第二个线程的执行完毕！");
			}
		}, delay2, period2);

		//取消任务,则会取消全部的任务
//		timer.cancel();
	}
}
~~~    

进化版：    

~~~java     
public class ScheduledExecutorUtil {

	public static ScheduledExecutorUtil getInstance(){
		return ScheduledExecutorUtilHoler.instance;
	}

	private static class ScheduledExecutorUtilHoler{
		private static  ScheduledExecutorUtil instance = new ScheduledExecutorUtil();
	}
	private ScheduledThreadPoolExecutor scheduExec = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(10);;
	private ConcurrentHashMap<String, ScheduledFuture<?>> result  = new ConcurrentHashMap<String, ScheduledFuture<?>>();
	public boolean putScheduledTask(Runnable task,int time,TimeUnit timeunit,String taskName){
		ScheduledFuture<?> value = scheduExec.schedule(task,time,timeunit);
		result.put(taskName, value);
		return result.containsKey(taskName);
	}

	private ScheduledExecutorUtil(){
		//定时清理result中已经执行完毕的定时任务
		scheduExec.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				if(result.size() > 0){
					for (String taskname : result.keySet()) {
						ScheduledFuture<?> task = result.get(taskname);
						if(task.isCancelled() || task.isDone()){
							result.remove(taskname);
						}
					}
				}

			}
		}, 1, 24*60, TimeUnit.MINUTES);
	}

	private boolean cancelScheduledTask(String taskName){
		if(!result.containsKey(taskName)){
			return true;
		}else{
			ScheduledFuture<?> task = result.get(taskName);
			if(task.isCancelled()){
				result.remove(taskName);
				return true;
			}
			if(task.isDone()){
				result.remove(taskName);
				throw new IllegalArgumentException(taskName + " have been cancelled !");
			}
			boolean isCancel =  task.cancel(true);
			if(isCancel){
				result.remove(taskName);
			}
			return isCancel;

		}
	}

	public void shutDownScheduedTaskPool(){
		scheduExec.shutdown();
	}

	public static void main(String[] args) {
		final long start = System.currentTimeMillis();
		ScheduledExecutorUtil test = new ScheduledExecutorUtil();
		test.putScheduledTask(new Runnable() {
			public void run() {
				System.out.println("timerOne,the time:" + (System.currentTimeMillis() - start));
				try {
					Thread.sleep(4000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}, 20000, TimeUnit.MILLISECONDS,"test1");
		System.out.println(test.cancelScheduledTask("test1"));
		test.putScheduledTask(new Runnable() {
			public void run() {
				System.out.println("timerTwo,the time:" + (System.currentTimeMillis() - start));
			}
		}, 10000, TimeUnit.MILLISECONDS,"test2");
		System.out.println(test.cancelScheduledTask("test1"));
	}
}
~~~
