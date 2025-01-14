package gov.cms.bfd.pipeline.rda.grpc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import gov.cms.mpsm.rda.v1.ClaimSequenceNumberRange;
import jakarta.annotation.Nonnull;
import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

/** Tests the {@link RdaSink}. */
public class RdaSinkTest {
  /** The rda sink under test. */
  private final RdaSink<Integer, Integer> sink = new TestSink();

  /**
   * Verifies that {@link RdaSink#writeMessages} correctly counts the number of written messages.
   *
   * @throws Exception indicates test exception
   */
  @Test
  public void batchSuccessful() throws Exception {
    int count = sink.writeMessages("", Arrays.asList(1, 2, 3, 4));
    assertEquals(4, count);
  }

  /**
   * Verifies that {@link RdaSink#writeMessages} correctly throws an exception on failures and
   * counts the number of exceptions.
   */
  @Test
  public void batchFailures() {
    try {
      sink.writeMessages("", Arrays.asList(1, 2, 5, 4));
      fail("sink should have thrown");
    } catch (ProcessingException ex) {
      assertEquals(12, ex.getProcessedCount());
      MatcherAssert.assertThat(ex.getCause(), Matchers.instanceOf(IOException.class));
    }
    try {
      sink.writeMessages("", Arrays.asList(1, 2, 6, 5));
      fail("sink should have thrown");
    } catch (ProcessingException ex) {
      assertEquals(2, ex.getProcessedCount());
      MatcherAssert.assertThat(ex.getCause(), Matchers.instanceOf(RuntimeException.class));
    }
  }

  /** A test implementation of an RDA sink. */
  private static class TestSink implements RdaSink<Integer, Integer> {
    /** {@inheritDoc} */
    @Override
    public int writeMessage(String dataVersion, Integer object) throws ProcessingException {
      if (object == 5) {
        throw new ProcessingException(new IOException("oops"), 10);
      }
      if (object == 6) {
        throw new RuntimeException("oops");
      }
      return 1;
    }

    /** {@inheritDoc} */
    @Override
    public void checkErrorCount() {
      // Do Nothing
    }

    @Override
    public String getClaimIdForMessage(Integer object) {
      return String.valueOf(object);
    }

    /** {@inheritDoc} */
    @Override
    public void updateLastSequenceNumber(long lastSequenceNumber) {}

    /** {@inheritDoc} */
    @Override
    public long getSequenceNumberForObject(Integer object) {
      return object;
    }

    @Override
    public void updateSequenceNumberRange(ClaimSequenceNumberRange sequenceNumberRange) {}

    /** {@inheritDoc} */
    @Override
    public void close() throws Exception {}

    /** {@inheritDoc} */
    @Nonnull
    @Override
    public Optional<Integer> transformMessage(String apiVersion, Integer integer) {
      return Optional.of(integer);
    }

    /** {@inheritDoc} */
    @Override
    public int writeClaims(Collection<Integer> objects) {
      throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Override
    public int getProcessedCount() throws ProcessingException {
      return 0;
    }

    /** {@inheritDoc} */
    @Override
    public void shutdown(Duration waitTime) {}
  }
}
