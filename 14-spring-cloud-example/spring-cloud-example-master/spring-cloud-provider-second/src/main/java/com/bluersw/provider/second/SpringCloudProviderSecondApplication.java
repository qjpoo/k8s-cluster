package com.bluersw.provider.second;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class SpringCloudProviderSecondApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringCloudProviderSecondApplication.class, args);
	}

}
