package gov.cms.bfd.server.ng.util;

import gov.cms.bfd.server.ng.Configuration;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/** Utility for handling server certificates. */
@RequiredArgsConstructor
@Component
public class CertificateUtil {
  private final Configuration configuration;
  private final Environment environment;

  private static final String LEAF_CERT_HEADER = "X-Amzn-Mtls-Clientcert";
  private static final String CLIENT_CERT_ALIAS_ATTRIBUTE = "CLIENT_CERT_ALIAS";

  /**
   * Returns whether the current configuration is allowed to bypass auth.
   *
   * @return boolean
   */
  public boolean canBypassAuth() {
    return Arrays.stream(environment.getActiveProfiles())
        .allMatch(Configuration::canProfileBypassAuth);
  }

  /**
   * Returns the cert alias from the request, if found.
   *
   * @param request request
   * @return cert alias
   */
  public Optional<String> getAliasFromCert(@NotNull HttpServletRequest request) {
    final var rawLeafCert = request.getHeader(LEAF_CERT_HEADER);
    if (rawLeafCert == null) {
      return Optional.empty();
    }
    // We need to replace these characters with their URL-encoding counterparts because AWS
    // considers them "safe" and therefore does not encode them when sending the leaf certificate
    // from the client certificate in the header. So, when we try to URL Decode them, they get lost.
    final var encodedLeafCert =
        rawLeafCert.replace("+", "%2b").replace("=", "%3d").replace("/", "%2f");
    var leafCert =
        StringUtils.deleteWhitespace(URLDecoder.decode(encodedLeafCert, StandardCharsets.UTF_8));

    final var clientCertsToAliases = configuration.getClientCertsToAliases();
    return Optional.ofNullable(clientCertsToAliases.getOrDefault(leafCert, null));
  }

  /**
   * Attaches the alias to the request, so it can be reused.
   *
   * @param request request
   * @param certAlias alias
   */
  public void attachCertAliasToRequest(@NotNull HttpServletRequest request, String certAlias) {
    request.setAttribute(CLIENT_CERT_ALIAS_ATTRIBUTE, certAlias);
  }

  /**
   * Gets the alias from the request attribute if it was set previously.
   *
   * @param request request
   * @return alias
   */
  public Optional<String> getAliasAttribute(@NotNull HttpServletRequest request) {
    var alias = request.getAttribute(CLIENT_CERT_ALIAS_ATTRIBUTE);
    return Optional.ofNullable(alias).map(a -> (String) a);
  }
}
