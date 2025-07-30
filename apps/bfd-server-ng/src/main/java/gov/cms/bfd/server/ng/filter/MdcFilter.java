package gov.cms.bfd.server.ng.filter;

import gov.cms.bfd.server.ng.CertificateUtil;
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
// This should run directly after the auth filter
@Order(2)
@WebFilter(filterName = "MdcFilter")
public class MdcFilter implements Filter {
  private final CertificateUtil certificateUtil;
  private static final String URI = "uri";
  private static final String REQUEST_ID = "requestId";
  private static final String CLIENT = "client";
  private static final String REMOTE_ADDRESS = "remoteAddress";

  @Override
  public void doFilter(
      ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
      throws IOException, ServletException {

    if (servletRequest instanceof HttpServletRequest httpRequest) {
      MDC.put(URI, httpRequest.getRequestURI());
      MDC.put(REQUEST_ID, httpRequest.getRequestId());
      MDC.put(REMOTE_ADDRESS, httpRequest.getRemoteAddr());
      var aliasAttribute = certificateUtil.getAliasAttribute(httpRequest);
      aliasAttribute.ifPresent((attr) -> MDC.put(CLIENT, attr));
    }
    filterChain.doFilter(servletRequest, servletResponse);

    // Clean up to prevent leaks
    MDC.remove(URI);
    MDC.remove(REQUEST_ID);
    MDC.remove(CLIENT);
    MDC.remove(REMOTE_ADDRESS);
  }
}
