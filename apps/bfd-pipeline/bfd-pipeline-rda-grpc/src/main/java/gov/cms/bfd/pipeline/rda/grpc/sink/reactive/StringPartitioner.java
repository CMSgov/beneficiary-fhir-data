package gov.cms.bfd.pipeline.rda.grpc.sink.reactive;

import com.google.common.collect.ImmutableList;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * Uniformly distributes string values across a fixed list of partitions. Each partition is an
 * object of a generic type so that this can be used in different contexts. Every partition is
 * assigned a range of integers in the set of positive integers. Each string to be assigned to a
 * partition is hashed and mapped to the partition that contains the hash code in its assigned
 * range.
 */
@Slf4j
public class StringPartitioner<T> {
  /**
   * Hash function used to generate a solid hash value for each string. This function updates all
   * bits in the hash value evenly as characters are added to the string.
   */
  private static final HashFunction Hasher = Hashing.goodFastHash(32);

  /** List containing all of the partition objects. */
  private final List<T> partitions;

  /**
   * The maximum integer value of a hash code generated from a key. Restricting the range of hash
   * values ensures that every partition is assigned exactly the same number of integers.
   */
  private final int numIntegersAssignedToAllPartitions;

  /**
   * The number of integer values per partition. Used as divisor with a hash code to get the index
   * of the partition in {@link #partitions}.
   */
  private final int numIntegersAssignedToOnePartition;

  /**
   * Constructs an instance for the given partitions.
   *
   * @param partitions List containing all of the partition objects.
   */
  public StringPartitioner(List<T> partitions) {
    this.partitions = ImmutableList.copyOf(partitions);
    numIntegersAssignedToOnePartition = Integer.MAX_VALUE / partitions.size();
    numIntegersAssignedToAllPartitions = numIntegersAssignedToOnePartition * partitions.size();
  }

  /**
   * Select the partition for the given string.
   *
   * @param stringToAssign string to assign to a partition
   * @return partition object that string is assigned to
   */
  public T partitionFor(String stringToAssign) {
    final var rawHash = Hasher.hashString(stringToAssign, StandardCharsets.UTF_8).asInt();
    final var inBoundsHash = Math.abs(rawHash) % numIntegersAssignedToAllPartitions;
    final var partitionIndex = inBoundsHash / numIntegersAssignedToOnePartition;
    return partitions.get(partitionIndex);
  }
}
