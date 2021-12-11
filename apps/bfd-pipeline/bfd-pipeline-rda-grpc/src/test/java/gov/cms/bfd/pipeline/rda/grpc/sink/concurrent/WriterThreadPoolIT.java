package gov.cms.bfd.pipeline.rda.grpc.sink.concurrent;

import static org.junit.Assert.*;

import gov.cms.bfd.pipeline.rda.grpc.ProcessingException;
import gov.cms.bfd.pipeline.rda.grpc.source.DataTransformer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.Test;

public class WriterThreadPoolIT {
  private static final String VERSION = "Version";

  @Test
  public void testSuccess() throws Exception {
    final TestDatabase database = new TestDatabase();
    final List<TestDatabase.Message> messages = createTestMessages();
    try (WriterThreadPool<TestDatabase.Message, TestDatabase.Claim> pool =
        new WriterThreadPool<>(17, 11, database::createSink)) {
      for (TestDatabase.Message message : messages) {
        pool.addToQueue(VERSION, message);
      }
    }
    assertTrue("all sinks closed", database.allClosed());
    assertEquals(expectedClaims(messages), database.getClaims());
    assertEquals(messages.size(), database.getLastSequenceNumber());
  }

  @Test
  public void testTransformFailure() throws Exception {
    final TestDatabase database = new TestDatabase();
    final List<TestDatabase.Message> messages = createTestMessages();
    // trigger a transform error on last message
    messages.set(messages.size() - 1, messages.get(messages.size() - 1).withFailOnTransform(true));

    Exception error = null;
    try (WriterThreadPool<TestDatabase.Message, TestDatabase.Claim> pool =
        new WriterThreadPool<>(5, 9, database::createSink)) {
      for (TestDatabase.Message message : messages) {
        pool.addToQueue(VERSION, message);
      }
    } catch (Exception ex) {
      error = ex;
    }
    assertTrue("caught the exception", error instanceof ProcessingException);
    assertTrue(
        "exception is a transformation error",
        ((ProcessingException) error).getOriginalCause()
            instanceof DataTransformer.TransformationException);
    assertTrue("all sinks closed", database.allClosed());
  }

  @Test
  public void testWriteFailure() throws Exception {
    final TestDatabase database = new TestDatabase();
    final List<TestDatabase.Message> messages = createTestMessages();
    // trigger an i/o error on last message
    messages.set(messages.size() - 1, messages.get(messages.size() - 1).withFailOnWrite(true));

    Exception error = null;
    try (WriterThreadPool<TestDatabase.Message, TestDatabase.Claim> pool =
        new WriterThreadPool<>(5, 9, database::createSink)) {
      for (TestDatabase.Message message : messages) {
        pool.addToQueue(VERSION, message);
      }
    } catch (Exception ex) {
      error = ex;
    }
    assertTrue("caught the exception", error instanceof ProcessingException);
    assertTrue(
        "exception is an i/o error",
        ((ProcessingException) error).getOriginalCause() instanceof IOException);
    assertTrue("all sinks closed", database.allClosed());
  }

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

  private List<TestDatabase.Claim> expectedClaims(List<TestDatabase.Message> messages) {
    Map<String, TestDatabase.Claim> uniqueClaims = new TreeMap<>();
    for (TestDatabase.Message message : messages) {
      uniqueClaims.put(message.getClaimId(), message.toClaim(VERSION));
    }
    return List.copyOf(uniqueClaims.values());
  }
}
