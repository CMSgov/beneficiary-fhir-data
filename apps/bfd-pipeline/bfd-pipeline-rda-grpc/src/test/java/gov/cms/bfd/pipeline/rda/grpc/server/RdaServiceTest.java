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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/** Tests the {@link RdaService}. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class RdaServiceTest {

  /** The mocked RdaMessageSourceFactory. */
  @Mock private RdaMessageSourceFactory mockRdaMessageSourceFactory;
  /** The mocked RDA version. */
  @Mock private RdaService.Version mockVersion;
  /** The mocked api version data. */
  @Mock private ApiVersion mockApiVersion;
  /** The mocked api observer. */
  @Mock private StreamObserver<ApiVersion> mockAPIObserver;
  /** The mocked Fiss observer. */
  @Mock private StreamObserver<FissClaimChange> mockFissObserver;
  /** The mocked Fiss responder. */
  @Mock private RdaService.Responder<FissClaimChange> mockFissResponder;
  /** The mocked source. */
  @Mock private MessageSource<FissClaimChange> mockFissSource;
  /** The mocked MCS observer. */
  @Mock private StreamObserver<McsClaimChange> mockMcsObserver;
  /** The mocked MCS responder. */
  @Mock private RdaService.Responder<McsClaimChange> mockMcsResponder;
  /** The mocked MCS source. */
  @Mock private MessageSource<McsClaimChange> mockMcsSource;
  /** The mocked MCS request. */
  @Mock private ClaimRequest mockRequest;

  /** The empty request to use when the request contents don't matter. */
  private static final Empty request = Empty.newBuilder().build();
  /** Sequence number used in test setup. */
  private static final long SINCE_VALUE = 5L;

  /**
   * Initializes mock methods with the expected returns.
   *
   * @throws Exception issue with mock setup
   */
  @BeforeEach
  void init() throws Exception {
    doReturn(mockApiVersion).when(mockVersion).toApiVersion();

    doReturn(mockVersion).when(mockRdaMessageSourceFactory).getVersion();

    doReturn(mockFissSource)
        .when(mockRdaMessageSourceFactory)
        .createFissMessageSource(SINCE_VALUE + 1);

    doReturn(mockMcsSource)
        .when(mockRdaMessageSourceFactory)
        .createMcsMessageSource(SINCE_VALUE + 1);
  }

  /**
   * Tests if the API Version observer methods are invoked correctly, and that no exception is
   * thrown.
   */
  @Test
  void shouldInvokeApiVersionResponseObserverMethods() {
    RdaService service = new RdaService(mockRdaMessageSourceFactory);
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

    RdaService service = new RdaService(mockRdaMessageSourceFactory);
    service.getVersion(request, mockAPIObserver);

    verify(mockAPIObserver, times(1)).onError(captor.capture());

    StatusException exception = captor.getValue();

    assertEquals(Status.DEADLINE_EXCEEDED, exception.getStatus());
  }

  /** Tests if the MCS observer methods are invoked correctly, and that no exception is thrown. */
  @Test
  void shouldSendFissResponses() {
    doReturn(SINCE_VALUE).when(mockRequest).getSince();

    RdaService serviceSpy = spy(new RdaService(mockRdaMessageSourceFactory));

    doReturn(mockFissResponder)
        .when(serviceSpy)
        .createFissResponder(mockFissObserver, mockFissSource);

    serviceSpy.getFissClaims(mockRequest, mockFissObserver);

    verify(mockFissResponder, times(1)).start();
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

    RdaService serviceSpy = spy(new RdaService(mockRdaMessageSourceFactory));

    doThrow(originalException).when(mockFissResponder).start();

    doReturn(mockFissResponder)
        .when(serviceSpy)
        .createFissResponder(mockFissObserver, mockFissSource);

    serviceSpy.getFissClaims(mockRequest, mockFissObserver);

    verify(mockFissResponder, times(1)).start();

    ArgumentCaptor<StatusException> captor = ArgumentCaptor.forClass(StatusException.class);

    verify(mockFissObserver, times(1)).onError(captor.capture());

    StatusException exception = captor.getValue();

    assertEquals(Status.DEADLINE_EXCEEDED, exception.getStatus());
  }

  /** Tests if the FISS observer methods are invoked correctly, and that no exception is thrown. */
  @Test
  void shouldSendMcsResponses() {
    doReturn(SINCE_VALUE).when(mockRequest).getSince();

    RdaService serviceSpy = spy(new RdaService(mockRdaMessageSourceFactory));

    doReturn(mockMcsResponder).when(serviceSpy).createMcsResponder(mockMcsObserver, mockMcsSource);

    serviceSpy.getMcsClaims(mockRequest, mockMcsObserver);

    verify(mockMcsResponder, times(1)).start();
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

    RdaService serviceSpy = spy(new RdaService(mockRdaMessageSourceFactory));

    doThrow(originalException).when(mockMcsResponder).start();

    doReturn(mockMcsResponder).when(serviceSpy).createMcsResponder(mockMcsObserver, mockMcsSource);

    serviceSpy.getMcsClaims(mockRequest, mockMcsObserver);

    verify(mockMcsResponder, times(1)).start();

    ArgumentCaptor<StatusException> captor = ArgumentCaptor.forClass(StatusException.class);

    verify(mockMcsObserver, times(1)).onError(captor.capture());

    StatusException exception = captor.getValue();

    assertEquals(Status.DEADLINE_EXCEEDED, exception.getStatus());
  }
}
