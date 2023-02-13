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
import java.util.Optional;
import java.util.TreeMap;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.With;

/** Provides infrastructure needed when unit testing ConcurrentRdaSink and associated classes. */
@ThreadSafe
public class TestDatabase {

  /** The sinks for the database. */
  private final List<Sink> sinks = new ArrayList<>();
  /** The claims 'written' to the database. */
  private final Map<String, Claim> claims = new TreeMap<>();
  /** The last set sequence number. */
  private long lastSequenceNumber;

  /**
   * Adds a new sync to the internal list of sinks and returns it.
   *
   * @return the created rda sink
   */
  public synchronized RdaSink<Message, Claim> createSink() {
    var sink = new Sink();
    sinks.add(sink);
    return sink;
  }

  /**
   * Gets the {@link #lastSequenceNumber}.
   *
   * @return the last sequence number
   */
  public synchronized long getLastSequenceNumber() {
    return lastSequenceNumber;
  }

  /**
   * Gets an immutable list of the {@link #claims}.
   *
   * @return the claims
   */
  public synchronized List<Claim> getClaims() {
    return ImmutableList.copyOf(claims.values());
  }

  /**
   * Determines if all sinks are closed.
   *
   * @return {@link true} if ALL sinks are closed
   */
  public synchronized boolean allClosed() {
    return sinks.stream().allMatch(s -> s.closed);
  }

  /**
   * Sets the {@link #lastSequenceNumber}. Will fail an assertion if the sequence number being set
   * is not larger than the current {@link #lastSequenceNumber}.
   *
   * @param value the new sequence number to set
   */
  private synchronized void setLastSequenceNumber(long value) {
    assertTrue(value >= lastSequenceNumber, "sequenceNumber should only increase");
    lastSequenceNumber = value;
  }

  /**
   * Adds a {@link Claim} to the list of {@link #claims}.
   *
   * @param claim the claim to add
   */
  private synchronized void addClaim(Claim claim) {
    claims.put(claim.getClaimId(), claim);
  }

  /** Represents a simplified message as used in a sink for use in testing. */
  @Value
  @AllArgsConstructor
  public static class Message {
    /** The message claim id. */
    String claimId;
    /** The claim data of this message. */
    String claimData;
    /** The sequence number of this message. */
    long sequenceNumber;
    /** If this message should simulate a failure on transformation. */
    @With public boolean failOnTransform;
    /** If this message should simulate a failure on writing. */
    @With public boolean failOnWrite;

    /**
     * Instantiates a new Message.
     *
     * @param claimId the claim id
     * @param claimData the claim data
     * @param sequenceNumber the sequence number
     */
    public Message(String claimId, String claimData, long sequenceNumber) {
      this(claimId, claimData, sequenceNumber, false, false);
    }

    /**
     * Creates a test claim given an api version using the data of this message.
     *
     * @param apiVersion the api version
     * @return the claim
     */
    public Claim toClaim(String apiVersion) {
      return new Claim(claimId, claimData, sequenceNumber, apiVersion, failOnWrite);
    }
  }

  /** Represents a simplified test claim to be used in a sink. */
  @Value
  @AllArgsConstructor
  public static class Claim {
    /** The claim id. */
    String claimId;
    /** The claim's simplified data. */
    String claimData;
    /** The sequence number. */
    long sequenceNumber;
    /** The api version. */
    String apiVersion;
    /** If this claim should simulate failing on writing. */
    boolean failOnWrite;

    /**
     * Instantiates a new Claim.
     *
     * @param claimId the claim id
     * @param claimData the claim data
     * @param sequenceNumber the sequence number
     * @param apiVersion the api version
     */
    public Claim(String claimId, String claimData, long sequenceNumber, String apiVersion) {
      this(claimId, claimData, sequenceNumber, apiVersion, false);
    }

    /**
     * Creates a test message from this claim's data.
     *
     * @return the message
     */
    public Message toMessage() {
      return new Message(claimId, claimData, sequenceNumber);
    }
  }

  /** A test sink that uses simplified models for messages and claims. */
  @ThreadSafe
  private class Sink implements RdaSink<Message, Claim> {
    /** If this sink is closed. */
    private boolean closed;

    /**
     * Gets if this sink is {@link #closed}.
     *
     * @return {@link true} if this sink is closed
     */
    public synchronized boolean isClosed() {
      return closed;
    }

    /** {@inheritDoc} */
    @Override
    public void updateLastSequenceNumber(long lastSequenceNumber) {
      setLastSequenceNumber(lastSequenceNumber);
    }

    /** {@inheritDoc} */
    @Override
    public void writeError(
        String apiVersion, Message message, DataTransformer.TransformationException exception)
        throws ProcessingException {
      throw new ProcessingException(new IllegalStateException("Error limit reached"), 0);
    }

    @Override
    public void checkErrorCount() {
      // Do Nothing
    }

    /** {@inheritDoc} */
    @Override
    public String getClaimIdForMessage(Message message) {
      return message.claimId;
    }

    /** {@inheritDoc} */
    @Override
    public long getSequenceNumberForObject(Message message) {
      return message.getSequenceNumber();
    }

    /** {@inheritDoc} */
    @Nonnull
    @Override
    public Optional<Claim> transformMessage(String apiVersion, Message message) {
      if (message.isFailOnTransform()) {
        throw new DataTransformer.TransformationException(
            "fail", Collections.singletonList(new DataTransformer.ErrorMessage("none", "fail")));
      }
      return Optional.of(message.toClaim(apiVersion));
    }

    /** {@inheritDoc} */
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

    /** {@inheritDoc} */
    @Override
    public int getProcessedCount() throws ProcessingException {
      return 0;
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void shutdown(Duration waitTime) {
      throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void close() throws Exception {
      closed = true;
    }
  }
}
