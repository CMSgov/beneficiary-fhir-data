package gov.cms.bfd.pipeline.rda.grpc.sink.concurrent;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import gov.cms.bfd.pipeline.rda.grpc.MultiCloser;
import gov.cms.bfd.pipeline.rda.grpc.ProcessingException;
import gov.cms.bfd.pipeline.rda.grpc.RdaSink;
import gov.cms.model.dsl.codegen.library.DataTransformer;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.Nonnull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
public class ReactiveRdaSink<TMessage, TClaim> implements RdaSink<TMessage, TClaim> {
  /** Used to identify {@link #IdleMessage}. */
  private static final long IdleSequenceNumber = -1;

  /** Used to identify {@link #FlushMessage}. */
  private static final long FlushSequenceNumber = -2;

  /**
   * Interval used to check if a claim worker is idle. Two consecutive checks when a worker is idle
   * will flush the worker's buffer to the database. Thus the period of time after which the flush
   * will happen is actually twice the interval.
   */
  private static final Duration IdleCheckInterval = Duration.ofMillis(500);

  /** Used to synchronize access to {@link #running}. */
  private final RWLock lock = new RWLock();

  /** Message used to tell a claim writer to flush its buffer immediately. */
  final Message<TMessage> FlushMessage = new Message<>(null, FlushSequenceNumber, null, null);

  /**
   * Message used to allow a claim writer to flush its buffer when it has been idle for too long.
   */
  final Message<TMessage> IdleMessage = new Message<>(null, IdleSequenceNumber, null, null);

  /** Used to assign claims to workers based on their claimId values. */
  private static final HashFunction Hasher = Hashing.goodFastHash(64);

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
  private Throwable error;

  /**
   * Used to synchronize shutdown by waiting for both claim and sequence number processing to
   * complete.
   */
  private final CountDownLatch latch;

