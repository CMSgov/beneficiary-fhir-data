package gov.cms.bfd.server.war;

import gov.cms.bfd.server.sharedutils.BfdMDC;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.EOFException;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.security.auth.x500.X500Principal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

/**
 * Ensure that every request-response pair adds data to the logging {@link BfdMDC} which will be
 * present on all log messages, particularly the log messages written to the access log via Jetty.
 * This {@link OncePerRequestFilter} should be declared before all others in the {@code web.xml}, or
 * have its precedence set to the highest value.
 *
 * <p>(Note: We don't use or extend Logback's builtin <code>MDCInsertingServletFilter</code>, as it
 * includes more properties than we really need. It also doesn't fully clear the {@link BfdMDC}
 * after each request, only partially.)
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestResponsePopulateMdcFilter extends OncePerRequestFilter {
  /** The logger for this filter. */
  private static final Logger LOGGER =
      LoggerFactory.getLogger(RequestResponsePopulateMdcFilter.class);

  /** The HTTP Access Request Logger. */
  private static final Logger LOGGER_HTTP_ACCESS = LoggerFactory.getLogger("HTTP_ACCESS");

  /** Used to compute the MDC key. */
  private static final String MDC_PREFIX = "http_access";

  /** Prefix for requests. */
  private static final String REQUEST_PREFIX = "request";

  /** Prefix for responses. */
  private static final String RESPONSE_PREFIX = "response";

  /** obfuscated replacement for MBI. */
  private static final String OBFUSCATED_MBI = "*********";

  /** Regex Pattern to check for an MBI. */
  private static final Pattern MBI_REGEX =
      // Real MBIs disallow certain characters, but we'll be conservative here for simplicity's
      // sake, and so we can easily test against synthetic MBIs.

      // See the following link for more info on the MBI format
      // https://www.cms.gov/medicare/new-medicare-card/understanding-the-mbi-with-format.pdf
      Pattern.compile("(?:\\d\\p{Alpha}\\p{Alnum}){2}\\d\\p{Alpha}{2}\\d{2}");

  /** {@inheritDoc} */
  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException {

    /*
     * This should be cleared by Jetty via the request log handling but just in case we have a situation where
     * that handler does not fire (say, due to a Jetty defect) clear it now before the request starts.
     */
    BfdMDC.clear();
    ContentCachingRequestWrapper reqWrapper = new ContentCachingRequestWrapper(request);
    ContentCachingResponseWrapper resWrapper = new ContentCachingResponseWrapper(response);
    // Requests aren't cached until their parameters have been accessed.
    reqWrapper.getParameterMap();

    handleRequest(reqWrapper);
    try {
      chain.doFilter(reqWrapper, resWrapper);
      // The original response will not return with its body unless this method is called.
      resWrapper.copyBodyToResponse();
    } catch (EOFException e) {
      /*
       * The EOFException is a checked exception and is expected when the response body's GZIP stream has reached its end.
       * We can't control whether external filters or applications continue to read the streamed response body
       * after its completion, so this is the best we can do.
       */
      LOGGER.debug("End of stream", e);
    } catch (IOException e) {
      /*
       * The IOException is a checked exception and will be thrown whenever the response body's GZIP stream is interrupted.
       * This can occur when the ASG is scaling down (like after a load test), or if the client cancels a request mid-stream.
       * There isn't much we can do on our end aside from catching it.
       */
      LOGGER.debug("Tried closing stream", e);
    } finally {
      handleResponse(reqWrapper, resWrapper);
    }
  }

  /**
   * Handles populating the MDC logger for http requests.
   *
   * @param request the {@link HttpServletRequest} to record the standard {@link BfdMDC} entries for
   */
  private void handleRequest(HttpServletRequest request) {
    request.setAttribute(BfdMDC.REQUEST_START_KEY, System.currentTimeMillis());

    // Record the request type.
    BfdMDC.put(BfdMDC.computeMDCKey(MDC_PREFIX, "request_type"), request.getClass().getName());

    // Record the basic request components.
    CanonicalOperation operation =
        new CanonicalOperation(CanonicalOperation.Endpoint.matchByHttpUri(request));
    BfdMDC.put(
        BfdMDC.computeMDCKey(MDC_PREFIX, REQUEST_PREFIX, "http_method"), request.getMethod());
    String url = replaceAllMbis(request.getRequestURL().toString());
    BfdMDC.put(BfdMDC.computeMDCKey(MDC_PREFIX, REQUEST_PREFIX, "url"), url);

    // There shouldn't be any MBIs in the URI or URL, but we can't prevent someone from including
    // one accidentally
    String uri = replaceAllMbis(request.getRequestURI());
    BfdMDC.put(BfdMDC.computeMDCKey(MDC_PREFIX, REQUEST_PREFIX, "uri"), uri);

    String queryString = replaceAllMbis(request.getQueryString());

    BfdMDC.put(BfdMDC.computeMDCKey(MDC_PREFIX, REQUEST_PREFIX, "query_string"), queryString);
    BfdMDC.put(
        BfdMDC.computeMDCKey(MDC_PREFIX, REQUEST_PREFIX, "clientSSL", "DN"),
        getClientSslPrincipalDistinguishedName(request));

    // Record the request headers.
    Enumeration<String> headerNames = request.getHeaderNames();
    while (headerNames.hasMoreElements()) {
      String headerName = headerNames.nextElement();
      List<String> headerValues = Collections.list(request.getHeaders(headerName));
      if (headerValues.isEmpty())
        BfdMDC.put(BfdMDC.computeMDCKey(MDC_PREFIX, REQUEST_PREFIX, "header", headerName), "");
      else if (headerValues.size() == 1)
        BfdMDC.put(
            BfdMDC.computeMDCKey(MDC_PREFIX, REQUEST_PREFIX, "header", headerName),
            replaceAllMbis(headerValues.getFirst()));
      else
        BfdMDC.put(
            BfdMDC.computeMDCKey(MDC_PREFIX, REQUEST_PREFIX, "header", headerName),
            replaceAllMbis(headerValues.toString()));
    }

    // Publish the Operation name for monitoring systems.
    operation.publishOperationName();
  }

  /**
   * Replace all MBIs with placeholders.
   *
   * @param input input string
   * @return modified input
   */
  private String replaceAllMbis(String input) {
    Matcher matcher = MBI_REGEX.matcher(input);
    while (matcher.find()) {
      input = input.replace(matcher.group(), OBFUSCATED_MBI);
    }
    return input;
  }

  /**
   * Handles populating the MDC logger for http responses.
   *
   * @param request the {@link HttpServletResponse} to record the standard {@link BfdMDC} entries
   *     for
   * @param response the {@link HttpServletResponse} to record the standard {@link BfdMDC} entries
   *     for
   */
  private void handleResponse(HttpServletRequest request, HttpServletResponse response) {
    /*
     * Capture the payload size in MDC. This Jetty specific call is the same one that is used by the
     * CustomRequestLog to write the payload size to the access.log:
     * org.eclipse.jetty.server.CustomRequestLog.logBytesSent().
     */
    try {
      BfdMDC.put(
          BfdMDC.computeMDCKey(MDC_PREFIX, RESPONSE_PREFIX, "status"),
          Integer.toString(response.getStatus()));
      // Record the response headers.
      Collection<String> headerNames = response.getHeaderNames();
      for (String headerName : headerNames) {
        Collection<String> headerValues = response.getHeaders(headerName);
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
      String contentLength = response.getHeader("Content-Length");
      Long outputSizeInBytes = Long.valueOf(contentLength);
      BfdMDC.put(
          BfdMDC.HTTP_ACCESS_RESPONSE_OUTPUT_SIZE_IN_BYTES, String.valueOf(outputSizeInBytes));

      // Record the response duration.
      Long requestStartMilliseconds = (Long) request.getAttribute(BfdMDC.REQUEST_START_KEY);
      if (requestStartMilliseconds != null) {
        Long responseDurationInMilliseconds = System.currentTimeMillis() - requestStartMilliseconds;
        BfdMDC.put(
            BfdMDC.computeMDCKey(BfdMDC.HTTP_ACCESS_RESPONSE_DURATION_MILLISECONDS),
            Long.toString(responseDurationInMilliseconds));

        if (outputSizeInBytes != 0 && responseDurationInMilliseconds != 0) {
          Long responseDurationPerKB =
              ((1024 * responseDurationInMilliseconds) / outputSizeInBytes);
          BfdMDC.put(
              BfdMDC.HTTP_ACCESS_RESPONSE_DURATION_PER_KB, String.valueOf(responseDurationPerKB));
        } else {
          BfdMDC.put(BfdMDC.HTTP_ACCESS_RESPONSE_DURATION_PER_KB, null);
        }
      } else {
        BfdMDC.put(BfdMDC.HTTP_ACCESS_RESPONSE_DURATION_PER_KB, null);
      }
      LOGGER_HTTP_ACCESS.info("response complete");
    } finally {
      BfdMDC.clear();
    }
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
      LOGGER.debug("No client SSL principal available: {}", clientCert);
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
        (X509Certificate[]) request.getAttribute("jakarta.servlet.request.X509Certificate");
    if (certs == null || certs.length == 0) {
      LOGGER.debug("No client certificate found for request.");
      return null;
    }
    return certs[certs.length - 1];
  }
}
