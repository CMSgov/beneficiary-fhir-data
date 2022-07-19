package gov.cms.bfd.pipeline.rda.grpc.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import com.google.protobuf.Empty;
import gov.cms.mpsm.rda.v1.ApiVersion;
import gov.cms.mpsm.rda.v1.ClaimRequest;
import gov.cms.mpsm.rda.v1.FissClaimChange;
import gov.cms.mpsm.rda.v1.McsClaimChange;
import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class RdaServiceTest {

  @Mock private RdaService.Config mockConfig;

  @Mock private RdaService.Version mockVersion;

  @Mock private ApiVersion mockApiVersion;

  @Mock private StreamObserver<ApiVersion> mockAPIObserver;

  @Mock private StreamObserver<FissClaimChange> mockFissObserver;

  @Mock private RdaService.Responder<FissClaimChange> mockFissResponder;

  @Mock private MessageSource.Factory<FissClaimChange> mockFissFactory;

  @Mock private MessageSource<FissClaimChange> mockFissSource;

  @Mock private StreamObserver<McsClaimChange> mockMcsObserver;

  @Mock private RdaService.Responder<McsClaimChange> mockMcsResponder;

  @Mock private MessageSource.Factory<McsClaimChange> mockMcsFactory;

  @Mock private MessageSource<McsClaimChange> mockMcsSource;

  @Mock private ClaimRequest mockRequest;

  private static final Empty request = Empty.newBuilder().build();

  private static final long SINCE_VALUE = 5L;

  @BeforeEach
  void init() throws Exception {
    lenient().doReturn(mockApiVersion).when(mockVersion).toApiVersion();

    lenient().doReturn(mockVersion).when(mockConfig).getVersion();

    // resource - We’re creating a mock, not invoking the method
    //noinspection resource
    lenient().doReturn(mockFissSource).when(mockFissFactory).apply(SINCE_VALUE);

    lenient().doReturn(mockFissFactory).when(mockConfig).getFissSourceFactory();

    // resource - We’re creating a mock, not invoking the method
    //noinspection resource
    lenient().doReturn(mockMcsSource).when(mockMcsFactory).apply(SINCE_VALUE);

    lenient().doReturn(mockMcsFactory).when(mockConfig).getMcsSourceFactory();
  }

  /**
   * Tests if the API Version observer methods are invoked correctly, and that no exception is
   * thrown.
   */
  @Test
  void shouldInvokeApiVersionResponseObserverMethods() {
    RdaService service = new RdaService(mockConfig);
    service.getVersion(request, mockAPIObserver);

    verify(mockAPIObserver, times(1)).onNext(mockApiVersion);
    verify(mockAPIObserver, times(1)).onCompleted();
    verify(mockAPIObserver, times(0)).onError(any(Exception.class));
  }

  /**
   * Tests that the observer onError method is invoked if an exception was thrown for API Version
   * requests.
   */
  @Test
  void shouldInvokeOnErrorWhenExceptionRaisedOnApiVersionCall() {
    Exception originalException = new StatusRuntimeException(Status.DEADLINE_EXCEEDED);

    doThrow(originalException).when(mockAPIObserver).onNext(mockApiVersion);

    ArgumentCaptor<StatusException> captor = ArgumentCaptor.forClass(StatusException.class);

    RdaService service = new RdaService(mockConfig);
    service.getVersion(request, mockAPIObserver);

    verify(mockAPIObserver, times(1)).onError(captor.capture());

    StatusException exception = captor.getValue();

    assertEquals(Status.DEADLINE_EXCEEDED, exception.getStatus());
  }

  /** Tests if the MCS observer methods are invoked correctly, and that no exception is thrown. */
  @Test
  void shouldSendFissResponses() {
    doReturn(SINCE_VALUE).when(mockRequest).getSince();

    RdaService serviceSpy = spy(new RdaService(mockConfig));

    doReturn(mockFissResponder)
        .when(serviceSpy)
        .createFissResponder(mockFissObserver, mockFissSource);

    serviceSpy.getFissClaims(mockRequest, mockFissObserver);

    verify(mockFissResponder, times(1)).sendResponses();
    verify(mockFissObserver, times(0)).onError(any(Exception.class));
  }

  /**
   * Tests that the observer onError method is invoked if an exception was thrown for FISS requests.
   */
  @Test
  void shouldInvokeOnErrorWhenExceptionRaisedOnFissCall() {
    // ThrowableNotThrown - We’re mocking this
    RuntimeException originalException = new StatusRuntimeException(Status.DEADLINE_EXCEEDED);

    doReturn(SINCE_VALUE).when(mockRequest).getSince();

    RdaService serviceSpy = spy(new RdaService(mockConfig));

    doThrow(originalException).when(mockFissResponder).sendResponses();

    doReturn(mockFissResponder)
        .when(serviceSpy)
        .createFissResponder(mockFissObserver, mockFissSource);

    serviceSpy.getFissClaims(mockRequest, mockFissObserver);

    verify(mockFissResponder, times(1)).sendResponses();

    ArgumentCaptor<StatusException> captor = ArgumentCaptor.forClass(StatusException.class);

    verify(mockFissObserver, times(1)).onError(captor.capture());

    StatusException exception = captor.getValue();

    assertEquals(Status.DEADLINE_EXCEEDED, exception.getStatus());
  }

  /** Tests if the FISS observer methods are invoked correctly, and that no exception is thrown. */
  @Test
  void shouldSendMcsResponses() {
    doReturn(SINCE_VALUE).when(mockRequest).getSince();

    RdaService serviceSpy = spy(new RdaService(mockConfig));

    doReturn(mockMcsResponder).when(serviceSpy).createMcsResponder(mockMcsObserver, mockMcsSource);

    serviceSpy.getMcsClaims(mockRequest, mockMcsObserver);

    verify(mockMcsResponder, times(1)).sendResponses();
    verify(mockMcsObserver, times(0)).onError(any(Exception.class));
  }

  /**
   * Tests that the observer onError method is invoked if an exception was thrown for MCS requests.
   */
  @Test
  void shouldInvokeOnErrorWhenExceptionRaisedOnMcsCall() {
    // ThrowableNotThrown - We’re mocking this
    RuntimeException originalException = new StatusRuntimeException(Status.DEADLINE_EXCEEDED);

    doReturn(SINCE_VALUE).when(mockRequest).getSince();

    RdaService serviceSpy = spy(new RdaService(mockConfig));

    doThrow(originalException).when(mockMcsResponder).sendResponses();

    doReturn(mockMcsResponder).when(serviceSpy).createMcsResponder(mockMcsObserver, mockMcsSource);

    serviceSpy.getMcsClaims(mockRequest, mockMcsObserver);

    verify(mockMcsResponder, times(1)).sendResponses();

    ArgumentCaptor<StatusException> captor = ArgumentCaptor.forClass(StatusException.class);

    verify(mockMcsObserver, times(1)).onError(captor.capture());

    StatusException exception = captor.getValue();

    assertEquals(Status.DEADLINE_EXCEEDED, exception.getStatus());
  }
}
