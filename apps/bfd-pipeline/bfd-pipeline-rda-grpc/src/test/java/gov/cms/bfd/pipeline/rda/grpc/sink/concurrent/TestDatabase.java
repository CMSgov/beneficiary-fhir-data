package gov.cms.bfd.pipeline.rda.grpc.sink.concurrent;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.ImmutableList;
import gov.cms.bfd.pipeline.rda.grpc.ProcessingException;
import gov.cms.bfd.pipeline.rda.grpc.RdaSink;
import gov.cms.model.dsl.codegen.library.DataTransformer;
import gov.cms.mpsm.rda.v1.ClaimSequenceNumberRange;
import jakarta.annotation.Nonnull;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import javax.annotation.concurrent.ThreadSafe;
import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.With;

/** Provides infrastructure needed when unit testing ConcurrentRdaSink and associated classes. */
@ThreadSafe
public class TestDatabase {
  /** List of all of the {@link TestDatabase.Sink} instances that have been created. */
  private final List<Sink> sinks = new ArrayList<>();

  /** Map of all of the claims that have been written. Key is the claim id. */
  private final Map<String, Claim> claims = new TreeMap<>();

  /** Sequence number value simulating a progress table. */
  private long lastSequenceNumber;

  /**
   * Creates a new {@link TestDatabase.Sink} instance.
   *
   * @return the sink that was created
   */
  public synchronized RdaSink<Message, Claim> createSink() {
    var sink = new Sink();
    sinks.add(sink);
    return sink;
  }

  /**
   * Gets the current sequence number value.
   *
   * @return the sequence number
   */
  public synchronized long getLastSequenceNumber() {
    return lastSequenceNumber;
  }

  /**
   * Gets an immutable list of all claims that have been written to the database.
   *
   * @return the list of claims
   */
  public synchronized List<Claim> getClaims() {
    return ImmutableList.copyOf(claims.values());
  }

  /**
   * Verifies that all sinks have been closed.
   *
   * @return true if all of the sinks have been closed
   */
  public synchronized boolean allClosed() {
    return sinks.stream().allMatch(s -> s.closed);
  }

  /**
   * Sets the last sequence number. Simulates having a progress table.
   *
   * @param value sequence number value to save
   */
  private synchronized void setLastSequenceNumber(long value) {
    assertTrue(value >= lastSequenceNumber, "sequenceNumber should only increase");
    lastSequenceNumber = value;
  }

  /**
   * Simulates writing a claim to the database by adding it to our map.
   *
   * @param claim the claim to write
   */
  private synchronized void addClaim(Claim claim) {
    claims.put(claim.getClaimId(), claim);
  }

  /** Simulated message object meant to be used in tests alongside {@link TestDatabase.Claim}. */
  @Value
  @AllArgsConstructor
  public static class Message {
    /** Unique key for the claim. */
    String claimId;

    /** The claim. */
    String claimData;

    /** The message sequence number. */
    long sequenceNumber;

    /** When true attempts to transform the message into a claim should throw an exception. */
    @With public boolean failOnTransform;

    /** When true attempts to write the claim to the database should throw an exception. */
    @With public boolean failOnWrite;

    /**
     * Constructs an instance.
     *
     * @param claimId Unique key for the claim.
     * @param claimData the claim
     * @param sequenceNumber the message sequence number
     */
    public Message(String claimId, String claimData, long sequenceNumber) {
      this(claimId, claimData, sequenceNumber, false, false);
    }

    /**
     * Transforms this message into a {@link TestDatabase.Claim}.
     *
     * @param apiVersion RDA API version number string
     * @return the claim
     */
    public Claim toClaim(String apiVersion) {
      return new Claim(claimId, claimData, sequenceNumber, apiVersion, failOnWrite);
    }
  }

  /** Simulated claim object meant to be used in tests alongside {@link TestDatabase.Message}. */
  @Value
  @AllArgsConstructor
  public static class Claim {
    /** Unique key for the claim. */
    String claimId;

    /** The claim. */
    String claimData;

    /** The message sequence number. */
    long sequenceNumber;

    /** RDA API version number string. */
    String apiVersion;

    /** When true attempts to write the claim to the database should throw an exception. */
    boolean failOnWrite;

    /**
     * Constructs an instance.
     *
     * @param claimId Unique key for the claim.
     * @param claimData the claim
     * @param sequenceNumber the message sequence number
     * @param apiVersion RDA API version number string
     */
    public Claim(String claimId, String claimData, long sequenceNumber, String apiVersion) {
      this(claimId, claimData, sequenceNumber, apiVersion, false);
    }

    /**
     * Creates a {@link TestDatabase.Message} containing this claim.
     *
     * @return the message
     */
    public Message toMessage() {
      return new Message(claimId, claimData, sequenceNumber);
    }
  }

  /** An {@link RdaSink} instance used for testing. */
  @ThreadSafe
  private class Sink implements RdaSink<Message, Claim> {
    /** True if {@link #close()} has been called. */
    private boolean closed;

    /**
     * Determine if {@link #close()} has been called.
     *
     * @return true if {@link #close()} has been called.
     */
    public synchronized boolean isClosed() {
      return closed;
    }

    /** {@inheritDoc} */
    @Override
    public void updateLastSequenceNumber(long lastSequenceNumber) {
      setLastSequenceNumber(lastSequenceNumber);
    }

    @Override
    public void updateSequenceNumberRange(ClaimSequenceNumberRange sequenceNumberRange) {}

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
