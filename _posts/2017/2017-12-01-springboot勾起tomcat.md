---    
layout: post  
title: "springboot启动的时候，怎么勾起tomcat启动？"  
subtitle: "梳理自己遇到的问题，开始进行总结分析"  
date: 2017-12-01 09:00:00  
author: "zhailzh"  
header-img: "img/post-bg-2015.jpg"  
catalog: true  
tags:  
- 学习  
categories:  
- question
---  
springboot加载工程，到底是怎么启动的，我知道springboot中加了一个tomcat的源码，可是这个tomcat是怎么启动的呢？
<!--more-->

spring的启动是从一个java类的main函数开始的，这个应该没有异议：     

~~~java  
public static void main(String[] args) {
		SpringApplication application = new SpringApplication(App.class);
		application.run(args);
	}
~~~     

A: 首先是初始化

启动新建的类     

~~~java
	public SpringApplication(Object... sources) {
		initialize(sources);
	}

private final Set<Object> sources = new LinkedHashSet<Object>();
@SuppressWarnings({ "unchecked", "rawtypes" })
	private void initialize(Object[] sources) {
		if (sources != null && sources.length > 0) {
			this.sources.addAll(Arrays.asList(sources));
		}
		//推断是否是WEB环境
		this.webEnvironment = deduceWebEnvironment();
		//应用上下文初始化实例 的加载
		setInitializers((Collection) getSpringFactoriesInstances(ApplicationContextInitializer.class));
		//监听器
		setListeners((Collection) getSpringFactoriesInstances(ApplicationListener.class));
		//推断出启动类
		this.mainApplicationClass = deduceMainApplicationClass();
	}
~~~    



推断是否是WEB环境的判断就是，看看能否加出来：   

~~~java
private static final String[] WEB_ENVIRONMENT_CLASSES = { "javax.servlet.Servlet",
			"org.springframework.web.context.ConfigurableWebApplicationContext" };
~~~

这里的类。

