package gov.cms.bfd.pipeline.rda.grpc.sink.concurrent;

import gov.cms.bfd.pipeline.rda.grpc.ProcessingException;
import gov.cms.bfd.pipeline.rda.grpc.RdaSink;
import gov.cms.bfd.pipeline.sharedutils.MultiCloser;
import gov.cms.bfd.pipeline.sharedutils.SequenceNumberTracker;
import gov.cms.model.dsl.codegen.library.DataTransformer;
import gov.cms.mpsm.rda.v1.ClaimSequenceNumberRange;
import jakarta.annotation.Nonnull;
import java.io.Closeable;
import java.io.IOException;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

/**
 * A sink implementation that uses a thread pool to perform all writes asynchronously.
 *
 * @param <TMessage> RDA API message class
 * @param <TClaim> JPA entity class
 */
@Slf4j
public class ConcurrentRdaSink<TMessage, TClaim> implements RdaSink<TMessage, TClaim> {
  /**
   * Interval used to check if a claim worker is idle. Two consecutive checks when a worker is idle
   * will flush the worker's buffer to the database. Thus the period of time after which the flush
   * will happen is actually twice the interval.
   */
  private static final Duration IdleCheckInterval = Duration.ofMillis(2_500);

  /**
   * Interval used to update the sequence number in the {@link gov.cms.bfd.model.rda.RdaApiProgress}
   * table. More frequent updates reduce memory consumption by the {@link SequenceNumberTracker} but
   * increase I/O overhead. This value is a good compromise.
   */
  private static final Duration SequenceNumberUpdateInterval = Duration.ofMillis(100);

  /** Message used to tell a claim writer to flush its buffer immediately. */
  private final ApiMessage<TMessage> FlushMessage = ApiMessage.createFlushMessage();

  /**
   * Message used to allow a claim writer to flush its buffer when it has been idle for too long.
   */
  private final ApiMessage<TMessage> IdleMessage = ApiMessage.createIdleMessage();

  /** Used to track sequence numbers to update progress table in database. */
  private final SequenceNumberTracker sequenceNumbers;

  /** Used to perform database i/o. */
  private final RdaSink<TMessage, TClaim> sink;

  /** Used to signal when a shutdown is in progress. Remains true until shutdown is triggered. */
  private final AtomicBoolean running;

  /**
   * Holds the unreported processed count until it can be collected by {@link #getProcessedCount()}.
   */
  private final AtomicInteger unreportedProcessedCount;

  /**
   * {@link Exception} (if any) that terminated processing. In some cases multiple exceptions might
   * be thrown by the various threads. The first exception enough to stop processing so we just
   * capture that one to avoid storing or logging hundreds of identical exceptions.
   */
  private final AtomicReference<Exception> error;

  /**
   * Used to synchronize shutdown by waiting for both claim and sequence number processing to
   * complete.
   */
  private final CountDownLatch shutdownSynchronizationLatch;

  /**
   * Instance of {@link BlockingPublisher} used to push messages to the claim and sequence number
   * writers as well as to signal when there are no more messages.
   */
  private final BlockingPublisher<ApiMessage<TMessage>> publisher;

  /**
   * Used to hold a reference to the claim and sequence number writers to ensure they are not
   * garbage collected. We never call dispose on this since our shutdown closes the flux using our
   * publisher rather than cancelling the flux from the subscriber end.
   */
  @SuppressWarnings("FieldCanBeLocal")
  private final Disposable referenceToProcessors;

  /**
   * Each of these handles a subset of the incoming claims. All claims with a given claim id are
   * processed by the same writer.
   */
  private final List<ClaimWriter<TMessage, TClaim>> claimWriters;

  /**
   * Used to periodically update the progress table with the our highest known to be complete
   * sequence number. Refer to {@link SequenceNumberTracker} for details on sequence number
   * tracking.
   */
  private final SequenceNumberWriter<TMessage, TClaim> sequenceNumberWriter;

