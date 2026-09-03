package gov.cms.bfd.server.ng.testUtil;

import au.com.origin.snapshots.Snapshot;
import au.com.origin.snapshots.comparators.SnapshotComparator;

/**
 * Canonicalizes the snapshot from disk and the output from the test, so that neither have to change
 * to get them to pass as long as we trust the ordering logic.
 */
public class JsonSnapshotComparator implements SnapshotComparator {
  @Override
  public boolean matches(Snapshot previous, Snapshot current) {
    return EobSnapshotCanonicalizer.canonicalize(previous.getBody())
        .equals(EobSnapshotCanonicalizer.canonicalize(current.getBody()));
  }
}
