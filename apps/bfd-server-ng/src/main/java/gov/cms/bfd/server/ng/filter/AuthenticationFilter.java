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
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authenticates each request by checking if a certificate alias was extracted from the request. If
 * not, the request is denied.
 */
@Component
@RequiredArgsConstructor
// This should run directly after the metadata filter
@Order(2)
@WebFilter(filterName = "AuthenticationFilter")
public class AuthenticationFilter extends OncePerRequestFilter {
  private static final String MISSING_INVALID_HEADER_MSG = "Missing or invalid certificate header.";

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
    // An empty certAlias implies that the upstream ExtractMetadataFilter did not find a matching
    // certificate. For non-local requests, this means the request is not authorized
    final var certAlias = certificateUtil.getAliasAttribute(request);
    if (certAlias.isEmpty()) {
      response.sendError(401, MISSING_INVALID_HEADER_MSG);
      return;
    }

    filterChain.doFilter(request, response);
  }
}
