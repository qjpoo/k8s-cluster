package com.bluersw.s.c.s.b;

import org.springframework.cloud.stream.annotation.Input;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;

public interface ChatProcessor {

	String OUTPUT = "ChatExchanges-A-Input";
	String INPUT  = "ChatExchanges-A-Output";

	@Input(ChatProcessor.INPUT)
	SubscribableChannel input();

	@Output(ChatProcessor.OUTPUT)
	MessageChannel output();
}
