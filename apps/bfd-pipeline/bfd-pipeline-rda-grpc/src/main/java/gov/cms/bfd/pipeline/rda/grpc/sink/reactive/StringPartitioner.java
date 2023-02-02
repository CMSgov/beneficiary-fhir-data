package gov.cms.bfd.pipeline.rda.grpc.sink.reactive;

import com.google.common.collect.ImmutableList;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * Uniformly distributes string values across a fixed list of partitions. Each partition is an
 * object of a generic type so that this can be used in different contexts.
 */
@Slf4j
public class StringPartitioner<T> {
  private static final HashFunction Hasher = Hashing.goodFastHash(32);

  private final List<T> partitions;
  private final int maxHash;
  private final int hashToIndexDivisor;

  public StringPartitioner(List<T> partitions) {
    this.partitions = ImmutableList.copyOf(partitions);
    hashToIndexDivisor = Integer.MAX_VALUE / partitions.size();
    maxHash = hashToIndexDivisor * partitions.size();
  }

  /**
   * Select the partition for the given string.
   *
   * @param stringToHash value to assign
   * @return partition object for string
   */
  public T partitionFor(String stringToHash) {
    final var rawHash = Hasher.hashString(stringToHash, StandardCharsets.UTF_8).asInt();
    final var inBoundsHash = Math.abs(rawHash) % maxHash;
    final var index = inBoundsHash / hashToIndexDivisor;
    return partitions.get(index);
  }
}
