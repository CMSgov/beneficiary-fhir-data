package gov.cms.bfd.pipeline.rda.grpc;

/**
 * RDA API wraps each claim in a `ClaimChange` object that indicates the type of change. This class
 * wraps that concept into an immutable bean used by the sink classes to optimize how they write
 * claims to the database.
 *
 * @param <T> The database entity type for the change.
 */
public class RdaChange<T> {
  public enum Type {
    INSERT,
    UPDATE,
    DELETE
  }

  private final Type type;
  private final T claim;

  public RdaChange(Type type, T claim) {
    this.type = type;
    this.claim = claim;
  }

  public Type getType() {
    return type;
  }

  public T getClaim() {
    return claim;
  }
}
