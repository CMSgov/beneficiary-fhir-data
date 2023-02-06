package gov.cms.bfd.pipeline.rda.grpc.sink.reactive;

import gov.cms.bfd.pipeline.rda.grpc.MultiCloser;
import gov.cms.bfd.pipeline.rda.grpc.ProcessingException;
import gov.cms.bfd.pipeline.rda.grpc.RdaSink;
import gov.cms.bfd.pipeline.rda.grpc.sink.concurrent.SequenceNumberTracker;
import gov.cms.model.dsl.codegen.library.DataTransformer;
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
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

@Slf4j
public class ReactiveRdaSink<TMessage, TClaim> implements RdaSink<TMessage, TClaim> {
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
  final ApiMessage<TMessage> FlushMessage = ApiMessage.createFlushMessage();

  /**
   * Message used to allow a claim writer to flush its buffer when it has been idle for too long.
   */
  final ApiMessage<TMessage> IdleMessage = ApiMessage.createIdleMessage();

  /** Used to track sequence numbers to update progress table in database. */
  private final SequenceNumberTracker sequenceNumbers;

  /** Used to perform database i/o. */
  private final RdaSink<TMessage, TClaim> sink;

  /** Used to signal when a shutdown is in progress. */
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
  private final AtomicReference<Exception> errors;

  /**
   * Used to synchronize shutdown by waiting for both claim and sequence number processing to
   * complete.
   */
  private final CountDownLatch latch;

  /**
   * Used to push messages to the claim and sequence number writers as well as to signal when there
   * are no more messages.
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
   * Constructs an instance with the specified configuration. Actual writes are delegated to
   * single-threaded sink objects produced using the provided factory method.
   *
   * @param maxThreads number of writer threads used to write claims
   * @param batchSize number of messages per batch for database writes
   * @param sinkFactory factory method to produce appropriate single threaded sinks
   */
  public ReactiveRdaSink(
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
    errors = new AtomicReference<>();
    latch = new CountDownLatch(2); // one each for claim and sequence number fluxes
    final var claimWriterScheduler =
        Schedulers.newBoundedElastic(
            maxThreads, maxThreads, sink.getClass().getSimpleName() + "ClaimWriter");
    final var sequenceNumberWriterScheduler =
        Schedulers.newBoundedElastic(
            1, 1, sink.getClass().getSimpleName() + "-SequenceNumberWriter");
    final var otherScheduler = Schedulers.boundedElastic();
    final var claimPartitioner = new StringPartitioner<>(claimWriters);
    publisher = new BlockingPublisher<>(4 * maxThreads * batchSize);
    final var idleTimerFlux =
        Flux.interval(IdleCheckInterval, claimWriterScheduler)
            .takeWhile(o -> isRunning())
            .onBackpressureLatest()
            .map(o -> IdleMessage);
    var claimProcessing =
        publisher
            .flux()
            .publishOn(otherScheduler)
            .groupBy(message -> claimPartitioner.partitionFor(message.getClaimId()))
            .flatMap(
                group ->
                    group
                        .concatWithValues(FlushMessage)
                        .mergeWith(idleTimerFlux)
                        .publishOn(claimWriterScheduler)
                        .concatMap(message -> group.key().processMessage(message)),
                maxThreads)
            .publishOn(otherScheduler)
            .doFinally(o -> latch.countDown())
            .subscribe(this::processBatchResult);
    var sequenceNumberProcessing =
        Flux.interval(SequenceNumberUpdateInterval, sequenceNumberWriterScheduler)
            .takeWhile(o -> isRunning())
            .onBackpressureLatest()
            .flatMap(sequenceNumberWriter::updateDb)
            .doFinally(o -> latch.countDown())
            .subscribe(seq -> {}, ex -> processBatchResult(new BatchResult<>((Exception) ex)));
    referenceToProcessors = Disposables.composite(claimProcessing, sequenceNumberProcessing);
  }

  @Override
  public int writeMessages(String dataVersion, List<TMessage> messages) throws ProcessingException {
    throwIfErrorPresent();
    for (TMessage message : messages) {
      String claimId = getClaimIdForMessage(message);
      long sequenceNumber = getSequenceNumberForObject(message);
      sequenceNumbers.addActiveSequenceNumber(sequenceNumber);
      try {
        publisher.emit(new ApiMessage<>(claimId, sequenceNumber, dataVersion, message));
      } catch (InterruptedException ex) {
        throw new ProcessingException(ex, 0);
      }
    }
    return getProcessedCount();
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
      errors.compareAndSet(null, result.getError());
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
   * This method is not implemented since that would bypass the queue used to schedule writes.
   *
   * @param lastSequenceNumber sequence number to write to the database
   */
  @Override
  public void updateLastSequenceNumber(long lastSequenceNumber) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getClaimIdForMessage(TMessage object) {
    return sink.getClaimIdForMessage(object);
  }

  @Override
  public long getSequenceNumberForObject(TMessage object) {
    return sink.getSequenceNumberForObject(object);
  }

  @Override
  public Optional<Long> readMaxExistingSequenceNumber() throws ProcessingException {
    return sink.readMaxExistingSequenceNumber();
  }

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

  @Override
  public int writeClaims(Collection<TClaim> objects) throws ProcessingException {
    throw new ProcessingException(new UnsupportedOperationException(), 0);
  }

  @Override
  public int getProcessedCount() {
    return unreportedProcessedCount.getAndSet(0);
  }

  private void throwIfErrorPresent() throws ProcessingException {
    final var error = errors.getAndSet(null);
    if (error != null) {
      throw new ProcessingException(error, 0);
    }
  }

  @Override
  public void shutdown(Duration waitTime) throws ProcessingException {
    log.info("shutdown called");
    if (running.getAndSet(false)) {
      publisher.complete();
      log.info("shutdown emit complete");
    }
    try {
      final var closer = new MultiCloser();
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
      log.info("shutdown check for errrors");
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

  /**
   * Waits for countdown latch to reach zero so that rest of shutdown can proceed. The claim and
   * sequence number writers decrement the latch as they complete so once it reach zero we know they
   * are finished. We retry a few times in case of interrupts to resolve problems with spurious
   * interrupts during cancellation. We can't stop the shutdown process completely so we return even
   * if the latch doesn't reach zero before the timeout elapses.
   *
   * @param waitTime how long to wait for the latch
   * @throws Exception if any exception caused the wait to fail
   */
  private void waitForLatch(Duration waitTime) throws Exception {
    final long startMillis = System.currentTimeMillis();
    InterruptedException error = null;
    boolean successful = false;
    for (int i = 1; i <= 10; ++i) {
      try {
        long elapsedMillis = System.currentTimeMillis() - startMillis;
        long waitMillis = waitTime.toMillis() - elapsedMillis;
        if (waitMillis <= 0) {
          break;
        }
        if (latch.await(waitMillis, TimeUnit.MILLISECONDS)) {
          error = null;
          successful = true;
          break;
        }
      } catch (InterruptedException ex) {
        if (error == null) {
          error = ex;
        }
      }
    }
    if (error != null) {
      throw error;
    }
    if (!successful) {
      log.warn("waitForLatch: wait time exceeded without reaching zero");
    }
  }

  @Override
  public void close() throws Exception {
    log.info("close called");
    shutdown(Duration.ofMinutes(2));
    log.info("close complete");
  }
}
