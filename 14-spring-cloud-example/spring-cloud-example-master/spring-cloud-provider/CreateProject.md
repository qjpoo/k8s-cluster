# 创建一个测试用的微服务项目HelloWorld  

## 创建项目  

![Alt text](http://static.bluersw.com/images/spring-cloud-provider-01.png)  
![Alt text](http://static.bluersw.com/images/spring-cloud-provider-02.png)  
![Alt text](http://static.bluersw.com/images/spring-cloud-provider-03.png)  
![Alt text](http://static.bluersw.com/images/spring-cloud-provider-04.png)  

## 编写服务代码

```java
@RestController
public class HelloWorld {

	@RequestMapping("/hello")
	public String Hello(@RequestParam String name){
		return "你好！" + name + ",这是一个微服务。";
	}
}
```

编辑配置文件application.properties：

```text
spring.application.name=spring-cloud-provider-01
server.port=9000
```

![Alt text](http://static.bluersw.com/images/spring-cloud-provider-05.png)  

## 测试运行

![Alt text](http://static.bluersw.com/images/spring-cloud-provider-06.png)  
