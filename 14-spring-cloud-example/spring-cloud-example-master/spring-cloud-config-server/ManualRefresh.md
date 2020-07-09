# 手动刷新客户端配置内容

## 客户端项目增加依赖项

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

## 客户端项目修改配置文件

增加management.endpoints.web.exposure.include=refresh,health,info

```text
spring.application.name=spring-cloud-config-client
server.port=9006
spring.cloud.consul.host=127.0.0.1
spring.cloud.consul.port=8500
#设置不需要注册到 consul 中
spring.cloud.consul.discovery.register=false
#显示的暴露接入点
management.endpoints.web.exposure.include=refresh,health,info
```

## 客户端程序增加支持刷新注解

在使用配置中心的类上添加@RefreshScope注解：

```java
@RestController
//刷新触发地址/actuator/refresh
@RefreshScope
public class ConfigTestController {

	//配置信息通过@Value注解读取,配置项用${配置项}读取
	@Value("${bluersw.config}")
	private String configBluersw;

	@RequestMapping("/ConfigTest")
	public String ConfigTest(){
		return this.configBluersw;
	}
}
```

## 测试刷新效果

将Git仓库里的配置内容改外Test-5（bluersw.config=Test-5），启动客户端程序（spring-cloud-config-client），刷新客户端页面127.0.0.1:9006/ConfigTest，发现显示内容还是Test-3，然后执行:

```shell
curl -X POST http://127.0.0.1:9006/actuat/refresh
```

再次刷新页面127.0.0.1:9006/ConfigTest，页面内容显示为Test-5，说明客户端程序内的配置信息读取了最新的值。
