package gov.cms.bfd.pipeline.rda.grpc;

/**
 * Interface for methods that accept no parameters and can throw a checked exception. Similar to
 * Runnable except that method is allowed to throw.
 */
public interface ThrowableAction {
  void act() throws Exception;
}
