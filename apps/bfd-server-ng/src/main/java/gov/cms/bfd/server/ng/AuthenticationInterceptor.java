package gov.cms.bfd.server.ng;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

/**
 * Authenticates each request by checking the leaf/client certificate provided by the client to see
 * if it is in the trusted certificate configuration map. If not, the request is denied.
 */
@RequiredArgsConstructor
@Interceptor
public class AuthenticationInterceptor {
  private static final String MISSING_INVALID_HEADER_MSG = "Missing or invalid certificate header.";

  private final Configuration configuration;

  /**
   * TODO.
   *
   * @param theRequestDetails TODO
   * @param theRequest TODO
   * @param theResponse TODO
   * @return TODO
   * @throws AuthenticationException TODO
   */
  @Hook(Pointcut.SERVER_INCOMING_REQUEST_POST_PROCESSED)
  public boolean incomingRequestPostProcessed(
      RequestDetails theRequestDetails,
      HttpServletRequest theRequest,
      HttpServletResponse theResponse)
      throws AuthenticationException {
    final var rawLeafCert = theRequest.getHeader("X-Amzn-Mtls-Clientcert");
    System.out.printf("Received raw cert: %s%n", rawLeafCert);
    if (rawLeafCert == null) {
      throw new AuthenticationException(MISSING_INVALID_HEADER_MSG);
    }

    // We need to replace these characters with their URL-encoding counterparts because AWS
    // considers them "safe" and therefore does not encode them when sending the leaf certificate
    // from the client certificate in the header. So, when we try to URL Decode them, they get lost.
    final var encodedLeafCert =
        rawLeafCert.replace("+", "%2b").replace("=", "%3d").replace("/", "%2f");
    System.out.printf("Received encoded cert: %s%n", encodedLeafCert);

    final var clientCertsToAliases = configuration.getClientCertsToAliases();
    final var leafCert =
        StringUtils.deleteWhitespace(URLDecoder.decode(encodedLeafCert, StandardCharsets.UTF_8));
    System.out.printf("Received decoded cert: %s%n", leafCert);

    String certAlias = clientCertsToAliases.getOrDefault(leafCert, null);
    if (certAlias == null) {
      throw new AuthenticationException(MISSING_INVALID_HEADER_MSG);
    }

    System.out.printf("Matched certificate %s%n", certAlias);
    // Return true to allow the request to proceed
    return true;
  }
}
