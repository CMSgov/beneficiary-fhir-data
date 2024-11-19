package gov.cms.bfd.pipeline.rda.grpc.sink.direct;

import com.codahale.metrics.MetricRegistry;
import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.util.JsonFormat;
import gov.cms.bfd.model.rda.MessageError;
import gov.cms.bfd.model.rda.RdaApiProgress;
import gov.cms.bfd.model.rda.RdaClaimMessageMetaData;
import gov.cms.bfd.pipeline.rda.grpc.NumericGauges;
import gov.cms.bfd.pipeline.rda.grpc.ProcessingException;
import gov.cms.bfd.pipeline.rda.grpc.RdaChange;
import gov.cms.bfd.pipeline.rda.grpc.RdaSink;
import gov.cms.bfd.pipeline.sharedutils.PipelineApplicationState;
import gov.cms.bfd.pipeline.sharedutils.SamhsaUtil;
import gov.cms.bfd.pipeline.sharedutils.TransactionManager;
import gov.cms.model.dsl.codegen.library.DataTransformer;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.Nonnull;
import jakarta.persistence.EntityManager;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.atomic.AtomicLong;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RdaSink implementation that writes TClaim objects to the database in batches within the calling
 * thread. This class is abstract and derived classes implement the methods that depend on the
 * actual class of RDA API message and database entity.
 *
 * @param <TMessage> type of RDA API messages written to the database
 * @param <TClaim> type of entity objects written to the database
 */
