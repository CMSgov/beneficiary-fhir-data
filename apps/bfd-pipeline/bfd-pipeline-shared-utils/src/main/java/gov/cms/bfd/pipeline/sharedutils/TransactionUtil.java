package gov.cms.bfd.pipeline.sharedutils;

import gov.cms.bfd.sharedutils.interfaces.ThrowingConsumer;
import gov.cms.bfd.sharedutils.interfaces.ThrowingFunction;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

/** Utility methods to execute transactions with proper commit or rollback. */
public final class TransactionUtil {
  /** Prevent instantiation of any instances. */
  private TransactionUtil() {}

  /**
   * Runs a transaction that produces a result. Ensures that the transaction is properly committed
   * if the function completes normally or rolled back if the function throws an exception.
   *
   * @param entityManagerFactory the {@link EntityManagerFactory} used to produce an {@link
   *     EntityManager}
   * @param functionLogic logic to invoke with the {@link EntityManager}
   * @param <T> return type of the function
   * @param <E> exception type thrown by the function
   * @throws E pass through exception from calling the function
   * @return return value from the function
   */
  public static <T, E extends Exception> T executeFunction(
      EntityManagerFactory entityManagerFactory,
      ThrowingFunction<T, EntityManager, E> functionLogic)
      throws E {
    final var entityManager = entityManagerFactory.createEntityManager();
    var shouldCommit = false;
    try {
      entityManager.getTransaction().begin();
      var result = functionLogic.apply(entityManager);
      shouldCommit = true;
      return result;
    } finally {
      closeEntityManager(entityManager, shouldCommit);
    }
  }

  /**
   * Runs a transaction that does not produce a result. Ensures that the transaction is properly
   * committed if the procedure completes normally or rolled back if the procedure throws an
   * exception.
   *
   * @param entityManagerFactory the {@link EntityManagerFactory} used to produce an {@link
   *     EntityManager}
   * @param procedureLogic logic to invoke with the {@link EntityManager}
   * @param <E> exception type thrown by the procedure
   * @throws E pass through exception from calling the procedure
   */
  public static <E extends Exception> void executeProcedure(
      EntityManagerFactory entityManagerFactory, ThrowingConsumer<EntityManager, E> procedureLogic)
      throws E {
    final var entityManager = entityManagerFactory.createEntityManager();
    var shouldCommit = false;
    try {
      entityManager.getTransaction().begin();
      procedureLogic.accept(entityManager);
      shouldCommit = true;
    } finally {
      closeEntityManager(entityManager, shouldCommit);
    }
  }

  /**
   * Commits or rolls back the active transaction and closes the {@link EntityManager}. Ensures that
   * any exception thrown during commit/rollback does not prevent close from being called.
   *
   * @param entityManager the {@link EntityManager} to close
   * @param shouldCommit true if commit is needed, false if rollback is needed
   */
  private static void closeEntityManager(EntityManager entityManager, boolean shouldCommit) {
    try {
      if (shouldCommit) {
        entityManager.getTransaction().commit();
      } else {
        entityManager.getTransaction().rollback();
      }
    } finally {
      entityManager.close();
    }
  }
}
