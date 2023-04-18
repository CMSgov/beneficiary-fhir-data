package gov.cms.bfd.server.war;

import gov.cms.bfd.server.sharedutils.BfdMDC;
import java.io.EOFException;
import java.io.IOException;
import java.util.Collection;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

/** Testing. */
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CachingBodyFilter extends OncePerRequestFilter {
  /** Logger that logs. */
  private static final Logger LOGGER = LoggerFactory.getLogger(CachingBodyFilter.class);

  /** Log http access. */
  private static final Logger LOGGER_HTTP_ACCESS = LoggerFactory.getLogger("HTTP_ACCESS");
  /** Used to compute the MDC key. */
  private static final String MDC_PREFIX = "http_access";
  /** Prefix for requests. */
  private static final String REQUEST_PREFIX = "request";
  /** Prefix for responses. */
  private static final String RESPONSE_PREFIX = "response";

  /** {@inheritDoc} */
  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    ContentCachingRequestWrapper reqWrapper = new ContentCachingRequestWrapper(request);
    ContentCachingResponseWrapper resWrapper = new ContentCachingResponseWrapper(response);
    reqWrapper.getParameterMap(); // needed for caching!!
    try {
      chain.doFilter(reqWrapper, resWrapper);
      resWrapper.copyBodyToResponse();
    } catch (EOFException e) {
      LOGGER.info("End of stream", "" + e);
    } catch (IOException e) {
      LOGGER.info("Tried closing stream", "" + e);
    } finally {
      BfdMDC.put(
          BfdMDC.computeMDCKey(MDC_PREFIX, RESPONSE_PREFIX, "status"),
          Integer.toString(resWrapper.getStatus()));
      // Record the response headers.
      Collection<String> headerNames = resWrapper.getHeaderNames();
      for (String headerName : headerNames) {
        Collection<String> headerValues = resWrapper.getHeaders(headerName);
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
      BfdMDC.clear();
    }
  }
}
