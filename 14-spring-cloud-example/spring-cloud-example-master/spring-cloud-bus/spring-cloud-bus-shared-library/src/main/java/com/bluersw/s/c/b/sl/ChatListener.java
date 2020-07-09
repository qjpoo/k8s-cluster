package com.bluersw.s.c.b.sl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.ApplicationListener;

/**
 * 聊天事件监听
 */
public class ChatListener implements ApplicationListener<ChatRemoteApplicationEvent> {

	private static Log log = LogFactory.getLog(ChatListener.class);

	public ChatListener(){}

	@Override
	public void onApplicationEvent(ChatRemoteApplicationEvent event){
		log.info(String.format("应用%s对应用%s悄悄的说：\"%s\"",
				event.getOriginService(),
				event.getDestinationService(),
				event.getMessage()));
	}
}
