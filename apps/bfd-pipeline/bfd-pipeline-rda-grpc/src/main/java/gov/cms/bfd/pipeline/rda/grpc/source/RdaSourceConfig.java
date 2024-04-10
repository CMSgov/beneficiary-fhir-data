package gov.cms.bfd.pipeline.rda.grpc.source;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import gov.cms.bfd.model.rda.MessageError;
import io.grpc.CallOptions;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.inprocess.InProcessChannelBuilder;
import jakarta.annotation.Nullable;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/** This class contains the configuration settings specific to the RDA rRPC service. */
@Slf4j
@Getter
@EqualsAndHashCode
public class RdaSourceConfig {

  /** The type of RDA API server to connect to. */
  private final ServerType serverType;

  /** The hostname or IP address of the host running the RDA API. */
  private final String host;

  /** The port on which the RDA API listens for connections. */
  private final int port;

  /** The name used to connect to an in-process mock RDA API server. */
  private final String inProcessServerName;

  /** The maximum time the stream is allowed to remain idle before being automatically closed. */
  private final Duration maxIdle;

  /**
   * Minimum amount of idle time before a dropped connection is considered to be normal behavior by
   * the RDA API server and thus not requiring an ERROR log entry.
   */
  private final Duration minIdleTimeBeforeConnectionDrop;

  /** Authorization token expiration date, in epoch seconds. */
  private final Long expirationDate;

  /** The token to pass to the RDA API server to authenticate the client. */
  @Nullable private final String authenticationToken;

  /** Maximum number of days to retain processed {@link MessageError} records in the database. */
  @Nullable private final Integer messageErrorExpirationDays;

  /**
   * Specifies which type of server we want to connect to. {@code Remote} is the normal
   * configuration. {@code InProcess} is used when populating an environment with synthetic data
   * served by a mock RDA API server running as a pipeline job.
   */
  public enum ServerType {
    /** Represents a remote server, the normal configuration. */
    Remote,
    /**
     * Represents an in-process server, used when populating an environment with synthetic data
     * served by a mock RDA API server running as a pipeline job.
     */
    InProcess
  }

  /**
   * Instantiates a new Rda source config.
   *
   * @param serverType the server type
   * @param host the host
   * @param port the port
   * @param inProcessServerName the in process server name
   * @param maxIdle the max idle
   * @param minIdleTimeBeforeConnectionDrop the min idle time before connection drop
   * @param authenticationToken the authentication token
   * @param messageErrorExpirationDays days until message errors expire
   */
  @Builder
  private RdaSourceConfig(
      ServerType serverType,
      String host,
      int port,
      String inProcessServerName,
      Duration maxIdle,
      @Nullable Duration minIdleTimeBeforeConnectionDrop,
      @Nullable String authenticationToken,
      @Nullable Integer messageErrorExpirationDays) {
    this.serverType = Preconditions.checkNotNull(serverType, "serverType is required");
    this.host = host;
    this.port = port;
    this.inProcessServerName = inProcessServerName;
    this.maxIdle = maxIdle;
    this.minIdleTimeBeforeConnectionDrop =
        minIdleTimeBeforeConnectionDrop == null
            ? Duration.ofMillis(Long.MAX_VALUE)
            : minIdleTimeBeforeConnectionDrop;
    if (serverType == ServerType.Remote) {
      Preconditions.checkArgument(!Strings.isNullOrEmpty(host), "host name is required");
      Preconditions.checkArgument(port >= 1, "port is negative (%s)", port);
    } else {
      Preconditions.checkArgument(
          !Strings.isNullOrEmpty(inProcessServerName), "inProcessServerName is required");
    }

    Preconditions.checkArgument(maxIdle.toMillis() >= 1_000, "maxIdle less than 1 second");

    if (!Strings.isNullOrEmpty(authenticationToken)) {
      this.authenticationToken = authenticationToken;
      this.expirationDate = parseJWTExpirationDate(this.authenticationToken);
    } else {
      this.authenticationToken = null;
      this.expirationDate = null;
    }
    this.messageErrorExpirationDays = messageErrorExpirationDays;
  }

