# 细聊Spring Cloud Bus

## Spring 事件驱动模型

因为Spring Cloud Bus的运行机制也是Spring事件驱动模型所以需要先了解相关知识点：
![Alt text](http://static.bluersw.com/images/spring-cloud-bus/spring-cloud-bus-03.png)  
上面图中是Spring事件驱动模型的实现示意图，以下再补充一些图中未提现的实现细节：抽象类abstract class AbstractApplicationEventMulticaster中根据事件和事件类型获取对应的观察者的方法是：

```java
	protected Collection<ApplicationListener<?>> getApplicationListeners(
			ApplicationEvent event, ResolvableType eventType)  
```

该方法内具体检索监听器（观察者的方法）是：

```java
private Collection<ApplicationListener<?>> retrieveApplicationListeners(
            ResolvableType eventType, @Nullable Class<?> sourceType, @Nullable ListenerRetriever retriever)
            
            .....
        // Add programmatically registered listeners, including ones coming
		// from ApplicationListenerDetector (singleton beans and inner beans).
		for (ApplicationListener<?> listener : listeners) {
			if (supportsEvent(listener, eventType, sourceType)) {
				if (retriever != null) {
					retriever.applicationListeners.add(listener);
				}
				allListeners.add(listener);
			}
		}
            .....
```

此方法内根据传入参数事的件对象遍历所有对应（订阅）的监听者，其中有个很重要的方法boolean supportsEvent，此方法用于判断是否是订阅的监听者：

```java
	protected boolean supportsEvent(
			ApplicationListener<?> listener, ResolvableType eventType, @Nullable Class<?> sourceType) {

		GenericApplicationListener smartListener = (listener instanceof GenericApplicationListener ?
				(GenericApplicationListener) listener : new GenericApplicationListenerAdapter(listener));
		return (smartListener.supportsEventType(eventType) && smartListener.supportsSourceType(sourceType));
	}
```

其中接口GenericApplicationListener和GenericApplicationListenerAdapter类都是为了定义或实现supportsEventType方法和supportsSourceType方法，通过这两个方法确定是否是事件的监听器（观察者、订阅者）。

```java
public interface GenericApplicationListener extends ApplicationListener<ApplicationEvent>, Ordered {

	/**
	 * Determine whether this listener actually supports the given event type.
	 * @param eventType the event type (never {@code null})
	 */
	boolean supportsEventType(ResolvableType eventType);

	/**
	 * Determine whether this listener actually supports the given source type.
	 * <p>The default implementation always returns {@code true}.
	 * @param sourceType the source type, or {@code null} if no source
	 */
	default boolean supportsSourceType(@Nullable Class<?> sourceType) {
		return true;
	}

	/**
	 * Determine this listener's order in a set of listeners for the same event.
	 * <p>The default implementation returns {@link #LOWEST_PRECEDENCE}.
	 */
	@Override
	default int getOrder() {
		return LOWEST_PRECEDENCE;
	}

}
```

其中判断发布事件的来源对象supportsSourceType方法默认就返回true，意味着如果不重写这个接口方法，是否是订阅事件的监听器不以事件来源对象进行判断，只根据事件类型进行筛选,该方法的具体实现可参考GenericApplicationListenerAdapter类包装的supportsSourceType方法实现：

```java
public boolean supportsSourceType(@Nullable Class<?> sourceType) {
		return !(this.delegate instanceof SmartApplicationListener) ||
				((SmartApplicationListener) this.delegate).supportsSourceType(sourceType);
	}
```

## Spring Cloud Bus的事件、发布、订阅

Spring Cloud Bus的事件都继承于RemoteApplicationEvent类，RemoteApplicationEvent类继承于Spring事件驱动模型的事件抽象类ApplicationEvent，也就说Spring Cloud Bus的事件、发布、订阅也是基于Spring的事件驱动模型，例如Spring Cloud Bus的配置刷新事件RefreshRemoteApplicationEvent：

![Alt text](http://static.bluersw.com/images/spring-cloud-bus/spring-cloud-bus-04.png)  

同理订阅事件也是标准的Spring事件驱动模型，例如配置刷新的监听器源码继承了Spring事件驱动模型中的接口ApplicationListener\<E extends ApplicationEvent>：

```java
public class RefreshListener
		implements ApplicationListener<RefreshRemoteApplicationEvent> {

	private static Log log = LogFactory.getLog(RefreshListener.class);

	private ContextRefresher contextRefresher;

	public RefreshListener(ContextRefresher contextRefresher) {
		this.contextRefresher = contextRefresher;
	}

	@Override
	public void onApplicationEvent(RefreshRemoteApplicationEvent event) {
		Set<String> keys = this.contextRefresher.refresh();
		log.info("Received remote refresh request. Keys refreshed " + keys);
	}

}
```

在BusRefreshAutoConfiguration类中会将RefreshListener对象注册到Spring的BeanFactory中（不把监听器类注册到Spring的BeanFactory中就无法利用Spring的事件驱动模型对刷新事件进行处理）。

```java
	@Bean
	@ConditionalOnProperty(value = "spring.cloud.bus.refresh.enabled",matchIfMissing = true)
	@ConditionalOnBean(ContextRefresher.class)
	public RefreshListener refreshListener(ContextRefresher contextRefresher) {
		return new RefreshListener(contextRefresher);
	}
```

也可以使用@EventListener创建监听器，例如TraceListener类：

```java
	@EventListener
	public void onAck(AckRemoteApplicationEvent event) {
		Map<String, Object> trace = getReceivedTrace(event);
		// FIXME boot 2 this.repository.add(trace);
	}

	@EventListener
	public void onSend(SentApplicationEvent event) {
		Map<String, Object> trace = getSentTrace(event);
		// FIXME boot 2 this.repository.add(trace);
	}
```

发布事件也是利用应用程序上下文进行事件发布，比如配置刷新的实现代码：

```java
@Endpoint(id = "bus-refresh") // TODO: document new id
public class RefreshBusEndpoint extends AbstractBusEndpoint {

	public RefreshBusEndpoint(ApplicationEventPublisher context, String id) {
		super(context, id);
	}

	@WriteOperation
	public void busRefreshWithDestination(@Selector String destination) { // TODO:
																			// document
																			// destination
		publish(new RefreshRemoteApplicationEvent(this, getInstanceId(), destination));
	}

	@WriteOperation
	public void busRefresh() {
		publish(new RefreshRemoteApplicationEvent(this, getInstanceId(), null));
	}

}
```

注解@WriteOperation实现POST操作，@Endpoint结合management.endpoints.web.exposure.include=* 配置项可实现一个接入点，接入点的URL是：/actuator/bus-refresh
父类AbstractBusEndpoint内用应用程序上下文实现事件的发布：

```java
public class AbstractBusEndpoint {

	private ApplicationEventPublisher context;

	private String appId;

	public AbstractBusEndpoint(ApplicationEventPublisher context, String appId) {
		this.context = context;
		this.appId = appId;
	}

	protected String getInstanceId() {
		return this.appId;
	}

	protected void publish(ApplicationEvent event) {
		this.context.publishEvent(event);
	}

}
```

## Spring Cloud Bus的底层通讯实现（对使用者透明）

Spring Cloud Bus的底层通讯基础是Spring Cloud Stream，定义发送总线事件和接收总线事件监听器的类是BusAutoConfiguration（在网络上发送和接收其他节点的事件消息），因为继承了ApplicationEventPublisherAware所以该类也具备发布本地事件的功能（可以查询Aware接口作用），发布网络事件消息的方法是：

```java
@EventListener(classes = RemoteApplicationEvent.class)
	public void acceptLocal(RemoteApplicationEvent event) {
		if (this.serviceMatcher.isFromSelf(event)
				&& !(event instanceof AckRemoteApplicationEvent)) {
			this.cloudBusOutboundChannel.send(MessageBuilder.withPayload(event).build());
		}
	}
```

如果监听到RemoteApplicationEvent类事件，首先检查是否是自己发布并且不是ACK事件，如果是自己发布的非ACK事件就在总线上发送这个事件消息。发送AckRemoteApplicationEvent（ACK事件）已经在接收其他节点发的事件消息时触发了，所以这里不用管发送ACK事件的工作了。

接收事件消息：

```java
@StreamListener(SpringCloudBusClient.INPUT)
	public void acceptRemote(RemoteApplicationEvent event) {
		if (event instanceof AckRemoteApplicationEvent) {
			if (this.bus.getTrace().isEnabled() && !this.serviceMatcher.isFromSelf(event)
					&& this.applicationEventPublisher != null) {
				this.applicationEventPublisher.publishEvent(event);
			}
			// If it's an ACK we are finished processing at this point
			return;
		}
		if (this.serviceMatcher.isForSelf(event)
				&& this.applicationEventPublisher != null) {
			if (!this.serviceMatcher.isFromSelf(event)) {
				this.applicationEventPublisher.publishEvent(event);
			}
			if (this.bus.getAck().isEnabled()) {
				AckRemoteApplicationEvent ack = new AckRemoteApplicationEvent(this,
						this.serviceMatcher.getServiceId(),
						this.bus.getAck().getDestinationService(),
						event.getDestinationService(), event.getId(), event.getClass());
				this.cloudBusOutboundChannel
						.send(MessageBuilder.withPayload(ack).build());
				this.applicationEventPublisher.publishEvent(ack);
			}
		}
		if (this.bus.getTrace().isEnabled() && this.applicationEventPublisher != null) {
			// We are set to register sent events so publish it for local consumption,
			// irrespective of the origin
			this.applicationEventPublisher.publishEvent(new SentApplicationEvent(this,
					event.getOriginService(), event.getDestinationService(),
					event.getId(), event.getClass()));
		}
	}
```

接收到其他节点发来的事件消息后会将此事件发布到本地的应用程序上下文中（this.applicationEventPublisher），监听此事件类型的订阅者就会相应的进行处理。

## 两个跟踪事件AckRemoteApplicationEvent和SentApplicationEvent

从他们的继承关系可以看出，AckRemoteApplicationEvent可以发送到其他网络节点（继承于RemoteApplicationEvent），SentApplicationEvent只是本地事件（继承于ApplicationEvent），SentApplicationEvent事件可以显示收到事件消息的类型，AckRemoteApplicationEvent事件只显示收到事件消息的ID，TraceListener类负责监听和记录他们的内容（配置项要打开spring.cloud.bus.trace.enabled=true）：

```java
public class TraceListener {

	@EventListener
	public void onAck(AckRemoteApplicationEvent event) {
		Map<String, Object> trace = getReceivedTrace(event);
		// FIXME boot 2 this.repository.add(trace);
	}

	@EventListener
	public void onSend(SentApplicationEvent event) {
		Map<String, Object> trace = getSentTrace(event);
		// FIXME boot 2 this.repository.add(trace);
	}

	protected Map<String, Object> getSentTrace(SentApplicationEvent event) {
		.....
	}

	protected Map<String, Object> getReceivedTrace(AckRemoteApplicationEvent event) {
		.....
	}

}
```

在总线事件发送端和总线事件接收端日志的记录流程如下：
![Alt text](http://static.bluersw.com/images/spring-cloud-bus/spring-cloud-bus-05.png)  

## 测试A应用和B应用进行“聊天”

首先准备环境：
创建3个项目：spring-cloud-bus-shared-library、spring-cloud-bus-a、spring-cloud-bus-b

* spring-cloud-bus-shared-library：负责定义事件和监听器还有配置类
* spring-cloud-bus-a：扮演A应用负责引用shared-library并利用BUS发送消息给B应用（此消息实际为广播消息）
* spring-cloud-bus-b：扮演B应用负责引用shared-library并利用BUS回复A应用发来的消息（此消息非广播消息）

spring-cloud-bus-shared-library的POM的依赖项：

```xml
<properties>
		<java.version>1.8</java.version>
		<spring-cloud.version>Greenwich.SR3</spring-cloud.version>
	</properties>

	<dependencies>
		<dependency>
			<groupId>org.springframework.cloud</groupId>
			<artifactId>spring-cloud-starter-bus-amqp</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-actuator</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-test</artifactId>
			<scope>test</scope>
		</dependency>
	</dependencies>

	<dependencyManagement>
		<dependencies>
			<dependency>
				<groupId>org.springframework.cloud</groupId>
				<artifactId>spring-cloud-dependencies</artifactId>
				<version>${spring-cloud.version}</version>
				<type>pom</type>
				<scope>import</scope>
			</dependency>
		</dependencies>
```

删除构建的Maven插件节点否则构建后其他项目引用不了（格式不对）：

```xml
<build>
		<plugins>
			<plugin>
				<groupId>org.springframework.boot</groupId>
				<artifactId>spring-boot-maven-plugin</artifactId>
			</plugin>
		</plugins>
	</build>
```

启动一个rabbitmq:

```shell
docker pull rabbitmq:3-management

docker run -d --hostname my-rabbit --name rabbit -p 5672:5672 -p 15672:15672 rabbitmq:3-management
```

application.properties配置定义：

```text
spring.application.name=spring-cloud-bus-shared-library
server.port=9007
# 开启消息跟踪
spring.cloud.bus.trace.enabled=true
spring.rabbitmq.host=127.0.0.1
spring.rabbitmq.port=5672
spring.rabbitmq.username=guest
spring.rabbitmq.password=guest

#显示的暴露接入点
management.endpoints.web.exposure.include=*
```

spring-cloud-bus-a、spring-cloud-bus-b的配置信息除了spring.application.name和server.port不一样，其他都是一样的。

自定义一个聊天事件类：

```java
/**
 * 聊天事件
 */
public class ChatRemoteApplicationEvent extends RemoteApplicationEvent {

	private String message;

	//for serializers
	private ChatRemoteApplicationEvent(){}

	public ChatRemoteApplicationEvent(Object source, String originService,
			String destinationService,String message){
		super(source, originService, destinationService);

		this.message = message;
	}

	public void setMessage(String message){
		this.message = message;
	}

	public String getMessage(){
		return this.message;
	}
}
```

自定义聊天事件监听器：

```java
/**
 * 聊天事件监听
 */
public class ChatListener implements ApplicationListener<ChatRemoteApplicationEvent> {

	private static Log log = LogFactory.getLog(ChatListener.class);

	public ChatListener(){}

	@Override
	public void onApplicationEvent(ChatRemoteApplicationEvent event){
		log.info(String.format("应用%s对应用%s悄悄的说：\"%s\"",
				event.getOriginService(),
				event.getDestinationService(),
				event.getMessage()));
	}
}
```

配置类将监听器注册到BeanFactory中，并需要显示的告诉Spring Cloud Bus我们有一个自定义事件：@RemoteApplicationEventScan(basePackageClasses=ChatRemoteApplicationEvent.class)，否则BUS收到消息后无法识别事件类型。

```java
@Configuration
@ConditionalOnClass(ChatListener.class)
@RemoteApplicationEventScan(basePackageClasses=ChatRemoteApplicationEvent.class)
public class BusChatConfiguration {

	@Bean
	public ChatListener ChatListener(){
		return new ChatListener();
	}
}

```

发布到本地Maven仓库：

```shell
mvn install
```

spring-cloud-bus-a、spring-cloud-bus-b的POM依赖：

```xml
<properties>
		<java.version>1.8</java.version>
		<spring-cloud.version>Greenwich.SR3</spring-cloud.version>
	</properties>

	<dependencies>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-actuator</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.cloud</groupId>
			<artifactId>spring-cloud-bus</artifactId>
		</dependency>

		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-test</artifactId>
			<scope>test</scope>
		</dependency>
		<dependency>
			<groupId>com.bluersw</groupId>
			<artifactId>spring-cloud-bus-shared-library</artifactId>
			<version>0.0.1-SNAPSHOT</version>
		</dependency>
	</dependencies>

	<dependencyManagement>
		<dependencies>
			<dependency>
				<groupId>org.springframework.cloud</groupId>
				<artifactId>spring-cloud-dependencies</artifactId>
				<version>${spring-cloud.version}</version>
				<type>pom</type>
				<scope>import</scope>
			</dependency>
		</dependencies>
	</dependencyManagement>
```

在spring-cloud-bus-a、spring-cloud-bus-b的启动Main函数上增加@ComponentScan(value = "com.bluersw")注解，否则不会扫描引用spring-cloud-bus-shared-library项目的配置类（也就加载不了自定义的事件和监听器类型）。

spring-cloud-bus-a:

```java
@SpringBootApplication
@ComponentScan(value = "com.bluersw")
public class SpringCloudBusAApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringCloudBusAApplication.class, args);
	}

}
```

spring-cloud-bus-b:

```java
@SpringBootApplication
@ComponentScan(value = "com.bluersw")
public class SpringCloudBusBApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringCloudBusBApplication.class, args);
	}

}
```

spring-cloud-bus-a发送消息给spring-cloud-bus-b（启动spring-cloud-bus-a程序和spring-cloud-bus-b程序）:

```java
@RunWith(SpringRunner.class)
@SpringBootTest
public class SpringCloudBusAApplicationTests {

	@Autowired
	private ApplicationEventPublisher context;

	@Autowired
	private BusProperties bp;

	@Test
	public void AChat() {
		context.publishEvent(new ChatRemoteApplicationEvent(this,bp.getId(),null,"hi!B应用，我是A应用，。"));
	}

}
```

执行AChat()之后，spring-cloud-bus-b的控制台会输出：  
”应用spring-cloud-bus-a:-1:33b6374cba32e6a3e7e2c8e7631de8c0对应用**悄悄的说："hi!B应用，我是A应用，。”，说明spring-cloud-bus-b收到了消息并正确解析和执行了事件处理函数，但这条消息是群发的，因为destinationService参数我们给的是个null，所有引用spring-cloud-bus-shared-library项目注册监听器的项目都可以收到此信息。

spring-cloud-bus-b回复消息给spring-cloud-bus-a：

```java
@RunWith(SpringRunner.class)
@SpringBootTest
public class SpringCloudBusBApplicationTests {

	@Autowired
	private ApplicationEventPublisher context;

	@Autowired
	private BusProperties bp;

	@Test
	public void BChat() {
		context.publishEvent(new ChatRemoteApplicationEvent(this,bp.getId(),"spring-cloud-bus-a:9008","hi!我是B应用,这样才能不被其他应用接收到。"));
	}
}
```

spring-cloud-bus-a是项目名称，9008是spring-cloud-bus-a项目的端口号，指定了目标服务参数destinationService后，其他应用就接收不到这条消息了。执行BChat()之后，spring-cloud-bus-a控制台会显示：  
“应用spring-cloud-bus-b:-1:d577ac1ab28f0fc465a1e4700e7f538a对应用spring-cloud-bus-a:9008:**悄悄的说："hi!我是B应用,这样才能不被其他应用接收到。”  
此消息现在只有spring-cloud-bus-a项目会接收到。  
