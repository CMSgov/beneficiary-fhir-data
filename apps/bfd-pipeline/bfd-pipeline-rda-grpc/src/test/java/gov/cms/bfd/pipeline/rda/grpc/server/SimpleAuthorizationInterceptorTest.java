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

public class SimpleAuthorizationInterceptorTest {
  @Mock private ServerCall<String, String> call;
  @Mock private ServerCallHandler<String, String> handler;
  @Mock private ServerCall.Listener<String> listener;

  @BeforeEach
  public void init() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  public void noTokenRequiredNoneProvided() {
    SimpleAuthorizationInterceptor interceptor =
        new SimpleAuthorizationInterceptor(Collections.emptySet());

    Metadata metaData = new Metadata();

    doReturn(listener).when(handler).startCall(call, metaData);
    assertSame(listener, interceptor.interceptCall(call, metaData, handler));
  }

  @Test
  public void noTokenRequiredAnyProvided() {
    SimpleAuthorizationInterceptor interceptor =
        new SimpleAuthorizationInterceptor(Collections.emptySet());

    Metadata metaData = new Metadata();
    metaData.put(SimpleAuthorizationInterceptor.METADATA_KEY, headerValue("secret"));

    doReturn(listener).when(handler).startCall(call, metaData);
    assertSame(listener, interceptor.interceptCall(call, metaData, handler));
  }

  @Test
  public void tokenRequiredCorrectProvided() {
    SimpleAuthorizationInterceptor interceptor =
        new SimpleAuthorizationInterceptor(Collections.singleton("secret"));

    Metadata metaData = new Metadata();
    metaData.put(SimpleAuthorizationInterceptor.METADATA_KEY, headerValue("secret"));

    doReturn(listener).when(handler).startCall(call, metaData);
    assertSame(listener, interceptor.interceptCall(call, metaData, handler));
  }

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

  private static String headerValue(String token) {
    return SimpleAuthorizationInterceptor.AUTH_HEADER_PREFIX + token;
  }
}
