package gov.cms.bfd.pipeline.sharedutils;

import com.google.common.annotations.VisibleForTesting;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import org.apache.commons.codec.binary.Hex;

/**
 * Utility class to encapsulate the hash algorithm and settings used to compute secure hash values
 * for ID values such as HICN and MBI. Code adopted from RifLoader. TODO: refactor RifLoader to use
 * and instance of this class.
 */
public class IdHasher {

  private final Config config;
  private final SecretKeyFactory secretKeyFactory;

  public IdHasher(Config config) {
    this.config = config;
    secretKeyFactory = createSecretKeyFactory();
  }

  @VisibleForTesting
  public static IdHasher createInstanceForTestingOnly() {
    return new IdHasher(new Config(1000, "nottherealpepper".getBytes(StandardCharsets.UTF_8)));
  }

  /**
   * Computes a one-way cryptographic hash of the specified ID value.
   *
   * @param mbi any ID to be hashed
   * @return a one-way cryptographic hash of the specified ID value, exactly 64 characters long
   */
  public String computeIdentifierHash(String identifier) {
    try {
      /*
       * Our approach here is NOT using a salt, as salts must be randomly
       * generated for each value to be hashed and then included in
       * plaintext with the hash results. Random salts would prevent the
       * Blue Button API frontend systems from being able to produce equal
       * hashes for the same identifiers. Instead, we use a secret "pepper" that
       * is shared out-of-band with the frontend. This value MUST be kept
       * secret.
       *
       * We are re-using the same pepper between HICNs and MBIs
       */
      byte[] salt = config.hashPepper;

      /*
       * Bigger is better here as it reduces chances of collisions, but
       * the equivalent Python Django hashing functions used by the
       * frontend default to this value, so we'll go with it.
       */
      int derivedKeyLength = 256;

      /* We're reusing the same hicn hash iterations, so the algorithm is exactly the same */
      PBEKeySpec keySpec =
          new PBEKeySpec(identifier.toCharArray(), salt, config.hashIterations, derivedKeyLength);
      SecretKey secret = secretKeyFactory.generateSecret(keySpec);
      String hexEncodedHash = Hex.encodeHexString(secret.getEncoded());

      return hexEncodedHash;
    } catch (InvalidKeySpecException e) {
      throw new BadCodeMonkeyException(e);
    }
  }

  /** @return a new {@link SecretKeyFactory} for the <code>PBKDF2WithHmacSHA256</code> algorithm */
  private SecretKeyFactory createSecretKeyFactory() {
    try {
      return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  /** Configuration options that encapsulates the settings for computing a hash. */
  public static class Config implements Serializable {
    private static final long serialVersionUID = 4911655334835485L;

    private final int hashIterations;
    private final byte[] hashPepper;

    public Config(int hashIterations, byte[] hashPepper) {
      this.hashIterations = hashIterations;
      this.hashPepper = hashPepper;
    }
  }
}
