package com.bluersw.s.c.b.b;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(value = "com.bluersw")
public class SpringCloudBusBApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringCloudBusBApplication.class, args);
	}

}
