package gov.cms.bfd.server.ng.interceptor;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import gov.cms.bfd.server.ng.Configuration;
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
  private static final String LEAF_CERT_HEADER = "X-Amzn-Mtls-Clientcert";

  private final Configuration configuration;

  /**
   * Interceptor that authenticates the incoming request based upon the certificate provided in the
   * {@value #LEAF_CERT_HEADER} header.
   *
   * <p>This method is invoked at the {@link Pointcut#SERVER_INCOMING_REQUEST_POST_PROCESSED} point.
   * It performs authentication based on the URL-encoded client certificate provided in the {@value
   * #LEAF_CERT_HEADER} header. If the certificate is missing or invalid, an {@link
   * AuthenticationException} is thrown.
   *
   * @param requestDetails the details of the incoming request, including information about the
   *     resource and operation
   * @param request the HTTP servlet request object containing client-specific data such as headers
   * @param response the HTTP servlet response object that can be used to send a response back to
   *     the client if needed
   * @return true to allow the request to proceed; false to prevent the request from being processed
   *     further
   * @throws AuthenticationException if the {@value #LEAF_CERT_HEADER} header is missing or contains
   *     an invalid or unauthorized certificate
   */
  @Hook(Pointcut.SERVER_INCOMING_REQUEST_POST_PROCESSED)
  public boolean authenticateClientCertificate(
      RequestDetails requestDetails, HttpServletRequest request, HttpServletResponse response)
      throws AuthenticationException {
    final var rawLeafCert = request.getHeader(LEAF_CERT_HEADER);
    if (rawLeafCert == null) {
      throw new AuthenticationException(MISSING_INVALID_HEADER_MSG);
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
      throw new AuthenticationException(MISSING_INVALID_HEADER_MSG);
    }

    return true;
  }
}
