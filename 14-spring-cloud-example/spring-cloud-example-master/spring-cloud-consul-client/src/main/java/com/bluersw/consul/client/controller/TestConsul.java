package com.bluersw.consul.client.controller;

import com.bluersw.consul.client.service.GatewayRemote;
import com.bluersw.consul.client.service.ServiceProviderRemote;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestConsul {

	@Autowired
	ServiceProviderRemote remote;

	@Autowired
	GatewayRemote gatewayRemote;

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

	@RequestMapping("/TestGW")
	public String TestGW(){
		String first = gatewayRemote.Hello("first-SWS");
		String second = gatewayRemote.Hello("second-SWS");
		return first + " | " + second;
	}
}
