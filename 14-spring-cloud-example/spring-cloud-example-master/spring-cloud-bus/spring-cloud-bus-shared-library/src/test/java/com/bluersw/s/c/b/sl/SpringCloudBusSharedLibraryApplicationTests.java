package com.bluersw.s.c.b.sl;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.bus.BusProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class SpringCloudBusSharedLibraryApplicationTests {

	@Autowired
	private ApplicationEventPublisher context;

	@Autowired
	private BusProperties bp;

	@Test
	public void PublishEventTest() {
		context.publishEvent(new ChatRemoteApplicationEvent(this,bp.getId(),null,"测试群聊"));
	}

}