  /**
   * {@link Scheduler} used to run {@link ClaimWriter#processMessage} calls. Using a custom
   * scheduler to ensure thread pool size matches our configuration and also to provide meaningful
   * names for worker threads when logging. Schedulers are {@link Closeable} so this is closed in
   * {@link #close}.
   */
  private final Scheduler claimWriterScheduler;

  /**
   * {@link Scheduler} used to run {@link SequenceNumberWriter#updateSequenceNumberInDatabase}
   * calls. Using a custom scheduler to ensure thread pool size matches our configuration and also
   * to provide meaningful names for worker threads when logging. Schedulers are {@link Closeable}
   * so this is closed in {@link #close}.
   */
  private final Scheduler sequenceNumberWriterScheduler;

  /**
   * Constructs a ConcurrentRdaSink with the specified configuration. Actual writes are delegated to
   * single-threaded sink objects produced using the provided factory method.
   *
   * @param maxThreads number of writer threads used to write claims
   * @param batchSize number of messages per batch for database writes
   * @param sinkFactory factory method to produce appropriate single threaded sinks
   */
  public ConcurrentRdaSink(
      int maxThreads, int batchSize, Supplier<RdaSink<TMessage, TClaim>> sinkFactory) {
    sequenceNumbers = new SequenceNumberTracker(0);
    sink = sinkFactory.get();
    claimWriters =
        IntStream.rangeClosed(1, maxThreads)
            .mapToObj(writerId -> new ClaimWriter<>(writerId, sinkFactory.get(), batchSize))
            .collect(Collectors.toUnmodifiableList());
    sequenceNumberWriter = new SequenceNumberWriter<>(sinkFactory.get(), sequenceNumbers);
    running = new AtomicBoolean(true);
    unreportedProcessedCount = new AtomicInteger(0);
    error = new AtomicReference<>();
    // Latch count is one each for claim and sequence number fluxes.
    shutdownSynchronizationLatch = new CountDownLatch(2);
    claimWriterScheduler =
        Schedulers.newBoundedElastic(
            maxThreads, maxThreads, sink.getClass().getSimpleName() + "ClaimWriter");
    sequenceNumberWriterScheduler =
        Schedulers.newBoundedElastic(
            1, 1, sink.getClass().getSimpleName() + "-SequenceNumberWriter");
    publisher = new BlockingPublisher<>(4 * maxThreads * batchSize);
    var claimProcessing =
        createClaimWriterFlux()
            .doFinally(o -> shutdownSynchronizationLatch.countDown())
            .subscribe(this::processBatchResult);
    var sequenceNumberProcessing =
        createSequenceNumberWriterFlux()
            .doFinally(o -> shutdownSynchronizationLatch.countDown())
            .subscribe(seq -> {}, ex -> processBatchResult(new BatchResult<>((Exception) ex)));
    referenceToProcessors = Disposables.composite(claimProcessing, sequenceNumberProcessing);
  }

  /**
   * Create an RdaSink using the specified number of threads. If maxThreads is one a single-threaded
   * sink is created using sinkFactory. Otherwise a ConcurrentRdaSink is created using the specified
   * number of threads. The sinkFactory function takes a boolean indicating whether the created sink
   * should manage sequence number updates itself (true) or not update sequence numbers (false).
   * This is needed because asynchronous sinks need to manage sequence numbers in a special way
   * while synchronous ones can just update the sequence numbers at same time they update claims.
   *
   * @param maxThreads number of writer threads used to write claims
   * @param batchSize number of messages per batch for database writes
   * @param sinkFactory factory method to produce appropriate single threaded sinks
   * @param <TMessage> RDA API message class
   * @param <TClaim> JPA entity class
   * @return either a simple sink or a ConcurrentRdaSink
   */
  public static <TMessage, TClaim> RdaSink<TMessage, TClaim> createSink(
      int maxThreads, int batchSize, Function<Boolean, RdaSink<TMessage, TClaim>> sinkFactory) {
    if (maxThreads == 1) {
      return sinkFactory.apply(true);
    } else {
      return new ConcurrentRdaSink<>(maxThreads, batchSize, () -> sinkFactory.apply(false));
    }
  }

