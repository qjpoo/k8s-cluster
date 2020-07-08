package com.bluersw.consul.client.service;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

//网关服务
@FeignClient(name="PC-ApiGateWay")
public interface GatewayRemote {

	//网关上的请求地址和外部用浏览器浏览的路径相同
	@RequestMapping("/service-provider/hello")
	public String Hello(@RequestParam String name);

}
