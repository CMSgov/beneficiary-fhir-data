package gov.cms.bfd.pipeline.bridge.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class DataSampler<T> implements Iterable<T> {
  private final Map<Integer, Set<T>> dataSet = new HashMap<>();

  private final Map<Integer, Float> sampleRatios;
  private int maxValues;

  private DataSampler(int maxValues, Map<Integer, Float> sampleRatios) {
    this.maxValues = maxValues;

    float totalRatios = sampleRatios.values().stream().reduce(0.0f, Float::sum);
    float coef = 1.0f / totalRatios;

    this.sampleRatios = sampleRatios;
    this.sampleRatios.replaceAll((k, v) -> v * coef);

    for (Integer id : this.sampleRatios.keySet()) {
      dataSet.put(id, new HashSet<>());
    }
  }

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

  public static <T> Builder<T> builder() {
    return new Builder<>();
  }

  @Override
  public Iterator<T> iterator() {
    return new DataSamplerIterator();
  }

  public static class Builder<T> {
    private int maxValues = Integer.MAX_VALUE;
    private final Map<Integer, Float> sampleRatios = new HashMap<>();

    public Builder<T> maxValues(int maxValues) {
      this.maxValues = maxValues;
      return this;
    }

    public Builder<T> registerSampleSet(int id, float ratio) {
      sampleRatios.put(id, ratio);
      return this;
    }

    public DataSampler<T> build() {
      return new DataSampler<>(maxValues, sampleRatios);
    }
  }

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