  /**
   * Used to push messages to the claim and sequence number writers as well as to signal when there
   * are no more messages.
   */
  private final BlockingPublisher<Message<TMessage>> publisher;

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
            .groupBy(message -> claimPartitioner.partitionFor(message.claimId))
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
            .subscribe(this::processResult, this::processError);
    var sequenceNumberProcessing =
        Flux.interval(Duration.ofMillis(250), sequenceNumberWriterScheduler)
            .takeWhile(o -> isRunning())
            .flatMap(sequenceNumberWriter::updateDb)
            .doFinally(o -> latch.countDown())
            .subscribe(seq -> {}, this::processError);
    referenceToProcessors = Disposables.composite(claimProcessing, sequenceNumberProcessing);
    log.debug("created instance: threads={} batchSize={}", maxThreads, batchSize);
  }

  @Override
  public int writeMessages(String dataVersion, List<TMessage> messages) throws ProcessingException {
    for (TMessage message : messages) {
      String claimId = getClaimIdForMessage(message);
      long sequenceNumber = getSequenceNumberForObject(message);
      sequenceNumbers.addActiveSequenceNumber(sequenceNumber);
      try {
        publisher.emit(new Message<>(claimId, sequenceNumber, dataVersion, message));
      } catch (InterruptedException ex) {
        throw new ProcessingException(ex, 0);
      }
    }
    return getProcessedCount();
  }

  private void processResult(Result<TMessage> result) {
    publisher.allow(result.messages.size());
    for (Message<TMessage> message : result.messages) {
      sequenceNumbers.removeWrittenSequenceNumber(message.sequenceNumber);
    }
    lock.doWrite(() -> currentProcessedCount += result.processed);
  }

  private void processError(Throwable ex) {
    lock.doWrite(
        () -> {
          if (error == null) {
            error = ex;
          } else {
            error.addSuppressed(ex);
          }
          running = false;
        });
  }

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
  public int getProcessedCount() throws ProcessingException {
    synchronized (lock) {
      int answer = currentProcessedCount - prevProcessedCount;
      prevProcessedCount = currentProcessedCount;
      return answer;
    }
  }

  @Override
  public void shutdown(Duration waitTime) throws ProcessingException {
    //    new RuntimeException("SHUTDOWN CALLED").printStackTrace();
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
        log.info("shutdown close claimWriter {}", claimWriter.id);
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
   * interrupts during cancellation.
   *
   * @param waitTime how long to wait for the latch
   * @throws Exception if any exception caused the wait to fail
   */
  private void waitForLatch(Duration waitTime) throws Exception {
    InterruptedException error = null;
    for (int i = 1; i <= 10; ++i) {
      try {
        log.info("interrupted? {}", Thread.interrupted());
        latch.await(waitTime.toMillis(), TimeUnit.MILLISECONDS);
        error = null;
        break;
      } catch (InterruptedException ex) {
        if (error == null) {
          error = ex;
        }
      }
    }
    if (error != null) {
      throw error;
    }
  }

  @Override
  public void close() throws Exception {
    log.info("close called");
    shutdown(Duration.ofMinutes(2));
    log.info("close complete");
  }

  @Data
  @AllArgsConstructor
  private static class Message<TMessage> {
    private final String claimId;
    private final long sequenceNumber;
    private final String apiVersion;
    private final TMessage message;
  }

  @AllArgsConstructor
  private static class Result<TMessage> {
    private final int processed;
    private final List<Message<TMessage>> messages;
  }

  private static class SequenceNumberWriter<TMessage, TClaim> {
    private final RdaSink<TMessage, TClaim> sink;
    /** Used to track sequence numbers to update progress table in database. */
    private final SequenceNumberTracker sequenceNumbers;

    private long lastSequenceNumber = 0;

    private SequenceNumberWriter(
        RdaSink<TMessage, TClaim> sink, SequenceNumberTracker sequenceNumbers) {
      this.sink = sink;
      this.sequenceNumbers = sequenceNumbers;
    }

    private Flux<Long> updateDb(Long time) {
      long newSequenceNumber = sequenceNumbers.getSafeResumeSequenceNumber();
      if (newSequenceNumber != lastSequenceNumber) {
        try {
          sink.updateLastSequenceNumber(newSequenceNumber);
          log.debug(
              "SequenceNumberWriter updated last={} new={}", lastSequenceNumber, newSequenceNumber);
          lastSequenceNumber = newSequenceNumber;
          return Flux.just(newSequenceNumber);
        } catch (Exception ex) {
          log.error("SequenceNumberWriter error: {}", ex.getMessage(), ex);
          return Flux.error(ex);
        }
      } else {
        return Flux.empty();
      }
    }

    private void close() throws Exception {
      log.debug("SequenceNumberWriter closing");
      updateDb(0L).singleOrEmpty().block();
      sink.close();
      log.info("SequenceNumberWriter closed");
    }
  }

  @EqualsAndHashCode(onlyExplicitlyIncluded = true)
  private static class ClaimWriter<TMessage, TClaim> {
    @EqualsAndHashCode.Include private final int id;
    private final RdaSink<TMessage, TClaim> sink;
    private final int batchSize;
    private final List<Message<TMessage>> messageBuffer;
    private final Map<String, TClaim> claimBuffer;
    private boolean idle;

    private ClaimWriter(int id, RdaSink<TMessage, TClaim> sink, int batchSize) {
      this.id = id;
      this.sink = sink;
      this.batchSize = batchSize;
      messageBuffer = new ArrayList<>();
      claimBuffer = new LinkedHashMap<>();
    }

    private synchronized Mono<Result<TMessage>> processMessage(Message<TMessage> message) {
      Mono<Result<TMessage>> result = Mono.empty();
      try {
        final boolean writeNeeded;
        if (message.sequenceNumber == IdleSequenceNumber) {
          writeNeeded = idle && claimBuffer.size() > 0;
          idle = true;
        } else if (message.sequenceNumber == FlushSequenceNumber) {
          writeNeeded = claimBuffer.size() > 0;
          idle = false;
        } else {
          sink.transformMessage(message.apiVersion, message.message)
              .ifPresent(claim -> claimBuffer.put(message.claimId, claim));
          messageBuffer.add(message);
          writeNeeded = claimBuffer.size() >= batchSize;
          idle = false;
        }
        if (writeNeeded) {
          var messages = List.copyOf(messageBuffer);
          var claims = List.copyOf(claimBuffer.values());
          messageBuffer.clear();
          claimBuffer.clear();
          final int processed = sink.writeClaims(claims);
          //          log.info(
          //              "ClaimWriter {} wrote unique={} all={} processed={} idle={} seq={}",
          //              id,
          //              claims.size(),
          //              messages.size(),
          //              processed,
          //              idle,
          //              message.sequenceNumber);
          result = Mono.just(new Result<>(processed, messages));
        }
      } catch (Exception ex) {
        log.error("ClaimWriter {} error: {}", id, ex.getMessage(), ex);
        result = Mono.error(ex);
      }
      return result;
    }

    private synchronized void close() throws Exception {
      log.debug("ClaimWriter {} closing", id);
      sink.close();
      log.info("ClaimWriter {} closed", id);
    }
  }
}
