package gov.cms.bfd.server.launcher;

import org.eclipse.jetty.security.AbstractLoginService;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.util.security.Credential;

/** A Jetty {@link LoginService} for mutual TLS. */
public class ClientCertLoginService extends AbstractLoginService {
  /** @see org.eclipse.jetty.security.AbstractLoginService#getName() */
  @Override
  public String getName() {
    return "bfd-realm";
  }

  /** @see org.eclipse.jetty.security.AbstractLoginService#loadUserInfo(java.lang.String) */
  @Override
  protected UserPrincipal loadUserInfo(String username) {
    return new UserPrincipal(username, new ClientCertCredential());
  }

  /**
   * @see
   *     org.eclipse.jetty.security.AbstractLoginService#loadRoleInfo(org.eclipse.jetty.security.AbstractLoginService.UserPrincipal)
   */
  @Override
  protected String[] loadRoleInfo(UserPrincipal user) {
    return new String[] {};
  }

  /**
   * A {@link Credential} implementation for the {@link ClientCertLoginService}'s {@link
   * UserPrincipal}s.
   */
  private static final class ClientCertCredential extends Credential {
    private static final long serialVersionUID = 1L;

    /** @see org.eclipse.jetty.util.security.Credential#check(java.lang.Object) */
    @Override
    public boolean check(Object credentials) {
      /*
       * All of the authentication was done by the server's SSLContextFactory, which we can trust.
       * No need for an extra (silly) secret handshake here.
       */
      return true;
    }
  }
}
