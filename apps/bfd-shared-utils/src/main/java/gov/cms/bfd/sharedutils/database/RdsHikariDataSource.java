package gov.cms.bfd.sharedutils.database;

import com.google.common.annotations.VisibleForTesting;
import com.zaxxer.hikari.HikariDataSource;
import java.time.Clock;
import javax.annotation.concurrent.GuardedBy;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.model.GenerateAuthenticationTokenRequest;

/**
 * An {@link HikariDataSource} subclass that looks up its password by calling the RDS API instead of
 * always returning a fixed password. Tokens are cached briefly to avoid making a large number of
 * simultaneous calls when the application is first started.
 */
@RequiredArgsConstructor
public class RdsHikariDataSource extends HikariDataSource {
  /** Settings used to build token requests. */
  private final Config config;

  /** Client used to submit token requests. */
  private final RdsClient rdsClient;

  /**
   * Because we are subclassing {@link HikariDataSource} its safest not to use a synchronized method
   * to synchronize token updates. Instead we synchronize on this private object.
   */
  private final Object tokenLock = new Object();

  /** Time in millis at which we consider our token to have expired. */
  @GuardedBy("tokenLock")
  @Getter(AccessLevel.PACKAGE)
  @VisibleForTesting
  private long expires;

  /** Most recently generated token. */
  @GuardedBy("tokenLock")
  @Getter
  private String token;

  /**
   * Overrides default method to return a token obtained from RDS.
   *
   * @return a token from RDS
   */
  @Override
  public String getPassword() {
    synchronized (tokenLock) {
      long now = config.currentTimeMillis();
      if (token == null || now > expires) {
        final var tokenRequest = config.createTokenRequest();
        token = rdsClient.utilities().generateAuthenticationToken(tokenRequest);
        expires = now + config.tokenTtlMillis;
      }
      return token;
    }
  }

  @Override
  public void close() {
    try {
      rdsClient.close();
    } finally {
      super.close();
    }
  }

  /** Immutable configuration settings used when generating token requests. */
  @Data
  @Builder
  public static class Config {
    /** Used to get the current time. */
    private final Clock clock;

    /** Minimum time between token requests. */
    private final long tokenTtlMillis;

    /** IAM user id used in the token requests and to authenticate to the database. */
    private final String databaseUser;

    /** Database instance/host id. */
    private final String databaseHost;

    /** Database port. */
    private final int databasePort;

    /**
     * Gets the current time in milliseconds.
     *
     * @return current time millis
     */
    private long currentTimeMillis() {
      return clock.millis();
    }

    /**
     * Create a new token request object.
     *
     * @return the token request object
     */
    private GenerateAuthenticationTokenRequest createTokenRequest() {
      return GenerateAuthenticationTokenRequest.builder()
          .username(databaseUser)
          .hostname(databaseHost)
          .port(databasePort)
          .build();
    }
  }
}
