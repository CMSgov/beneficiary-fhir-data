package gov.cms.bfd.pipeline.rda.grpc.sink.concurrent;

import com.google.common.collect.ImmutableList;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import gov.cms.bfd.pipeline.rda.grpc.MultiCloser;
import gov.cms.bfd.pipeline.rda.grpc.ProcessingException;
import gov.cms.bfd.pipeline.rda.grpc.RdaSink;
import gov.cms.model.dsl.codegen.library.DataTransformer;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.AbstractQueue;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
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
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

@Slf4j
public class ReactiveRdaSink<TMessage, TClaim> implements RdaSink<TMessage, TClaim> {
  private final Object lock = new Object();

  /** Used to assign claims to workers based on their claimId values. */
  private static final HashFunction Hasher = Hashing.goodFastHash(32);

  /** Used to track sequence numbers to update progress table in database. */
  private final SequenceNumberTracker sequenceNumbers;
  /** Used to perform database i/o. */
  private final RdaSink<TMessage, TClaim> sink;

  private boolean running;
  private int prevProcessedCount;
  private int currentProcessedCount;
  private Throwable error;
  private final CountDownLatch latch;

  private final Sinks.Many<Message> reactiveSink;
  private final Disposable disposable;

  private final int batchSize;
  private final List<ClaimWriter> claimWriters;
  private final SequenceNumberWriter sequenceNumberWriter;

  public ReactiveRdaSink(
      int maxThreads, int batchSize, Supplier<RdaSink<TMessage, TClaim>> sinkFactory) {
    this.batchSize = batchSize;
    claimWriters =
        IntStream.rangeClosed(1, maxThreads)
            .boxed()
            .map(i -> new ClaimWriter(i, sinkFactory.get()))
            .collect(ImmutableList.toImmutableList());
    sequenceNumberWriter = new SequenceNumberWriter(sinkFactory.get());
    sink = sinkFactory.get();
    sequenceNumbers = new SequenceNumberTracker(0);
    running = true;
    prevProcessedCount = 0;
    currentProcessedCount = 0;
    error = null;
    latch = new CountDownLatch(2);
    reactiveSink =
        Sinks.many()
            .unicast()
            .onBackpressureBuffer(new BoundedQueue(maxThreads + maxThreads * batchSize));
    log.info("created instance: threads={} batchSize={}", maxThreads, batchSize);
    log.info("creating claim process");
    var reactiveFlux = reactiveSink.asFlux();
    var claimProcessing =
        reactiveFlux
            //            .doOnNext(m -> log.info(getClaimIdForMessage(m.message)))
            .groupBy(this::workerForMessage)
            .flatMap(
                group ->
                    group
                        .publishOn(Schedulers.boundedElastic())
                        .bufferTimeout(batchSize, Duration.ofSeconds(1))
                        .flatMap(message -> group.key().process(message), claimWriters.size()))
            .subscribe(this::processResult, this::addError, latch::countDown);
    log.info("creating sequenceNumber process");
    var sequenceNumberProcessing =
        Flux.interval(Duration.ofMillis(250))
            .takeWhile(o -> isRunning())
            .flatMap(sequenceNumberWriter::updateDb)
            .subscribeOn(Schedulers.boundedElastic())
            .subscribe(seq -> {}, this::addError, latch::countDown);
    log.info("combining disposables");
    disposable = Disposables.composite(claimProcessing, sequenceNumberProcessing);
    log.info("constructor complete");
  }

  private ClaimWriter workerForMessage(Message message) {
    final var claimId = getClaimIdForMessage(message.message);
    final var hash = Hasher.hashString(claimId, StandardCharsets.UTF_8);
    final var index = Math.abs(hash.asInt()) % claimWriters.size();
    log.info("hash {} to claimWriter {}", claimId, claimWriters.get(index).id);
    return claimWriters.get(index);
  }

  @Override
  public int writeMessages(String dataVersion, List<TMessage> messages) throws ProcessingException {
    for (TMessage message : messages) {
      sequenceNumbers.addActiveSequenceNumber(getSequenceNumberForObject(message));
      final var emitResult = reactiveSink.tryEmitNext(new Message(dataVersion, message));
      //      log.info("writeMessages: id={} res={}", getClaimIdForMessage(message), emitResult);
      if (emitResult != Sinks.EmitResult.OK) {
        throw new ProcessingException(new IOException("publish failed: " + emitResult), 0);
      }
    }
    log.info("writeMessages: complete count={}", messages.size());
    return getProcessedCount();
  }

