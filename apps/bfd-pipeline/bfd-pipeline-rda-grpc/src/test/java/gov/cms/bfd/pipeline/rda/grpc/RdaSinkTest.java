package gov.cms.bfd.pipeline.rda.grpc;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import javax.annotation.Nonnull;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

public class RdaSinkTest {
  private final RdaSink<Integer, Integer> sink = new TestSink();

  @Test
  public void batchSuccessful() throws Exception {
    int count = sink.writeMessages("", Arrays.asList(1, 2, 3, 4));
    Assert.assertEquals(4, count);
  }

  @Test
  public void batchFailures() throws Exception {
    try {
      sink.writeMessages("", Arrays.asList(1, 2, 5, 4));
      Assert.fail("sink should have thrown");
    } catch (ProcessingException ex) {
      Assert.assertEquals(12, ex.getProcessedCount());
      MatcherAssert.assertThat(ex.getCause(), Matchers.instanceOf(IOException.class));
    }
    try {
      sink.writeMessages("", Arrays.asList(1, 2, 6, 5));
      Assert.fail("sink should have thrown");
    } catch (ProcessingException ex) {
      Assert.assertEquals(2, ex.getProcessedCount());
      MatcherAssert.assertThat(ex.getCause(), Matchers.instanceOf(RuntimeException.class));
    }
  }

  private static class TestSink implements RdaSink<Integer, Integer> {
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

    @Override
    public String getDedupKeyForMessage(Integer object) {
      return String.valueOf(object);
    }

    @Override
    public void updateLastSequenceNumber(long lastSequenceNumber) {}

    @Override
    public long getSequenceNumberForObject(Integer object) {
      return object;
    }

    @Override
    public void close() throws Exception {}

    @Nonnull
    @Override
    public Integer transformMessage(String apiVersion, Integer integer) {
      return integer;
    }

    @Override
    public int writeClaims(Collection<Integer> objects) throws ProcessingException {
      throw new UnsupportedOperationException();
    }

    @Override
    public int getProcessedCount() throws ProcessingException {
      return 0;
    }

    @Override
    public void shutdown(Duration waitTime) throws ProcessingException {}
  }
}
