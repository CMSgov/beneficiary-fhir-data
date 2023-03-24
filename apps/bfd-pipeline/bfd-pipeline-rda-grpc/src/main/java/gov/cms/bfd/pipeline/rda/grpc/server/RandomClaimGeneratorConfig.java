package gov.cms.bfd.pipeline.rda.grpc.server;

import java.time.Clock;
import lombok.Builder;
import lombok.Data;

/**
 * Configuration settings that define how the {@link AbstractRandomClaimGenerator} instances
 * generate claims.
 */
@Data
@Builder(toBuilder = true)
public class RandomClaimGeneratorConfig {
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

  /**
   * Seed for separate random number generator used in conjunction with {@link #randomErrorRate} to
   * decide when to randomly insert a transformation error into claims.
   */
  @Builder.Default private final long randomErrorSeed = System.currentTimeMillis();

  /** When positive this number indicates the maximum number of unique MBI values to generate. */
  private final int maxUniqueMbis;

  /**
   * When positive this number indicates the maximum number of unique claim id values to generate.
   */
  private final int maxUniqueClaimIds;
}
