package gov.cms.bfd.server.war;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import javax.security.auth.x500.X500Principal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * Ensure that every request-response pair lands in the application's NDJSON-formatted HTTP access
 * log. Also adds a bunch of data to the logging {@link MDC} and also ensures that the {@link MDC}
 * is completely cleared after every request. This {@link Filter} must be declared before all others
 * in the {@code web.xml}.
 *
 * <p>(Note: We don't use or extend Logback's builtin <code>MDCInsertingServletFilter</code>, as it
 * includes more properties than we really need. It also doesn't fully clear the {@link MDC} after
 * each request, only partially.)
 */
public final class RequestResponseLoggingFilter implements Filter {
  private static final Logger LOGGER_HTTP_ACCESS = LoggerFactory.getLogger("HTTP_ACCESS");
  private static final Logger LOGGER_MISC =
      LoggerFactory.getLogger(RequestResponseLoggingFilter.class);

  private static final String REQUEST_ATTRIB_START = computeMdcKey("request_start_milliseconds");

  /**
   * @see jakarta.servlet.Filter#doFilter(jakarta.servlet.ServletRequest,
   *     jakarta.servlet.ServletResponse, jakarta.servlet.FilterChain)
   */
  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    handleRequest(request);
    try {
      chain.doFilter(request, response);
    } finally {
      handleResponse(request, response);
      addToHttpAccessLog();
      clearMdc();
    }
  }

  /** @param request the {@link ServletRequest} to record the standard {@link MDC} entries for */
  private static void handleRequest(ServletRequest request) {
    request.setAttribute(REQUEST_ATTRIB_START, System.currentTimeMillis());

    // Record the request type.
    MDC.put(computeMdcKey("request_type"), request.getClass().getName());

    // Set the default Operation (will hopefully be customized further in specific handler methods).
    Operation operation = new Operation(Operation.Endpoint.OTHER);

    if (request instanceof HttpServletRequest) {
      HttpServletRequest servletRequest = (HttpServletRequest) request;

      // Record the basic request components.
      operation = new Operation(Operation.Endpoint.matchByHttpUri(servletRequest));
      MDC.put(computeMdcRequestKey("http_method"), servletRequest.getMethod());
      MDC.put(computeMdcRequestKey("url"), servletRequest.getRequestURL().toString());
      MDC.put(computeMdcRequestKey("uri"), servletRequest.getRequestURI());
      MDC.put(computeMdcRequestKey("query_string"), servletRequest.getQueryString());
      MDC.put(
          computeMdcRequestKey("clientSSL.DN"),
          getClientSslPrincipalDistinguishedName(servletRequest));

      // Record the request headers.
      Enumeration<String> headerNames = servletRequest.getHeaderNames();
      while (headerNames.hasMoreElements()) {
        String headerName = headerNames.nextElement();
        List<String> headerValues = Collections.list(servletRequest.getHeaders(headerName));
        if (headerValues.isEmpty()) MDC.put(computeMdcRequestKey("header." + headerName), "");
        else if (headerValues.size() == 1)
          MDC.put(computeMdcRequestKey("header." + headerName), headerValues.get(0));
        else MDC.put(computeMdcRequestKey("header." + headerName), headerValues.toString());
      }
    }

    // Publish the Operation name for monitoring systems.
    operation.publishOperationName();
  }

  /**
   * @param keySuffix the suffix to build a full key for
   * @return the key to use for {@link MDC#put(String, String)}
   */
  private static String computeMdcKey(String keySuffix) {
    return String.format("%s.%s", "http_access", keySuffix);
  }

  /**
   * @param keySuffix the suffix to build a full MDC key for
   * @return the key to use for {@link MDC#put(String, String)}, for an access log entry that's
   *     related to the HTTP request
   */
  public static String computeMdcRequestKey(String keySuffix) {
    return String.format("%s.%s", computeMdcKey("request"), keySuffix);
  }

  /**
   * @param request the {@link HttpServletRequest} to get the client principal DN (if any) for
   * @return the {@link X500Principal#getName()} for the client certificate, or <code>null</code> if
   *     that's not available
   */
  private static String getClientSslPrincipalDistinguishedName(HttpServletRequest request) {
    /*
     * Note: Now that Wildfly/JBoss is properly configured with a security realm,
     * this method is equivalent to calling `request.getRemoteUser()`.
     */
    X509Certificate clientCert = getClientCertificate(request);
    if (clientCert == null || clientCert.getSubjectX500Principal() == null) {
      LOGGER_MISC.debug("No client SSL principal available: {}", clientCert);
      return null;
    }

    return clientCert.getSubjectX500Principal().getName();
  }

  /**
   * @param request the {@link HttpServletRequest} to get the client SSL certificate for
   * @return the {@link X509Certificate} for the {@link HttpServletRequest}'s client SSL
   *     certificate, or <code>null</code> if that's not available
   */
  private static X509Certificate getClientCertificate(HttpServletRequest request) {
    X509Certificate[] certs =
        (X509Certificate[]) request.getAttribute("jakarta.servlet.request.X509Certificate");
    if (certs == null || certs.length <= 0) {
      LOGGER_MISC.debug("No client certificate found for request.");
      return null;
    }
    return certs[certs.length - 1];
  }

  /**
   * @param request the {@link ServletRequest} to record the standard {@link MDC} entries for
   * @param response the {@link ServletResponse} to record the standard {@link MDC} entries for
   */
  private void handleResponse(ServletRequest request, ServletResponse response) {
    if (response instanceof HttpServletResponse) {
      HttpServletResponse servletResponse = (HttpServletResponse) response;

      MDC.put(computeMdcKey("response.status"), Integer.toString(servletResponse.getStatus()));

      // Record the response headers.
      Collection<String> headerNames = servletResponse.getHeaderNames();
      for (String headerName : headerNames) {
        Collection<String> headerValues = servletResponse.getHeaders(headerName);
        if (headerValues.isEmpty()) MDC.put(computeMdcKey("response.header." + headerName), "");
        else if (headerValues.size() == 1)
          MDC.put(computeMdcKey("response.header." + headerName), headerValues.iterator().next());
        else MDC.put(computeMdcKey("response.header." + headerName), headerValues.toString());
      }
    }

    // Record the response duration.
    Long requestStartMilliseconds = (Long) request.getAttribute(REQUEST_ATTRIB_START);
    if (requestStartMilliseconds != null)
      MDC.put(
          computeMdcKey("response.duration_milliseconds"),
          Long.toString(System.currentTimeMillis() - requestStartMilliseconds));
  }

  /** Write a single entry out to {@link #LOGGER_HTTP_ACCESS} for the request-response. */
  private static void addToHttpAccessLog() {
    /*
     * The message here isn't actually the payload; the MDC context that will get
     * automatically included with it is!
     */
    LOGGER_HTTP_ACCESS.info("response complete");
  }

  /** Completely clears the MDC after each request. */
  private void clearMdc() {
    MDC.clear();
  }

  /** @see jakarta.servlet.Filter#init(jakarta.servlet.FilterConfig) */
  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    // Nothing to do here.
  }

  /** @see jakarta.servlet.Filter#destroy() */
  @Override
  public void destroy() {
    // Nothing to do here.
  }
}
