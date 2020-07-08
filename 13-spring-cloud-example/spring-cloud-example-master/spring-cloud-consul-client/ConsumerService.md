# 调用Consul服务（消费服务）

## 依赖项

在spring-cloud-consul-client项目中添加依赖项，POM文件内容中添加如下依赖项：  

```text
                <dependency>
			<groupId>org.springframework.cloud</groupId>
			<artifactId>spring-cloud-starter-consul-discovery</artifactId>
		</dependency>

                <dependency>
			<groupId>org.springframework.cloud</groupId>
			<artifactId>spring-cloud-starter-openfeign</artifactId>
		</dependency>

                <dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-web</artifactId>
		</dependency>
```

spring-cloud-starter-consul-discovery为Consul提供支持，spring-cloud-starter-openfeign为HTTP请求提供Feign风格的调用，spring-boot-starter-web仅仅为了使用HTTP MVC测试方便。  

## 配置信息

```text
spring.application.name=spring-cloud-consul-client
server.port=9002
spring.cloud.consul.host=127.0.0.1
spring.cloud.consul.port=8500
#设置不需要注册到 consul 中
spring.cloud.consul.discovery.register=false
```

本测试不用把自己注册到服务中心里：spring.cloud.consul.discovery.register=false，所以在启动类里也不用声明@EnableDiscoveryClient注解。

## 远程服务调用接口

```java
@FeignClient(name= "service-provider")
public interface ServiceProviderRemote {

	@RequestMapping("/hello")
	public String Hello(@RequestParam String name);
}
```

使用openfeign调用远程服务接口，openfeign是Spring封装后的Feign，本项目中openfeign需要使用spring-cloud-starter-consul-discovery。

## 测试消费服务接口

```java
@RestController
public class TestConsul {

	@Autowired
	ServiceProviderRemote remote;

	@RequestMapping("/TestHello")
	public String TestHello(){
		String first = remote.Hello("first-SWS");
		String second = remote.Hello("second-SWS");
		return first + " | " + second;
	}

	@RequestMapping("/Test")
	public String Test(){
		return "OK";
	}
}
```

为了测试负载均衡所以调用两次服务接口。

## 启动类

```java
@SpringBootApplication
@EnableFeignClients
public class SpringCloudConsulClientApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringCloudConsulClientApplication.class, args);
	}

}
```

在启动类里需要使用@EnableFeignClients注解启动openfeign。

## 启动项目测试

访问<http://127.0.0.1:9002/TestHello>查看结果：  
![Alt text](http://static.bluersw.com/images/spring-cloud-consul-client-07.png)  
