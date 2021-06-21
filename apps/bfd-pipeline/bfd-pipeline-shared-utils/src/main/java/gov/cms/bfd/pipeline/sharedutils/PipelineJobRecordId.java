package gov.cms.bfd.pipeline.sharedutils;

import java.util.concurrent.atomic.AtomicInteger;

/** Models the unique identifier for a {@link PipelineJob} that has been submitted for execution. */
public final class PipelineJobRecordId {
  /** Used to generate unique values for {@link #id}. */
  private static final AtomicInteger ID_SEQUENCE = new AtomicInteger(0);

  private final long id;

  /** Constructs a new unique {@link PipelineJobRecordId}. */
  public PipelineJobRecordId() {
    this.id = ID_SEQUENCE.getAndIncrement();
  }

  /** @see java.lang.Object#hashCode() */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (int) (id ^ (id >>> 32));
    return result;
  }

  /** @see java.lang.Object#equals(java.lang.Object) */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    PipelineJobRecordId other = (PipelineJobRecordId) obj;
    if (id != other.id) return false;
    return true;
  }

  /** @see java.lang.Object#toString() */
  @Override
  public String toString() {
    return "" + id;
  }
}
