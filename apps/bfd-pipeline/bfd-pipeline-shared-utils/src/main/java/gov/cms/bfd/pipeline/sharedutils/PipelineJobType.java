package gov.cms.bfd.pipeline.sharedutils;

/**
 * Uniquely identifies a type of {@link PipelineJob}, generally corresponding to the {@link
 * PipelineJob} implementation {@link Class}.
 *
 * @param <A> the {@link PipelineJobArguments} type associated with the {@link PipelineJob}
 *     implementation (see {@link NullPipelineJobArguments} for those {@link PipelineJob}
 *     implementations which do not need arguments)
 */
public final class PipelineJobType<A extends PipelineJobArguments> {
  private final String typeId;

  /**
   * Constructs a new {@link PipelineJobType} for the specified {@link PipelineJob}.
   *
   * @param job the {@link PipelineJob} to build a {@link PipelineJobType} for
   */
  @SuppressWarnings("unchecked")
  public PipelineJobType(PipelineJob<A> job) {
    this((Class<PipelineJob<A>>) job.getClass());
  }

  /**
   * Constructs a new {@link PipelineJobType} for the specified {@link PipelineJob} implementation
   * {@link Class}.
   *
   * @param <J> the {@link PipelineJob} implementation that this {@link PipelineJobType} will be for
   * @param jobClass the {@link PipelineJob} implementation {@link Class} build a {@link
   *     PipelineJobType} for
   */
  public <J extends PipelineJob<A>> PipelineJobType(Class<J> jobClass) {
    this.typeId = jobClass.getTypeName();
  }

  /** @see java.lang.Object#hashCode() */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((typeId == null) ? 0 : typeId.hashCode());
    return result;
  }

  /** @see java.lang.Object#equals(java.lang.Object) */
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

  /** @see java.lang.Object#toString() */
  @Override
  public String toString() {
    return typeId;
  }
}
