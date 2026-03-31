package gov.cms.bfd.server.ng.filter;

import gov.cms.bfd.server.ng.Configuration;
import gov.cms.bfd.server.ng.util.CertificateUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Rejects the request if the URI is in the denylist and the certificate is not on the allowlist.
 */
@Component
@RequiredArgsConstructor
// This should run directly after the metadata filter
@Order(2)
public class AllowedUriFilter extends OncePerRequestFilter {
  private static final String NOT_ALLOWED_MSG = "Resource not allowed.";

  private static final Logger LOGGER = LoggerFactory.getLogger(AllowedUriFilter.class);

  private final CertificateUtil certificateUtil;
  private final Configuration configuration;

  @Override
  public void doFilterInternal(
      @NotNull HttpServletRequest request,
      @NotNull HttpServletResponse response,
      @NotNull FilterChain filterChain)
      throws IOException, ServletException {
    // An empty certAlias implies that the upstream ExtractMetadataFilter did not find a matching
    // certificate. For non-local requests, this means the request is not authorized
    final var certAlias = certificateUtil.getAliasAttribute(request).orElse("");

    var internalCerts = configuration.getInternalCertificateAliases();
    var disabledUris = configuration.getDisabledUris();
    var requestUri = request.getRequestURI();
    var isUriDisabled = disabledUris.stream().anyMatch(requestUri::startsWith);
    if (isUriDisabled) {
      LOGGER
          .atInfo()
          .setMessage("request URI disabled")
          .addKeyValue("requestUri", requestUri)
          .addKeyValue("disabledUris", disabledUris)
          .addKeyValue("internalCerts", internalCerts)
          .log();
    }
    if (!internalCerts.contains(certAlias) && isUriDisabled) {
      LOGGER.warn("Disallowing disabled URI");
      response.sendError(401, NOT_ALLOWED_MSG);
      return;
    }

    filterChain.doFilter(request, response);
  }
}
