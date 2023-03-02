package gov.cms.bfd.pipeline.rda.grpc.sink.concurrent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import gov.cms.bfd.pipeline.rda.grpc.ProcessingException;
import gov.cms.bfd.pipeline.rda.grpc.RdaSink;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for {@link ClaimWriter}. */
@ExtendWith(MockitoExtension.class)
public class ClaimWriterTest {
  /** RDA API version used for all tests. */
  private static final String ApiVersion = "1.0";

  /** Batch size used in all tests. */
  private static final int BatchSize = 3;

  /** Mock used for verifying database writes. */
  @Mock private RdaSink<Integer, Long> sink;

  /** Instance being tested. */
  private ClaimWriter<Integer, Long> writer;

  /** Sets up objects used for test cases. */
  @BeforeEach
  void setUp() {
    writer = new ClaimWriter<>(1, sink, BatchSize);
  }

  /**
   * Verifies claims are transformed.
   *
   * @throws Exception passed through if thrown during test
   */
  @Test
  void shouldTransformEachClaimMessage() throws Exception {
    doReturn(Optional.of(1L)).when(sink).transformMessage(ApiVersion, 1);
    var result = writer.processMessage(new ApiMessage<>("1", 1, ApiVersion, 1));
    assertEquals(Optional.empty(), result.blockOptional());
    verify(sink).transformMessage(ApiVersion, 1);
    verifyNoMoreInteractions(sink);
  }

  /**
   * Verifies claims are written as soon as a full batch has been collected.
   *
   * @throws Exception passed through if thrown during test
   */
  @Test
  void shouldWriteCompleteBatch() throws Exception {
    // mock all of the expected transformations
    doReturn(Optional.of(1L)).doReturn(Optional.of(11L)).when(sink).transformMessage(ApiVersion, 1);
    doReturn(Optional.empty()).when(sink).transformMessage(ApiVersion, 2);
    doReturn(Optional.of(3L)).when(sink).transformMessage(ApiVersion, 3);
    doReturn(Optional.of(4L)).when(sink).transformMessage(ApiVersion, 4);

    // mock the expected write of a complete batch
    doReturn(3).when(sink).writeClaims(List.of(11L, 3L, 4L));

    // create the messages we'll process
    var seqNum = 100;
    final var message1 = new ApiMessage<>("1", ++seqNum, ApiVersion, 1);
    final var message2 = new ApiMessage<>("2", ++seqNum, ApiVersion, 2);
    final var message1update = new ApiMessage<>("1", ++seqNum, ApiVersion, 1);
    final var message3 = new ApiMessage<>("3", ++seqNum, ApiVersion, 3);
    final var message4 = new ApiMessage<>("4", ++seqNum, ApiVersion, 4);
    final var allMessages = List.of(message1, message2, message1update, message3, message4);

    // incomplete batch so no write, this version of claim will be replaced later
    var result = writer.processMessage(message1);
    assertEquals(Optional.empty(), result.blockOptional());
    assertFalse(writer.isEmpty());

    // incomplete batch so no write, claim doesn't transform so it winds up being skipped
    result = writer.processMessage(message2);
    assertEquals(Optional.empty(), result.blockOptional());
    assertFalse(writer.isEmpty());

    // incomplete batch so no write, this version of claim replaces prior version
    result = writer.processMessage(message1update);
    assertEquals(Optional.empty(), result.blockOptional());
    assertFalse(writer.isEmpty());

    // incomplete batch so no write
    result = writer.processMessage(message3);
    assertEquals(Optional.empty(), result.blockOptional());
    assertFalse(writer.isEmpty());

    // this makes a complete batch so write will happen
    result = writer.processMessage(message4);
    assertEquals(Optional.of(new BatchResult<>(allMessages, 3)), result.blockOptional());
    assertTrue(writer.isEmpty());

    verify(sink, times(2)).transformMessage(ApiVersion, 1);
    verify(sink).transformMessage(ApiVersion, 2);
    verify(sink).transformMessage(ApiVersion, 3);
    verify(sink).transformMessage(ApiVersion, 4);
    verify(sink).writeClaims(List.of(11L, 3L, 4L));
    verifyNoMoreInteractions(sink);
  }

