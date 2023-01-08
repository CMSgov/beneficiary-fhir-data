package gov.cms.bfd.pipeline.rda.grpc.sink.concurrent;

import com.google.common.collect.ImmutableList;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import gov.cms.bfd.pipeline.rda.grpc.MultiCloser;
import gov.cms.bfd.pipeline.rda.grpc.ProcessingException;
import gov.cms.bfd.pipeline.rda.grpc.RdaSink;
import gov.cms.model.dsl.codegen.library.DataTransformer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import javax.annotation.Nonnull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

@Slf4j
public class ReactiveRdaSink<TMessage, TClaim> implements RdaSink<TMessage, TClaim> {
  private final Object lock = new Object();

  /** Used to assign claims to workers based on their claimId values. */
  private static final HashFunction Hasher = Hashing.goodFastHash(64);

  /** Used to track sequence numbers to update progress table in database. */
  private final SequenceNumberTracker sequenceNumbers;
  /** Used to perform database i/o. */
  private final RdaSink<TMessage, TClaim> sink;

  private boolean running;
  private int prevProcessedCount;
  private int currentProcessedCount;
  private Throwable error;
  private final CountDownLatch latch;

  private final Publisher<TMessage> publisher;
  private final Disposable disposable;

  /** Used to assign claims to workers based on their claimId values. */
  private final HashPartitioner hashPartitioner;

  private final List<ClaimWriter<TMessage, TClaim>> claimWriters;
  private final SequenceNumberWriter<TMessage, TClaim> sequenceNumberWriter;

  public ReactiveRdaSink(
      int maxThreads, int batchSize, Supplier<RdaSink<TMessage, TClaim>> sinkFactory) {
    hashPartitioner = new HashPartitioner(maxThreads);
    sequenceNumbers = new SequenceNumberTracker(0);
    sink = sinkFactory.get();
    claimWriters =
        IntStream.rangeClosed(1, maxThreads)
            .boxed()
            .map(i -> new ClaimWriter<>(i, sinkFactory.get()))
            .collect(ImmutableList.toImmutableList());
    sequenceNumberWriter = new SequenceNumberWriter<>(sinkFactory.get(), sequenceNumbers);
    running = true;
    prevProcessedCount = 0;
    currentProcessedCount = 0;
    error = null;
    latch = new CountDownLatch(2);
    publisher = new Publisher<>(batchSize * maxThreads * 3);
    var claimWriterScheduler =
        Schedulers.newBoundedElastic(
            claimWriters.size(),
            25 * claimWriters.size(),
            "claims-" + sink.getClass().getSimpleName());
    var sequenceNumberWriterScheduler =
        Schedulers.newBoundedElastic(1, 25, "seqs-" + sink.getClass().getSimpleName());
    var claimProcessing =
        publisher
            .flux
            .groupBy(this::workerForMessage)
            .flatMap(
                group ->
                    group
                        .publishOn(claimWriterScheduler)
                        .buffer(batchSize)
                        .flatMap(message -> group.key().writeBuffer(message), claimWriters.size()))
            .doFinally(o -> latch.countDown())
            .subscribe(this::processResult, this::addError);
    var sequenceNumberProcessing =
        Flux.interval(Duration.ofMillis(250))
            .takeWhile(o -> isRunning())
            .flatMap(sequenceNumberWriter::updateDb)
            .subscribeOn(sequenceNumberWriterScheduler)
            .doFinally(o -> latch.countDown())
            .subscribe(seq -> {}, this::addError);
    disposable = Disposables.composite(claimProcessing, sequenceNumberProcessing);
    log.debug("created instance: threads={} batchSize={}", maxThreads, batchSize);
  }

  private ClaimWriter<TMessage, TClaim> workerForMessage(Message<TMessage> message) {
    final var index = hashPartitioner.bucketIndexForString(message.claimId);
    return claimWriters.get(index);
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
    log.debug("writeMessages: complete count={}", messages.size());
    return getProcessedCount();
  }

  private void processResult(Result<TMessage> result) {
    for (Message<TMessage> message : result.messages) {
      sequenceNumbers.removeWrittenSequenceNumber(message.sequenceNumber);
    }
    addToProcessedCount(result.processed);
  }

