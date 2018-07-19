package gov.hhs.cms.bluebutton.server.app;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.security.auth.x500.X500Principal;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import ch.qos.logback.classic.ClassicConstants;
import ch.qos.logback.classic.helpers.MDCInsertingServletFilter;
import gov.hhs.cms.bluebutton.server.app.stu3.providers.TransformerUtils;

/**
 * Adds some common {@link HttpServletRequest} properties to the logging
 * {@link MDC} and also ensures that the {@link MDC} is completely cleared after
 * every request. This {@link Filter} must be declared before all others in the
 * {@code web.xml} or whatever.
 *
 * (Note: We don't use or extend {@link MDCInsertingServletFilter}, as it
 * includes more properties than we really need. It also doesn't fully clear the
 * {@link MDC} after each request, only partially.)
 */
public final class LoggingContextFilter implements Filter {
	private static final Logger LOGGER = LoggerFactory.getLogger(LoggingContextFilter.class);

	/**
	 * <p>
	 * The set of {@link Pattern}s for finding and stripping sensitive info (i.e.
	 * PII/PHI) from {@link HttpServletRequest#getRequestURI()} values. Each
	 * {@link Pattern}'s {@code 1} group will identify the data to be masked.
	 * </p>
	 * <p>
	 * See these methods for more info on the URIs masked here:
	 * </p>
	 * <ul>
	 * <li>{@link TransformerUtils#buildPatientId(String)}</li>
	 * <li>{@link TransformerUtils#buildCoverageId(gov.hhs.cms.bluebutton.server.app.stu3.providers.MedicareSegment, String)}</li>
	 * <li>{@link TransformerUtils#buildEobId(gov.hhs.cms.bluebutton.server.app.stu3.providers.ClaimType, String)}</li>
	 * </ul>
	 */
	private static final Pattern[] URI_SENSITIVE_INFO_PATTERNS = new Pattern[] {

			Pattern.compile(".*/Patient/(-?\\d+).*"),

			Pattern.compile(".*/Coverage/part-[abcd]-(-?\\d+).*"),

			Pattern.compile(".*/ExplanationOfBenefit/(?:[a-z]+)-(-?\\d+).*") };

	/**
	 * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest,
	 *      javax.servlet.ServletResponse, javax.servlet.FilterChain)
	 */
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		recordStandardRequestEntries(request);
		try {
			chain.doFilter(request, response);
		} finally {
			clearMdc();
		}
	}

	/**
	 * @param request
	 *            the {@link ServletRequest} to record the standard {@link MDC}
	 *            entries for
	 */
	private void recordStandardRequestEntries(ServletRequest request) {
		if (request instanceof HttpServletRequest) {
			HttpServletRequest httpServletRequest = (HttpServletRequest) request;

			MDC.put(ClassicConstants.REQUEST_METHOD, httpServletRequest.getMethod());
			MDC.put(ClassicConstants.REQUEST_REQUEST_URI, httpServletRequest.getRequestURI());
			StringBuffer requestURL = httpServletRequest.getRequestURL();
			if (requestURL != null)
				MDC.put(ClassicConstants.REQUEST_REQUEST_URL, requestURL.toString());
			else
				MDC.put(ClassicConstants.REQUEST_REQUEST_URL, null);
			MDC.put(ClassicConstants.REQUEST_QUERY_STRING, httpServletRequest.getQueryString());
			MDC.put("req.clientSSL.DN", getClientSslPrincipalDistinguishedName(httpServletRequest));
		}
	}

	/**
	 * @param request
	 *            the {@link HttpServletRequest} to get the client principal DN (if
	 *            any) for
	 * @return the {@link X500Principal#getName()} for the client certificate, or
	 *         <code>null</code> if that's not available
	 */
	private static String getClientSslPrincipalDistinguishedName(HttpServletRequest request) {
		X509Certificate clientCert = getClientCertificate(request);
		if (clientCert == null || clientCert.getSubjectX500Principal() == null) {
			LOGGER.debug("No client SSL principal available: {}", clientCert);
			return null;
		}

		return clientCert.getSubjectX500Principal().getName();
	}

	/**
	 * @param request
	 *            the {@link HttpServletRequest} to get the client SSL certificate
	 *            for
	 * @return the {@link X509Certificate} for the {@link HttpServletRequest}'s
	 *         client SSL certificate, or <code>null</code> if that's not available
	 */
	private static X509Certificate getClientCertificate(HttpServletRequest request) {
		X509Certificate[] certs = (X509Certificate[]) request.getAttribute("javax.servlet.request.X509Certificate");
		if (certs == null || certs.length <= 0) {
			LOGGER.debug("No client certificate found for request.");
			return null;
		}
		return certs[certs.length - 1];
	}

	/**
	 * @param requestUri
	 *            the {@link HttpServletRequest#getRequestURI()} value to mask
	 *            sensitive info (if any) in
	 * @return the URI that was passed in, but with any sensitive info masked
	 */
	static String stripSensitiveInfoFromUri(String requestUri) {
		if (requestUri == null)
			return null;

		for (Pattern uriSensitiveInfoPattern : URI_SENSITIVE_INFO_PATTERNS) {
			Matcher uriSensitiveInfoMatcher = uriSensitiveInfoPattern.matcher(requestUri);
			if (uriSensitiveInfoMatcher.matches()) {
				/*
				 * Replace the URI with itself, except capture group 1 from the regex is just
				 * "***".
				 */
				requestUri = String.format("%s%s%s", requestUri.substring(0, uriSensitiveInfoMatcher.start(1)), "***",
						requestUri.substring(uriSensitiveInfoMatcher.end(1)));
			}
		}

		return requestUri;
	}

	/**
	 * Completely clears the MDC after each request.
	 */
	private void clearMdc() {
		MDC.clear();
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
