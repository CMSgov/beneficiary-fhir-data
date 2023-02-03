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
  private static final Duration IdleCheckInterval = Duration.ofMillis(500);

  /** Used to synchronize access to mutable fields. */
  private final RWLock lock = new RWLock();

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
  private boolean running;

  /** Holds the last reported processed count. */
  private int prevProcessedCount;

  /** Holds the current processed count. */
  private int currentProcessedCount;

  /** The {@link Exception} (if any) that terminated processing. */
  private Exception error;

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

  public ReactiveRdaSink(
      int maxThreads, int batchSize, Supplier<RdaSink<TMessage, TClaim>> sinkFactory) {
    sequenceNumbers = new SequenceNumberTracker(0);
    sink = sinkFactory.get();
    claimWriters =
        IntStream.rangeClosed(1, maxThreads)
            .mapToObj(writerId -> new ClaimWriter<>(writerId, sinkFactory.get(), batchSize))
            .collect(Collectors.toUnmodifiableList());
    sequenceNumberWriter = new SequenceNumberWriter<>(sinkFactory.get(), sequenceNumbers);
    running = true;
    prevProcessedCount = 0;
    currentProcessedCount = 0;
    error = null;
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
            .map(o -> IdleMessage)
            .takeWhile(o -> isRunning());
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
        Flux.interval(Duration.ofMillis(250), sequenceNumberWriterScheduler)
            .takeWhile(o -> isRunning())
            .flatMap(sequenceNumberWriter::updateDb)
            .doFinally(o -> latch.countDown())
            .subscribe(seq -> {}, ex -> processBatchResult(new BatchResult<>((Exception) ex)));
    referenceToProcessors = Disposables.composite(claimProcessing, sequenceNumberProcessing);
    log.debug("created instance: threads={} batchSize={}", maxThreads, batchSize);
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
   * Updates state with the outcome of a batch write.
   *
   * @param result the details of the successful batch
   */
  private void processBatchResult(BatchResult<TMessage> result) {
    lock.doWrite(
        () -> {
          currentProcessedCount += result.getProcessedCount();
          if (result.getError() != null) {
            if (error == null) {
              error = result.getError();
            } else {
              error.addSuppressed(result.getError());
            }
          }
        });
    publisher.allow(result.getMessages().size());
    if (result.getError() == null) {
      for (ApiMessage<TMessage> message : result.getMessages()) {
        sequenceNumbers.removeWrittenSequenceNumber(message.getSequenceNumber());
      }
    }
  }

  /**
   * Remains true until shutdown is triggered.
   *
   * @return true if shutdown has not ben triggered
   */
  private boolean isRunning() {
    return lock.read(() -> running);
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
    final var countValue = new AtomicInteger();
    lock.doWrite(
        () -> {
          countValue.set(currentProcessedCount - prevProcessedCount);
          prevProcessedCount = currentProcessedCount;
        });
    return countValue.get();
  }

  private void throwIfErrorPresent() throws ProcessingException {
    final var errorValue = new AtomicReference<Exception>();
    lock.doRead(() -> errorValue.set(error));
    if (errorValue.get() != null) {
      throw new ProcessingException(errorValue.get(), 0);
    }
  }

  @Override
  public void shutdown(Duration waitTime) throws ProcessingException {
    log.info("shutdown called");
    lock.doWrite(
        () -> {
          if (running) {
            publisher.complete();
            log.info("shutdown emit complete");
            running = false;
          }
        });
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
      log.info("shutdown finish");
      closer.finish();
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
