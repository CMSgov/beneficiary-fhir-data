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
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.Nonnull;
import lombok.AllArgsConstructor;
import lombok.Data;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

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
        IntStream.rangeClosed(0, maxThreads)
            .boxed()
            .map(i -> new ClaimWriter(sinkFactory.get()))
            .collect(ImmutableList.toImmutableList());
    sequenceNumberWriter = new SequenceNumberWriter(sinkFactory.get());
    sink = sinkFactory.get();
    sequenceNumbers = new SequenceNumberTracker(0);
    running = true;
    prevProcessedCount = 0;
    currentProcessedCount = 0;
    error = null;
    latch = new CountDownLatch(2);
    reactiveSink = Sinks.many().unicast().onBackpressureBuffer();
    var claimProcessing =
        reactiveSink
            .asFlux()
            .groupBy(this::workerForMessage)
            .flatMap(
                group ->
                    group
                        .publishOn(Schedulers.boundedElastic())
                        .flatMap(message -> group.key().process(message), claimWriters.size()))
            .subscribe(this::processResult, this::addError, latch::countDown);
    var sequenceNumberProcessing =
        Flux.interval(Duration.ofSeconds(30))
            .flatMap(sequenceNumberWriter::updateDb)
            .subscribeOn(Schedulers.boundedElastic())
            .subscribe(seq -> {}, this::addError, latch::countDown);
    disposable = Disposables.composite(claimProcessing, sequenceNumberProcessing);
  }

  private ClaimWriter workerForMessage(Message message) {
    final var claimId = getClaimIdForMessage(message.message);
    final var hash = Hasher.hashString(claimId, StandardCharsets.UTF_8);
    final var index = Math.abs(hash.asInt()) % claimWriters.size();
    return claimWriters.get(index);
  }

  @Override
  public int writeMessages(String dataVersion, List<TMessage> messages) throws ProcessingException {
    for (TMessage message : messages) {
      final var emitResult = reactiveSink.tryEmitNext(new Message(dataVersion, message));
      if (emitResult != Sinks.EmitResult.OK) {
        throw new ProcessingException(new IOException("publish failed: " + emitResult), 0);
      }
    }
    return 0;
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
    synchronized (lock) {
      if (running) {
        reactiveSink.tryEmitComplete();
        running = false;
      }
    }
    try {
      var closer = new MultiCloser();
      closer.close(() -> latch.await(waitTime.toMillis(), TimeUnit.MILLISECONDS));
      closer.close(disposable::dispose);
      for (ClaimWriter claimWriter : claimWriters) {
        closer.close(claimWriter::close);
      }
      closer.close(sequenceNumberWriter::close);
      closer.finish();
    } catch (Exception ex) {
      throw new ProcessingException(ex, 0);
    }
  }

  @Override
  public void close() throws Exception {
    shutdown(Duration.ofMinutes(2));
  }

  @Data
  private class Message {
    private final String apiVersion;
    private final TMessage message;

    private TClaim asClaim() {
      return sink.transformMessage(apiVersion, message);
    }
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
          lastSequenceNumber = newSequenceNumber;
          return Flux.just(newSequenceNumber);
        } catch (Exception ex) {
          return Flux.error(ex);
        }
      } else {
        return Flux.empty();
      }
    }

    private void close() throws Exception {
      updateDb(0L).singleOrEmpty().block();
      sink.close();
    }
  }

  private class ClaimWriter {
    private final List<Message> allMessages = new ArrayList<>();
    private final Map<String, Message> uniqueMessages = new LinkedHashMap<>();
    private final RdaSink<TMessage, TClaim> sink;

    private ClaimWriter(RdaSink<TMessage, TClaim> sink) {
      this.sink = sink;
    }

    private Flux<Result> process(Message message) {
      boolean flushNeeded = false;
      var claimId = sink.getClaimIdForMessage(message.message);
      uniqueMessages.put(claimId, message);
      allMessages.add(message);
      if (uniqueMessages.size() >= batchSize) {
        flushNeeded = true;
      }
      if (flushNeeded) {
        return flush();
      } else {
        return Flux.empty();
      }
    }

    @Nonnull
    private Flux<Result> flush() {
      int processed = 0;
      try {
        var batch = new ArrayList<TClaim>();
        for (Message messageForBatch : uniqueMessages.values()) {
          batch.add(messageForBatch.asClaim());
        }
        processed = sink.writeClaims(batch);
        return Flux.just(new Result(processed, messageList()));
      } catch (Exception ex) {
        return Flux.error(ex);
      } finally {
        allMessages.clear();
        uniqueMessages.clear();
      }
    }

    private void close() throws Exception {
      flush().singleOrEmpty().block();
      sink.close();
    }

    private List<TMessage> messageList() {
      return allMessages.stream().map(Message::getMessage).collect(Collectors.toList());
    }
  }
}
