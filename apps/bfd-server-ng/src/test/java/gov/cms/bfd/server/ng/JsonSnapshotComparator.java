package gov.cms.bfd.server.ng;

import au.com.origin.snapshots.Snapshot;
import au.com.origin.snapshots.comparators.v1.PlainTextEqualsComparator;

public class JsonSnapshotComparator extends PlainTextEqualsComparator {
  @Override
  public boolean matches(Snapshot previous, Snapshot current) {
    var matches = super.matches(previous, current);

    return matches;
  }
}
