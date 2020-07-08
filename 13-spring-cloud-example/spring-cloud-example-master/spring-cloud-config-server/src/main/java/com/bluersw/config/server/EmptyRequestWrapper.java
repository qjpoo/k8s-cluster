package com.bluersw.config.server;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.ByteArrayInputStream;
import java.io.IOException;

//清空请求中的Body
public class EmptyRequestWrapper extends HttpServletRequestWrapper{

	public EmptyRequestWrapper(HttpServletRequest request) {
		super(request);
	}

	@Override
	public ServletInputStream getInputStream() throws IOException {
		byte[] bytes = new byte[0];
		ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);

		return new ServletInputStream() {
			@Override
			public boolean isFinished() {
				return byteArrayInputStream.read() == -1 ? true:false;
			}

			@Override
			public boolean isReady() {
				return false;
			}

			@Override
			public void setReadListener(ReadListener readListener) {

			}

			@Override
			public int read() throws IOException {
				return byteArrayInputStream.read();
			}
		};
	}
}
