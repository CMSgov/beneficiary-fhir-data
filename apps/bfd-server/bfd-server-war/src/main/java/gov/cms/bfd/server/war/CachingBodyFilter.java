package gov.cms.bfd.server.war;

import gov.cms.bfd.server.sharedutils.BfdMDC;
import java.io.EOFException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
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
      logResponse(reqWrapper, resWrapper);
    } catch (EOFException e) {
      LOGGER.info("End of stream", "" + e);
    } finally {
      resWrapper.copyBodyToResponse();
    }
  }

  private void logResponse(
      ContentCachingRequestWrapper request, ContentCachingResponseWrapper response)
      throws UnsupportedEncodingException {
    try {
      /*
       * Capture the payload size in MDC. This Jetty specific call is the same one that is used by the
       * CustomRequestLog to write the payload size to the access.log:
       * org.eclipse.jetty.server.CustomRequestLog.logBytesSent().
       *
       * We capture this field here rather than in the RequestResponsePopulateMdcFilter because we need access to
       * the underlying Jetty classes in the response that are in classes that are not loaded in the war file so not
       * accessible to the filter.
       */
      Long outputSizeInBytes = Long.valueOf(response.getContentSize());
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

      /*
       * Write to the access.json. The message here isn't actually the payload; the MDC context that will get
       * automatically included with it is!
       */
      LOGGER.info("response complete");
    } finally {
      BfdMDC.clear();
    }
  }
}
