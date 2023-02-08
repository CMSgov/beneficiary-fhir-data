package gov.cms.bfd.pipeline.rda.grpc.sink.concurrent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import gov.cms.bfd.pipeline.rda.grpc.ProcessingException;
import gov.cms.model.dsl.codegen.library.DataTransformer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

/** Tests the {@link ConcurrentRdaSink}. */
public class ConcurrentRdaSinkIT {
  /** Test value for version. */
  private static final String VERSION = "Version";

  /**
   * Tests that the writer successfully writes all the queued claims to the database, all sinks are
   * closed after writing, and the last sequence number is recorded correctly.
   *
   * @throws Exception indicates test failure
   */
  @Test
  public void testSuccess() throws Exception {
    final TestDatabase database = new TestDatabase();
    final List<TestDatabase.Message> messages = createTestMessages();
    try (ConcurrentRdaSink<TestDatabase.Message, TestDatabase.Claim> pool =
        new ConcurrentRdaSink<>(17, 11, database::createSink)) {
      for (List<TestDatabase.Message> messageList : createBatches(messages, 9)) {
        pool.writeMessages(VERSION, messageList);
      }
    }
    assertTrue(database.allClosed(), "all sinks closed");
    assertEquals(expectedClaims(messages), database.getClaims());
    assertEquals(messages.size(), database.getLastSequenceNumber());
  }

  /**
   * Tests that when there is an exception when transforming one of the messages, we get a {@link
   * ProcessingException} and the sinks are closed correctly.
   */
  @Test
  public void testTransformFailure() {
    final TestDatabase database = new TestDatabase();
    final List<TestDatabase.Message> messages = createTestMessages();
    // trigger a transform error on last message
    messages.set(messages.size() - 1, messages.get(messages.size() - 1).withFailOnTransform(true));

    Exception error = null;
    try (ConcurrentRdaSink<TestDatabase.Message, TestDatabase.Claim> pool =
        new ConcurrentRdaSink<>(5, 9, database::createSink)) {
      for (List<TestDatabase.Message> messageList : createBatches(messages, 9)) {
        pool.writeMessages(VERSION, messageList);
      }
    } catch (Exception ex) {
      error = ex;
    }
    assertTrue(error instanceof ProcessingException, "caught the exception");
    assertTrue(
        ((ProcessingException) error).getOriginalCause()
            instanceof DataTransformer.TransformationException,
        "exception is a transformation error");
    assertTrue(database.allClosed(), "all sinks closed");
  }

  /**
   * Tests that when there is a failure writing one of the messages, we get a {@link
   * ProcessingException} and the sinks are closed correctly.
   */
  @Test
  public void testWriteFailure() {
    final TestDatabase database = new TestDatabase();
    final List<TestDatabase.Message> messages = createTestMessages();
    // trigger an i/o error on last message
    messages.set(messages.size() - 1, messages.get(messages.size() - 1).withFailOnWrite(true));

    Exception error = null;
    try (ConcurrentRdaSink<TestDatabase.Message, TestDatabase.Claim> pool =
        new ConcurrentRdaSink<>(5, 9, database::createSink)) {
      for (List<TestDatabase.Message> messageList : createBatches(messages, 9)) {
        pool.writeMessages(VERSION, messageList);
      }
    } catch (Exception ex) {
      error = ex;
    }
    assertTrue(error instanceof ProcessingException, "caught the exception");
    assertTrue(
        ((ProcessingException) error).getOriginalCause() instanceof IOException,
        "exception is an i/o error");
    assertTrue(database.allClosed(), "all sinks closed");
  }

  /**
   * Creates some messages for the test to queue and write.
   *
   * @return the list of messages
   */
  private List<TestDatabase.Message> createTestMessages() {
    List<String> claimIds =
        IntStream.range(1000, 2000).mapToObj(String::valueOf).collect(Collectors.toList());
    List<String> repeatedClaimIds =
        IntStream.range(0, 10)
            .boxed()
            .flatMap(ignored -> claimIds.stream())
            .collect(Collectors.toList());
    Collections.shuffle(repeatedClaimIds, new Random(1000));
    List<TestDatabase.Message> messages = new ArrayList<>();
    for (int i = 0; i < repeatedClaimIds.size(); ++i) {
      String claimId = repeatedClaimIds.get(i);
      long sequenceNumber = i + 1;
      messages.add(
          new TestDatabase.Message(claimId, claimId + "-" + sequenceNumber, sequenceNumber));
    }
    return messages;
  }

  /**
   * Gets the lists of claims expected to be written.
   *
   * @param messages the messages to be turned into claims
   * @return the list of expected claims
   */
  private List<TestDatabase.Claim> expectedClaims(List<TestDatabase.Message> messages) {
    Map<String, TestDatabase.Claim> uniqueClaims = new TreeMap<>();
    for (TestDatabase.Message message : messages) {
      uniqueClaims.put(message.getClaimId(), message.toClaim(VERSION));
    }
    return List.copyOf(uniqueClaims.values());
  }

  private List<List<TestDatabase.Message>> createBatches(
      List<TestDatabase.Message> messages, int batchSize) {
    List<List<TestDatabase.Message>> batches = new ArrayList<>();
    List<TestDatabase.Message> batch = new ArrayList<>();
    for (TestDatabase.Message message : messages) {
      batch.add(message);
      if (batch.size() == batchSize) {
        batches.add(List.copyOf(batch));
        batch.clear();
      }
    }
    if (batch.size() > 0) {
      batches.add(List.copyOf(batch));
    }
    return List.copyOf(batches);
  }
}
