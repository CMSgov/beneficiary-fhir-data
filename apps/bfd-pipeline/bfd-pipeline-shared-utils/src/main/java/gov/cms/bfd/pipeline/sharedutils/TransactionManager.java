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
   * Runs a transaction that produces a result. Ensures that the transaction is properly committed
   * if the function completes normally or rolled back if the function throws an exception.
   *
   * @param functionLogic logic to invoke with the {@link EntityManager}
   * @param <T> return type of the function
   * @param <E> exception type thrown by the function
   * @throws E pass through exception from calling the function
   * @return return value from the function
   */
  public synchronized <T, E extends Exception> T executeFunction(
      ThrowingFunction<T, EntityManager, E> functionLogic) throws E {
    final var entityManager = getOrCreateEntityManager();
    var shouldCommit = false;
    try {
      entityManager.getTransaction().begin();
      var result = functionLogic.apply(entityManager);
      shouldCommit = true;
      return result;
    } finally {
      completeTransaction(entityManager, shouldCommit);
    }
  }

  /**
   * Runs a transaction that does not produce a result. Ensures that the transaction is properly
   * committed if the procedure completes normally or rolled back if the procedure throws an
   * exception.
   *
   * @param procedureLogic logic to invoke with the {@link EntityManager}
   * @param <E> exception type thrown by the procedure
   * @throws E pass through exception from calling the procedure
   */
  public synchronized <E extends Exception> void executeProcedure(
      ThrowingConsumer<EntityManager, E> procedureLogic) throws E {
    final var entityManager = getOrCreateEntityManager();
    var shouldCommit = false;
    try {
      entityManager.getTransaction().begin();
      procedureLogic.accept(entityManager);
      shouldCommit = true;
    } finally {
      completeTransaction(entityManager, shouldCommit);
    }
  }

  /** {@inheritDoc} */
  @Override
  public synchronized void close() {
    closeEntityManager();
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
   * Commits or rolls back the active transaction. If a commit fails, or if {@code shouldCommit} is
   * false the current {@link EntityManager} is also closed.
   *
   * @param entityManager the {@link EntityManager} owning the transaction
   * @param shouldCommit true if commit is needed, false if rollback is needed
   */
  private void completeTransaction(EntityManager entityManager, boolean shouldCommit) {
    boolean shouldClose = true;
    try {
      if (shouldCommit) {
        entityManager.getTransaction().commit();
        entityManager.clear();
        shouldClose = false;
      } else {
        entityManager.getTransaction().rollback();
      }
    } finally {
      if (shouldClose) {
        closeEntityManager();
      }
    }
  }

  /**
   * Close the current {@link EntityManager} if there is one. Resets {@link #entityManager} to null
   * so a new one will be created next time a transaction is executed.
   */
  private void closeEntityManager() {
    if (entityManager != null) {
      if (entityManager.isOpen()) {
        entityManager.close();
      }
      entityManager = null;
    }
  }
}
