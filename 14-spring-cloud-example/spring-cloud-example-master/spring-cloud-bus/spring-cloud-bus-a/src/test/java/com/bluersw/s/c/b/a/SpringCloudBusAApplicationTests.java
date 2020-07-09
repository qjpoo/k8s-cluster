package com.bluersw.s.c.b.a;

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
public class SpringCloudBusAApplicationTests {

	@Autowired
	private ApplicationEventPublisher context;

	@Autowired
	private BusProperties bp;

	@Test
	public void AChat() {
		context.publishEvent(new ChatRemoteApplicationEvent(this,bp.getId(),null,"hi!B应用，我是A应用，。"));
	}

}
