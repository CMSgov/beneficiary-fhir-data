package gov.cms.bfd.server.war;

import java.io.File;
import java.nio.file.Path;

/**
 * Enumerates the {@link ClientSslIdentity}s available for use when connecting to the
 * development/test application.
 */
public enum ClientSslIdentity {
  TRUSTED(ServerTestUtils.getSslStoresDirectory().resolve("client-trusted-keystore.jks")),

  UNTRUSTED(ServerTestUtils.getSslStoresDirectory().resolve("client-untrusted-keystore.jks"));

  private final Path identityKeyStore;

  /**
   * Enum constant constructor.
   *
   * @param identityKeyStore the value to use for {@link #getKeyStore()}
   */
  private ClientSslIdentity(Path identityKeyStore) {
    this.identityKeyStore = identityKeyStore;
  }

  /** @return the {@link File} path to the client SSL identity key store to use */
  public File getKeyStore() {
    return identityKeyStore.toFile();
  }

  /** @return the password to use for the key store in {@link #getKeyStore()} */
  public char[] getStorePassword() {
    // Note: hard-coded for now, as all key stores use the same one.
    return "changeit".toCharArray();
  }

  /** @return the password to use for the private key in {@link #getKeyPass()} */
  public char[] getKeyPass() {
    // Note: hard-coded for now, as all key stores use the same one.
    return "changeit".toCharArray();
  }
}
