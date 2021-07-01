package gov.cms.bfd.pipeline.rda.grpc.sink;

import static gov.cms.bfd.pipeline.rda.grpc.sink.FissClaimRdaSink.isDuplicateKeyException;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableList;
import com.zaxxer.hikari.HikariDataSource;
import gov.cms.bfd.model.rda.PreAdjFissClaim;
import gov.cms.bfd.pipeline.rda.grpc.ProcessingException;
import gov.cms.bfd.pipeline.sharedutils.PipelineApplicationState;
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
public class FissClaimRdaSinkTest {
  @Mock private HikariDataSource dataSource;
  @Mock private EntityManagerFactory entityManagerFactory;
  @Mock private EntityManager entityManager;
  @Mock private EntityTransaction transaction;
  private PipelineApplicationState appState;
  private FissClaimRdaSink sink;

  @Before
  public void setUp() throws Exception {
    this.appState =
        new PipelineApplicationState(new MetricRegistry(), dataSource, entityManagerFactory);
    doReturn(entityManager).when(entityManagerFactory).createEntityManager();
    doReturn(transaction).when(entityManager).getTransaction();
    sink = new FissClaimRdaSink(appState);
  }

  @Test
  public void persistSuccessful() throws Exception {
    final List<PreAdjFissClaim> batch =
        ImmutableList.of(createClaim("1"), createClaim("2"), createClaim("3"));

    final int count = sink.writeBatch(batch);
    assertEquals(3, count);

    for (PreAdjFissClaim claim : batch) {
      verify(entityManager).persist(claim);
    }
    verify(entityManager, times(0))
        .merge(any()); // no calls made to merge since all the persist succeeded
    verify(transaction).commit();

    assertMeterReading(1, FissClaimRdaSink.CALLS_METER_NAME);
    assertMeterReading(3, FissClaimRdaSink.OBJECTS_PERSISTED_METER_NAME);
    assertMeterReading(0, FissClaimRdaSink.OBJECTS_MERGED_METER_NAME);
    assertMeterReading(3, FissClaimRdaSink.OBJECTS_WRITTEN_METER_NAME);
    assertMeterReading(0, FissClaimRdaSink.FAILURES_METER_NAME);
  }

  @Test
  public void mergeSuccessful() throws Exception {
    final List<PreAdjFissClaim> batch =
        ImmutableList.of(createClaim("1"), createClaim("2"), createClaim("3"));
    doThrow(new EntityExistsException()).when(entityManager).persist(batch.get(1));

    final int count = sink.writeBatch(batch);
    assertEquals(3, count);

    verify(entityManager).persist(batch.get(0));
    verify(entityManager).persist(batch.get(1));
    verify(entityManager, times(0)).persist(batch.get(2)); // not called once a persist fails
    for (PreAdjFissClaim claim : batch) {
      verify(entityManager).merge(claim);
    }
    // the persist transaction will be rolled back
    verify(transaction).rollback();
    // the merge transaction will be committed
    verify(transaction).commit();

    assertMeterReading(1, FissClaimRdaSink.CALLS_METER_NAME);
    assertMeterReading(0, FissClaimRdaSink.OBJECTS_PERSISTED_METER_NAME);
    assertMeterReading(3, FissClaimRdaSink.OBJECTS_MERGED_METER_NAME);
    assertMeterReading(3, FissClaimRdaSink.OBJECTS_WRITTEN_METER_NAME);
    assertMeterReading(0, FissClaimRdaSink.FAILURES_METER_NAME);
  }

  @Test
  public void persistAndMergeFail() {
    final List<PreAdjFissClaim> batch =
        ImmutableList.of(createClaim("1"), createClaim("2"), createClaim("3"));
    doThrow(new EntityExistsException()).when(entityManager).persist(batch.get(0));
    doThrow(new EntityNotFoundException()).when(entityManager).merge(batch.get(1));

    try {
      sink.writeBatch(batch);
      fail("should have thrown");
    } catch (ProcessingException error) {
      assertEquals(0, error.getProcessedCount());
      assertThat(error.getCause(), CoreMatchers.instanceOf(EntityNotFoundException.class));
    }

    verify(entityManager).persist(batch.get(0));
    verify(entityManager, times(0)).persist(batch.get(1)); // not called once a persist fails
    verify(entityManager, times(0)).persist(batch.get(2)); // not called once a persist fails
    verify(entityManager).merge(batch.get(0));
    verify(entityManager).merge(batch.get(1));
    verify(entityManager, times(0)).persist(batch.get(2)); // not called once a merge fails
    verify(transaction, times(2)).rollback(); // both persist and merge transactions are rolled back

    assertMeterReading(1, FissClaimRdaSink.CALLS_METER_NAME);
    assertMeterReading(0, FissClaimRdaSink.OBJECTS_PERSISTED_METER_NAME);
    assertMeterReading(0, FissClaimRdaSink.OBJECTS_MERGED_METER_NAME);
    assertMeterReading(0, FissClaimRdaSink.OBJECTS_WRITTEN_METER_NAME);
    assertMeterReading(1, FissClaimRdaSink.FAILURES_METER_NAME);
  }

  @Test
  public void persistFatalError() {
    final List<PreAdjFissClaim> batch =
        ImmutableList.of(createClaim("1"), createClaim("2"), createClaim("3"));
    doThrow(new RuntimeException("oops")).when(entityManager).persist(batch.get(1));

    try {
      sink.writeBatch(batch);
      fail("should have thrown");
    } catch (ProcessingException error) {
      assertEquals(0, error.getProcessedCount());
      assertThat(error.getCause(), CoreMatchers.instanceOf(RuntimeException.class));
    }

    verify(entityManager).persist(batch.get(0));
    verify(entityManager).persist(batch.get(1));
    verify(entityManager, times(0)).persist(batch.get(2)); // not called once a persist fails
    verify(entityManager, times(0))
        .merge(any()); // non-duplicate key error prevents any calls to merge
    verify(transaction).rollback();

    assertMeterReading(1, FissClaimRdaSink.CALLS_METER_NAME);
    assertMeterReading(0, FissClaimRdaSink.OBJECTS_PERSISTED_METER_NAME);
    assertMeterReading(0, FissClaimRdaSink.OBJECTS_MERGED_METER_NAME);
    assertMeterReading(0, FissClaimRdaSink.OBJECTS_WRITTEN_METER_NAME);
    assertMeterReading(1, FissClaimRdaSink.FAILURES_METER_NAME);
  }

  @Test
  public void closeMethodsAreCalled() throws Exception {
    sink.close();
    verify(entityManager).close();
  }

  @Test
  public void exceptionMessageDuplicateKeyDetection() {
    assertEquals(
        true, isDuplicateKeyException(new RuntimeException(new IOException("key already existS"))));
    assertEquals(false, isDuplicateKeyException(new IOException("nothing to see here")));
  }

  private void assertMeterReading(long expected, String meterName) {
    long actual = appState.getMetrics().meter(meterName).getCount();
    assertEquals("Meter " + meterName, expected, actual);
  }

  private PreAdjFissClaim createClaim(String dcn) {
    PreAdjFissClaim claim = new PreAdjFissClaim();
    claim.setDcn(dcn);
    return claim;
  }
}