  /**
   * Creates a {@link Flux} that uses a pool of {@link ClaimWriter} objects to transform, batch, and
   * write claims to the database. An idle timer is used to periodically flush any incomplete
   * batches during extended idle time. Such idle time can happen with RDA API calls when we are
   * storing claims faster than the API can send them to us.
   *
   * <p>The database updates take place using a worker from the {@link #claimWriterScheduler}.
   *
   * @return {@link Flux} that emits a {@link BatchResult} each time a batch is processed
   */
  private Flux<BatchResult<TMessage>> createClaimWriterFlux() {
    // Used to assign messages to a specific worker based on claim id.
    final var claimPartitioner = new StringPartitioner<>(claimWriters);

    // Flux used to trigger idle checks.
    // The interval flux will emit a time in milliseconds every time it fires.
    final var idleTimerFlux =
        Flux.interval(IdleCheckInterval, claimWriterScheduler)
            // Stop sending messages when shutdown sets the running flag to false.
            .takeWhile(o -> isRunning())
            // Drop any extra messages if the writer is currently busy.
            .onBackpressureLatest()
            // Replaces the time value with an idle message.
            .map(time -> IdleMessage);

    // Creates the flux.  Each operator call in the chain decorates the original with some desired
    // behavior.  See https://projectreactor.io/docs for details on how these work.
    return publisher
        .flux()
        // Ensures main thread is never tied down doing any processing.
        .publishOn(Schedulers.boundedElastic())
        // Assigns the message to its claim writer based on claim id.
        .groupBy(message -> claimPartitioner.partitionFor(message.getClaimId()))
        // Processes claims in each writer's flux using a separate thread for each.
        .flatMap(
            claimWriterFlux ->
                claimWriterFlux
                    // When all messages have been received this sends a flush message to finish up
                    .concatWithValues(FlushMessage)
                    // Inserts idle messages when the timer fires
                    .mergeWith(idleTimerFlux)
                    // Ensures we process everything on writer's own thread
                    .publishOn(claimWriterScheduler)
                    // Makes the call and passes its result down stream.  The key is our
                    // ClaimWriter object.
                    .concatMap(message -> claimWriterFlux.key().processMessage(message)))
        // Ensures downstream processing happens on some other thread so writer is free to keep
        // working on incoming messages.
        .publishOn(Schedulers.boundedElastic());
  }

  /**
   * Creates a {@link Flux} that periodically calls {@link
   * SequenceNumberWriter#updateSequenceNumberInDatabase} to ensure that the progress table has the
   * latest known good sequence number value. Since database writes can sometimes be slow the
   * interval timer skips any updates that are triggered while one is already in progress. An
   * interval is used because it's not vital that the progress table be constantly up to date and
   * excessive writes would just slow progress. The database updates take place using a worker from
   * the {@link #sequenceNumberWriterScheduler}.
   *
   * @return {@link Flux} that emits a sequence number every time the progress table is updated
   */
  private Flux<Long> createSequenceNumberWriterFlux() {
    // Flux used to trigger idle checks.
    // The interval flux will emit a time in milliseconds every time it fires.
    return Flux.interval(SequenceNumberUpdateInterval, sequenceNumberWriterScheduler)
        // Stop sending messages when shutdown sets the running flag to false.
        .takeWhile(o -> isRunning())
        // Drop any extra messages if the writer is currently busy.
        .onBackpressureLatest()
        // Calls updateDb each time the timer fires.
        .flatMap(o -> sequenceNumberWriter.updateSequenceNumberInDatabase());
  }

  /** {@inheritDoc} */
  @Override
  public Optional<Long> readMaxExistingSequenceNumber() throws ProcessingException {
    return sink.readMaxExistingSequenceNumber();
  }

