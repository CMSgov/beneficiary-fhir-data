package gov.cms.bfd.pipeline.sharedutils;

/**
 * Uniquely identifies a type of {@link PipelineJob}, generally corresponding to the {@link
 * PipelineJob} implementation {@link Class}.
 */
public final class PipelineJobType {
  /** The type id. */
  private final String typeId;

  /**
   * Constructs a new {@link PipelineJobType} for the specified {@link PipelineJob}.
   *
   * @param job the {@link PipelineJob} to build a {@link PipelineJobType} for
   */
  @SuppressWarnings("unchecked")
  public PipelineJobType(PipelineJob job) {
    this((Class<PipelineJob>) job.getClass());
  }

  /**
   * Constructs a new {@link PipelineJobType} for the specified {@link PipelineJob} implementation
   * {@link Class}.
   *
   * @param <J> the {@link PipelineJob} implementation that this {@link PipelineJobType} will be for
   * @param jobClass the {@link PipelineJob} implementation {@link Class} build a {@link
   *     PipelineJobType} for
   */
  public <J extends PipelineJob> PipelineJobType(Class<J> jobClass) {
    this.typeId = jobClass.getTypeName();
  }

  /** {@inheritDoc} */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((typeId == null) ? 0 : typeId.hashCode());
    return result;
  }

  /** {@inheritDoc} */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    @SuppressWarnings("rawtypes")
    PipelineJobType other = (PipelineJobType) obj;
    if (typeId == null) {
      if (other.typeId != null) return false;
    } else if (!typeId.equals(other.typeId)) return false;
    return true;
  }

  /** {@inheritDoc} */
  @Override
  public String toString() {
    return typeId;
  }
}
