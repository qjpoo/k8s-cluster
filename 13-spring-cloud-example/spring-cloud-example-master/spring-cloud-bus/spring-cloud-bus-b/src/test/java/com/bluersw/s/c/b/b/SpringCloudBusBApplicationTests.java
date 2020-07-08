package com.bluersw.s.c.b.b;

import com.bluersw.s.c.b.sl.ChatRemoteApplicationEvent;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.bus.BusProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class SpringCloudBusBApplicationTests {

	@Autowired
	private ApplicationEventPublisher context;

	@Autowired
	private BusProperties bp;

	@Test
	public void BChat() {
		context.publishEvent(new ChatRemoteApplicationEvent(this,bp.getId(),"spring-cloud-bus-a:9008","hi!我是B应用,这样才能不被其他应用接收到。"));
	}
}
