package gov.cms.bfd.server.war;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.http.HttpStatus;
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
  private static final Logger LOGGER_MISC = LoggerFactory.getLogger(CachingBodyFilter.class);

  /** {@inheritDoc} */
  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain) {
    ContentCachingRequestWrapper reqWrapper = new ContentCachingRequestWrapper(request);
    ContentCachingResponseWrapper resWrapper = new ContentCachingResponseWrapper(response);
    try {
      chain.doFilter(reqWrapper, resWrapper);
      if (!resWrapper.isCommitted() && resWrapper.getStatus() == HttpStatus.SC_OK) {
        resWrapper.copyBodyToResponse();
      }
    } catch (IOException | ServletException e) {
      LOGGER_MISC.error("Error extracting body", e.getStackTrace().toString());
    }
  }
}
