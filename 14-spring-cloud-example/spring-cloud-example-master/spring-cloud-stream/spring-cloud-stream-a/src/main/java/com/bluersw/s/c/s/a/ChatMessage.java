package com.bluersw.s.c.s.a;

import java.io.Serializable;
import java.util.Date;

public class ChatMessage implements Serializable {

	private String name;
	private String message;
	private Date chatDate;

	//没有无参数的构造函数并行化会出错
	private ChatMessage(){}

	public ChatMessage(String name,String message,Date chatDate){
		this.name = name;
		this.message = message;
		this.chatDate = chatDate;
	}

	public String getName(){
		return this.name;
	}

	public String getMessage(){
		return this.message;
	}

	public Date getChatDate() { return this.chatDate; }

	public String ShowMessage(){
		return String.format("聊天消息：%s的时候，%s说%s。",this.chatDate,this.name,this.message);
	}
}
