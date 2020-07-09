package com.bluersw.config.client.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
