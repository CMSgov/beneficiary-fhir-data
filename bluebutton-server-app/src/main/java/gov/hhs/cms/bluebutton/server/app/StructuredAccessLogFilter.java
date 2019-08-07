package gov.hhs.cms.bluebutton.server.app;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * <p>
 * Logs each response in a structured format, to provide richer data about each
 * response, and to ease parsing of that data.
 * </p>
 * <p>
 * This {@link Filter} must be declared in the {@code web.xml} or whatever.
 * </p>
 */
public final class StructuredAccessLogFilter implements Filter {
	private static final Logger LOGGER = LoggerFactory.getLogger("HTTP_ACCESS");

	private static final String REQUEST_ATTRIB_START = computeMdcKey("request_start_milliseconds");

	/**
	 * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest,
	 *      javax.servlet.ServletResponse, javax.servlet.FilterChain)
	 */
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		recordRequestAttributes(request);
		try {
			chain.doFilter(request, response);
		} finally {
			logResponse(request, response);
		}
	}

	/**
	 * @param request the {@link ServletRequest} to set attributes for/in
	 */
	private static void recordRequestAttributes(ServletRequest request) {
		request.setAttribute(REQUEST_ATTRIB_START, System.currentTimeMillis());
	}

	/**
	 * Log the specified request-response pair.
	 * 
	 * @param request  the {@link ServletRequest} to log
	 * @param response the {@link ServletResponse} to log
	 */
	private static void logResponse(ServletRequest request, ServletResponse response) {
		// Record the request type.
		MDC.put(computeMdcKey("request_type"), request.getClass().getName());

		if (request instanceof HttpServletRequest) {
			HttpServletRequest servletRequest = (HttpServletRequest) request;

			// Record the basic request components.
			MDC.put(computeMdcKey("request.http_method"), servletRequest.getMethod());
			MDC.put(computeMdcKey("request.url"), servletRequest.getRequestURL().toString());
			MDC.put(computeMdcKey("request.uri"), servletRequest.getRequestURI());
			MDC.put(computeMdcKey("request.query_string"), servletRequest.getQueryString());

			// Record the request headers.
			Enumeration<String> headerNames = servletRequest.getHeaderNames();
			while (headerNames.hasMoreElements()) {
				String headerName = headerNames.nextElement();
				List<String> headerValues = Collections.list(servletRequest.getHeaders(headerName));
				if (headerValues.isEmpty())
					MDC.put(computeMdcKey("request.header." + headerName), "");
				else if (headerValues.size() == 1)
					MDC.put(computeMdcKey("request.header." + headerName), headerValues.get(0));
				else
					MDC.put(computeMdcKey("request.header." + headerName), headerValues.toString());
			}
		}

		if (response instanceof HttpServletResponse) {
			HttpServletResponse servletResponse = (HttpServletResponse) response;

			MDC.put(computeMdcKey("response.status"), Integer.toString(servletResponse.getStatus()));

			// Record the response headers.
			Collection<String> headerNames = servletResponse.getHeaderNames();
			for (String headerName : headerNames) {
				Collection<String> headerValues = servletResponse.getHeaders(headerName);
				if (headerValues.isEmpty())
					MDC.put(computeMdcKey("response.header." + headerName), "");
				else if (headerValues.size() == 1)
					MDC.put(computeMdcKey("response.header." + headerName), headerValues.iterator().next());
				else
					MDC.put(computeMdcKey("response.header." + headerName), headerValues.toString());
			}
		}

		// Record the response duration.
		Long requestStartMilliseconds = (Long) request.getAttribute(REQUEST_ATTRIB_START);
		if (requestStartMilliseconds != null)
			MDC.put("duration_milliseconds", Long.toString(System.currentTimeMillis() - requestStartMilliseconds));

		/*
		 * The message here isn't actually the payload; the MDC context that will get
		 * automatically included with it is!
		 */
		LOGGER.info("response complete");
	}

	/**
	 * @param keySuffix the suffix to build a full key for
	 * @return the key to use for {@link MDC#put(String, String)}
	 */
	private static String computeMdcKey(String keySuffix) {
		return String.format("%s.%s", "http_access", keySuffix);
	}

	/**
	 * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
	 */
	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		// Nothing to do here.
	}

	/**
	 * @see javax.servlet.Filter#destroy()
	 */
	@Override
	public void destroy() {
		// Nothing to do here.
	}
}
