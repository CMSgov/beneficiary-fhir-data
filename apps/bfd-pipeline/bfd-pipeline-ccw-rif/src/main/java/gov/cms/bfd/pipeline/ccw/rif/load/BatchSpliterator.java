package gov.cms.bfd.pipeline.ccw.rif.load;

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
  /** The base spliterator. */
  private final Spliterator<T> base;

  /** The batch size per split. */
  private final int batchSize;

  /**
   * Instantiates a new Batch spliterator.
   *
   * @param base the base spliterator
   * @param batchSize the batch size
   */
  public BatchSpliterator(Spliterator<T> base, int batchSize) {
    this.base = base;
    this.batchSize = batchSize;
  }

  /**
   * Creates batches from a stream of the given size.
   *
   * @param <T> the list type
   * @param stream the stream to split
   * @param batchSize the batch size per split
   * @return the split stream
   */
  public static <T> Stream<List<T>> batches(Stream<T> stream, int batchSize) {
    return batchSize <= 0
        ? Stream.of(stream.collect(Collectors.toList()))
        : StreamSupport.stream(
            new BatchSpliterator<>(stream.spliterator(), batchSize), stream.isParallel());
  }

  /** {@inheritDoc} */
  @Override
  public boolean tryAdvance(Consumer<? super List<T>> action) {
    final List<T> batch = new ArrayList<>(batchSize);
    for (int i = 0; i < batchSize && base.tryAdvance(batch::add); i++)
      ;
    if (batch.isEmpty()) return false;
    action.accept(batch);
    return true;
  }

  /** {@inheritDoc} */
  @Override
  public Spliterator<List<T>> trySplit() {
    if (base.estimateSize() <= batchSize) return null;
    final Spliterator<T> splitBase = this.base.trySplit();
    return splitBase == null ? null : new BatchSpliterator<>(splitBase, batchSize);
  }

  /** {@inheritDoc} */
  @Override
  public long estimateSize() {
    final double baseSize = base.estimateSize();
    return baseSize == 0 ? 0 : (long) Math.ceil(baseSize / (double) batchSize);
  }

  /** {@inheritDoc} */
  @Override
  public int characteristics() {
    return base.characteristics();
  }
}
