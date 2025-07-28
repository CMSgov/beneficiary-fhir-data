package gov.cms.bfd.server.ng.filter;

import gov.cms.bfd.server.ng.Configuration;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authenticates each request by checking the leaf/client certificate provided by the client to see
 * if it is in the trusted certificate configuration map. If not, the request is denied.
 */
@Component
@RequiredArgsConstructor
@WebFilter(filterName = "AuthenticationFilter")
public class AuthenticationFilter extends OncePerRequestFilter {
  private static final String MISSING_INVALID_HEADER_MSG = "Missing or invalid certificate header.";
  private static final String LEAF_CERT_HEADER = "X-Amzn-Mtls-Clientcert";

  private final Configuration configuration;
  private final Environment environment;

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    var path = request.getRequestURI();
    // Some URLs like the Swagger UI shouldn't require auth
    if (Configuration.canUrlBypassAuth(path)) {
      return true;
    }
    return Arrays.stream(environment.getActiveProfiles())
        .allMatch(Configuration::canProfileBypassAuth);
  }

  @Override
  public void doFilterInternal(
      @NotNull HttpServletRequest request,
      @NotNull HttpServletResponse response,
      @NotNull FilterChain filterChain)
      throws IOException, ServletException {

    final var rawLeafCert = request.getHeader(LEAF_CERT_HEADER);
    if (rawLeafCert == null) {
      response.sendError(400, MISSING_INVALID_HEADER_MSG);
      return;
    }

    final var clientCertsToAliases = configuration.getClientCertsToAliases();
    // We need to replace these characters with their URL-encoding counterparts because AWS
    // considers them "safe" and therefore does not encode them when sending the leaf certificate
    // from the client certificate in the header. So, when we try to URL Decode them, they get lost.
    final var encodedLeafCert =
        rawLeafCert.replace("+", "%2b").replace("=", "%3d").replace("/", "%2f");
    final var leafCert =
        StringUtils.deleteWhitespace(URLDecoder.decode(encodedLeafCert, StandardCharsets.UTF_8));

    final var certAlias = clientCertsToAliases.getOrDefault(leafCert, null);
    if (certAlias == null) {
      response.sendError(400, MISSING_INVALID_HEADER_MSG);
      return;
    }

    filterChain.doFilter(request, response);
  }
}
