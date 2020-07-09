package com.bluersw.s.c.b.sl;

import org.springframework.cloud.bus.event.RemoteApplicationEvent;

/**
 * 聊天事件
 */
public class ChatRemoteApplicationEvent extends RemoteApplicationEvent {

	private String message;

	private ChatRemoteApplicationEvent(){}

	public ChatRemoteApplicationEvent(Object source, String originService,
			String destinationService,String message){
		super(source, originService, destinationService);

		this.message = message;
	}

	public void setMessage(String message){
		this.message = message;
	}

	public String getMessage(){
		return this.message;
	}
}
