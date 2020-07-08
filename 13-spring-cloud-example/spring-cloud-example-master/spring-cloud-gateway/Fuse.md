# 网关中加入熔断机制

## 在网关中加入熔断机制

![Alt text](http://static.bluersw.com/images/spring-cloud-gateway/spring-cloud-gateway-09.png )  

## 添加依赖项

spring-cloud-gateway项目POM文件加入spring-cloud-starter-netflix-hystrix

```xml
                <dependency>
			<groupId>org.springframework.cloud</groupId>
			<artifactId>spring-cloud-starter-netflix-hystrix</artifactId>
		</dependency>
```

## 修改配置文件

修改application.yml配置文件  

```yml
server:
 port: 9000
spring:
  cloud:
    consul:
      host: 127.0.0.1
      port: 8500
      discovery:
        register: true
    gateway:
      routes:
        - id: test_route
          uri: lb://service-provider
          predicates:
            - Path=/service-provider/{segment}
          filters:
            - SetPath=/{segment}
            - name: Hystrix
              args:
                name: service-provider-fallback
                fallbackUri: forward:/service-provider-error
            - name: Retry
              args:
                retries: 3
                statuses: BAD_GATEWAY,BAD_REQUEST
      default-filters:
        - name: Hystrix
          args:
            name: fallbackcmd
            fallbackUri: forward:/default-error
  application:
    name: PC-ApiGateWay
```

### 在默认过滤器中加入熔断机制

```yml
default-filters:
        - name: Hystrix
          args:
            name: fallbackcmd
            fallbackUri: forward:/default-error
```

gateway下的default-filters代表默认过滤器，Hystrix是熔断机制的实现，fallbackcmd是HystrixCommand对象的名字（name属性），fallbackUri表示触发熔断机制后的跳转请求url,/default-error是在spring-cloud-gateway项目中实现的错误信息统一处理Controller：

```java
@RestController
public class ErrorHandle {

	@RequestMapping("/default-error")
	public String DefaultErrorHandle(){
		return "这是通用错误处理返回的信息。";
	}
}
```

### 自定义单条路由的熔断机制处理内容

```yml
gateway:
      routes:
        - id: test_route
          uri: lb://service-provider
          predicates:
            - Path=/service-provider/{segment}
          filters:
            - SetPath=/{segment}
            - name: Hystrix
              args:
                name: service-provider-fallback
                fallbackUri: forward:/service-provider-error
```

内容和上面介绍相同，同样需要spring-cloud-gateway项目实现service-provider-error处理过程。  

```java
@RestController
public class ErrorHandle {

	@RequestMapping("/default-error")
	public String DefaultErrorHandle(){
		return "这是通用错误处理返回的信息。";
	}

	@RequestMapping("/service-provider-error")
	public String ServiceProviderErrorHandle(){
		return "这是ServiceProvider服务专属的错误处理信息。";
	}
}
```

### 自动重试机制

```yml
gateway:
      routes:
        - id: test_route
          uri: lb://service-provider
          predicates:
            - Path=/service-provider/{segment}
          filters:
            - SetPath=/{segment}
            - name: Hystrix
              args:
                name: service-provider-fallback
                fallbackUri: forward:/service-provider-error
            - name: Retry
              args:
                retries: 3
                statuses: BAD_GATEWAY,BAD_REQUEST
```

在gateway的filters下声明name为Retry的过滤器，retries重试次数，statuses返回HTTP状态码为何值时重试（还有methods和series参数），请参考org.springframework.http.HttpStatus、org.springframework.http.HttpMethod和org.springframework.http.HttpStatus.Series。  

## 启动项目测试

启动 Consul服务中心和spring-cloud-provider微服务，最后启动spring-cloud-gateway项目，正常情况下：
![Alt text](http://static.bluersw.com/images/spring-cloud-gateway/spring-cloud-gateway-10.png )  

关闭spring-cloud-provider微服务进程之后再次刷新页面：  
![Alt text](http://static.bluersw.com/images/spring-cloud-gateway/spring-cloud-gateway-11.png )  
