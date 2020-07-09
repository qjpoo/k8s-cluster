package com.bluersw.s.c.s.a;

import org.springframework.cloud.stream.annotation.Output;
import org.springframework.messaging.MessageChannel;

public interface ChatOutput {

	String OUTPUT = "ChatExchanges-A-Output";

	@Output(ChatOutput.OUTPUT)
	MessageChannel output();
}
