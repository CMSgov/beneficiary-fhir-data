package gov.cms.bfd.server.ng.filter;

import static gov.cms.bfd.server.ng.util.LoggerConstants.*;

import com.google.common.base.Strings;
import gov.cms.bfd.server.ng.log.RequestTelemetryLogger;
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

  private final RequestTelemetryLogger requestTelemetryLogger;

  @Override
  public void doFilter(
      ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
      throws IOException, ServletException {

    if (servletRequest instanceof HttpServletRequest httpRequest
        && servletResponse instanceof HttpServletResponse httpResponse) {

      requestTelemetryLogger.setRequestStartTime(httpRequest);

      if (isAnInvalidPatientMatchRequest(httpRequest)) {
        httpResponse.sendError(
            HttpServletResponse.SC_BAD_REQUEST,
            "Missing Required Headers: X-CLIENT-IP, X-CLIENT-NAME, X-CLIENT-ID");
        return;
      }

      requestTelemetryLogger.recordRequestHeaders(httpRequest);
    }

    try {
      filterChain.doFilter(servletRequest, servletResponse);
    } finally {
      if (servletRequest instanceof HttpServletRequest httpRequest
          && servletResponse instanceof HttpServletResponse httpResponse) {
        requestTelemetryLogger.recordResponse(httpRequest, httpResponse);
        requestTelemetryLogger.logRequestComplete(httpRequest, httpResponse);
      }
      // Clean up to prevent leaks
      MDC.clear();
    }
  }

  private boolean isAnInvalidPatientMatchRequest(HttpServletRequest request) {
    var uri = request.getRequestURI();
    var isPatientMatchRequest = uri != null && uri.contains("/Patient/$idi-match");

    if (!isPatientMatchRequest) {
      return false;
    }

    var clientIp = request.getHeader(CLIENT_IP_HEADER);
    var clientName = request.getHeader(CLIENT_NAME_HEADER);
    var clientId = request.getHeader(CLIENT_ID_HEADER);

    return Strings.isNullOrEmpty(clientIp)
        || Strings.isNullOrEmpty(clientName)
        || Strings.isNullOrEmpty(clientId);
  }
}
