package gov.cms.bfd.server.ng.filter;

import gov.cms.bfd.server.ng.LoggerConstants;
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

  @Override
  public void doFilter(
      ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
      throws IOException, ServletException {

    if (servletRequest instanceof HttpServletRequest httpRequest) {
      MDC.put(LoggerConstants.URI_KEY, httpRequest.getRequestURI());
      MDC.put(LoggerConstants.REQUEST_ID_KEY, httpRequest.getRequestId());
      MDC.put(LoggerConstants.REMOTE_ADDRESS_KEY, httpRequest.getRemoteAddr());
    }
    filterChain.doFilter(servletRequest, servletResponse);

    // Clean up to prevent leaks
    MDC.remove(LoggerConstants.URI_KEY);
    MDC.remove(LoggerConstants.REQUEST_ID_KEY);
    MDC.remove(LoggerConstants.REMOTE_ADDRESS_KEY);
  }
}