  private void addToProcessedCount(int count) {
    synchronized (lock) {
      currentProcessedCount += count;
    }
  }

  private void addError(Throwable ex) {
    synchronized (lock) {
      if (error == null) {
        error = ex;
      } else {
        error.addSuppressed(ex);
      }
      running = false;
    }
  }

  private boolean isRunning() {
    synchronized (lock) {
      return running;
    }
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

  @Nonnull
  @Override
  public TClaim transformMessage(String apiVersion, TMessage message)
      throws DataTransformer.TransformationException {
    return sink.transformMessage(apiVersion, message);
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
    synchronized (lock) {
      if (running) {
        publisher.complete();
        log.info("shutdown emit complete");
        running = false;
      }
    }
    try {
      var closer = new MultiCloser();
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
          log.info(
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

    private ClaimWriter(int id, RdaSink<TMessage, TClaim> sink) {
      this.id = id;
      this.sink = sink;
    }

    private Flux<Result<TMessage>> writeBuffer(List<Message<TMessage>> allMessages) {
      final var uniqueMessages = new LinkedHashMap<String, Message<TMessage>>();
      for (Message<TMessage> message : allMessages) {
        uniqueMessages.put(message.claimId, message);
      }
      final var batch = new ArrayList<TClaim>();
      try {
        for (Message<TMessage> message : uniqueMessages.values()) {
          final var claim = sink.transformMessage(message.apiVersion, message.message);
          batch.add(claim);
        }
        final int processed = sink.writeClaims(batch);
        log.info(
            "ClaimWriter {} wrote unique={} all={} processed={}",
            id,
            uniqueMessages.size(),
            allMessages.size(),
            processed);
        return Flux.just(new Result<>(processed, allMessages));
      } catch (Exception ex) {
        log.error("ClaimWriter {} error: {}", id, ex.getMessage(), ex);
        return Flux.error(ex);
      }
    }

    private void close() throws Exception {
      log.debug("ClaimWriter {} closing", id);
      sink.close();
      log.info("ClaimWriter {} closed", id);
    }
  }

  private static class Publisher<TMessage> {
    private final Semaphore available;
    private final Sinks.Many<Message<TMessage>> reactiveSink;
    private final Flux<Message<TMessage>> flux;

    private Publisher(int maxAvailable) {
      available = new Semaphore(maxAvailable, true);
      reactiveSink = Sinks.many().unicast().onBackpressureBuffer();
      flux = reactiveSink.asFlux().publishOn(Schedulers.boundedElastic()).doOnNext(o -> consumed());
    }

    private void emit(Message<TMessage> message) throws InterruptedException {
      available.acquire();
      reactiveSink.emitNext(message, Sinks.EmitFailureHandler.FAIL_FAST);
    }

    private void complete() {
      reactiveSink.emitComplete(Sinks.EmitFailureHandler.FAIL_FAST);
    }

    private void consumed() {
      available.release();
    }
  }

  /**
   * Divides strings into buckets using a hash function. Determines which bucket to assign to by
   * assigning a range of hash values to each bucket. This provides a more even distribution than
   * simple modulo by number of buckets.
   */
  private static class HashPartitioner {
    private static final HashFunction Hasher = Hashing.goodFastHash(32);

    private final int maxBuckets;
    private final int maxHash;
    private final int hashToIndexDivisor;

    private HashPartitioner(int maxBuckets) {
      this.maxBuckets = maxBuckets;
      hashToIndexDivisor = Integer.MAX_VALUE / maxBuckets;
      maxHash = hashToIndexDivisor * maxBuckets;
    }

    /**
     * Select the bucket index for the given string.
     *
     * @param stringToHash value to hash
     * @return index of bucket (zero to maxBuckets-1)
     */
    private int bucketIndexForString(String stringToHash) {
      final var rawHash = Hasher.hashString(stringToHash, StandardCharsets.UTF_8).asInt();
      final var inBoundsHash = Math.abs(rawHash) % maxHash;
      final var index = inBoundsHash / hashToIndexDivisor;
      if (index >= maxBuckets) {
        log.error(
            "division produced large index: divisor={} max={} hash={} inRange={} index={}",
            hashToIndexDivisor,
            maxHash,
            rawHash,
            inBoundsHash,
            index);
      }
      return index;
    }
  }
}
