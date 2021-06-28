package gov.cms.bfd.pipeline.rda.grpc.sink;

import static gov.cms.bfd.pipeline.rda.grpc.sink.JpaClaimRdaSink.isDuplicateKeyException;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableList;
import com.zaxxer.hikari.HikariDataSource;
import gov.cms.bfd.model.rda.PreAdjFissClaim;
import gov.cms.bfd.pipeline.rda.grpc.ProcessingException;
import gov.cms.bfd.pipeline.rda.grpc.RdaChange;
import java.io.IOException;
import java.util.List;
import javax.persistence.EntityExistsException;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityNotFoundException;
import javax.persistence.EntityTransaction;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class JpaClaimRdaSinkTest {
  @Mock private HikariDataSource dataSource;
  @Mock private EntityManagerFactory entityManagerFactory;
  @Mock private EntityManager entityManager;
  @Mock private EntityTransaction transaction;
  private MetricRegistry appMetrics;
  private JpaClaimRdaSink<PreAdjFissClaim> sink;

  @Before
  public void setUp() {
    appMetrics = new MetricRegistry();
    sink = new JpaClaimRdaSink<>(dataSource, entityManagerFactory, entityManager, appMetrics);
    doReturn(transaction).when(entityManager).getTransaction();
  }

  @Test
  public void persistSuccessful() throws Exception {
    final List<RdaChange<PreAdjFissClaim>> batch =
        ImmutableList.of(createClaim("1"), createClaim("2"), createClaim("3"));

    final int count = sink.writeBatch(batch);
    assertEquals(3, count);

    for (RdaChange<PreAdjFissClaim> change : batch) {
      verify(entityManager).persist(change.getClaim());
    }
    verify(entityManager, times(0))
        .merge(any()); // no calls made to merge since all the persist succeeded
    verify(transaction).commit();

    assertMeterReading(1, JpaClaimRdaSink.CALLS_METER_NAME);
    assertMeterReading(3, JpaClaimRdaSink.OBJECTS_PERSISTED_METER_NAME);
    assertMeterReading(0, JpaClaimRdaSink.OBJECTS_MERGED_METER_NAME);
    assertMeterReading(3, JpaClaimRdaSink.OBJECTS_WRITTEN_METER_NAME);
    assertMeterReading(0, JpaClaimRdaSink.FAILURES_METER_NAME);
  }

  @Test
  public void mergeSuccessful() throws Exception {
    final List<RdaChange<PreAdjFissClaim>> batch =
        ImmutableList.of(createClaim("1"), createClaim("2"), createClaim("3"));
    doThrow(new EntityExistsException()).when(entityManager).persist(batch.get(1).getClaim());

    final int count = sink.writeBatch(batch);
    assertEquals(3, count);

    verify(entityManager).persist(batch.get(0).getClaim());
    verify(entityManager).persist(batch.get(1).getClaim());
    verify(entityManager, times(0))
        .persist(batch.get(2).getClaim()); // not called once a persist fails
    for (RdaChange<PreAdjFissClaim> change : batch) {
      verify(entityManager).merge(change.getClaim());
    }
    // the persist transaction will be rolled back
    verify(transaction).rollback();
    // the merge transaction will be committed
    verify(transaction).commit();

    assertMeterReading(1, JpaClaimRdaSink.CALLS_METER_NAME);
    assertMeterReading(0, JpaClaimRdaSink.OBJECTS_PERSISTED_METER_NAME);
    assertMeterReading(3, JpaClaimRdaSink.OBJECTS_MERGED_METER_NAME);
    assertMeterReading(3, JpaClaimRdaSink.OBJECTS_WRITTEN_METER_NAME);
    assertMeterReading(0, JpaClaimRdaSink.FAILURES_METER_NAME);
  }

  @Test
  public void persistAndMergeFail() {
    final List<RdaChange<PreAdjFissClaim>> batch =
        ImmutableList.of(createClaim("1"), createClaim("2"), createClaim("3"));
    doThrow(new EntityExistsException()).when(entityManager).persist(batch.get(0).getClaim());
    doThrow(new EntityNotFoundException()).when(entityManager).merge(batch.get(1).getClaim());

    try {
      sink.writeBatch(batch);
      fail("should have thrown");
    } catch (ProcessingException error) {
      assertEquals(0, error.getProcessedCount());
      assertThat(error.getCause(), CoreMatchers.instanceOf(EntityNotFoundException.class));
    }

    verify(entityManager).persist(batch.get(0).getClaim());
    verify(entityManager, times(0))
        .persist(batch.get(1).getClaim()); // not called once a persist fails
    verify(entityManager, times(0))
        .persist(batch.get(2).getClaim()); // not called once a persist fails
    verify(entityManager).merge(batch.get(0).getClaim());
    verify(entityManager).merge(batch.get(1).getClaim());
    verify(entityManager, times(0))
        .persist(batch.get(2).getClaim()); // not called once a merge fails
    verify(transaction, times(2)).rollback(); // both persist and merge transactions are rolled back

    assertMeterReading(1, JpaClaimRdaSink.CALLS_METER_NAME);
    assertMeterReading(0, JpaClaimRdaSink.OBJECTS_PERSISTED_METER_NAME);
    assertMeterReading(0, JpaClaimRdaSink.OBJECTS_MERGED_METER_NAME);
    assertMeterReading(0, JpaClaimRdaSink.OBJECTS_WRITTEN_METER_NAME);
    assertMeterReading(1, JpaClaimRdaSink.FAILURES_METER_NAME);
  }

  @Test
  public void persistFatalError() {
    final List<RdaChange<PreAdjFissClaim>> batch =
        ImmutableList.of(createClaim("1"), createClaim("2"), createClaim("3"));
    doThrow(new RuntimeException("oops")).when(entityManager).persist(batch.get(1).getClaim());

    try {
      sink.writeBatch(batch);
      fail("should have thrown");
    } catch (ProcessingException error) {
      assertEquals(0, error.getProcessedCount());
      assertThat(error.getCause(), CoreMatchers.instanceOf(RuntimeException.class));
    }

    verify(entityManager).persist(batch.get(0).getClaim());
    verify(entityManager).persist(batch.get(1).getClaim());
    verify(entityManager, times(0))
        .persist(batch.get(2).getClaim()); // not called once a persist fails
    verify(entityManager, times(0))
        .merge(any()); // non-duplicate key error prevents any calls to merge
    verify(transaction).rollback();

    assertMeterReading(1, JpaClaimRdaSink.CALLS_METER_NAME);
    assertMeterReading(0, JpaClaimRdaSink.OBJECTS_PERSISTED_METER_NAME);
    assertMeterReading(0, JpaClaimRdaSink.OBJECTS_MERGED_METER_NAME);
    assertMeterReading(0, JpaClaimRdaSink.OBJECTS_WRITTEN_METER_NAME);
    assertMeterReading(1, JpaClaimRdaSink.FAILURES_METER_NAME);
  }

  @Test
  public void closeMethodsAreCalled() throws Exception {
    sink.close();
    verify(dataSource).close();
    verify(entityManager).close();
    verify(entityManagerFactory).close();
  }

  @Test
  public void exceptionMessageDuplicateKeyDetection() {
    assertEquals(
        true, isDuplicateKeyException(new RuntimeException(new IOException("key already existS"))));
    assertEquals(false, isDuplicateKeyException(new IOException("nothing to see here")));
  }

  private void assertMeterReading(long expected, String meterName) {
    long actual = appMetrics.meter(meterName).getCount();
    assertEquals("Meter " + meterName, expected, actual);
  }

  private RdaChange<PreAdjFissClaim> createClaim(String dcn) {
    PreAdjFissClaim claim = new PreAdjFissClaim();
    claim.setDcn(dcn);
    return new RdaChange<>(RdaChange.Type.INSERT, claim);
  }
}