abstract class AbstractClaimRdaSink<TMessage, TClaim>
    implements RdaSink<TMessage, RdaChange<TClaim>> {
  /** The {@link TransactionManager} used to execute transactions. */
  protected final TransactionManager transactionManager;

  /** The metric reporter. */
  protected final Metrics metrics;

  /** Clock for creating timestamps. */
  protected final Clock clock;

  /** The log manager. */
  protected final Logger logger;

  /** Represents the claim type for this sink. */
  protected final RdaApiProgress.ClaimType claimType;

  /** Whether to automatically update the sequence number. */
  protected final boolean autoUpdateLastSeq;

  /** The number of claim errors that can exist before the job will stop processing. */
  private final int errorLimit;

  /** Holds the underlying value of our sequence number gauges. */
  private static final NumericGauges GAUGES = new NumericGauges();

  /** Used to write out RDA messages to json strings. */
  protected static final JsonFormat.Printer protobufObjectWriter =
      JsonFormat.printer().omittingInsignificantWhitespace();

  /**
   * Constructs an instance using the provided appState and claimType. Sequence numbers can either
   * be written in the same transaction as their associated claims (autoUpdateLastSeq=true) or not
   * auto updated (autoUpdateLastSeq=false). Generally the former is used for single-threaded
   * processing and the latter for multi-threaded processing.
   *
   * @param appState provides database and metrics configuration
   * @param claimType used to write claim type when recording sequence number updates
   * @param autoUpdateLastSeq controls whether sequence numbers are automatically written to the
   *     database
   * @param errorLimit the number of claim errors that can exist before the job will stop processing
   */
  protected AbstractClaimRdaSink(
      PipelineApplicationState appState,
      RdaApiProgress.ClaimType claimType,
      boolean autoUpdateLastSeq,
      int errorLimit) {
    transactionManager = new TransactionManager(appState.getEntityManagerFactory());
    metrics = new Metrics(getClass(), appState.getMeters());
    clock = appState.getClock();
    logger = LoggerFactory.getLogger(getClass());
    this.claimType = claimType;
    this.autoUpdateLastSeq = autoUpdateLastSeq;
    this.errorLimit = errorLimit;
  }

  /** {@inheritDoc} */
  @Override
  public void close() {
    resetLatencyMetrics();
    transactionManager.close();
  }

  /**
   * Queries the RdaApiProgress table to get the maximum sequence number for our claim type.
   *
   * @return max sequence number or empty if there is no record for this claim type
   * @throws ProcessingException if the operation fails
   */
  @Override
  public Optional<Long> readMaxExistingSequenceNumber() throws ProcessingException {
    try {
      return transactionManager.executeFunction(
          entityManager -> {
            logger.info("running query to find max sequence number");
            String query =
                String.format(
                    "select p.%s from RdaApiProgress p where p.claimType='%s'",
                    RdaApiProgress.Fields.lastSequenceNumber, claimType.name());
            final List<Long> sequenceNumber =
                entityManager.createQuery(query, Long.class).getResultList();
            final Optional<Long> answer =
                sequenceNumber.isEmpty() ? Optional.empty() : Optional.of(sequenceNumber.get(0));
            logger.info(
                "max sequence number result is {}",
                answer.map(n -> Long.toString(n)).orElse("none"));
            return answer;
          });
    } catch (Exception ex) {
      throw new ProcessingException(ex, 0);
    }
  }

  /**
   * Writes the sequence number to the database in the calling thread.
   *
   * @param lastSequenceNumber value to write to the database
   */
  @Override
  public void updateLastSequenceNumber(long lastSequenceNumber) {
    transactionManager.executeProcedure(
        entityManager -> updateLastSequenceNumberImpl(entityManager, lastSequenceNumber));
  }

  /**
   * Writes out the transformation error to the database for the given message and given apiVersion.
   *
   * @param apiVersion The version of the api used to get the message.
   * @param message The message that was being transformed when the error occurred.
   * @param exception The exception that was thrown while transforming the message.
   * @throws IOException if there was an issue writing to the database.
   * @throws ProcessingException If the error limit was reached.
   */
  @Override
  public void writeError(
      String apiVersion, TMessage message, DataTransformer.TransformationException exception)
      throws IOException, ProcessingException {
    transactionManager.executeProcedure(
        entityManager ->
            entityManager.merge(createMessageError(apiVersion, message, exception.getErrors())));
    checkErrorCount();
  }

  /**
   * Checks if the allowed number of unresolved {@link MessageError}s in the database was exceeded.
   *
   * @throws ProcessingException If the error limit was reached.
   */
  @Override
  public void checkErrorCount() throws ProcessingException {
    final long errorCount =
        transactionManager.executeFunction(
            entityManager -> {
              var query =
                  entityManager.createQuery(
                      "select count(error) from MessageError error where status = :status and claimType = :claimType",
                      Long.class);
              query.setParameter("status", MessageError.Status.UNRESOLVED);
              query.setParameter("claimType", MessageError.ClaimType.valueOf(claimType.name()));
              return query.getSingleResult();
            });
    if (errorCount > errorLimit) {
      throw new ProcessingException(new IllegalStateException("Error limit reached"), 0);
    }
  }

  /**
   * Writes the claims immediately and returns the number written.
   *
   * @param apiVersion value for the apiSource column of the claim record
   * @param messages zero or more objects to be written to the data store
   * @return number of objects successfully processed
   * @throws ProcessingException if the operation fails
   */
  @Override
  public int writeMessages(String apiVersion, List<TMessage> messages) throws ProcessingException {
    try {
      final List<RdaChange<TClaim>> claims = transformMessages(apiVersion, messages);
      return writeClaims(claims);
    } catch (DataTransformer.TransformationException e) {
      throw new ProcessingException(e, 0);
    }
  }

  /**
   * Writes the claims to the database in the calling thread.
   *
   * @param claims objects to be written
   * @return number successfully written
   * @throws ProcessingException if the operation fails
   */
  @Override
  public int writeClaims(Collection<RdaChange<TClaim>> claims) throws ProcessingException {
    final long maxSeq = maxSequenceInBatch(claims);
    try {
      metrics.calls.increment();
      updateLatencyMetrics(claims);
      mergeBatch(maxSeq, claims);
      metrics.objectsMerged.increment(claims.size());
      logger.debug("writeBatch succeeded using merge: size={} maxSeq={} ", claims.size(), maxSeq);
    } catch (Exception error) {
      logger.error(
          "writeBatch failed: size={} maxSeq={} error={}",
          claims.size(),
          maxSeq,
          error.getMessage(),
          error);
      metrics.failures.increment();
      throw new ProcessingException(error, 0);
    }
    metrics.successes.increment();
    metrics.objectsWritten.increment(claims.size());
    return claims.size();
  }

  /**
   * Always returns zero since all claims are written synchronously by writeMessages.
   *
   * @return zero
   */
  @Override
  public int getProcessedCount() {
    return 0;
  }

  /**
   * Does nothing since there is no thread pool to close.
   *
   * @param waitTime maximum amount of time to wait for shutdown to complete
   * @throws ProcessingException if the operation fails
   */
  @Override
  public void shutdown(Duration waitTime) throws ProcessingException {}

  /**
   * Gets the {@link #metrics}.
   *
   * @return the metrics
   */
  @VisibleForTesting
  Metrics getMetrics() {
    return metrics;
  }

  /**
   * Apply implementation specific logic to produce a populated {@link RdaClaimMessageMetaData}
   * object suitable for insertion into the database to track this update.
   *
   * @param change an incoming RdaChange object from which to extract metadata
   * @return an object ready for insertion into the database
   */
  abstract RdaClaimMessageMetaData createMetaData(RdaChange<TClaim> change);

  /**
   * Helper method to generate {@link MessageError} entities from a given claim object. This is
   * implementation specific logic for each claim type.
   *
   * @param apiVersion The version of the api the message was pulled from.
   * @param change The claim change object that was being transformed when the error occurred.
   * @param errors The transformation errors that occurred during the claim transformation.
   * @return A new {@link MessageError} entity containing the details of the transformation error
   *     and associated claim change object.
   * @throws IOException If there was an issue writing the details to the {@link MessageError}
   *     entity.
   */
  abstract MessageError createMessageError(
      String apiVersion, TMessage change, List<DataTransformer.ErrorMessage> errors)
      throws IOException;

  /**
   * Updates the {@link RdaApiProgress} table with the sequence number for the most recently added
   * claim of a given type.
   *
   * @param entityManager the {@link EntityManager} to use for the update.
   * @param lastSequenceNumber The sequence number of the most recently added claim of a given type.
   */
  private void updateLastSequenceNumberImpl(EntityManager entityManager, long lastSequenceNumber) {
    RdaApiProgress progress =
        RdaApiProgress.builder()
            .claimType(claimType)
            .lastSequenceNumber(lastSequenceNumber)
            .lastUpdated(clock.instant())
            .build();
    entityManager.merge(progress);
    metrics.setLatestSequenceNumber(lastSequenceNumber);
    logger.debug("updated max sequence number: type={} seq={}", claimType, lastSequenceNumber);
  }

  /**
   * Transforms all the messages in the collection into {@link RdaChange} objects and returns a
   * {@link List} of the converted changes.
   *
   * @param apiVersion appropriate string for the apiSource column of the claim table
   * @param messages collection of RDA API message objects of the correct type for this sync
   * @return the converted claims
   * @throws ProcessingException if any message in the collection triggered an error
   */
  @VisibleForTesting
  List<RdaChange<TClaim>> transformMessages(String apiVersion, Collection<TMessage> messages)
      throws ProcessingException {
    var claims = new ArrayList<RdaChange<TClaim>>();
    try {
      for (TMessage message : messages) {
        transformMessage(apiVersion, message).ifPresent(claims::add);
      }
    } catch (IOException e) {
      throw new ProcessingException(e, 0);
    }
    return claims;
  }

  /**
   * {@inheritDoc}
   *
   * <p>Delegates the actual transformation to derived classes by calling their {@link
   * #transformMessageImpl} method. Takes care of tracking transformations and errors in {@link
   * #metrics}.
   *
   * @param apiVersion appropriate string for the apiSource column of the claim table
   * @param message an RDA API message object of the correct type for this sync
   * @return an optional containing the converted claim if successful, {@link Optional#empty()}
   *     otherwise
   * @throws IOException if there was an issue writing out a {@link MessageError}
   * @throws ProcessingException if there was an issue transforming the message
   */
  @Nonnull
  @Override
  public Optional<RdaChange<TClaim>> transformMessage(String apiVersion, TMessage message)
      throws IOException, ProcessingException {
    Optional<RdaChange<TClaim>> result;

    try {
      var change = transformMessageImpl(apiVersion, message);
      metrics.transformSuccesses.increment();
      result = Optional.of(change);
    } catch (DataTransformer.TransformationException transformationException) {
      metrics.transformFailures.increment();
      logger.error("Claim transformation error", transformationException);
      writeError(apiVersion, message, transformationException);
      result = Optional.empty();
    }

    return result;
  }

  /**
   * Called by {@link #transformMessage} to perform just the message transformation step that is
   * specific to a particular message/claim combination.
   *
   * @param apiVersion appropriate string for the apiSource column of the claim table
   * @param message an RDA API message object of the correct type for this sync
   * @return an appropriate entity object containing the data from the message
   * @throws DataTransformer.TransformationException if the message is invalid
   */
  @Nonnull
  abstract RdaChange<TClaim> transformMessageImpl(String apiVersion, TMessage message)
      throws DataTransformer.TransformationException;

  /**
   * Implementation specific method to count the number of expected inserts that will be used to
   * load all the data into the database. Used for metrics and analysis.
   *
   * @param claim The claim data to be inserted
   * @return The calculated number of expected insert statements needed for the claim data.
   */
  abstract int getInsertCount(TClaim claim);

  /**
   * Uses {@link EntityManager#merge} to write each claim and its associated metadata to the
   * database.
   *
   * @param maxSeq highest sequence number from claims in the collection
   * @param changes collection of claims to write to the database
   */
  private void mergeBatch(long maxSeq, Collection<RdaChange<TClaim>> changes) {
    SamhsaUtil samhsaUtil = SamhsaUtil.getSamhsaUtil();
    transactionManager.executeProcedure(
        entityManager -> {
          final Instant startTime = Instant.now();
          int insertCount = 0;
          try {
            for (RdaChange<TClaim> change : changes) {
              if (change.getType() != RdaChange.Type.DELETE) {
                var metaData = createMetaData(change);
                entityManager.merge(metaData);
                entityManager.merge(change.getClaim());
                samhsaUtil.processClaim(change.getClaim(), entityManager);
                insertCount += getInsertCount(change.getClaim());
              } else {
                // We would expect this to have been filtered by the RdaSource so it is safe
                // to stop processing with an exception here.
                throw new IllegalArgumentException(
                    "RDA API DELETE changes are not currently supported");
              }
            }
            if (autoUpdateLastSeq) {
              updateLastSequenceNumberImpl(entityManager, maxSeq);
            }
          } finally {
            metrics.dbUpdateTime.record(Duration.between(startTime, Instant.now()));
            metrics.dbBatchSize.record(changes.size());
            metrics.insertCount.record(insertCount);
          }
        });
  }

  /**
   * Finds the highest sequence number in a collection of claims.
   *
   * @param claims claims to search
   * @return highest sequence number
   */
  private long maxSequenceInBatch(Collection<RdaChange<TClaim>> claims) {
    OptionalLong value = claims.stream().mapToLong(RdaChange::getSequenceNumber).max();
    if (value.isEmpty()) {
      // This should never happen! But if it does, we'll shout about it rather than throw an
      // exception
      logger.warn("processed an empty batch!");
    }
    return value.orElse(0L);
  }

  /**
   * Updates the latency metrics by adding the age of each claim to the histograms.
   *
   * @param claims claims to process
   */
  private void updateLatencyMetrics(Collection<RdaChange<TClaim>> claims) {
    final long nowMillis = clock.millis();
    for (RdaChange<TClaim> claim : claims) {
      final long changeMillis = claim.getTimestamp().toEpochMilli();
      final long changeAge = Math.max(0L, nowMillis - changeMillis);
      metrics.changeAgeMillis.record(changeAge);

      final LocalDate extractDate = claim.getSource().getExtractDate();
      if (extractDate != null) {
        final ZonedDateTime extractTime = extractDate.atStartOfDay().atZone(clock.getZone());
        final long extractMillis = extractTime.toInstant().toEpochMilli();
        final long extractAge = Math.max(0L, nowMillis - extractMillis);
        metrics.extractAgeMillis.record(extractAge);
      }
    }
  }

  /**
   * Called by {@link #close} method to reset latency metrics to zero when job has completed. This
   * prevents the dashboard latency graphs showing the last value continuously between job
   * executions.
   */
  private void resetLatencyMetrics() {
    metrics.changeAgeMillis.record(0L);
    metrics.extractAgeMillis.record(0L);
  }

  /**
   * Metrics are tested in unit tests so they need to be easily accessible from tests. Also this
   * class is used to write both MCS and FISS claims so the metric names need to include a claim
   * type to distinguish them.
   */
  @Getter
  @VisibleForTesting
  static class Metrics {
    /** Number of times the sink has been called to store objects. */
    private final Counter calls;

    /** Number of calls that successfully stored the objects. */
    private final Counter successes;

    /** Number of calls that failed to store the objects. */
    private final Counter failures;

    /** Number of objects written. */
    private final Counter objectsWritten;

    /** Number of objects stored using {@code persist()}. */
    private final Counter objectsPersisted;

    /** Number of objects stored using {@code merge()}. */
    private final Counter objectsMerged;

    /** Number of objects successfully transformed. */
    private final Counter transformSuccesses;

    /** Number of objects which failed to be transformed. */
    private final Counter transformFailures;

    /**
     * Milliseconds between change timestamp and current time, measures the latency between BFD
     * ingestion and when RDA ingestion.
     */
    private final DistributionSummary changeAgeMillis;

    /**
     * Milliseconds between extract date and current time, measures the latency between BFD
     * ingestion and when the MAC processes the data .
     */
    private final DistributionSummary extractAgeMillis;

    /** Tracks the elapsed time when we write claims to the database. */
    private final Timer dbUpdateTime;

    /** Tracks the number of updates per database transaction. */
    private final DistributionSummary dbBatchSize;

    /** Latest sequnce number from writing a batch. * */
    private final AtomicLong latestSequenceNumber;

    /** The value returned by the latestSequenceNumber gauge. * */
    private final AtomicLong latestSequenceNumberValue;

    /** The number of insert statements executed. */
    private final DistributionSummary insertCount;

    /**
     * Initializes all the metrics.
     *
     * @param klass used to derive metric names
     * @param appMetrics where to store the metrics
     */
    private Metrics(Class<?> klass, MeterRegistry appMetrics) {
      final String base = klass.getSimpleName();
      calls = appMetrics.counter(MetricRegistry.name(base, "calls"));
      successes = appMetrics.counter(MetricRegistry.name(base, "successes"));
      failures = appMetrics.counter(MetricRegistry.name(base, "failures"));
      objectsWritten = appMetrics.counter(MetricRegistry.name(base, "writes", "total"));
      objectsPersisted = appMetrics.counter(MetricRegistry.name(base, "writes", "persisted"));
      objectsMerged = appMetrics.counter(MetricRegistry.name(base, "writes", "merged"));
      transformSuccesses = appMetrics.counter(MetricRegistry.name(base, "transform", "successes"));
      transformFailures = appMetrics.counter(MetricRegistry.name(base, "transform", "failures"));
      changeAgeMillis =
          appMetrics.summary(MetricRegistry.name(base, "change", "latency", "millis"));
      extractAgeMillis =
          appMetrics.summary(MetricRegistry.name(base, "extract", "latency", "millis"));
      dbUpdateTime = appMetrics.timer(MetricRegistry.name(base, "writes", "elapsed"));
      dbBatchSize = appMetrics.summary(MetricRegistry.name(base, "writes", "batchSize"));
      String latestSequenceNumberGaugeName = MetricRegistry.name(base, "lastSeq");
      latestSequenceNumber = GAUGES.getGaugeForName(appMetrics, latestSequenceNumberGaugeName);
      latestSequenceNumberValue = GAUGES.getValueForName(latestSequenceNumberGaugeName);
      insertCount = appMetrics.summary(MetricRegistry.name(base, "insertCount"));
    }

    /**
     * Sets the {@link #latestSequenceNumber}.
     *
     * @param value value to set
     */
    @VisibleForTesting
    void setLatestSequenceNumber(long value) {
      latestSequenceNumberValue.set(value);
    }
  }
}
