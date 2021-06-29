package gov.cms.bfd.server.launcher;

import java.security.cert.X509Certificate;
import javax.security.auth.x500.X500Principal;
import javax.servlet.ServletRequest;
import org.eclipse.jetty.servlets.DoSFilter;

public class UserAwareDoSFilter extends DoSFilter {

  @Override
  public String extractUserId(ServletRequest request) {
    return getClientSslPrincipalDistinguishedName(request);
  }

  /**
   * @param request the {@link ServletRequest} to get the client principal DN (if any) for
   * @return the {@link X500Principal#getName()} for the client certificate, or <code>null</code> if
   *     that's not available
   */
  private static String getClientSslPrincipalDistinguishedName(ServletRequest request) {
    /*
     * Note: Now that Wildfly/JBoss is properly configured with a security realm,
     * this method is equivalent to calling `request.getRemoteUser()`.
     */
    X509Certificate clientCert = getClientCertificate(request);
    if (clientCert == null || clientCert.getSubjectX500Principal() == null) {
      return null;
    }

    return clientCert.getSubjectX500Principal().getName();
  }

  /**
   * @param request the {@link ServletRequest} to get the client SSL certificate for
   * @return the {@link X509Certificate} for the {@link ServletRequest}'s client SSL certificate, or
   *     <code>null</code> if that's not available
   */
  private static X509Certificate getClientCertificate(ServletRequest request) {
    X509Certificate[] certs =
        (X509Certificate[]) request.getAttribute("javax.servlet.request.X509Certificate");
    if (certs == null || certs.length <= 0) {
      return null;
    }
    return certs[certs.length - 1];
  }
}