![推测是否是web环境](http://7xtrwx.com1.z0.glb.clouddn.com/2e31619ab5e4cafb5f5bffe2f73af536.png)   

然后就是初始化监听器和初始化工厂，这部分不关心。

---   

4 ********************** 非常精彩的代码***********************

根据运行的堆栈，查找到启动的类！

~~~java

	private Class<?> deduceMainApplicationClass() {
		try {
			StackTraceElement[] stackTrace = new RuntimeException().getStackTrace();
			for (StackTraceElement stackTraceElement : stackTrace) {
				if ("main".equals(stackTraceElement.getMethodName())) {
					return Class.forName(stackTraceElement.getClassName());
				}
			}
		}
		catch (ClassNotFoundException ex) {
			// Swallow and continue
		}
		return null;
	}

~~~
---
B:  找到具体的main方法所在的类，然后就是run方法了：

~~~java

/**
	 * Run the Spring application, creating and refreshing a new
	 * {@link ApplicationContext}.
	 * @param args the application arguments (usually passed from a Java main method)
	 * @return a running {@link ApplicationContext}
	 */
	public ConfigurableApplicationContext run(String... args) {
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();
		ConfigurableApplicationContext context = null;
		configureHeadlessProperty();
		// listeners 发不的是application启动的event
		SpringApplicationRunListeners listeners = getRunListeners(args);
		listeners.started();
		try {
			ApplicationArguments applicationArguments = new DefaultApplicationArguments(
					args);
      // 创建执行的上下文环境
			context = createAndRefreshContext(listeners, applicationArguments);
			afterRefresh(context, applicationArguments);
			listeners.finished(context, null);
			stopWatch.stop();
			if (this.logStartupInfo) {
				new StartupInfoLogger(this.mainApplicationClass)
						.logStarted(getApplicationLog(), stopWatch);
			}
			return context;
		}
		catch (Throwable ex) {
			handleRunFailure(context, listeners, ex);
			throw new IllegalStateException(ex);
		}
	}
~~~     

————————————————————————————————————
2. 创建并且刷新配置上下文：      

~~~java
	private ConfigurableApplicationContext createAndRefreshContext(
			SpringApplicationRunListeners listeners,
			ApplicationArguments applicationArguments) {
		ConfigurableApplicationContext context;
		// Create and configure the environment
		ConfigurableEnvironment environment = getOrCreateEnvironment();
		configureEnvironment(environment, applicationArguments.getSourceArgs());
		listeners.environmentPrepared(environment);
		if (isWebEnvironment(environment) && !this.webEnvironment) {
			environment = convertToStandardEnvironment(environment);
		}

		//打印Spring boot的流程
		if (this.bannerMode != Banner.Mode.OFF) {
			printBanner(environment);
		}

		// Create, load, refresh and run the ApplicationContext
		context = createApplicationContext();
		context.setEnvironment(environment);
		postProcessApplicationContext(context);
		applyInitializers(context);
		listeners.contextPrepared(context);
		if (this.logStartupInfo) {
			logStartupInfo(context.getParent() == null);
			logStartupProfileInfo(context);
		}

		// Add boot specific singleton beans
		context.getBeanFactory().registerSingleton("springApplicationArguments",
				applicationArguments);

		// Load the sources
		Set<Object> sources = getSources();
		Assert.notEmpty(sources, "Sources must not be empty");
		load(context, sources.toArray(new Object[sources.size()]));
		listeners.contextLoaded(context);

		// Refresh the context
		refresh(context);
		if (this.registerShutdownHook) {
			try {
				context.registerShutdownHook();
			}
			catch (AccessControlException ex) {
				// Not allowed in some environments.
			}
		}
		return context;
	}
~~~


2.3  // refresh(context);

最主要的调用的方法

~~~java
	protected void refresh(ApplicationContext applicationContext) {
		Assert.isInstanceOf(AbstractApplicationContext.class, applicationContext);
		((AbstractApplicationContext) applicationContext).refresh();
	}
~~~    

![refresh调用的过程](http://7xtrwx.com1.z0.glb.clouddn.com/e986d3f51188c1b5a9215b5213e1c436.png)   

~~~java    

@Override
	public void refresh() throws BeansException, IllegalStateException {
		synchronized (this.startupShutdownMonitor) {
		。。。。。。。。。。

			try {
				。。。。。。。。。。。。。。。

				// Initialize other special beans in specific context subclasses.
				onRefresh(); //启动tomcat开始

				。。。。。。。。。。。。。

				// Last step: publish corresponding event.
				finishRefresh();
			}

			catch (BeansException ex) {
				。。。。。。。。。。。。
			finally {
				// Reset common introspection caches in Spring's core, since we
				// might not ever need metadata for singleton beans anymore...
				resetCommonCaches();
			}
		}
	}

~~~   

创建servlet容器的过程：     

~~~java
@Override
	protected void onRefresh() {
		super.onRefresh();
		try {
			createEmbeddedServletContainer();//创建servlet的容器
		}
		catch (Throwable ex) {
			throw new ApplicationContextException("Unable to start embedded container",
					ex);
		}
	}

	// 创建servlet的容器
	private void createEmbeddedServletContainer() {
		EmbeddedServletContainer localContainer = this.embeddedServletContainer; //servlet的容器的具有的特征：start，stop，getport
		ServletContext localServletContext = getServletContext();
		if (localContainer == null && localServletContext == null) {
			EmbeddedServletContainerFactory containerFactory = getEmbeddedServletContainerFactory();
			this.embeddedServletContainer = containerFactory
					.getEmbeddedServletContainer(getSelfInitializer());
		}
		else if (localServletContext != null) {
			try {
				getSelfInitializer().onStartup(localServletContext);
			}
			catch (ServletException ex) {
				throw new ApplicationContextException("Cannot initialize servlet context",
						ex);
			}
		}
		initPropertySources();
	}
~~~

具体运行的过程中的servlet的容器建造类：      

![servlet容器建造者](http://7xtrwx.com1.z0.glb.clouddn.com/7e1567b22c50eff83c80d5116ca9fd8f.png)     

![构建tomcat对象](http://7xtrwx.com1.z0.glb.clouddn.com/a6a29422767dde565dc398ea71048dbd.png)    

新建tomcat servlet 容器的时候，直接的把tomcat启动！！     

~~~java
	public TomcatEmbeddedServletContainer(Tomcat tomcat, boolean autoStart) {
		Assert.notNull(tomcat, "Tomcat Server must not be null");
		this.tomcat = tomcat;
		this.autoStart = autoStart;
		initialize();
	}

	private synchronized void initialize() throws EmbeddedServletContainerException {
		TomcatEmbeddedServletContainer.logger
				.info("Tomcat initialized with port(s): " + getPortsDescription(false));
		try {
			addInstanceIdToEngineName();

			// Remove service connectors to that protocol binding doesn't happen yet
			removeServiceConnectors();

			// Start the server to trigger initialization listeners
			this.tomcat.start();

			// We can re-throw failure exception directly in the main thread
			rethrowDeferredStartupExceptions();

			// Unlike Jetty, all Tomcat threads are daemon threads. We create a
			// blocking non-daemon to stop immediate shutdown
			startDaemonAwaitThread();
		}
		catch (Exception ex) {
			throw new EmbeddedServletContainerException("Unable to start embedded Tomcat",
					ex);
		}
	}
~~~

this.tomcat.start() 这个算是将tomcat启动：     
~~~
2018-01-02 19:06:05.426 [main] INFO  o.s.b.c.e.tomcat.TomcatEmbeddedServletContainer - Tomcat initialized with port(s): 20011 (http)
2018-01-02 19:06:07.534 [main] INFO  org.apache.catalina.core.StandardService - Starting service Tomcat
2018-01-02 19:06:07.542 [main] INFO  org.apache.catalina.core.StandardEngine - Starting Servlet Engine: Apache Tomcat/8.0.32
2018-01-02 19:06:07.721 [localhost-startStop-1] INFO  o.a.c.core.ContainerBase.[Tomcat].[localhost].[/] - Initializing Spring embedded WebApplicationContext
2018-01-02 19:06:07.722 [localhost-startStop-1] INFO  org.springframework.web.context.ContextLoader - Root WebApplicationContext: initialization completed in 11890 ms
2018-01-02 19:06:07.982 [localhost-startStop-1] INFO  o.s.boot.context.embedded.ServletRegistrationBean - Mapping servlet: 'dispatcherServlet' to [/]
2018-01-02 19:06:07.987 [localhost-startStop-1] INFO  o.s.boot.context.embedded.FilterRegistrationBean - Mapping filter: 'characterEncodingFilter' to: [/*]
2018-01-02 19:06:07.988 [localhost-startStop-1] INFO  o.s.boot.context.embedded.FilterRegistrationBean - Mapping filter: 'hiddenHttpMethodFilter' to: [/*]
2018-01-02 19:06:07.988 [localhost-startStop-1] INFO  o.s.boot.context.embedded.FilterRegistrationBean - Mapping filter: 'httpPutFormContentFilter' to: [/*]
2018-01-02 19:06:07.988 [localhost-startStop-1] INFO  o.s.boot.context.embedded.FilterRegistrationBean - Mapping filter: 'requestContextFilter' to: [/*]

~~~  

总结来说：springboot在运行的过程中，确定是web体系，找到main类的run方法，执行的过程中
确定了：org.springframework.boot.context.embedded.EmbeddedWebApplicationContext
上下文环境，在创建Servlet容器的时候，新建Tomcat对象，初始化的时候，直接启动tomcat。
