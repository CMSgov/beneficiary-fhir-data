package gov.cms.bfd.server.war;

import java.io.IOException;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
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
import org.springframework.core.annotation.Order;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

/** Testing. */
@Order(1)
public class CachingBodyFilter implements Filter {

  /** Logger that logs. */
  private static final Logger LOGGER_MISC = LoggerFactory.getLogger(CachingBodyFilter.class);

  /** {@inheritDoc} */
  @Override
  public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) {
    ContentCachingRequestWrapper reqWrapper =
        new ContentCachingRequestWrapper((HttpServletRequest) req);
    ContentCachingResponseWrapper resWrapper =
        new ContentCachingResponseWrapper((HttpServletResponse) res);
    if (!(res instanceof ContentCachingResponseWrapper)) {

      try {
        chain.doFilter(reqWrapper, resWrapper);
        if (reqWrapper.isAsyncStarted()) {
          reqWrapper
              .getAsyncContext()
              .addListener(
                  new AsyncListener() {
                    public void onComplete(AsyncEvent asyncEvent) throws IOException {
                      resWrapper.copyBodyToResponse();
                    }

                    public void onTimeout(AsyncEvent asyncEvent) throws IOException {}

                    public void onError(AsyncEvent asyncEvent) throws IOException {}

                    public void onStartAsync(AsyncEvent asyncEvent) throws IOException {}
                  });
        } else {
          resWrapper.copyBodyToResponse();
        }
      } catch (IOException | ServletException e) {
        LOGGER_MISC.error("Error extracting body", e);
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
