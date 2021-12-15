package gov.cms.bfd.pipeline.rda.grpc.server;

import com.google.common.collect.ImmutableSet;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.util.Set;

/**
 * A gRPC ServerInterceptor that checks the value a specific header against a set of authorized
 * tokens. The header value must have the form "Bearer token" where token is generally a JWT but for
 * testing purposes could be anything.
 *
 * <p>NOTE: This class is not intended to be used for serious security. It exists strictly for use
 * with the mock server to test client authentication.
 */
public class SimpleAuthorizationInterceptor implements ServerInterceptor {
  static final String AUTH_HEADER_NAME = "authorization";
  static final String AUTH_HEADER_PREFIX = "Bearer ";
  static final Metadata.Key<String> METADATA_KEY =
      Metadata.Key.of(AUTH_HEADER_NAME, Metadata.ASCII_STRING_MARSHALLER);

  private final Set<String> authorizedTokens;

  public SimpleAuthorizationInterceptor(Iterable<String> authorizedTokens) {
    this.authorizedTokens = ImmutableSet.copyOf(authorizedTokens);
  }

  /**
   * Verifies that the client is authorized by comparing the appropriate header in the Metadata to
   * our configuration. Throws a StatusRuntimeException if the client is not authorized.
   */
  public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
      final ServerCall<ReqT, RespT> serverCall,
      final Metadata metadata,
      final ServerCallHandler<ReqT, RespT> serverCallHandler) {
    if (!isAuthorized(metadata)) {
      throw new StatusRuntimeException(Status.UNAUTHENTICATED);
    }
    return serverCallHandler.startCall(serverCall, metadata);
  }

  /**
   * A client is authorized if we have no required tokens or if the client provided an appropriate
   * header with a value containing one of our allowed tokens.
   *
   * @param metadata the Metadata from a client call
   * @return true iff the client is authorized
   */
  private boolean isAuthorized(Metadata metadata) {
    if (authorizedTokens.isEmpty()) {
      return true;
    }

    final String authHeaderValue = metadata.get(METADATA_KEY);

    if (authHeaderValue == null || !authHeaderValue.startsWith(AUTH_HEADER_PREFIX)) {
      return false;
    }

    final String authToken = authHeaderValue.substring(AUTH_HEADER_PREFIX.length());
    return authorizedTokens.contains(authToken);
  }
}
