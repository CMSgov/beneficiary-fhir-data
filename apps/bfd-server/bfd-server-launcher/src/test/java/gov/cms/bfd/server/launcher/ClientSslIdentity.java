package gov.cms.bfd.server.launcher;

import java.io.File;
import java.nio.file.Path;

/**
 * Enumerates the {@link ClientSslIdentity}s available for use when connecting to the
 * development/test application.
 */
public enum ClientSslIdentity {
  /** The Trusted client ssl identity path. */
  TRUSTED(ServerTestUtils.getSslStoresDirectory().resolve("client-trusted-keystore.pfx")),

  /** The Untrusted client ssl identity path. */
  UNTRUSTED(ServerTestUtils.getSslStoresDirectory().resolve("client-untrusted-keystore.pfx"));

  /** The identity key store. */
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
   * Gets the {@link #identityKeyStore}.
   *
   * @return the {@link File} path to the client SSL identity key store to use
   */
  public File getKeyStore() {
    return identityKeyStore.toFile();
  }

  /**
   * Gets the password to use for the key store in {@link #identityKeyStore}.
   *
   * @return the password
   */
  public char[] getStorePassword() {
    // Note: hard-coded for now, as all key stores use the same one.
    return "changeit".toCharArray();
  }

  /**
   * Gets the password to use for the private key in {@link #identityKeyStore}.
   *
   * @return the password
   */
  public char[] getKeyPass() {
    // Note: hard-coded for now, as all key stores use the same one.
    return "changeit".toCharArray();
  }
}
