package gov.cms.bfd.server.launcher;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.jetty.security.AbstractLoginService;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.security.RolePrincipal;
import org.eclipse.jetty.security.UserPrincipal;
import org.eclipse.jetty.util.security.Credential;

/** A Jetty {@link LoginService} for mutual TLS. */
public class ClientCertLoginService extends AbstractLoginService {
  /** {@inheritDoc} */
  @Override
  public String getName() {
    return "bfd-realm";
  }

  /** {@inheritDoc} */
  @Override
  protected UserPrincipal loadUserInfo(String username) {
    return new UserPrincipal(username, new ClientCertCredential());
  }

  /** {@inheritDoc} */
  @Override
  protected List<RolePrincipal> loadRoleInfo(UserPrincipal user) {
    return new ArrayList<>();
  }

  /**
   * A {@link Credential} implementation for the {@link ClientCertLoginService}'s {@link
   * UserPrincipal}s.
   */
  private static final class ClientCertCredential extends Credential {
    private static final long serialVersionUID = 1L;

    /** {@inheritDoc} */
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
