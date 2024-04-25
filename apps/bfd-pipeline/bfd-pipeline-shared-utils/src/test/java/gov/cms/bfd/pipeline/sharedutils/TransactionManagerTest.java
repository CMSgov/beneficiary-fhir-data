package gov.cms.bfd.pipeline.sharedutils;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import gov.cms.bfd.sharedutils.interfaces.ThrowingFunction;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/** Unit tests for {@link TransactionManager}. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class TransactionManagerTest {
  /** Mock for the entity manager factory. */
  @Mock private EntityManagerFactory entityManagerFactory;

  /** Mock for the first created entity manager. */
  @Mock private EntityManager entityManager1;

  /** Mock for the second created entity manager. */
  @Mock private EntityManager entityManager2;

  /** Mock for the second created entity manager. */
  @Mock private EntityManager entityManager3;

  /** Mock for transactions within first entity manager. */
  @Mock private EntityTransaction transaction1;

  /** Mock for transactions within second entity manager. */
  @Mock private EntityTransaction transaction2;

  /** Mock for transactions within second entity manager. */
  @Mock private EntityTransaction transaction3;

  /** The {@link TransactionManager} being tested. */
  private TransactionManager transactionManager;

  /** Configures the mocks that are used in all test cases. */
  @BeforeEach
  void setUp() {
    doReturn(entityManager1)
        .doReturn(entityManager2)
        .doReturn(entityManager3)
        .when(entityManagerFactory)
        .createEntityManager();
    doReturn(transaction1).when(entityManager1).getTransaction();
    doReturn(true).when(entityManager1).isOpen();
    doReturn(transaction2).when(entityManager2).getTransaction();
    doReturn(true).when(entityManager2).isOpen();
    doReturn(transaction3).when(entityManager3).getTransaction();
    doReturn(true).when(entityManager3).isOpen();
    transactionManager = new TransactionManager(entityManagerFactory);
  }

  /** Verifies nothing is opened until needed and thus nothing is closed. */
  @Test
  void closeShouldDoNothingIfNoEntityManagerCreated() {
    transactionManager.close();
    verifyNoInteractions(transaction1, transaction2);
    verifyNoInteractions(entityManager1, entityManager2);
  }

  /**
   * Runs multiple successful queries and ensures all of them used the same {@link EntityManager}.
   */
  @Test
  void shouldReuseSingleEntityManagerIfNoErrors() {
    final int transactionCount = 5;
    for (int i = 1; i <= transactionCount; ++i) {
      transactionManager.executeProcedure(em -> assertSame(em, entityManager1));
    }
    transactionManager.close();

    // only first transaction creates an EntityManager
    verify(entityManagerFactory, times(1)).createEntityManager();

    // all transactions succeed so all commits and no rollbacks
    verify(transaction1, times(transactionCount)).commit();
    verify(transaction1, times(0)).rollback();
    verify(entityManager1, times(1)).close();

    // second entity manager was never used
    verifyNoInteractions(transaction2, entityManager2);
  }

  /**
   * Runs a transaction that throws an exception followed by some successful ones and verifies that
   * the initial {@link EntityManager} was closed following the exception and a new one was used for
   * the remaining transactions.
   */
  @Test
  void shouldUseNewEntityManagerIfExceptionThrown() {
    final var error = new RuntimeException("oops");
    transactionManager.executeProcedure(em -> assertSame(em, entityManager1));
    final var thrown =
        assertThrows(
            RuntimeException.class,
            () ->
                transactionManager.executeProcedure(
                    em -> {
                      throw error;
                    }));
    assertSame(thrown, error);

    // the exception should have closed the EntityManager
    verify(entityManager1, times(1)).close();

    // after the exception we start using a new EntityManager
    transactionManager.executeProcedure(em -> assertSame(em, entityManager2));
    transactionManager.executeProcedure(em -> assertSame(em, entityManager2));

    // close will close the second EntityManager because first one already closed
    transactionManager.close();

    // first transaction succeeded, second failed so one commit and one rollback
    verify(transaction1, times(1)).commit();
    verify(transaction1, times(1)).rollback();
    verify(entityManager1, times(1)).close();

    // both transactions succeeded so two commits and no rollback
    verify(transaction2, times(2)).commit();
    verify(transaction2, times(0)).rollback();
    verify(entityManager2, times(1)).close();
  }

  /**
   * Ensures that {@link TransactionManager#executeFunction} passes through the function's return
   * value.
   */
  @Test
  void shouldReturnFunctionValueOnSuccess() {
    final var expectedResult = 42;
    final var actualResult =
        transactionManager.executeFunction(
            em -> {
              assertSame(em, entityManager1);
              return expectedResult;
            });
    assertEquals(expectedResult, actualResult);
  }

  /**
   * Runs a transaction that succeeds but fails to commit and ensures that the commit exception is
   * passed through to the caller and that all cleanup related exceptions are added to it as
   * suppressed exceptions.
   */
  @Test
  void shouldThrowCommitException() {
    final var commitException = new RuntimeException("commit failed!");
    final var closeException = new RuntimeException("close failed!");

    doThrow(commitException).when(transaction1).commit();
    doThrow(closeException).when(entityManager1).close();
    try {
      transactionManager.executeProcedure(em -> assertSame(em, entityManager1));
      fail("should have thrown commit exception");
    } catch (RuntimeException exception) {
      assertSame(commitException, exception);
      assertArrayEquals(new Throwable[] {closeException}, exception.getSuppressed());
    }
  }

  /**
   * Runs a transaction that fails with an exception and ensures that the exception passed through
   * to the caller and that all cleanup related exceptions are added to it as suppressed exceptions.
   */
  @Test
  void shouldSuppressRollbackException() {
    final var transactionException = new IOException("transaction failed");
    final var rollbackException = new RuntimeException("rollback failed!");
    final var closeException = new RuntimeException("close failed!");

    doThrow(rollbackException).when(transaction1).rollback();
    doThrow(closeException).when(entityManager1).close();
    try {
      transactionManager.executeFunction(
          em -> {
            assertSame(em, entityManager1);
            throw transactionException;
          });
      fail("should have thrown commit exception");
    } catch (Exception exception) {
      assertSame(transactionException, exception);
      assertArrayEquals(
          new Throwable[] {rollbackException, closeException}, exception.getSuppressed());
    }
  }

  /** Verifies a transaction that throws a non-retriable exception does no retries. */
  @Test
  void shouldNotRetryInvalidFirstException() {
    final var transactionException = new IOException("transaction failed");
    final Predicate<Exception> isRetriable = e -> false;
    final ThrowingFunction<String, EntityManager, IOException> function =
        em -> {
          throw transactionException;
        };

    var exception =
        assertThrows(
            IOException.class,
            () -> transactionManager.executeFunctionWithRetries(2, isRetriable, function));
    assertSame(transactionException, exception);
    verify(transaction1).rollback();
    verifyNoInteractions(transaction2);
  }

  /**
   * Verifies a transaction that always throws retriable exceptions throws the first exception after
   * exhausting allowed retries.
   */
  @Test
  void shouldThrowAfterMaxRetries() {
    final var firstException = new IOException("error 1");
    final var secondException = new IOException("error 2");
    final var thirdException = new IOException("error 3");
    final var errors = List.of(firstException, secondException, thirdException);
    final Predicate<Exception> isRetriable = e -> e instanceof IOException;
    final var retryCount = new AtomicInteger();
    final ThrowingFunction<String, EntityManager, IOException> function =
        em -> {
          throw errors.get(retryCount.getAndIncrement());
        };

    var exception =
        assertThrows(
            IOException.class,
            () -> transactionManager.executeFunctionWithRetries(2, isRetriable, function));
    assertSame(firstException, exception);
    assertEquals(errors.subList(1, errors.size()), Arrays.asList(exception.getSuppressed()));
    verify(transaction1).rollback();
    verify(transaction2).rollback();
    verify(transaction3).rollback();
  }

  /**
   * Verifies a transaction that throws a retriable exception on first call but a non-retriable one
   * on retry throws immediately with no more retries.
   */
  @Test
  void shouldThrowAfterRetryWrongException() {
    final var goodException = new IOException("transaction failed");
    final var badException = new IOException("transaction failed badly");
    final Predicate<Exception> isRetriable = e -> e == goodException;
    final var retryCount = new AtomicInteger();
    final ThrowingFunction<String, EntityManager, IOException> function =
        em -> {
          if (retryCount.getAndIncrement() == 1) {
            throw badException;
          } else {
            throw goodException;
          }
        };

    var exception =
        assertThrows(
            IOException.class,
            () -> transactionManager.executeFunctionWithRetries(2, isRetriable, function));
    assertSame(goodException, exception);
    assertSame(badException, exception.getSuppressed()[0]);
    verify(transaction1).rollback();
    verify(transaction2).rollback();
    verifyNoInteractions(transaction3);
  }

  /**
   * Verifies a transaction that throws retriable exceptions and then returns a value returns that
   * value plus the correct number of retries and the first exception with proper suppressed
   * exceptions array.
   */
  @Test
  void shouldReturnSuccessAfterRetries() throws IOException {
    final var firstException = new IOException("transaction failed");
    final var secondException = new IOException("transaction failed");
    final Predicate<Exception> isRetriable = e -> true;
    final var retryCount = new AtomicInteger();
    final ThrowingFunction<String, EntityManager, IOException> function =
        em -> {
          switch (retryCount.getAndIncrement()) {
            case 0:
              throw firstException;
            case 1:
              throw secondException;
            default:
              return "ok";
          }
        };

    var result = transactionManager.executeFunctionWithRetries(2, isRetriable, function);
    assertEquals("ok", result.getValue());
    assertEquals(2, result.getNumRetries());
    assertSame(firstException, result.getFirstException());
    assertSame(secondException, firstException.getSuppressed()[0]);
    verify(transaction1).rollback();
    verify(transaction2).rollback();
    verify(transaction3).commit();
  }
}
