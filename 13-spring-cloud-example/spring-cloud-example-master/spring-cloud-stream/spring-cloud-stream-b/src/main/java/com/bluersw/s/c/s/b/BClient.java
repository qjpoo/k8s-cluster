package com.bluersw.s.c.s.b;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.annotation.InboundChannelAdapter;
import org.springframework.integration.annotation.Poller;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.annotation.Transformer;
import org.springframework.messaging.support.GenericMessage;

@EnableBinding(ChatProcessor.class)
public class BClient {

	private static Logger logger = LoggerFactory.getLogger(BClient.class);

	//@ServiceActivator没有Json转对象的能力需要借助@Transformer注解
	@ServiceActivator(inputChannel=ChatProcessor.INPUT)
	public void PrintInput(ChatMessage message) {

		logger.info(message.ShowMessage());
	}

	@Transformer(inputChannel = ChatProcessor.INPUT,outputChannel = ChatProcessor.INPUT)
	public ChatMessage transform(String message) throws Exception{
		//logger.info(message);
		ObjectMapper objectMapper = new ObjectMapper();
		return objectMapper.readValue(message,ChatMessage.class);
	}

	//每秒发出一个消息给A
	@Bean
	@InboundChannelAdapter(value = ChatProcessor.OUTPUT,poller = @Poller(fixedDelay="1000"))
	public GenericMessage<ChatMessage> SendChatMessage(){
		ChatMessage message = new ChatMessage("ClientB","B To A Message.", new Date());

		//这里只是测试实际业务根据需要设计特征值的范围，这个和消费组内有多少实例有关，然后把特征值放在消息头router属性中
		int feature = 1;
		Map<String, Object> headers = new HashMap<>();
		headers.put("router", feature);

		return  new GenericMessage<>(message,headers);
	}
}