  /**
   * Verifies that if a flush control message is processed a write is triggered immediately.
   *
   * @throws Exception passed through if thrown during test
   */
  @Test
  void shouldWriteIncompleteBatchOnFlushMessage() throws Exception {
    // mock all of the expected transformations
    doReturn(Optional.of(1L)).when(sink).transformMessage(ApiVersion, 1);
    doReturn(Optional.of(2L)).when(sink).transformMessage(ApiVersion, 2);

    // mock the expected write of a complete batch
    doReturn(2).when(sink).writeClaims(List.of(1L, 2L));

    // create the messages we'll process
    var seqNum = 100;
    final var message1 = new ApiMessage<>("1", ++seqNum, ApiVersion, 1);
    final var message2 = new ApiMessage<>("2", ++seqNum, ApiVersion, 2);
    final var allMessages = List.of(message1, message2);

    // incomplete batch so no write
    var result = writer.processMessage(message1);
    assertEquals(Optional.empty(), result.blockOptional());
    assertFalse(writer.isEmpty());

    // incomplete batch so no write
    result = writer.processMessage(message2);
    assertEquals(Optional.empty(), result.blockOptional());
    assertFalse(writer.isEmpty());

    // flush control message causes write of incomplete batch
    result = writer.processMessage(ApiMessage.createFlushMessage());
    assertEquals(Optional.of(new BatchResult<>(allMessages, 2)), result.blockOptional());
    assertTrue(writer.isEmpty());

    verify(sink).transformMessage(ApiVersion, 1);
    verify(sink).transformMessage(ApiVersion, 2);
    verify(sink).writeClaims(List.of(1L, 2L));
    verifyNoMoreInteractions(sink);
  }

  /**
   * Verifies that two consecutive idle control messages cause an immediate write of claims.
   *
   * @throws Exception passed through if thrown during test
   */
  @Test
  void shouldWriteIncompleteBatchOnConsecutiveIdleMessages() throws Exception {
    // mock all of the expected transformations
    doReturn(Optional.of(1L)).when(sink).transformMessage(ApiVersion, 1);
    doReturn(Optional.of(2L)).when(sink).transformMessage(ApiVersion, 2);

    // mock the expected write of a complete batch
    doReturn(2).when(sink).writeClaims(List.of(1L, 2L));

    // create the messages we'll process
    var seqNum = 100;
    final var message1 = new ApiMessage<>("1", ++seqNum, ApiVersion, 1);
    final var message2 = new ApiMessage<>("2", ++seqNum, ApiVersion, 2);
    final var allMessages = List.of(message1, message2);

    // incomplete batch so no write
    var result = writer.processMessage(message1);
    assertEquals(Optional.empty(), result.blockOptional());
    assertFalse(writer.isEmpty());

    // first idle message does not cause a write, idle status will be reset by next message
    result = writer.processMessage(ApiMessage.createIdleMessage());
    assertEquals(Optional.empty(), result.blockOptional());
    assertFalse(writer.isEmpty());

    // incomplete batch so no write, resets the internal idle flag
    result = writer.processMessage(message2);
    assertEquals(Optional.empty(), result.blockOptional());
    assertFalse(writer.isEmpty());

    // first consecutive idle message does not cause a write
    result = writer.processMessage(ApiMessage.createIdleMessage());
    assertEquals(Optional.empty(), result.blockOptional());
    assertFalse(writer.isEmpty());

    // second consecutive idle control message causes write of incomplete batch
    result = writer.processMessage(ApiMessage.createIdleMessage());
    assertEquals(Optional.of(new BatchResult<>(allMessages, 2)), result.blockOptional());
    assertTrue(writer.isEmpty());

    verify(sink).transformMessage(ApiVersion, 1);
    verify(sink).transformMessage(ApiVersion, 2);
    verify(sink).writeClaims(List.of(1L, 2L));
    verifyNoMoreInteractions(sink);
  }