  /**
   * Creates a managed channel.
   *
   * @return the managed channel
   */
  public ManagedChannel createChannel() {
    return createChannelBuilder()
        .idleTimeout(maxIdle.toMillis(), TimeUnit.MILLISECONDS)
        .enableRetry()
        .build();
  }

  /**
   * Creates a managed channel builder with settings determined by the {@link ServerType}.
   *
   * @return the managed channel builder
   */
  private ManagedChannelBuilder<?> createChannelBuilder() {
    if (serverType == ServerType.InProcess) {
      return createInProcessChannelBuilder();
    } else {
      return createRemoteChannelBuilder();
    }
  }

  /**
   * Creates a CallOptions object for an API call. Will be {@link CallOptions#DEFAULT} if no token
   * has been defined or a BearerToken if one has been defined.
   *
   * @return a valid CallOptions object
   */
  public CallOptions createCallOptions() {
    CallOptions answer = CallOptions.DEFAULT;

    if (authenticationToken != null) {
      /*
       * The RDA API uses a JWT token for authentication, by design this is set to expire X days
       * after being issued. This check will alert the team of a token that is close to expiring,
       * so we can coordinate having a new one issued by the RDA API team before authentication
       * fails.
       */
      if (expirationDate != null) {
        long daysToExpire =
            Instant.now().until(Instant.ofEpochMilli(expirationDate * 1000), ChronoUnit.DAYS);

        if (daysToExpire < 0) {
          log.error("JWT is expired!");
        } else if (daysToExpire < 31) {
          String logMessage = String.format("JWT will expire in %d days", daysToExpire);

          if (daysToExpire < 14) {
            log.error(logMessage);
          } else {
            log.warn(logMessage);
          }
        }
      }

      answer = answer.withCallCredentials(new BearerToken(authenticationToken));
    } else {
      log.warn("authenticationToken has not been set - calling server with no token");
    }

    return answer;
  }

  /**
   * The maximum number of days to retain records in the DLQ after they have been processed.
   *
   * @return max age for processed records
   */
  public Optional<Integer> getMessageErrorExpirationDays() {
    return Optional.ofNullable(messageErrorExpirationDays);
  }

  /**
   * Creates a remove channel builder.
   *
   * @return the channel builder
   */
  private ManagedChannelBuilder<?> createRemoteChannelBuilder() {
    final ManagedChannelBuilder<?> builder = ManagedChannelBuilder.forAddress(host, port);
    if (host.equals("localhost")) {
      builder.usePlaintext();
    }
    return builder;
  }

  /**
   * Creates an in process channel builder.
   *
   * @return the channel builder
   */
  private ManagedChannelBuilder<?> createInProcessChannelBuilder() {
    return InProcessChannelBuilder.forName(inProcessServerName);
  }

  /**
   * Parses the given auth token as a JWT, extracting the commonly used `exp` claim to get the
   * token's expiration date.
   *
   * @param token The token to parse
   * @return The expiration date of the token, in epoch seconds.
   */
  private Long parseJWTExpirationDate(String token) {
    try {
      String[] jwtBits = token.split("\\.");
      String claimsString = new String(Base64.getDecoder().decode(jwtBits[1]));
      JWTClaims claims =
          new ObjectMapper()
              .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
              .readValue(claimsString, JWTClaims.class);
      if (claims.exp == null) throw new NullPointerException();
      return claims.exp;
    } catch (NullPointerException e) {
      log.warn("Could not find expiration claim", e);
    } catch (Exception e) {
      log.warn("Could not parse Authorization token as JWT", e);
    }

    return null;
  }

  /**
   * Gets value of {@code minIdleTimeBeforeConnectionDrop} in milliseconds.
   *
   * @return value of {@code minIdleTimeBeforeConnectionDrop} in milliseconds.
   */
  public long getMinIdleMillisBeforeConnectionDrop() {
    return minIdleTimeBeforeConnectionDrop.toMillis();
  }

  /** A data object for JWT claims. */
  @Data
  private static class JWTClaims {
    /** The expiration date of the token, in epoch seconds. */
    private Long exp;
  }
}