  private void processResult(Result result) {
    for (TMessage message : result.messages) {
      sequenceNumbers.removeWrittenSequenceNumber(getSequenceNumberForObject(message));
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
    log.info("shutdown called");
    synchronized (lock) {
      if (running) {
        reactiveSink.tryEmitComplete();
        running = false;
        log.info("shutdown emit complete");
      }
    }
    try {
      var closer = new MultiCloser();
      log.info("shutdown wait for latch");
      closer.close(
          () -> {
            try {
              latch.await(waitTime.toMillis(), TimeUnit.MILLISECONDS);
            } catch (Exception ex) {
              Thread.interrupted();
              latch.await(waitTime.toMillis(), TimeUnit.MILLISECONDS);
            }
          });
      log.info("shutdown call dispose");
      closer.close(disposable::dispose);
      for (ClaimWriter claimWriter : claimWriters) {
        log.info("shutdown close claimWriter {}", claimWriter.id);
        closer.close(claimWriter::close);
      }
      log.info("shutdown close sequenceWriter");
      closer.close(sequenceNumberWriter::close);
      log.info("shutdown close finish");
      closer.finish();
    } catch (Exception ex) {
      throw new ProcessingException(ex, 0);
    }
  }

  @Override
  public void close() throws Exception {
    log.info("close called");
    shutdown(Duration.ofMinutes(2));
    log.info("close complete");
  }

  @Data
  private class Message {
    private final String apiVersion;
    private final TMessage message;
  }

  @AllArgsConstructor
  private class Result {
    private final int processed;
    private final List<TMessage> messages;
  }

  private class SequenceNumberWriter {
    private final RdaSink<TMessage, TClaim> sink;
    private long lastSequenceNumber = 0;

    private SequenceNumberWriter(RdaSink<TMessage, TClaim> sink) {
      this.sink = sink;
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
      log.info("SequenceNumberWriter closing");
      updateDb(0L).singleOrEmpty().block();
      sink.close();
      log.info("SequenceNumberWriter closed");
    }
  }

  @EqualsAndHashCode(onlyExplicitlyIncluded = true)
  private class ClaimWriter {
    @EqualsAndHashCode.Include private final int id;
    private final RdaSink<TMessage, TClaim> sink;

    private ClaimWriter(int id, RdaSink<TMessage, TClaim> sink) {
      this.id = id;
      this.sink = sink;
    }

    private Flux<Result> process(List<Message> allMessages) {
      log.info("ClaimWriter {} received {} messages", id, allMessages.size());
      final var uniqueMessages = new LinkedHashMap<String, Message>();
      for (Message message : allMessages) {
        uniqueMessages.put(sink.getClaimIdForMessage(message.message), message);
      }
      final var batch = new ArrayList<TClaim>();
      try {
        for (Message messageForBatch : uniqueMessages.values()) {
          batch.add(transform(messageForBatch));
        }
        int processed = 0;
        if (batch.size() > 0) {
          processed = sink.writeClaims(batch);
        }
        log.info(
            "ClaimWriter {} wrote unique={} all={} processed={}",
            id,
            uniqueMessages.size(),
            allMessages.size(),
            processed);
        return Flux.just(new Result(processed, messageList(allMessages)));
      } catch (Exception ex) {
        log.error("ClaimWriter {} error: {}", id, ex.getMessage(), ex);
        return Flux.error(ex);
      }
    }

    private void close() throws Exception {
      log.info("ClaimWriter closing");
      sink.close();
      log.info("ClaimWriter closed");
    }

    private TClaim transform(Message message) {
      return sink.transformMessage(message.apiVersion, message.message);
    }

    private List<TMessage> messageList(List<Message> allMessages) {
      return allMessages.stream().map(Message::getMessage).collect(Collectors.toList());
    }
  }

  private class BoundedQueue extends AbstractQueue<Message> {
    private final BlockingQueue<Message> queue;

    public BoundedQueue(int maxSize) {
      queue = new ArrayBlockingQueue<>(maxSize);
    }

    @Override
    public Iterator<Message> iterator() {
      return queue.iterator();
    }

    @Override
    public int size() {
      return queue.size();
    }

    @Override
    public boolean offer(Message message) {
      try {
        queue.put(message);
        return true;
      } catch (InterruptedException ex) {
        log.info("queue.offer was interrupted");
        return false;
      }
    }

    @Override
    public Message poll() {
      return queue.poll();
    }

    @Override
    public Message peek() {
      return queue.peek();
    }
  }
}
