package com.bluersw.s.c.s.a;

import org.springframework.cloud.stream.annotation.Input;
import org.springframework.messaging.SubscribableChannel;

public interface ChatInput {

	String INPUT = "ChatExchanges-A-Input";

	@Input(ChatInput.INPUT)
	SubscribableChannel input();
}
