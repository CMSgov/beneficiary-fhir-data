package gov.cms.bfd.pipeline.rif.load;

import java.util.ArrayList;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * This small utility class can be used to process {@link Stream} elements in batches, without
 * violating any of the {@link Stream}'s assumptions about laziness and parallelizability.
 *
 * <p>It was taken from this Stack Overflow answer: <a
 * href="https://stackoverflow.com/a/41748361/1851299">Bruce Hamilton's custom BatchSpliterator</a>.
 */
public final class BatchSpliterator<T> implements Spliterator<List<T>> {
  private final Spliterator<T> base;
  private final int batchSize;

  public BatchSpliterator(Spliterator<T> base, int batchSize) {
    this.base = base;
    this.batchSize = batchSize;
  }

  public static <T> Stream<List<T>> batches(Stream<T> stream, int batchSize) {
    return batchSize <= 0
        ? Stream.of(stream.collect(Collectors.toList()))
        : StreamSupport.stream(
            new BatchSpliterator<>(stream.spliterator(), batchSize), stream.isParallel());
  }

  @Override
  public boolean tryAdvance(Consumer<? super List<T>> action) {
    final List<T> batch = new ArrayList<>(batchSize);
    for (int i = 0; i < batchSize && base.tryAdvance(batch::add); i++) ;
    if (batch.isEmpty()) return false;
    action.accept(batch);
    return true;
  }

  @Override
  public Spliterator<List<T>> trySplit() {
    if (base.estimateSize() <= batchSize) return null;
    final Spliterator<T> splitBase = this.base.trySplit();
    return splitBase == null ? null : new BatchSpliterator<>(splitBase, batchSize);
  }

  @Override
  public long estimateSize() {
    final double baseSize = base.estimateSize();
    return baseSize == 0 ? 0 : (long) Math.ceil(baseSize / (double) batchSize);
  }

  @Override
  public int characteristics() {
    return base.characteristics();
  }
}
