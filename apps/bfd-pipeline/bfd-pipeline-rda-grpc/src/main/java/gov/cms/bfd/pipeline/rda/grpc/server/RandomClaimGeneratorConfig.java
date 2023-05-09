package gov.cms.bfd.pipeline.rda.grpc.server;

import java.io.Serializable;
import java.time.Clock;
import lombok.Builder;
import lombok.Data;

/**
 * Configuration settings that define how the {@link AbstractRandomClaimGenerator} instances
 * generate claims.
 */
@Data
@Builder(toBuilder = true)
public class RandomClaimGeneratorConfig implements Serializable {
  /** The base seed value used for all generated random values. */
  private final long seed;

  /**
   * Denotes if all {@link AbstractRandomClaimGenerator#optional(String, Runnable)} or {@link
   * AbstractRandomClaimGenerator#optionalOneOf(String, Runnable...)} should be executed regardless
   * of random results.
   */
  private final boolean optionalOverride;

  /** The {@link Clock} object to use with generating time based values. */
  @Builder.Default private final Clock clock = Clock.systemUTC();

  /**
   * When positive this number indicates that roughly one claim in this number of claims should
   * contain a randomly inserted transformation error.
   */
  private final int randomErrorRate;

  /** When positive this number indicates the maximum number of unique MBI values to generate. */
  private final int maxUniqueMbis;

  /**
   * When positive this number indicates the maximum number of unique claim id values to generate.
   */
  private final int maxUniqueClaimIds;

  /**
   * When true this causes the random error generation feature to always use the current timestamp
   * instead of using {@link #seed}.
   */
  private final boolean useTimestampForErrorSeed;

  /** Maximum number of claims to return when using random generator as a message source. */
  @Builder.Default private final int maxToSend = Integer.MAX_VALUE;

  /**
   * Gets the appropriate seed value for the random error generation feature. Uses either {@link
   * #seed} or the current time as seed value depending on the {@link #useTimestampForErrorSeed}
   * setting.
   *
   * @return random number generator seed value
   */
  public long getRandomErrorSeed() {
    return useTimestampForErrorSeed ? clock.millis() : seed;
  }
}
