package gov.cms.bfd.pipeline.sharedutils;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
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
  /** Mock for transactions within first entity manager. */
  @Mock private EntityTransaction transaction1;
  /** Mock for transactions within second entity manager. */
  @Mock private EntityTransaction transaction2;
  /** The {@link TransactionManager} being tested. */
  private TransactionManager transactionManager;

  /** Configures the mocks that are used in all test cases. */
  @BeforeEach
  void setUp() {
    doReturn(entityManager1)
        .doReturn(entityManager2)
        .when(entityManagerFactory)
        .createEntityManager();
    doReturn(transaction1).when(entityManager1).getTransaction();
    doReturn(true).when(entityManager1).isOpen();
    doReturn(transaction2).when(entityManager2).getTransaction();
    doReturn(true).when(entityManager2).isOpen();
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
}
