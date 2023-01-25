package gov.cms.bfd.pipeline.rda.grpc.source;

import static io.grpc.Metadata.ASCII_STRING_MARSHALLER;

import com.google.common.base.Strings;
import io.grpc.CallCredentials;
import io.grpc.Metadata;
import io.grpc.Status;
import java.util.concurrent.Executor;

/**
 * A CallCredentials implementation that adds a bearer token to every request. Used to invoke the
 * RDA API with a JWT token for authorization.
 */
public class BearerToken extends CallCredentials {
  /** Used as a prefix for the header value. */
  private static final String HEADER_PREFIX = "Bearer ";
  /** Used as the header key. */
  private static final Metadata.Key<String> HEADER_KEY =
      Metadata.Key.of("Authorization", ASCII_STRING_MARSHALLER);
  /** Used as the header value, prefixed by {@link #HEADER_PREFIX}. */
  private final String headerValue;

  /**
   * Instantiates a new bearer token.
   *
   * @param headerValue the header value
   */
  public BearerToken(String headerValue) {
    if (Strings.isNullOrEmpty(headerValue)) {
      throw new IllegalArgumentException("Token value was null or empty.");
    }
    this.headerValue = HEADER_PREFIX + headerValue;
  }

  /** {@inheritDoc} */
  @Override
  public void applyRequestMetadata(
      RequestInfo requestInfo, Executor appExecutor, MetadataApplier applier) {
    appExecutor.execute(
        () -> {
          try {
            Metadata headers = new Metadata();
            headers.put(HEADER_KEY, headerValue);
            applier.apply(headers);
          } catch (Throwable e) {
            applier.fail(Status.UNAUTHENTICATED.withCause(e));
          }
        });
  }

  /** {@inheritDoc} */
  @Override
  public void thisUsesUnstableApi() {
    // nothing to do here
  }
}