  @Override
  public void updateSequenceNumberRange(ClaimSequenceNumberRange sequenceNumberRange) {
    sink.updateSequenceNumberRange(sequenceNumberRange);
  }

  /**
   * This method is not implemented since that would bypass the queue used to schedule writes.
   *
   * @param lastSequenceNumber sequence number to write to the database
   */
  @Override
  public void updateLastSequenceNumber(long lastSequenceNumber) {
    throw new UnsupportedOperationException();
  }

  /**
   * Enqueues the provided message objects for writing. They will be written to the database at some
   * unspecified point in the future. They may be written in a different order than they appear
   * within the collection EXCEPT that two objects corresponding to the same claim will always be
   * written in the order they appear in the collection.
   *
   * @param apiVersion version string for the apiSource column in the claim table
   * @param objects zero or more objects to be written to the data store
   * @return the number of objects written since last call to writeMessages or getProcessedCount
   * @throws ProcessingException if something goes wrong
   */
  @Override
  public int writeMessages(String apiVersion, List<TMessage> objects) throws ProcessingException {
    throwIfErrorPresent();
    try {
      for (TMessage object : objects) {
        final var claimId = getClaimIdForMessage(object);
        final var sequenceNumber = getSequenceNumberForObject(object);
        final var apiMessage = new ApiMessage<>(claimId, sequenceNumber, apiVersion, object);
        sequenceNumbers.addActiveSequenceNumber(sequenceNumber);
        publisher.emit(apiMessage);
      }
      return getProcessedCount();
    } catch (Exception ex) {
      throw new ProcessingException(ex, getProcessedCount());
    }
  }

  /** {@inheritDoc} */
  @Override
  public String getClaimIdForMessage(TMessage object) {
    return sink.getClaimIdForMessage(object);
  }

  /** {@inheritDoc} */
  @Override
  public long getSequenceNumberForObject(TMessage object) {
    return sink.getSequenceNumberForObject(object);
  }

  /** {@inheritDoc} */
  @Nonnull
  @Override
  public Optional<TClaim> transformMessage(String apiVersion, TMessage message)
      throws DataTransformer.TransformationException, IOException, ProcessingException {
    return sink.transformMessage(apiVersion, message);
  }

  @Override
  public void checkErrorCount() throws ProcessingException {
    sink.checkErrorCount();
  }

  /**
   * This method is not implemented since that would bypass the queue used to schedule writes.
   *
   * @param objects collection of entity objects to be written to the database
   * @throws ProcessingException always throws an exception
   */
  @Override
  public int writeClaims(Collection<TClaim> objects) throws ProcessingException {
    throw new ProcessingException(new UnsupportedOperationException(), 0);
  }

  /** {@inheritDoc} */
  @Override
  public int getProcessedCount() {
    return unreportedProcessedCount.getAndSet(0);
  }

  /**
   * Shuts down the publisher and all associated writers. Any unwritten data is flushed to the
   * database. Can safely be called multiple times.
   */
  @Override
  public void shutdown(Duration waitTime) throws ProcessingException {
    log.info("shutdown called");
    if (running.getAndSet(false)) {
      // Tells the claim writer flux there will be no more data to process.
      publisher.complete();
      log.info("shutdown emit complete");
      try {
        final var closer = new MultiCloser();
        // Waits until threads have stopped before closing other resoruces.
        log.info("shutdown wait for latch");
        closer.close(() -> waitForLatch(waitTime));
        for (ClaimWriter<TMessage, TClaim> claimWriter : claimWriters) {
          log.info("shutdown close claimWriter {}", claimWriter.getId());
          closer.close(claimWriter::close);
        }
        log.info("shutdown close sequenceWriter");
        closer.close(sequenceNumberWriter::close);
        log.info("shutdown close sink");
        closer.close(sink::close);
        log.info("shutdown close schedulers");
        closer.close(claimWriterScheduler::dispose);
        closer.close(sequenceNumberWriterScheduler::dispose);
        log.info("shutdown check for errors");
        closer.close(this::throwIfErrorPresent);
        log.info("shutdown finish");
        closer.finish();
      } catch (ProcessingException ex) {
        log.error("shutdown failed: ex={}", ex.getMessage(), ex);
        throw ex;
      } catch (Exception ex) {
        log.error("shutdown failed: ex={}", ex.getMessage(), ex);
        throw new ProcessingException(ex, 0);
      }
    }
  }

