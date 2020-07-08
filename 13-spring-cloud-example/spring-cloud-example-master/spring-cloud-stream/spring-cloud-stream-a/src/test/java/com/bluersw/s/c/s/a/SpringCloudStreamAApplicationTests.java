package com.bluersw.s.c.s.a;

import java.util.Date;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class SpringCloudStreamAApplicationTests {

	@Autowired
	private ChatOutput chatOutput;

	@Test
	public void contextLoads() throws JsonProcessingException {
		ChatMessage message = new ChatMessage("ClientA","Test Message", new Date());

		String strMessage = new ObjectMapper().writeValueAsString(message);

		chatOutput.output().send(MessageBuilder.withPayload(message).build());

	}

}
