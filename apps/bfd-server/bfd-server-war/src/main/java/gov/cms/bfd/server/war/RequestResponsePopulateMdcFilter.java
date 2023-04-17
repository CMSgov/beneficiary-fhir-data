package gov.cms.bfd.server.war;

import gov.cms.bfd.server.sharedutils.BfdMDC;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import javax.security.auth.x500.X500Principal;
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

/**
 * Ensure that every request-response pair adds data to the logging {@link BfdMDC} which will be
 * present on all log messages, particularly the log messages written to the access log via Jetty.
 * This {@link Filter} must be declared before all others in the {@code web.xml}.
 *
 * <p>(Note: We don't use or extend Logback's builtin <code>MDCInsertingServletFilter</code>, as it
 * includes more properties than we really need. It also doesn't fully clear the {@link BfdMDC}
 * after each request, only partially.)
 */
public final class RequestResponsePopulateMdcFilter implements Filter {
  /** The logger for this filter. */
  private static final Logger LOGGER_MISC =
      LoggerFactory.getLogger(RequestResponsePopulateMdcFilter.class);

  /** Used to compute the MDC key. */
  private static final String MDC_PREFIX = "http_access";
  /** Prefix for requests. */
  private static final String REQUEST_PREFIX = "request";
  /** Prefix for responses. */
  private static final String RESPONSE_PREFIX = "response";

  /** {@inheritDoc} */
  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    /*
     * This should be cleared by Jetty via the request log handling but just in case we have a situation where
     * that handler does not fire (say, due to a Jetty defect) clear it now before the request starts.
     */
    BfdMDC.clear();

    handleRequest(request);
    try {
      chain.doFilter(request, response);
    } finally {
      handleResponse(request, response);
    }
  }

  /**
   * Handles populating the MDC logger for http requests.
   *
   * @param request the {@link ServletRequest} to record the standard {@link BfdMDC} entries for
   */
  private static void handleRequest(ServletRequest request) {
    request.setAttribute(BfdMDC.REQUEST_START_KEY, System.currentTimeMillis());

    // Record the request type.
    BfdMDC.put(BfdMDC.computeMDCKey(MDC_PREFIX, "request_type"), request.getClass().getName());

    // Set the default Operation (will hopefully be customized further in specific handler methods).
    CanonicalOperation operation = new CanonicalOperation(CanonicalOperation.Endpoint.OTHER);

    if (request instanceof HttpServletRequest) {
      HttpServletRequest servletRequest = (HttpServletRequest) request;

      // Record the basic request components.
      operation =
          new CanonicalOperation(CanonicalOperation.Endpoint.matchByHttpUri(servletRequest));
      BfdMDC.put(
          BfdMDC.computeMDCKey(MDC_PREFIX, REQUEST_PREFIX, "http_method"),
          servletRequest.getMethod());
      BfdMDC.put(
          BfdMDC.computeMDCKey(MDC_PREFIX, REQUEST_PREFIX, "url"),
          servletRequest.getRequestURL().toString());
      BfdMDC.put(
          BfdMDC.computeMDCKey(MDC_PREFIX, REQUEST_PREFIX, "uri"), servletRequest.getRequestURI());
      BfdMDC.put(
          BfdMDC.computeMDCKey(MDC_PREFIX, REQUEST_PREFIX, "query_string"),
          servletRequest.getQueryString());
      BfdMDC.put(
          BfdMDC.computeMDCKey(MDC_PREFIX, REQUEST_PREFIX, "clientSSL", "DN"),
          getClientSslPrincipalDistinguishedName(servletRequest));

      // Record the request headers.
      Enumeration<String> headerNames = servletRequest.getHeaderNames();
      while (headerNames.hasMoreElements()) {
        String headerName = headerNames.nextElement();
        List<String> headerValues = Collections.list(servletRequest.getHeaders(headerName));
        if (headerValues.isEmpty())
          BfdMDC.put(BfdMDC.computeMDCKey(MDC_PREFIX, REQUEST_PREFIX, "header", headerName), "");
        else if (headerValues.size() == 1)
          BfdMDC.put(
              BfdMDC.computeMDCKey(MDC_PREFIX, REQUEST_PREFIX, "header", headerName),
              headerValues.get(0));
        else
          BfdMDC.put(
              BfdMDC.computeMDCKey(MDC_PREFIX, REQUEST_PREFIX, "header", headerName),
              headerValues.toString());
      }
    }

    // Publish the Operation name for monitoring systems.
    operation.publishOperationName();
  }

  /**
   * Gets the {@link X500Principal#getName()} for the client certificate if available.
   *
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
   * Gets the {@link X509Certificate} for the {@link HttpServletRequest}'s client SSL certificate if
   * available.
   *
   * @param request the {@link HttpServletRequest} to get the client SSL certificate for
   * @return the {@link X509Certificate} for the {@link HttpServletRequest}'s client SSL
   *     certificate, or <code>null</code> if that's not available
   */
  private static X509Certificate getClientCertificate(HttpServletRequest request) {
    X509Certificate[] certs =
        (X509Certificate[]) request.getAttribute("javax.servlet.request.X509Certificate");
    if (certs == null || certs.length <= 0) {
      LOGGER_MISC.debug("No client certificate found for request.");
      return null;
    }
    return certs[certs.length - 1];
  }

  /**
   * Handles populating the MDC logger for http responses.
   *
   * @param request the {@link ServletRequest} to record the standard {@link BfdMDC} entries for
   * @param response the {@link ServletResponse} to record the standard {@link BfdMDC} entries for
   */
  private void handleResponse(ServletRequest request, ServletResponse response) {
    if (response instanceof HttpServletResponse) {
      HttpServletResponse servletResponse = (HttpServletResponse) response;
      BfdMDC.put(
          BfdMDC.computeMDCKey(MDC_PREFIX, RESPONSE_PREFIX, "status"),
          Integer.toString(servletResponse.getStatus()));

      // Record the response headers.
      Collection<String> headerNames = servletResponse.getHeaderNames();
      for (String headerName : headerNames) {
        Collection<String> headerValues = servletResponse.getHeaders(headerName);
        if (headerValues.isEmpty())
          BfdMDC.put(BfdMDC.computeMDCKey(MDC_PREFIX, RESPONSE_PREFIX, "header", headerName), "");
        else if (headerValues.size() == 1)
          BfdMDC.put(
              BfdMDC.computeMDCKey(MDC_PREFIX, RESPONSE_PREFIX, "header", headerName),
              headerValues.iterator().next());
        else
          BfdMDC.put(
              BfdMDC.computeMDCKey(MDC_PREFIX, RESPONSE_PREFIX, "header", headerName),
              headerValues.toString());
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    // Nothing to do here.
  }

  /** {@inheritDoc} */
  @Override
  public void destroy() {
    // Nothing to do here.
  }
}
