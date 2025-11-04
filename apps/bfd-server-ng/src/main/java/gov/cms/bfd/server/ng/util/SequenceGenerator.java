package gov.cms.bfd.server.ng.util;

import java.util.Iterator;

/** Utility class to create a monotonic sequence. Useful for generating FHIR sequence numbers. */
public class SequenceGenerator implements Iterator<Integer> {
  private int current = 1;

  @Override
  public boolean hasNext() {
    return true;
  }

  /**
   * Creates a new {@code SequenceGenerator} starting from the specified number.
   *
   * @param start the initial value of the sequence
   */
  public SequenceGenerator(int start) {
    this.current = start;
  }

  /** Creates a new {@code SequenceGenerator} starting from 1. */
  public SequenceGenerator() {}

  @Override
  public Integer next() {
    return current++;
  }
}
