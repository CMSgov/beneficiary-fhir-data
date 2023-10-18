package gov.cms.bfd.server.war;

import java.io.File;
import java.nio.file.Path;

/**
 * Enumerates the {@link ClientSslIdentity}s available for use when connecting to the
 * development/test application.
 */
public enum ClientSslIdentity {
  /** The trusted keystore. */
  TRUSTED(ServerTestUtils.getSslStoresDirectory().resolve("client-trusted-keystore.pfx")),
  /** The untrusted keystore. */
  UNTRUSTED(ServerTestUtils.getSslStoresDirectory().resolve("client-untrusted-keystore.pfx"));

  /** The identity keystore. */
  private final Path identityKeyStore;

  /**
   * Enum constant constructor.
   *
   * @param identityKeyStore the value to use for {@link #getKeyStore()}
   */
  private ClientSslIdentity(Path identityKeyStore) {
    this.identityKeyStore = identityKeyStore;
  }

  /**
   * Gets the {@link #identityKeyStore} as a {@link File}.
   *
   * @return the {@link File} path to the client SSL identity key store to use
   */
  public File getKeyStore() {
    return identityKeyStore.toFile();
  }

  /**
   * Get the keystore password.
   *
   * @return the password to use for the key store in {@link #getKeyStore}
   */
  public char[] getStorePassword() {
    // Note: hard-coded for now, as all key stores use the same one.
    return "changeit".toCharArray();
  }

  /**
   * Get private key password.
   *
   * @return the password to use for the private key in {@link #getKeyPass}
   */
  public char[] getKeyPass() {
    // Note: hard-coded for now, as all key stores use the same one.
    return "changeit".toCharArray();
  }
}
