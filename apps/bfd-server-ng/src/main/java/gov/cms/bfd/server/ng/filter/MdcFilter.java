package gov.cms.bfd.server.ng.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Filter for attaching MDC properties. */
@Component
@RequiredArgsConstructor
// Ensure this runs first
@Order(1)
@WebFilter(filterName = "MdcFilter")
public class MdcFilter implements Filter {
  /** Name of the URI key for logging. */
  public static final String URI_KEY = "uri";

  /** Name of the request ID key for logging. */
  public static final String REQUEST_ID_KEY = "requestId";

  /** Name of the remote address key for logging. */
  public static final String REMOTE_ADDRESS_KEY = "remoteAddress";

  @Override
  public void doFilter(
      ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
      throws IOException, ServletException {

    if (servletRequest instanceof HttpServletRequest httpRequest) {
      MDC.put(URI_KEY, httpRequest.getRequestURI());
      MDC.put(REQUEST_ID_KEY, httpRequest.getRequestId());
      MDC.put(REMOTE_ADDRESS_KEY, httpRequest.getRemoteAddr());
    }
    filterChain.doFilter(servletRequest, servletResponse);

    // Clean up to prevent leaks
    MDC.remove(URI_KEY);
    MDC.remove(REQUEST_ID_KEY);
    MDC.remove(REMOTE_ADDRESS_KEY);
  }
}
