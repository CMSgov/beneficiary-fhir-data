package gov.cms.bfd.pipeline.sharedutils;

import com.google.common.annotations.VisibleForTesting;
import gov.cms.bfd.sharedutils.interfaces.ThrowingConsumer;
import gov.cms.bfd.sharedutils.interfaces.ThrowingFunction;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

/**
 * Manages life cycle of an {@link EntityManager} to execute one or more transactions. Executes as
 * many transactions as possible with a single {@link EntityManager} but obtains a new one if any
 * transaction throws an exception.
 *
 * <p>Takes care of the tedious process of ensuring that any exception thrown by the transaction
 * itself does not get lost if the subsequent rollback or close operations also throw.
 *
 * <p>Instances are intended to be dedicated to a particular thread such as a worker thread from a
 * thread pool rather than shared by many instances (although that is possible). Any given instance
 * will only allow one thread at a time to execute a transaction.
 */
public class TransactionManager implements AutoCloseable {
  /** Used to create new {@link EntityManager}s as needed. */
  private final EntityManagerFactory entityManagerFactory;
  /**
   * The current {@link EntityManager}. May be null if we have not yet created one or if it was
   * closed after a failed transaction.
   */
  @Nullable private EntityManager entityManager;

  /**
   * Creates a new instance using the provided {@link EntityManagerFactory}.
   *
   * @param entityManagerFactory the {@link EntityManagerFactory}
   */
  public TransactionManager(EntityManagerFactory entityManagerFactory) {
    this.entityManagerFactory = entityManagerFactory;
  }

  /**
   * Runs a transaction that produces a result (usually a query). Ensures that the transaction is
   * properly committed if the function completes normally or rolled back if the function throws an
   * exception.
   *
   * @param functionLogic logic to invoke with the {@link EntityManager}
   * @param <T> return type of the function
   * @param <E> exception type thrown by the function
   * @throws E pass through exception from calling the function
   * @return return value from the function
   */
  public synchronized <T, E extends Exception> T executeFunction(
      ThrowingFunction<T, EntityManager, E> functionLogic) throws E {
    Exception transactionException = null;
    final var entityManager = getOrCreateEntityManager();
    try {
      entityManager.getTransaction().begin();
      return functionLogic.apply(entityManager);
    } catch (Exception exception) {
      transactionException = exception;
      throw exception;
    } finally {
      completeTransaction(entityManager, transactionException);
    }
  }

  /**
   * Runs a transaction that does not produce a result (usually an insert or update). Ensures that
   * the transaction is properly committed if the procedure completes normally or rolled back if the
   * procedure throws an exception. Internally uses the adapter method {@link
   * ThrowingConsumer#executeAsFunction} and discards the null result.
   *
   * @param procedureLogic logic to invoke with the {@link EntityManager}
   * @param <E> exception type thrown by the procedure
   * @throws E pass through exception from calling the procedure
   */
  public <E extends Exception> void executeProcedure(
      ThrowingConsumer<EntityManager, E> procedureLogic) throws E {
    executeFunction(procedureLogic::executeAsFunction);
  }

  /** {@inheritDoc} */
  @Override
  public synchronized void close() {
    closeEntityManager(null);
  }

  /**
   * Getter intended solely for use by tests.
   *
   * @return the {@link EntityManager} if we have one or null if we do not
   */
  @VisibleForTesting
  @Nullable
  public synchronized EntityManager getEntityManager() {
    return entityManager;
  }

  /**
   * Gets the current {@link EntityManager}, creating a new one if necessary.
   *
   * @return the {@link EntityManager}
   */
  @Nonnull
  private EntityManager getOrCreateEntityManager() {
    if (entityManager == null) {
      entityManager = entityManagerFactory.createEntityManager();
    }
    return entityManager;
  }

  /**
   * Commits or rolls back the active transaction. If a commit fails, or if {@code
   * transactionException} is non-null the current {@link EntityManager} is also closed. Ensures
   * that any exceptions thrown during completion are either thrown (if the transaction had been
   * otherwise successful) or added to the failing exception as being suppressed.
   *
   * @param entityManager the {@link EntityManager} owning the transaction
   * @param transactionException null if transaction was successful, otherwise the exception that
   *     caused transaction to fail
   */
  private void completeTransaction(EntityManager entityManager, Exception transactionException) {
    if (transactionException == null) {
      completeSuccessfulTransaction(entityManager);
    } else {
      completeFailedTransaction(entityManager, transactionException);
    }
  }

  /**
   * Commits the transaction and clears the entity manager. If the commit fails the entity manager
   * is closed. Ensures that any exceptions thrown during completion are properly thrown.
   *
   * @param entityManager the {@link EntityManager} owning the transaction
   */
  private void completeSuccessfulTransaction(EntityManager entityManager) {
    RuntimeException completionException = null;
    try {
      entityManager.getTransaction().commit();
      entityManager.clear();
    } catch (RuntimeException commitException) {
      completionException = commitException;
      throw commitException;
    } finally {
      if (completionException != null) {
        closeEntityManager(completionException);
      }
    }
  }

  /**
   * Rolls back the transaction and closes the entity manager. Ensures that any exception during the
   * rollback or close are added to the transaction exception as suppressed exceptions.
   *
   * @param entityManager the {@link EntityManager} owning the transaction
   * @param transactionException the exception that caused transaction to fail
   */
  private void completeFailedTransaction(
      EntityManager entityManager, Exception transactionException) {
    try {
      entityManager.getTransaction().rollback();
    } catch (RuntimeException rollbackException) {
      transactionException.addSuppressed(rollbackException);
    } finally {
      closeEntityManager(transactionException);
    }
  }

  /**
   * Close the current {@link EntityManager} if there is one. Resets {@link #entityManager} to null
   * so a new one will be created next time a transaction is executed. Any exception thrown during
   * the close is either thrown (if {@code transactionException} is null) or added to {@code
   * transactionException} as a suppressed exception.
   *
   * @param transactionException the exception that caused transaction to fail or null if there is
   *     no exception
   */
  private void closeEntityManager(@Nullable Exception transactionException) {
    try {
      if (entityManager != null && entityManager.isOpen()) {
        entityManager.close();
      }
    } catch (RuntimeException closeException) {
      if (transactionException != null) {
        transactionException.addSuppressed(closeException);
      } else {
        throw closeException;
      }
    } finally {
      entityManager = null;
    }
  }
}
