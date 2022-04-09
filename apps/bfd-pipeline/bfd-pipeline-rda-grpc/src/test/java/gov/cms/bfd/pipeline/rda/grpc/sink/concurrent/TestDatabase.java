package gov.cms.bfd.pipeline.rda.grpc.sink.concurrent;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.ImmutableList;
import gov.cms.bfd.pipeline.rda.grpc.ProcessingException;
import gov.cms.bfd.pipeline.rda.grpc.RdaSink;
import gov.cms.model.dsl.codegen.library.DataTransformer;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.With;

/** Provides infrastructure needed when unit testing ConcurrentRdaSink and associated classes. */
@ThreadSafe
public class TestDatabase {
  private final List<Sink> sinks = new ArrayList<>();
  private final Map<String, Claim> claims = new TreeMap<>();
  private long lastSequenceNumber;

  public synchronized RdaSink<Message, Claim> createSink() {
    var sink = new Sink();
    sinks.add(sink);
    return sink;
  }

  public synchronized long getLastSequenceNumber() {
    return lastSequenceNumber;
  }

  public synchronized List<Claim> getClaims() {
    return ImmutableList.copyOf(claims.values());
  }

  public synchronized boolean allClosed() {
    return sinks.stream().allMatch(s -> s.closed);
  }

  private synchronized void setLastSequenceNumber(long value) {
    assertTrue(value >= lastSequenceNumber, "sequenceNumber should only increase");
    lastSequenceNumber = value;
  }

  private synchronized void addClaim(Claim claim) {
    claims.put(claim.getClaimId(), claim);
  }

  @Value
  @AllArgsConstructor
  public static class Message {
    String claimId;
    String claimData;
    long sequenceNumber;
    @With public boolean failOnTransform;
    @With public boolean failOnWrite;

    public Message(String claimId, String claimData, long sequenceNumber) {
      this(claimId, claimData, sequenceNumber, false, false);
    }

    public Claim toClaim(String apiVersion) {
      return new Claim(claimId, claimData, sequenceNumber, apiVersion, failOnWrite);
    }
  }

  @Value
  @AllArgsConstructor
  public static class Claim {
    String claimId;
    String claimData;
    long sequenceNumber;
    String apiVersion;
    boolean failOnWrite;

    public Claim(String claimId, String claimData, long sequenceNumber, String apiVersion) {
      this(claimId, claimData, sequenceNumber, apiVersion, false);
    }

    public Message toMessage() {
      return new Message(claimId, claimData, sequenceNumber);
    }
  }

  @ThreadSafe
  private class Sink implements RdaSink<Message, Claim> {
    private boolean closed;

    public synchronized boolean isClosed() {
      return closed;
    }

    @Override
    public void updateLastSequenceNumber(long lastSequenceNumber) {
      setLastSequenceNumber(lastSequenceNumber);
    }

    @Override
    public String getDedupKeyForMessage(Message message) {
      return message.claimId;
    }

    @Override
    public long getSequenceNumberForObject(Message message) {
      return message.getSequenceNumber();
    }

    @Nonnull
    @Override
    public Claim transformMessage(String apiVersion, Message message) {
      if (message.isFailOnTransform()) {
        throw new DataTransformer.TransformationException(
            "fail", Collections.singletonList(new DataTransformer.ErrorMessage("none", "fail")));
      }
      return message.toClaim(apiVersion);
    }

    @Override
    public int writeClaims(Collection<Claim> claims) throws ProcessingException {
      int count = 0;
      for (Claim claim : claims) {
        if (claim.isFailOnWrite()) {
          throw new ProcessingException(new IOException("fail"), count);
        }
        addClaim(claim);
        count += 1;
      }
      return claims.size();
    }

    @Override
    public int getProcessedCount() throws ProcessingException {
      return 0;
    }

    @Override
    public synchronized void shutdown(Duration waitTime) throws ProcessingException {
      throw new UnsupportedOperationException();
    }

    @Override
    public synchronized void close() throws Exception {
      closed = true;
    }
  }
}
