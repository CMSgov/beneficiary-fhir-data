package gov.cms.bfd.pipeline.sharedutils;

import com.google.common.annotations.VisibleForTesting;
import gov.cms.bfd.sharedutils.interfaces.ThrowingConsumer;
import gov.cms.bfd.sharedutils.interfaces.ThrowingFunction;
import java.security.SecureRandom;
import java.util.function.Predicate;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import lombok.Data;
import org.hibernate.exception.ConstraintViolationException;

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

  /** Secure Random generator for function retry logic. */
  private final SecureRandom random = new SecureRandom();

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
   * Base number used for transaction retry delay. This amount is adjusted using a random number
   * generator.
   */
  private static final long BaseRetryMilliseconds = 250;

  /**
   * Maximum multiplier for {@link #BaseRetryMilliseconds} used to compute overall delay before
   * retrying a failed transaction.
   */
  private static final int MaxRetryDelayMultiple = 4;

  /**
   * Runs a transaction that produces a result (usually a query). Ensures that the transaction is
   * properly committed if the function completes normally or rolled back if the function throws an
   * exception.
   *
   * <p>If the first attempt fails, this function retries the transaction up to {@code maxRetries}
   * times before throwing the original exception. The function must be reentrant since each retry
   * calls the function again. Only exceptions which satisfy the given predicate are considered safe
   * to retry.
   *
   * @param maxRetries max number of retry attempts
   * @param isRetriableException {@link Predicate} to determine if a given exception is retriable
   * @param functionLogic logic to invoke with the {@link EntityManager}
   * @param <T> return type of the function
   * @param <E> exception type thrown by the function
   * @throws E pass through exception from calling the function
   * @return return object containing result following possible retries
   */
  public synchronized <T, E extends Exception> RetryResult<T> executeFunctionWithRetries(
      int maxRetries,
      Predicate<Exception> isRetriableException,
      ThrowingFunction<T, EntityManager, E> functionLogic)
      throws E {
    try {
      T result = executeFunction(functionLogic);
      return new RetryResult<>(result);
    } catch (Exception firstException) {
      if (isRetriableException.test(firstException)) {
        for (int retryNumber = 1; retryNumber <= maxRetries; ++retryNumber) {
          try {
            int retryDelayMultiple = retryNumber + random.nextInt(MaxRetryDelayMultiple);
            Thread.sleep(BaseRetryMilliseconds * retryDelayMultiple);
            T result = executeFunction(functionLogic);
            return new RetryResult<>(result, retryNumber, firstException);
          } catch (Exception retryException) {
            firstException.addSuppressed(retryException);
            if (!isRetriableException.test(retryException)) {
              break;
            }
          }
        }
      }
      throw firstException;
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

  /**
   * Standard {@link Predicate} instance to recognize a constraint violation intended to simplify
   * passing a predicate to {@link #executeFunctionWithRetries}.
   *
   * @param exception the exception to test
   * @return true if the exception is a constraint violation
   */
  public static boolean isConstraintViolation(Exception exception) {
    Throwable throwable = exception;
    while (throwable != null) {
      if (throwable instanceof ConstraintViolationException) {
        return true;
      }
      throwable = throwable.getCause();
    }
    return false;
  }

  /**
   * Data class used to hold result of calling {@link #executeFunctionWithRetries}.
   *
   * @param <T> type of value returned by the function
   */
  @Data
  public static class RetryResult<T> {
    /** Value returned by the last call to the function. */
    private final T value;

    /** Number of retries needed to get function. */
    private final int numRetries;

    /** Original exception thrown by the function. Will be null if numRetries is 0. */
    @Nullable private final Exception firstException;

    /**
     * Creates an instance for a successful call with no retries.
     *
     * @param value result of the function
     */
    public RetryResult(T value) {
      this(value, 0, null);
    }

    /**
     * Creates an instance for a successful call after at least one retry.
     *
     * @param value result of the function
     * @param numRetries number of retries needed to get successful
     * @param firstException first exception that triggered the retries
     */
    public RetryResult(T value, int numRetries, @Nullable Exception firstException) {
      assert firstException != null || numRetries == 0;
      this.value = value;
      this.numRetries = numRetries;
      this.firstException = firstException;
    }
  }
}
