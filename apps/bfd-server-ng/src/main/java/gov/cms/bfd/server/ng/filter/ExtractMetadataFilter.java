package gov.cms.bfd.server.ng.filter;

import com.google.common.base.Strings;
import gov.cms.bfd.server.ng.util.CertificateUtil;
import gov.cms.bfd.server.ng.util.LoggerConstants;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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

    if (servletRequest instanceof HttpServletRequest httpRequest
        && servletResponse instanceof HttpServletResponse httpResponse) {
      final var certAlias = certificateUtil.getAliasFromCert(httpRequest);
      final var uri = httpRequest.getRequestURI();
      var isPatientMatchRequest = uri != null && uri.contains("/Patient/$idi-match");
      if (certAlias.isPresent()) {
        certificateUtil.attachCertAliasToRequest(httpRequest, certAlias.get());
        MDC.put(LoggerConstants.MDC_CERTIFICATE_ALIAS, certAlias.get());
      }

      var clientIp = httpRequest.getHeader(LoggerConstants.CLIENT_IP_HEADER);
      var clientName = httpRequest.getHeader(LoggerConstants.CLIENT_NAME_HEADER);
      var clientId = httpRequest.getHeader(LoggerConstants.CLIENT_ID_HEADER);

      if (isPatientMatchRequest
          && (Strings.isNullOrEmpty(clientIp)
              || Strings.isNullOrEmpty(clientName)
              || Strings.isNullOrEmpty(clientId))) {
        httpResponse.sendError(
            HttpServletResponse.SC_BAD_REQUEST,
            "Missing Required Headers: X-CLIENT-IP, X-CLIENT-NAME, X-CLIENT-ID");
        return;
      }

      MDC.put(LoggerConstants.MDC_URI_KEY, uri);
      MDC.put(LoggerConstants.MDC_REQUEST_ID_KEY, httpRequest.getRequestId());
      MDC.put(LoggerConstants.MDC_REMOTE_ADDRESS_KEY, httpRequest.getRemoteAddr());
      MDC.put(LoggerConstants.MDC_CLIENT_IP_KEY, clientIp);
      MDC.put(LoggerConstants.MDC_CLIENT_NAME_KEY, clientName);
      MDC.put(LoggerConstants.MDC_CLIENT_ID_KEY, clientId);
    }

    filterChain.doFilter(servletRequest, servletResponse);

    // Clean up to prevent leaks
    MDC.clear();
  }
}
