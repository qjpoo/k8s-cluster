# Consul集群加入网关服务

## 架构示意图

![Alt text](http://static.bluersw.com/images/spring-cloud-gateway/spring-cloud-gateway-12.png )  
外部的应用或网站通过外部网关服务消费各种服务，内部的生产者本身也可能是消费者，内部消费行为通过内部网关服务消费。  
一个内部网关和一个外部网关以及一个Consul Client部署在一台服务器上，这样的网关服务器至少2组，外部网关前面还会有负载均衡设备，内部网关服务使用Consul Client进行查询后使用，内部网关的负载均衡由Consul负责了。  

## 搭建演示环境

在[Consul集群Server+Client模式](./spring-cloud-consul-client/ConsulClusterServerClientMode.md)的基础上，我们更新并启动网关服务和消费者服务，演示环境中我们只启动一个网关服务进行模拟。  
删除spring-cloud-gateway和spring-cloud-consul-consumer这两个容器。

```shell
docker pull bluersw/spring-cloud-gateway:v3

docker run --name=spring-cloud-gateway -d -p 9000:9000 bluersw/spring-cloud-gateway:v3 /opt/consul/./consul agent -data-dir=/opt/consul/data -config-dir=/opt/consul/config -node=gw-cc  -join 172.17.0.2

docker exec spring-cloud-gateway  /usr/local/java/bin/java -jar /opt/spring-cloud-gateway-0.0.1-SNAPSHOT.jar

docker pull bluersw/spring-cloud-consul-consumer:v3

docker run --name=spring-cloud-consul-consumer -d -p 9003:9003 bluersw/spring-cloud-consul-consumer:v3  /opt/consul/./consul agent -data-dir=/opt/consul/data -config-dir=/opt/consul/config -node=consumer-cc  -join 172.17.0.2

docker exec  spring-cloud-consul-consumer /usr/local/java/bin/java -jar /opt/spring-cloud-consul-client-0.0.1-SNAPSHOT.jar

```

![Alt text](http://static.bluersw.com/images/spring-cloud-gateway/spring-cloud-gateway-13.png )  
![Alt text](http://static.bluersw.com/images/spring-cloud-gateway/spring-cloud-gateway-14.png )  

## TAG：V3版本的网关和消费者镜像修改内容

spring-cloud-gateway的项目配置文件修改如下(也是在本机Consul Client注册)，主要是为了增加prefer-ip-address否则Consul获取不到服务的IP地址：

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
        prefer-ip-address: true
        health-check-path: /actuator/health
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

为了模拟内部服务调用网关消费其他服务，spring-cloud-consul-client项目（spring-cloud-consul-consumer）添加如下代码：
创建Feign风格的代理类

```java
//网关服务
@FeignClient(name="PC-ApiGateWay")
public interface GatewayRemote {

	//网关上的请求地址和外部用浏览器浏览的路径相同
	@RequestMapping("/service-provider/hello")
	public String Hello(@RequestParam String name);

}
```

Controller里增加如下方法：

```java
    @Autowired
    GatewayRemote gatewayRemote;
    
       @RequestMapping("/TestGW")
	public String TestGW(){
		String first = gatewayRemote.Hello("first-SWS");
		String second = gatewayRemote.Hello("second-SWS");
		return first + " | " + second;
	}
```

## 模拟外部访问

直接在浏览器里访问127.0.0.1:9000/service-provider/hello?name=sws，得到服务的返回信息：
![Alt text](http://static.bluersw.com/images/spring-cloud-gateway/spring-cloud-gateway-15.png )  

## 模拟内部访问

在浏览器里访问127.0.0.1:9003/TestGW，得到服务的返回信息：
![Alt text](http://static.bluersw.com/images/spring-cloud-gateway/spring-cloud-gateway-16.png )  
