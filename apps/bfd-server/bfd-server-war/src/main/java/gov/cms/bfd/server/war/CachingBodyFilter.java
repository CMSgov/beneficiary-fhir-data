package gov.cms.bfd.server.war;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

/** Testing. */
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CachingBodyFilter implements Filter {
  /** Logger that logs. */
  private static final Logger LOGGER_MISC = LoggerFactory.getLogger(CachingBodyFilter.class);

  /** {@inheritDoc} */
  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
    ContentCachingRequestWrapper reqWrapper =
        new ContentCachingRequestWrapper((HttpServletRequest) request);
    ContentCachingResponseWrapper resWrapper =
        new ContentCachingResponseWrapper((HttpServletResponse) response);
    try {
      chain.doFilter(reqWrapper, resWrapper);
    } catch (IOException | ServletException e) {
      LOGGER_MISC.error("Error extracting body", e);
    }
  }
}
