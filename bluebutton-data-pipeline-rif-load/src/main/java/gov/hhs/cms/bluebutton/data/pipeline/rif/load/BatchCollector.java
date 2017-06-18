package gov.hhs.cms.bluebutton.data.pipeline.rif.load;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Stream;

/**
 * <p>
 * A somewhat hacky Java 8 {@link Stream} {@link Collector} that allows stream
 * elements to be grouped/windowed/batched together. Rather than returning a
 * single useful result, like other {@link Collector}s, this one actually just
 * returns an empty {@link List} from {@link BatchCollector#finisher()}.
 * However, it accepts a {@link Consumer} at construction that it will spit out
 * accumulated {@link List}s of batches to as it goes along. It's definitely a
 * non-standard usage of the API, but nonetheless very useful.
 * </p>
 * <p>
 * This approach was taken from here:
 * <a href="https://stackoverflow.com/a/39058988/1851299">Stack Overflow: Java 8
 * Stream with batch processing: rohitvats answer</a>.
 * </p>
 * 
 * @param <T>
 *            the type of input elements to the reduction operation
 */
final class BatchCollector<T> implements Collector<T, List<T>, List<T>> {

	private final int batchSize;
	private final Consumer<List<T>> batchProcessor;

	/**
	 * Creates a new batch collector
	 * 
	 * @param batchSize
	 *            the batch size after which the batchProcessor should be called
	 * @param batchProcessor
	 *            the batch processor which accepts batches of records to
	 *            process
	 * @param <T>
	 *            the type of elements being processed
	 * @return a batch collector instance
	 */
	public static <T> Collector<T, List<T>, List<T>> batchCollector(int batchSize, Consumer<List<T>> batchProcessor) {
		return new BatchCollector<T>(batchSize, batchProcessor);
	}

	/**
	 * Constructs the batch collector
	 *
	 * @param batchSize
	 *            the batch size after which the batchProcessor should be called
	 * @param batchProcessor
	 *            the batch processor which accepts batches of records to
	 *            process
	 */
	private BatchCollector(int batchSize, Consumer<List<T>> batchProcessor) {
		batchProcessor = Objects.requireNonNull(batchProcessor);

		this.batchSize = batchSize;
		this.batchProcessor = batchProcessor;
	}

	/**
	 * @see java.util.stream.Collector#supplier()
	 */
	public Supplier<List<T>> supplier() {
		return ArrayList::new;
	}

	/**
	 * @see java.util.stream.Collector#accumulator()
	 */
	public BiConsumer<List<T>, T> accumulator() {
		return (ts, t) -> {
			ts.add(t);
			if (ts.size() >= batchSize) {
				batchProcessor.accept(ts);
				ts.clear();
			}
		};
	}

	/**
	 * @see java.util.stream.Collector#combiner()
	 */
	public BinaryOperator<List<T>> combiner() {
		return (ts, ots) -> {
			// process each parallel list without checking for batch size
			// avoids adding all elements of one to another
			// can be modified if a strict batching mode is required
			batchProcessor.accept(ts);
			batchProcessor.accept(ots);
			return Collections.emptyList();
		};
	}

	/**
	 * @see java.util.stream.Collector#finisher()
	 */
	public Function<List<T>, List<T>> finisher() {
		return ts -> {
			if (!ts.isEmpty())
				batchProcessor.accept(ts);
			return Collections.emptyList();
		};
	}

	/**
	 * @see java.util.stream.Collector#characteristics()
	 */
	public Set<Characteristics> characteristics() {
		return Collections.emptySet();
	}
}