  /**
   * Verifies that messages which fail to transform have their exception passed through and are not
   * buffered internally.
   *
   * @throws Exception passed through if thrown during test
   */
  @Test
  void shouldPassThroughTransformationErrors() throws Exception {
    // mock all of the expected transformations
    final var transformError = new ProcessingException(new IOException("oops"), 0);
    doReturn(Optional.of(1L)).when(sink).transformMessage(ApiVersion, 1);
    doThrow(transformError).when(sink).transformMessage(ApiVersion, 2);

    // create the messages we'll process
    var seqNum = 100;
    final var message1 = new ApiMessage<>("1", ++seqNum, ApiVersion, 1);
    final var message2 = new ApiMessage<>("2", ++seqNum, ApiVersion, 2);
    final var allMessages = List.of(message1, message2);

    // first message transforms fine
    var result = writer.processMessage(message1);
    assertEquals(Optional.empty(), result.blockOptional());
    assertFalse(writer.isEmpty());

    // second message transformation fails
    result = writer.processMessage(message2);
    assertEquals(
        Optional.of(new BatchResult<>(List.of(message2), transformError)), result.blockOptional());
    assertFalse(writer.isEmpty());
    assertFalse(writer.containsMessage(message2));

    verify(sink).transformMessage(ApiVersion, 1);
    verify(sink).transformMessage(ApiVersion, 2);
    verifyNoMoreInteractions(sink);
  }

  /**
   * Verifies that errors during a write are returned in a result.
   *
   * @throws Exception passed through if thrown during test
   */
  @Test
  void shouldPassThroughWriteErrors() throws Exception {
    // mock all of the expected transformations
    doReturn(Optional.of(1L)).when(sink).transformMessage(ApiVersion, 1);
    doReturn(Optional.of(2L)).when(sink).transformMessage(ApiVersion, 2);
    doReturn(Optional.of(3L)).when(sink).transformMessage(ApiVersion, 3);

    // mock the expected write of a complete batch
    final var writeError = new ProcessingException(new IOException("oops"), 0);
    doThrow(writeError).when(sink).writeClaims(List.of(1L, 2L, 3L));

    // create the messages we'll process
    var seqNum = 100;
    final var message1 = new ApiMessage<>("1", ++seqNum, ApiVersion, 1);
    final var message2 = new ApiMessage<>("2", ++seqNum, ApiVersion, 2);
    final var message3 = new ApiMessage<>("3", ++seqNum, ApiVersion, 3);
    final var allMessages = List.of(message1, message2, message3);

    // first message transforms fine
    var result = writer.processMessage(message1);
    assertEquals(Optional.empty(), result.blockOptional());
    assertFalse(writer.isEmpty());

    // second message transforms fine
    result = writer.processMessage(message2);
    assertEquals(Optional.empty(), result.blockOptional());
    assertFalse(writer.isEmpty());

    // third message transforms fine
    // write error is passed through in result
    result = writer.processMessage(message3);
    assertEquals(Optional.of(new BatchResult<>(allMessages, writeError)), result.blockOptional());
    assertTrue(writer.isEmpty());

    verify(sink).transformMessage(ApiVersion, 1);
    verify(sink).transformMessage(ApiVersion, 2);
    verify(sink).transformMessage(ApiVersion, 3);
    verify(sink).writeClaims(List.of(1L, 2L, 3L));
    verifyNoMoreInteractions(sink);
  }

  /**
   * Verifies that close closes the sink.
   *
   * @throws Exception passed through if thrown during test
   */
  @Test
  void shouldCloseSinkOnClose() throws Exception {
    writer.close();
    verify(sink).close();
  }
}
