# Spring Cloud Gateway注册到服务器中心(Consul)

## 准备环境

启动Consul（./consul agent -dev）作为服务中心，默认是8500端口，然后启动spring-cloud-provider（9001端口）和spring-cloud-provider-second（9002端口）两个项目作为微服务。  
在Consul管理后台可以看见两个服务启动：  
![Alt text](http://static.bluersw.com/images/spring-cloud-gateway/spring-cloud-gateway-05.png )  

## 添加Spring Cloud Gateway项目的依赖项

POM内增加如下依赖：

```xml
                <dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-actuator</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.cloud</groupId>
			<artifactId>spring-cloud-starter-consul-discovery</artifactId>
		</dependency>
```

## 修改Spring Cloud Gateway项目配置

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
  application:
    name: PC-ApiGateWay
```

* host:Consul的IP地址
* port:Consul的端口号
* register:是否将自己注册到Consul中
* lb://service-provider:Consul的服务名称，以{lb://服务名}进行访问
* Path:路由要匹配的路径格式
* SetPath:设置路径过滤器，作用是匹配后可以根据分割符进行访问路径的设置
* name:自己注册到Consul中的名称

## 启动Spring Cloud Gateway项目

启动后Consul后台可以看见Spring Cloud Gateway项目的注册内容
![Alt text](http://static.bluersw.com/images/spring-cloud-gateway/spring-cloud-gateway-06.png )  

## 测试访问

访问 “127.0.0.1:9000/service-provider/hello?name=sws” 这个地址，并刷新页面测试网关的负载均衡。
![Alt text](http://static.bluersw.com/images/spring-cloud-gateway/spring-cloud-gateway-07.png )  
![Alt text](http://static.bluersw.com/images/spring-cloud-gateway/spring-cloud-gateway-08.png )  

可以看出网关代理了后台微服务的功能，并起到了轮询访问的作用。
