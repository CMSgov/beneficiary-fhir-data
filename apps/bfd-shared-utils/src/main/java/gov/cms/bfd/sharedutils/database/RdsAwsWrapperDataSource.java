package gov.cms.bfd.sharedutils.database;

import com.google.common.annotations.VisibleForTesting;
import com.zaxxer.hikari.HikariDataSource;
import javax.annotation.concurrent.GuardedBy;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.jdbc.ds.AwsWrapperDataSource;

/** Testing. */
@RequiredArgsConstructor
public class RdsAwsWrapperDataSource extends AwsWrapperDataSource {
  /** Settings used to build token requests. */
  private final RdsHikariDataSource.Config config;

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

  //    @Override
  //    public void close() {
  //        try {
  //            rdsClient.close();
  //        } finally {
  //            super.close();
  //        }
  //    }
}
