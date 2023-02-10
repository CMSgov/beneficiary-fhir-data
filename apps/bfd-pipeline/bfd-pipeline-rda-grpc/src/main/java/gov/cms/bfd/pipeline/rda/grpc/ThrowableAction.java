package gov.cms.bfd.pipeline.rda.grpc;

/**
 * Interface for methods that accept no parameters and can throw a checked exception. Similar to
 * Runnable except that method is allowed to throw.
 */
public interface ThrowableAction {
  /**
   * Performs an action.
   *
   * @throws Exception any exception that occurs as a result of this action
   */
  void act() throws Exception;
}
