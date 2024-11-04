package gov.cms.bfd.server.war.commons;

import jakarta.servlet.http.HttpServletRequest;
import java.security.cert.X509Certificate;
import javax.security.auth.x500.X500Principal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/***
 * test.
 */
public class ClientCertificateUtils {
  /** The logger for this filter. */
  private static final Logger LOGGER = LoggerFactory.getLogger(ClientCertificateUtils.class);

  /**
   * Gets the {@link X500Principal#getName()} for the client certificate if available.
   *
   * @param request the {@link HttpServletRequest} to get the client principal DN (if any) for
   * @return the {@link X500Principal#getName()} for the client certificate, or <code>null</code> if
   *     that's not available
   */
  public static String getClientSslPrincipalDistinguishedName(HttpServletRequest request) {
    /*
     * Note: Now that Wildfly/JBoss is properly configured with a security realm,
     * this method is equivalent to calling `request.getRemoteUser()`.
     */
    X509Certificate clientCert = getClientCertificate(request);
    if (clientCert == null || clientCert.getSubjectX500Principal() == null) {
      LOGGER.debug("No client SSL principal available: {}", clientCert);
      return null;
    }

    return clientCert.getSubjectX500Principal().getName();
  }

  /**
   * Gets the {@link X509Certificate} for the {@link HttpServletRequest}'s client SSL certificate if
   * available.
   *
   * @param request the {@link HttpServletRequest} to get the client SSL certificate for
   * @return the {@link X509Certificate} for the {@link HttpServletRequest}'s client SSL
   *     certificate, or <code>null</code> if that's not available
   */
  private static X509Certificate getClientCertificate(HttpServletRequest request) {
    X509Certificate[] certs =
        (X509Certificate[]) request.getAttribute("jakarta.servlet.request.X509Certificate");
    if (certs == null || certs.length == 0) {
      LOGGER.debug("No client certificate found for request.");
      return null;
    }
    return certs[certs.length - 1];
  }
}
