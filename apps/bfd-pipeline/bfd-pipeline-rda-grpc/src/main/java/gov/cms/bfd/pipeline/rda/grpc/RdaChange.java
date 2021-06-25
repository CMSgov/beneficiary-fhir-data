package gov.cms.bfd.pipeline.rda.grpc;

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
