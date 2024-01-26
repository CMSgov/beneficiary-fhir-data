package gov.cms.bfd.pipeline.rda.grpc.sink.concurrent;

import gov.cms.bfd.pipeline.sharedutils.SequenceNumberTracker;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import gov.cms.bfd.pipeline.rda.grpc.RdaSink;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for {@link SequenceNumberWriter}. */
@ExtendWith(MockitoExtension.class)
public class SequenceNumberWriterTest {
  /** Mock used for verifying database writes. */
  @Mock private RdaSink<Integer, Long> sink;

  /**
   * Instance of {@link SequenceNumberTracker} passed
   * to the {@link SequenceNumberWriter}.
   */
  private SequenceNumberTracker tracker;

  /** Instance being tested. */
  private SequenceNumberWriter<Integer, Long> writer;

  /** Sets up objects used for test cases. */
  @BeforeEach
  void setUp() {
    tracker = new SequenceNumberTracker(0);
    writer = new SequenceNumberWriter<>(sink, tracker);
    tracker.addActiveSequenceNumber(1);
    tracker.addActiveSequenceNumber(3);
    tracker.addActiveSequenceNumber(5);
  }

  /**
   * Ensures updates are written in response to sequence number changes and are not written when
   * they would be redundant.
   */
  @Test
  void shouldWriteUpdatesOnlyWhenSequenceNumberChanges() {
    tracker.removeWrittenSequenceNumber(1);
    assertEquals(Optional.of(2L), writer.updateSequenceNumberInDatabase().blockOptional());
    assertEquals(Optional.empty(), writer.updateSequenceNumberInDatabase().blockOptional());

    tracker.removeWrittenSequenceNumber(3);
    assertEquals(Optional.of(4L), writer.updateSequenceNumberInDatabase().blockOptional());
    assertEquals(Optional.empty(), writer.updateSequenceNumberInDatabase().blockOptional());

    verify(sink).updateLastSequenceNumber(2L);
    verify(sink).updateLastSequenceNumber(4L);
    verifyNoMoreInteractions(sink);
  }

  /** Ensures that exceptions thrown by the sink are passed through to the caller. */
  @Test
  void shouldPassThroughExceptionsFromSink() {
    doThrow(new RuntimeException()).when(sink).updateLastSequenceNumber(anyLong());

    tracker.removeWrittenSequenceNumber(1);
    assertThrows(RuntimeException.class, () -> writer.updateSequenceNumberInDatabase().block());
  }

  /**
   * Ensures that calling close does an update and closes the sink.
   *
   * @throws Exception if anything throws
   */
  @Test
  void shouldUpdateDbAndCloseSink() throws Exception {
    tracker.removeWrittenSequenceNumber(1);
    tracker.removeWrittenSequenceNumber(3);
    writer.close();
    verify(sink).updateLastSequenceNumber(4L);
    verify(sink).close();
    verifyNoMoreInteractions(sink);
  }

  /**
   * Ensures that calling close closes the sink even if the updateDb call throws.
   *
   * @throws Exception if anything throws
   */
  @Test
  void shouldCloseSinkEvenIfUpdateDbFails() throws Exception {
    doThrow(new RuntimeException()).when(sink).updateLastSequenceNumber(anyLong());

    tracker.removeWrittenSequenceNumber(1);
    tracker.removeWrittenSequenceNumber(3);
    assertThrows(RuntimeException.class, () -> writer.close());

    verify(sink).updateLastSequenceNumber(4L);
    verify(sink).close();
    verifyNoMoreInteractions(sink);
  }
}
