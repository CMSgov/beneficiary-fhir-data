package gov.cms.bfd.pipeline.bridge.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Used to create a sample of data from various sources, enforcing sampling ratios per source.
 *
 * <p>Data is stored up to a configured maximum size. If the size is exceeded, values are dropped
 * from each source that is over their ratio thresholds until the total data set size is no longer
 * above the configured maximum size.
 *
 * <p>There is no random sampling being utilized, aside from the arbitrary order of the utilized
 * data structures' backing hash tables.
 *
 * @param <T> The type of data being stored.
 */
public class DataSampler<T> implements Iterable<T> {
  private final Map<Integer, Set<T>> dataSet = new HashMap<>();

  private final Map<Integer, Float> sampleRatios;
  private final int maxValues;

  private DataSampler(int maxValues, Map<Integer, Float> sampleRatios) {
    this.maxValues = maxValues;

    // Normalize ratios in case they didn't add up to 1.0
    float totalRatios = sampleRatios.values().stream().reduce(0.0f, Float::sum);
    float coef = 1.0f / totalRatios;
    this.sampleRatios = sampleRatios;
    this.sampleRatios.replaceAll((k, v) -> v * coef);

    for (Integer id : this.sampleRatios.keySet()) {
      dataSet.put(id, new HashSet<>());
    }
  }

  /**
   * Attempts to add a given data value, associating it to the given sampleSetId.
   *
   * <p>The sampleSetId is the value used when invoking {@link Builder#registerSampleSet(int,
   * float)} to configure the ratio for the sample set.
   *
   * <p>If the total dataset size is greater than the configured maxValues size, {@link
   * #rebalance()} is called and data will be dropped based on it's configured ratio threshold.
   *
   * @param sampleSetId The id of the sample set that was previously configured.
   * @param value The value to add that is associated with the given sample set id.
   * @throws IllegalArgumentException If the given sampleSetId was never registered.
   */
  public void add(int sampleSetId, T value) {
    if (dataSet.containsKey(sampleSetId)) {
      boolean containsValue = false;

      Iterator<Integer> mapIterator = dataSet.keySet().iterator();

      while (!containsValue && mapIterator.hasNext()) {
        containsValue = dataSet.get(mapIterator.next()).contains(value);
      }

      if (!containsValue) {
        dataSet.get(sampleSetId).add(value);

        rebalance();
      }
    } else {
      throw new IllegalArgumentException("Unregistered sample set id: " + sampleSetId);
    }
  }

  /**
   * Checks to see if the size of the currently stored data has exceeded the maximum configured
   * size. If it has, it will go through and systematically drop data from different sources if they
   * are exceeding their current ratio configuration until the currently stored data is no longer
   * exceeding it's size restriction.
   */
  private void rebalance() {
    long totalValues = dataSet.values().stream().map(Set::size).reduce(0, Integer::sum);

    Iterator<Integer> mapIterator = sampleRatios.keySet().iterator();

    while (totalValues > maxValues && mapIterator.hasNext()) {
      Integer key = mapIterator.next();

      // Calculate maximum size of current sample set based on configurations
      int setMaxSize = (int) (maxValues * sampleRatios.get(key));

      Set<T> setData = dataSet.get(key);

      // Remove elements from current sample set until it's below it's individual threshold,
      // or the total amount of data is below the maximum threshold.
      while (totalValues > maxValues && setData.size() > setMaxSize) {
        Iterator<T> iterator = setData.iterator();
        iterator.next();
        iterator.remove();
        --totalValues;
      }
    }
  }

  /**
   * Simple builder method for {@link DataSampler} objects.
   *
   * @param <T> The type of data that will be stored.
   * @return A new {@link Builder} for configuring a new {@link DataSampler}.
   */
  public static <T> Builder<T> builder() {
    return new Builder<>();
  }

  /**
   * Simple iterator implementation.
   *
   * @return A {@link DataSamplerIterator} for iterating over the sample data.
   */
  @Override
  public Iterator<T> iterator() {
    return new DataSamplerIterator();
  }

  /**
   * Simple builder class for building {@link DataSampler} object instances.
   *
   * @param <T> The type of data to store in the created {@link DataSampler} instance.
   */
  public static class Builder<T> {
    private int maxValues = Integer.MAX_VALUE;
    private final Map<Integer, Float> sampleRatios = new HashMap<>();

    /**
     * Define the maximum number of data values the created {@link DataSampler} will be configured
     * to store.
     *
     * @param maxValues The maximum number of data values to configure.
     * @return The current {@link Builder} instance.
     */
    public Builder<T> maxValues(int maxValues) {
      this.maxValues = maxValues;
      return this;
    }

    /**
     * Registers a new sample set id for configuring ratios and tracking future data additions.
     *
     * <p>The sample set id is just an arbitrary integer defined by the caller to be used in future
     * calls to designate which sample set to apply values to.
     *
     * <p>The configured ratio is the PREFERRED maximum ratio, but if there is not enough data to
     * meet these desired levels, one or more sets could fall over/under their configured ratio.
     *
     * @param id The id for the sample set to start tracking.
     * @param ratio The preferred maximum ratio of values to store for this sample set id.
     * @return The current {@link Builder} instance.
     */
    public Builder<T> registerSampleSet(int id, float ratio) {
      sampleRatios.put(id, ratio);
      return this;
    }

    /**
     * Builds a {@link DataSampler} instance based on the current {@link Builder} configurations.
     *
     * @return A new {@link DataSampler} object instance.
     */
    public DataSampler<T> build() {
      return new DataSampler<>(maxValues, sampleRatios);
    }
  }

  /**
   * An {@link Iterator} implementation for {@link DataSampler} object iteration.
   *
   * <p>The iterator will iterate through every entry in a particular set before moving on to the
   * next sample set. The order in which the sample sets are chosen is based on the backing {@link
   * Set#iterator()} implementation.
   */
  public class DataSamplerIterator implements Iterator<T> {

    private final Iterator<Set<T>> mapIterator;
    private Iterator<T> currentSetIterator;

    private DataSamplerIterator() {
      mapIterator = dataSet.values().iterator();
      currentSetIterator = mapIterator.next().iterator();
    }

    @Override
    public boolean hasNext() {
      // If we're just starting, need to initialize the current set iterator
      if (currentSetIterator == null) {
        if (mapIterator.hasNext()) {
          currentSetIterator = mapIterator.next().iterator();
        } else {
          return false;
        }
      }

      // If the current set iterator is empty, move ahead to try to find non-empty one
      while (!currentSetIterator.hasNext() && mapIterator.hasNext()) {
        currentSetIterator = mapIterator.next().iterator();
      }

      return currentSetIterator.hasNext();
    }

    @Override
    public T next() {
      // If we're just starting, need to initialize the current set iterator.
      // This will throw if the map has no entries, which is good.
      if (currentSetIterator == null) {
        currentSetIterator = mapIterator.next().iterator();
      }

      // If the current set iterator is empty, keep grabbing the next one till
      // we find a non-empty one or we run out of sets.
      while (!currentSetIterator.hasNext() && mapIterator.hasNext()) {
        currentSetIterator = mapIterator.next().iterator();
      }

      return currentSetIterator.next();
    }
  }
}
