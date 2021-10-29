package gov.cms.bfd.pipeline.rda.grpc.source;

import static io.grpc.Metadata.ASCII_STRING_MARSHALLER;

import com.google.common.base.Strings;
import io.grpc.CallCredentials;
import io.grpc.Metadata;
import io.grpc.Status;
import java.util.concurrent.Executor;

public class BearerToken extends CallCredentials {
  private static final String HEADER_PREFIX = "Bearer ";
  private static final Metadata.Key<String> HEADER_KEY =
      Metadata.Key.of("Authorization", ASCII_STRING_MARSHALLER);

  private final String headerValue;

  public BearerToken(String headerValue) {
    if (Strings.isNullOrEmpty(headerValue)) {
      throw new IllegalArgumentException("Token value was null or empty.");
    }
    this.headerValue = HEADER_PREFIX + headerValue;
  }

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

  @Override
  public void thisUsesUnstableApi() {
    // nothing to do here
  }
}
