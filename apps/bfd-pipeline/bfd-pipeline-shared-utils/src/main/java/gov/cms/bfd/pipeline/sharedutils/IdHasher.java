package gov.cms.bfd.pipeline.sharedutils;

import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.apache.commons.codec.binary.Hex;

/**
 * Utility class to encapsulate the hash algorithm (PBKDF2WithHmacSHA256) and settings (iterations
 * and salt) used to compute secure hashed versions of ID values such as HICN and MBI. Code adopted
 * from RifLoader.
 */
public class IdHasher {
  /**
   * Key length for hashing.
   *
   * <p>Bigger is better here as it reduces chances of collisions, but the equivalent Python Django
   * hashing functions used by the frontend default to this value, so we'll go with it.
   */
  public static final int DERIVED_KEY_LENGTH = 256;

  /** The configuration. */
  @Getter private final Config config;
  /** The factory for generating secret keys. */
  private final SecretKeyFactory secretKeyFactory;

  /**
   * Instantiates a new {@link IdHasher}.
   *
   * @param config the config for the hasher
   */
  public IdHasher(Config config) {
    this.config = config;
    secretKeyFactory = createSecretKeyFactory();
  }

  /**
   * Computes a one-way cryptographic hash of the specified ID value.
   *
   * @param identifier any ID to be hashed
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
       */
      byte[] salt = config.hashPepper;

      /* We're reusing the same hicn hash iterations, so the algorithm is exactly the same */
      PBEKeySpec keySpec =
          new PBEKeySpec(identifier.toCharArray(), salt, config.hashIterations, DERIVED_KEY_LENGTH);
      SecretKey secret = secretKeyFactory.generateSecret(keySpec);
      String hexEncodedHash = Hex.encodeHexString(secret.getEncoded());

      return hexEncodedHash;
    } catch (InvalidKeySpecException e) {
      throw new BadCodeMonkeyException(e);
    }
  }

  /**
   * Creates a secret key factory.
   *
   * @return a new {@link SecretKeyFactory} for the <code>PBKDF2WithHmacSHA256</code> algorithm
   */
  private SecretKeyFactory createSecretKeyFactory() {
    try {
      return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  /** Configuration options that encapsulates the settings for computing a hash. */
  @Builder(toBuilder = true)
  @AllArgsConstructor
  public static class Config implements Serializable {
    private static final long serialVersionUID = 4911655334835485L;
    /** The default cache size. */
    private static final int DEFAULT_CACHE_SIZE = 100;

    /** The number of hash iterations to use. */
    @Getter private final int hashIterations;
    /** The hash pepper. */
    private final byte[] hashPepper;
    /** The cache size. */
    @Builder.Default @Getter private final int cacheSize = DEFAULT_CACHE_SIZE;

    /**
     * Instantiates a new Config.
     *
     * @param hashIterations the hash iterations
     * @param hashPepper the hash pepper
     */
    public Config(int hashIterations, byte[] hashPepper) {
      this(hashIterations, hashPepper, DEFAULT_CACHE_SIZE);
    }

    /**
     * Instantiates a new Config.
     *
     * @param hashIterations the hash iterations
     * @param hashPepper the hash pepper
     */
    public Config(int hashIterations, String hashPepper) {
      this(hashIterations, hashPepper.getBytes(StandardCharsets.UTF_8), DEFAULT_CACHE_SIZE);
    }

    /**
     * Gets the {@link #hashPepper}.
     *
     * @return a clone of the hash pepper
     */
    public byte[] getHashPepper() {
      // arrays aren't immutable so it's safest to return a copy
      return hashPepper.clone();
    }

    /**
     * This partial builder implementation will be filled out by lombok to produce the entire
     * builder. We just need to add extra method for setting the hashPepper as either a string.
     */
    public static class ConfigBuilder {
      /** The hash pepper. */
      private byte[] hashPepper;

      /**
       * Uses the hash pepper string to construct a new config builder with that value.
       *
       * @param hashPepper the hash pepper
       * @return the config builder
       */
      public ConfigBuilder hashPepperString(String hashPepper) {
        this.hashPepper = hashPepper.getBytes(StandardCharsets.UTF_8);
        return this;
      }
    }
  }
}
