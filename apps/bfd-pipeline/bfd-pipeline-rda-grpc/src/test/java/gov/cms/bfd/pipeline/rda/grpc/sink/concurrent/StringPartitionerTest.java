package gov.cms.bfd.pipeline.rda.grpc.sink.concurrent;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link StringPartitioner}. */
public class StringPartitionerTest {
  /** require at most 1% divergence between min and max partition counts. */
  private final double MAX_ACCEPTABLE_DIVERGENCE_BETWEEN_PARTITION_COUNTS = 0.01;

  /**
   * Worst case scenario with keys that change very little. Ensures they still obtain a good
   * distribution.
   */
  @Test
  void testSequentialKeys() {
    for (int numPartitions = 5; numPartitions <= 25; ++numPartitions) {
      var partitions = IntStream.rangeClosed(1, numPartitions).boxed().collect(Collectors.toList());
      var keys =
          IntStream.range(1_000_000, 1_250_000)
              .boxed()
              .map(String::valueOf)
              .collect(Collectors.toList());
      var assignments = partitionKeys(partitions, keys);
      assertDistributionIsEvenAcrossPartitions(partitions, assignments);
    }
  }

  /**
   * Various scenarios using random partition counts and key lengths. Ensures they still obtain a
   * good distribution.
   */
  @Test
  void testRandomKeys() {
    // Just a fixed seed to make the test stable. There is nothing special about the number 2000.
    final var rand = new Random(2000);
    final var characters = "abcdefghijklmnopqrstuvwxyz0123456789-_.";
    for (int numPartitions = 5; numPartitions <= 25; ++numPartitions) {
      var partitions = IntStream.rangeClosed(1, numPartitions).boxed().collect(Collectors.toList());
      var keys = new ArrayList<String>();
      for (int keyNumber = 1; keyNumber <= 100_000; ++keyNumber) {
        var keyLength = 5 + rand.nextInt(11);
        var key =
            IntStream.range(1, keyLength)
                .map(i -> rand.nextInt(characters.length()))
                .boxed()
                .map(i -> characters.substring(i, i + 1))
                .collect(Collectors.joining());
        keys.add(key);
      }
      var assignments = partitionKeys(partitions, keys);
      assertDistributionIsEvenAcrossPartitions(partitions, assignments);
    }
  }

  /**
   * Assign keys to partitions using a {@link StringPartitioner}.
   *
   * @param partitions the partitions
   * @param keys the keys to assign
   * @return {@link Map} of keys to counts
   */
  private Map<Integer, Integer> partitionKeys(List<Integer> partitions, List<String> keys) {
    var partitioner = new StringPartitioner<>(partitions);
    var counts = new HashMap<Integer, Integer>();
    for (String key : keys) {
      var partition = partitioner.partitionFor(key);
      counts.compute(partition, (p, count) -> count == null ? 1 : count + 1);
    }
    return counts;
  }

  /**
   * Compute the distribution of counts across the partitions.
   *
   * @param counts number to times each partition was assigned a value
   * @return the {@link Distribution}
   */
  private Distribution analyze(Iterable<Integer> counts) {
    int min = Integer.MAX_VALUE;
    int max = Integer.MIN_VALUE;
    int total = 0;
    for (Integer count : counts) {
      if (min > count) {
        min = count;
      }
      if (max < count) {
        max = count;
      }
      total += count;
    }
    return new Distribution(total, min, max);
  }

  /**
   * Analyzes partiion counts and asserts they vary by small amount.
   *
   * @param partitions the partitions
   * @param assignments the number of times each partition received a value
   */
  private void assertDistributionIsEvenAcrossPartitions(
      List<Integer> partitions, Map<Integer, Integer> assignments) {
    var distribution = analyze(assignments.values());
    var divergence = distribution.divergence();
    assertTrue(
        divergence <= MAX_ACCEPTABLE_DIVERGENCE_BETWEEN_PARTITION_COUNTS,
        () ->
            String.format(
                "divergence too large: partitions=%s divergence=%g distribution=%s",
                partitions, divergence, distribution));
  }

  /** Data object to hold overall distribution stats. */
  @Data
  @AllArgsConstructor
  private static class Distribution {
    /** Number of assignments. */
    private int total;
    /** Min number of assignments for any single partition. */
    private int min;
    /** Max number of assignments for any single partition. */
    private int max;

    /** Percentage difference between min and max as a ratio. */
    private double divergence() {
      return ((double) (max - min)) / (double) total;
    }
  }
}
