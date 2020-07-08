# 注册服务到服务中心（Consul）

## 添加POM文件中的依赖

在POM文件添加如下依赖：

```xml
                <dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-actuator</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.cloud</groupId>
			<artifactId>spring-cloud-starter-consul-discovery</artifactId>
			<version>2.1.3.RELEASE</version>
		</dependency>
```
spring-boot-starter-actuator负责健康检查，spring-cloud-starter-consul-discovery负责对Consul的支持。  
在引用spring-cloud-starter-consul-discovery时必须明确版本号，我们这个项目BOOT的版本是2.1.8.RELEASE,spring-cloud-starter-consul-discovery版本号是2.1.3.RELEASE，可以在[Spring Cloud官网](https://spring.io/projects/spring-cloud)查到。  
![Alt text](http://static.bluersw.com/images/spring-cloud-provider-07.png)  

或者使用dependencyManagement进行版本号的管理，在POM文件里添加如下内容可以不指明spring-cloud-starter-consul-discovery的版本号：  

```xml
	<properties>
		<java.version>1.8</java.version>
		<spring-cloud.version>Greenwich.SR3</spring-cloud.version>
	</properties>

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
	
	<dependencies>

		.....

		<dependency>
			<groupId>org.springframework.cloud</groupId>
			<artifactId>spring-cloud-starter-consul-discovery</artifactId>
		</dependency>

		.....

	</dependencies>
```

## 配置文件

```text
spring.application.name=spring-cloud-provider-01
server.port=9000
spring.cloud.consul.host=localhost
#consul端口可以自行修改
spring.cloud.consul.port=8500
#注册到consul的服务名称
spring.cloud.consul.discovery.serviceName=service-provider
```

## 启动类

SpringCloudProviderApplication.java

```java
@SpringBootApplication
//支持服务发现
@EnableDiscoveryClient
public class SpringCloudProviderApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringCloudProviderApplication.class, args);
	}

}
```  

## 启动服务

启动服务后自动完成注册服务的过程，回到consul控制界面可以看到服务已经注册好了：  
![Alt text](http://static.bluersw.com/images/spring-cloud-provider-08.png)  
点击service-provider可以看到该服务只有一个微服务：  
![Alt text](http://static.bluersw.com/images/spring-cloud-provider-09.png)  

## 负载均衡

我们参照spring-cloud-provider项目在复制一个微服务项目spring-cloud-provider-second，并对HelloWorld类进行修改以便区分：  
spring-cloud-provider项目中的HelloWorld类：  

```java
@RestController
public class HelloWorld {

	@RequestMapping("/hello")
	public String Hello(@RequestParam String name){
		return "你好！" + name + ",这是第一个微服务。";
	}
}
```

spring-cloud-provider-second项目中的HelloWorld类：  

```java
@RestController
public class HelloWorld {

	@RequestMapping("/hello")
	public String Hello(@RequestParam String name){
		return "你好！" + name + ",这是第二个微服务。";
	}
}
```

修改spring-cloud-provider-second项目的端口号：  
spring-cloud-provider-second项目中的application.properties文件内容：  

```text
spring.application.name=spring-cloud-provider-02
server.port=9001
spring.cloud.consul.host=localhost
spring.cloud.consul.port=8500
#注册到consul的服务名称
spring.cloud.consul.discovery.serviceName=service-provider
```

启动spring-cloud-provider-second项目，查看service-provider服务提供者已经出现了两个服务提供者：  
![Alt text](http://static.bluersw.com/images/spring-cloud-provider-10.png)  
