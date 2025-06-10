package gov.cms.bfd.server.ng;

import java.util.Iterator;

public class SequenceGenerator implements Iterator<Integer> {
  private int current = 1;

  @Override
  public boolean hasNext() {
    return true;
  }

  @Override
  public Integer next() {
    return current++;
  }
}
