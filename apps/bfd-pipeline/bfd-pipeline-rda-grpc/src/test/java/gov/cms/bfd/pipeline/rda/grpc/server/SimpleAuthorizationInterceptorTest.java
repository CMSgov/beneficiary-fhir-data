package gov.cms.bfd.pipeline.rda.grpc.server;

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.*;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.StatusRuntimeException;
import java.util.Collections;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SimpleAuthorizationInterceptorTest {
  @Mock private ServerCall<String, String> call;
  @Mock private ServerCallHandler<String, String> handler;
  @Mock private ServerCall.Listener<String> listener;

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

  @Test(expected = StatusRuntimeException.class)
  public void tokenRequiredNoneProvided() {
    SimpleAuthorizationInterceptor interceptor =
        new SimpleAuthorizationInterceptor(Collections.singleton("secret"));

    Metadata metaData = new Metadata();

    interceptor.interceptCall(call, metaData, handler);
  }

  @Test(expected = StatusRuntimeException.class)
  public void tokenRequiredWrongProvided() {
    SimpleAuthorizationInterceptor interceptor =
        new SimpleAuthorizationInterceptor(Collections.singleton("secret"));

    Metadata metaData = new Metadata();
    metaData.put(SimpleAuthorizationInterceptor.METADATA_KEY, headerValue("wrong"));

    interceptor.interceptCall(call, metaData, handler);
  }

  private static String headerValue(String token) {
    return SimpleAuthorizationInterceptor.AUTH_HEADER_PREFIX + token;
  }
}
