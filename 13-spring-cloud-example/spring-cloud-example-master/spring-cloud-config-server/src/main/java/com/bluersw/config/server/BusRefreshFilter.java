package com.bluersw.config.server;

import org.springframework.core.annotation.Order;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

@WebFilter(filterName = "bodyFilter", urlPatterns = "/*")
@Order(1)
//Git在进行webhood post请求的同时默认会在body加上这么一串载荷(payload),Spring Boot 无法并行化。
public class BusRefreshFilter implements Filter {

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {

	}

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
		HttpServletRequest httpServletRequest = (HttpServletRequest)servletRequest;

		String url = new String(httpServletRequest.getRequestURI());

		//只过滤/actuator/bus-refresh请求
		if (!url.endsWith("/bus-refresh")) {
			filterChain.doFilter(servletRequest, servletResponse);
			return;
		}

		//使用HttpServletRequest包装原始请求达到修改post请求中body内容的目的
		EmptyRequestWrapper requestWrapper = new EmptyRequestWrapper(httpServletRequest);

		filterChain.doFilter(requestWrapper, servletResponse);
	}

	@Override
	public void destroy() {

	}
}
