package gov.cms.bfd.server.ng.filter;

import gov.cms.bfd.server.ng.util.CertificateUtil;
import gov.cms.bfd.server.ng.util.LoggerConstants;
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

/**
 * Filter for extracting request "metadata" and attaching it to MDC properties and request
 * attributes.
 */
@Component
@RequiredArgsConstructor
// Ensure this runs first
@Order(1)
@WebFilter(filterName = "ExtractMetadataFilter")
public class ExtractMetadataFilter implements Filter {

  private final CertificateUtil certificateUtil;

  @Override
  public void doFilter(
      ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
      throws IOException, ServletException {

    if (servletRequest instanceof HttpServletRequest httpRequest) {
      final var certAlias = certificateUtil.getAliasFromCert(httpRequest);
      if (certAlias.isPresent()) {
        certificateUtil.attachCertAliasToRequest(httpRequest, certAlias.get());
        MDC.put(LoggerConstants.CERTIFICATE_ALIAS, certAlias.get());
      }

      MDC.put(LoggerConstants.URI_KEY, httpRequest.getRequestURI());
      MDC.put(LoggerConstants.REQUEST_ID_KEY, httpRequest.getRequestId());
      MDC.put(LoggerConstants.REMOTE_ADDRESS_KEY, httpRequest.getRemoteAddr());
    }

    filterChain.doFilter(servletRequest, servletResponse);

    // Clean up to prevent leaks
    MDC.remove(LoggerConstants.CERTIFICATE_ALIAS);
    MDC.remove(LoggerConstants.URI_KEY);
    MDC.remove(LoggerConstants.REQUEST_ID_KEY);
    MDC.remove(LoggerConstants.REMOTE_ADDRESS_KEY);
  }
}
