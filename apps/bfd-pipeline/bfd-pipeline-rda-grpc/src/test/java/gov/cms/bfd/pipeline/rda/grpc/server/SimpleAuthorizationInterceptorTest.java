package gov.cms.bfd.pipeline.rda.grpc.server;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.StatusRuntimeException;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/** Tests the {@link SimpleAuthorizationInterceptor}. */
public class SimpleAuthorizationInterceptorTest {

  /** A mock call to test is intercepted. */
  @Mock private ServerCall<String, String> call;
  /** The mock call handler. */
  @Mock private ServerCallHandler<String, String> handler;
  /** The mock call listener. */
  @Mock private ServerCall.Listener<String> listener;

  /** Sets up the mocks before each test. */
  @BeforeEach
  public void init() {
    MockitoAnnotations.openMocks(this);
  }

  /**
   * Verifies that if the interceptor does not require a token, and no token is provided on a call,
   * the interceptor is invoked but no exception is thrown.
   */
  @Test
  public void noTokenRequiredNoneProvided() {
    SimpleAuthorizationInterceptor interceptor =
        new SimpleAuthorizationInterceptor(Collections.emptySet());

    Metadata metaData = new Metadata();

    doReturn(listener).when(handler).startCall(call, metaData);
    assertSame(listener, interceptor.interceptCall(call, metaData, handler));
  }

  /**
   * Verifies that if the interceptor does not require a token, and a token is provided on a call,
   * the interceptor is invoked but no exception is thrown (token is ignored).
   */
  @Test
  public void noTokenRequiredAnyProvided() {
    SimpleAuthorizationInterceptor interceptor =
        new SimpleAuthorizationInterceptor(Collections.emptySet());

    Metadata metaData = new Metadata();
    metaData.put(SimpleAuthorizationInterceptor.METADATA_KEY, headerValue("secret"));

    doReturn(listener).when(handler).startCall(call, metaData);
    assertSame(listener, interceptor.interceptCall(call, metaData, handler));
  }

  /**
   * Verifies that if the interceptor requires a token, and the correct token is provided on a call,
   * the interceptor is invoked but no exception is thrown.
   */
  @Test
  public void tokenRequiredCorrectProvided() {
    SimpleAuthorizationInterceptor interceptor =
        new SimpleAuthorizationInterceptor(Collections.singleton("secret"));

    Metadata metaData = new Metadata();
    metaData.put(SimpleAuthorizationInterceptor.METADATA_KEY, headerValue("secret"));

    doReturn(listener).when(handler).startCall(call, metaData);
    assertSame(listener, interceptor.interceptCall(call, metaData, handler));
  }

  /**
   * Verifies that if the interceptor requires a token, and no token is provided on a call, the
   * interceptor is invoked and a {@link StatusRuntimeException} is thrown.
   */
  @Test
  public void tokenRequiredNoneProvided() {
    SimpleAuthorizationInterceptor interceptor =
        new SimpleAuthorizationInterceptor(Collections.singleton("secret"));

    Metadata metaData = new Metadata();

    assertThrows(
        StatusRuntimeException.class,
        () -> {
          interceptor.interceptCall(call, metaData, handler);
        });
  }

  /**
   * Verifies that if the interceptor requires a token, and the wrong token is provided on a call,
   * the interceptor is invoked and a {@link StatusRuntimeException} is thrown.
   */
  @Test
  public void tokenRequiredWrongProvided() {
    SimpleAuthorizationInterceptor interceptor =
        new SimpleAuthorizationInterceptor(Collections.singleton("secret"));

    Metadata metaData = new Metadata();
    metaData.put(SimpleAuthorizationInterceptor.METADATA_KEY, headerValue("wrong"));

    assertThrows(
        StatusRuntimeException.class,
        () -> {
          interceptor.interceptCall(call, metaData, handler);
        });
  }

  /**
   * Sets up a header value with the specified token appended to the auth header prefix.
   *
   * @param token the token value
   * @return the full header value with auth header prefix and token value
   */
  private static String headerValue(String token) {
    return SimpleAuthorizationInterceptor.AUTH_HEADER_PREFIX + token;
  }
}
