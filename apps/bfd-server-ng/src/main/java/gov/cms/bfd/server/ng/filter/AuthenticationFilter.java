package gov.cms.bfd.server.ng.filter;

import gov.cms.bfd.server.ng.util.CertificateUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authenticates each request by checking the leaf/client certificate provided by the client to see
 * if it is in the trusted certificate configuration map. If not, the request is denied.
 */
@Component
@RequiredArgsConstructor
// This should run directly after the MDC filter
@Order(2)
@WebFilter(filterName = "AuthenticationFilter")
public class AuthenticationFilter extends OncePerRequestFilter {
  private static final String MISSING_INVALID_HEADER_MSG = "Missing or invalid certificate header.";
  private static final String CLIENT = "client";

  private final CertificateUtil certificateUtil;

  @Override
  protected boolean shouldNotFilter(@NotNull HttpServletRequest request) {
    return certificateUtil.canBypassAuth();
  }

  @Override
  public void doFilterInternal(
      @NotNull HttpServletRequest request,
      @NotNull HttpServletResponse response,
      @NotNull FilterChain filterChain)
      throws IOException, ServletException {

    final var certAlias = certificateUtil.getAliasFromCert(request);

    if (certAlias.isEmpty()) {
      response.sendError(401, MISSING_INVALID_HEADER_MSG);
      return;
    }
    certificateUtil.attachCertAliasToRequest(request, certAlias.get());
    MDC.put(CLIENT, certAlias.get());
    filterChain.doFilter(request, response);

    // Clean up to prevent leaks
    MDC.remove(CLIENT);
  }
}
