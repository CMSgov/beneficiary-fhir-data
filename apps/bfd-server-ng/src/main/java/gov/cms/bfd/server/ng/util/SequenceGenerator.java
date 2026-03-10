package gov.cms.bfd.server.ng.util;

import java.util.Iterator;

/** Utility class to create a monotonic sequence. Useful for generating FHIR sequence numbers. */
public class SequenceGenerator implements Iterator<Integer> {
  private int current = 1;

  /** Default constructor start sequence at 1. */
  public SequenceGenerator() {}

  /**
   * Creates a sequence starting from a specific number.
   *
   * @param start number sequence should start at
   */
  public SequenceGenerator(int start) {
    this.current = start;
  }

  @Override
  public boolean hasNext() {
    return true;
  }

  @Override
  public Integer next() {
    return current++;
  }
}