  /**
   * Shuts down the publisher and all associated writers. Any unwritten data is flushed to the
   * database. Internally just calls {@link #shutdown}. Can safely be called multiple times.
   */
  @Override
  public void close() throws Exception {
    log.info("close called");
    shutdown(Duration.ofMinutes(2));
    log.info("close complete");
  }

  /**
   * Check to see if any exception has been reported by the writers. If one has this method will
   * throw that exception wrapped in a {@link ProcessingException}. Otherwise it will simply return.
   *
   * @throws ProcessingException wrapper around an exception reported by a writer
   */
  private void throwIfErrorPresent() throws ProcessingException {
    final var error = this.error.getAndSet(null);
    if (error != null) {
      throw new ProcessingException(error, 0);
    }
  }

  /**
   * Performs necessary state updates based on the outcome of a batch write. Increments the
   * uncollected processed messages count, records the error (if any) or (if successful) updates the
   * set of written sequence numbers, then tells the publisher to allow more messages to be emitted.
   *
   * @param result the details of a completed batch from {@link ClaimWriter}
   */
  private void processBatchResult(BatchResult<TMessage> result) {
    unreportedProcessedCount.addAndGet(result.getProcessedCount());
    if (result.getError() != null) {
      error.compareAndSet(null, result.getError());
    } else {
      for (ApiMessage<TMessage> message : result.getMessages()) {
        sequenceNumbers.removeWrittenSequenceNumber(message.getSequenceNumber());
      }
    }
    publisher.allow(result.getMessages().size());
  }

  /**
   * Remains true until shutdown is triggered.
   *
   * @return true if shutdown has not ben triggered
   */
  private boolean isRunning() {
    return running.get();
  }

  /**
   * Waits for countdown latch to reach zero so that rest of shutdown can proceed. The claim and
   * sequence number writers decrement the latch as they complete so once it reaches zero we know
   * they are finished. We retry until {@code waitTime} is exceeded in case of interrupts to resolve
   * problems with spurious interrupts during cancellation. We can't stop the shutdown process
   * completely so we return even if the latch doesn't reach zero before the timeout elapses.
   *
   * @param waitTime how long to wait for the latch
   * @throws Exception if any exception caused the wait to fail
   */
  private void waitForLatch(Duration waitTime) throws Exception {
    final long endMillis = System.currentTimeMillis() + waitTime.toMillis();
    InterruptedException error = null;
    boolean countdownInProgress = true;
    while (countdownInProgress) {
      // Update the maximum wait time on each iteration to stay within allowed time.
      // Stop the loop if the time has been exceeded.
      final long remainingMillis = endMillis - System.currentTimeMillis();
      if (remainingMillis <= 0) {
        break;
      }

      // Check for countdown completion.  Remember if we encountered any interrupts along the way.
      try {
        log.info("waitForLatch: waiting at most {} millis for latch", remainingMillis);
        if (shutdownSynchronizationLatch.await(remainingMillis, TimeUnit.MILLISECONDS)) {
          countdownInProgress = false;
        }
      } catch (InterruptedException ex) {
        // We'll pass this through to our caller if the countdown never completes.
        if (error == null) {
          error = ex;
        }
      }
    }

    // Pass on any interrupt or log the timeout if the countdown never finished.
    if (countdownInProgress) {
      if (error != null) {
        throw error;
      }
      log.warn("waitForLatch: wait time exceeded without reaching zero");
    }
  }
}
