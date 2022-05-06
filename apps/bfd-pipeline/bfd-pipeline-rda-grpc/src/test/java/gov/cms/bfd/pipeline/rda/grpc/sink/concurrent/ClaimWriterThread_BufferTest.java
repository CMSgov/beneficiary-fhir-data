package gov.cms.bfd.pipeline.rda.grpc.sink.concurrent;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import gov.cms.bfd.pipeline.rda.grpc.RdaSink;
import gov.cms.bfd.pipeline.rda.grpc.source.DataTransformer;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

// NewClassNamingConvention - Identifies hierarchy with test name
@SuppressWarnings("NewClassNamingConvention")
public class ClaimWriterThread_BufferTest {

  /** Checks that a valid claim is added successfully without throwing an exception. */
  @Test
  void shouldAddValidClaimSuccessfully() {
    final String mockApiVersion = "version";
    final String mockMessage = "message";
    final String mockClaimKey = "key";
    final String mockClaim = "claim";

    // This is ok for the purposes of this test
    //noinspection unchecked
    RdaSink<String, String> mockSink = mock(RdaSink.class);
    // This is ok for the purposes of this test
    //noinspection unchecked
    ClaimWriterThread.Entry<String> mockEntry = mock(ClaimWriterThread.Entry.class);

    doReturn(mockApiVersion).when(mockEntry).getApiVersion();

    doReturn(mockMessage).when(mockEntry).getObject();

    doReturn(mockClaimKey).when(mockSink).getDedupKeyForMessage(mockMessage);

    doReturn(mockClaim).when(mockSink).transformMessage(mockApiVersion, mockMessage);

    ClaimWriterThread.Buffer<String, String> buffer = new ClaimWriterThread.Buffer<>();

    assertDoesNotThrow(() -> buffer.add(mockSink, mockEntry));
    assertEquals(buffer.getClaims(), List.of(mockClaim));
    assertEquals(buffer.getMessages(), List.of(mockMessage));
  }

  /**
   * Checks that an invalid claim was sent to {@link RdaSink#writeError(Object,
   * DataTransformer.TransformationException)} and throws a {@link
   * DataTransformer.TransformationException}.
   */
  @Test
  void shouldThrowExceptionWhenAddingInvalidClaim() throws IOException {
    final String mockApiVersion = "version";
    final String mockMessage = "message";
    final String mockClaimKey = "key";

    // This is ok for the purposes of this test
    //noinspection unchecked
    RdaSink<String, String> mockSink = mock(RdaSink.class);
    // This is ok for the purposes of this test
    //noinspection unchecked
    ClaimWriterThread.Entry<String> mockEntry = mock(ClaimWriterThread.Entry.class);

    doReturn(mockApiVersion).when(mockEntry).getApiVersion();

    doReturn(mockMessage).when(mockEntry).getObject();

    doReturn(mockClaimKey).when(mockSink).getDedupKeyForMessage(mockMessage);

    doThrow(DataTransformer.TransformationException.class)
        .when(mockSink)
        .transformMessage(mockApiVersion, mockMessage);

    doNothing()
        .when(mockSink)
        .writeError(anyString(), anyString(), any(DataTransformer.TransformationException.class));

    ClaimWriterThread.Buffer<String, String> buffer = new ClaimWriterThread.Buffer<>();

    assertThrows(
        DataTransformer.TransformationException.class, () -> buffer.add(mockSink, mockEntry));
    verify(mockSink, times(1))
        .writeError(
            eq(mockApiVersion),
            eq(mockMessage),
            any(DataTransformer.TransformationException.class));
  }
}
