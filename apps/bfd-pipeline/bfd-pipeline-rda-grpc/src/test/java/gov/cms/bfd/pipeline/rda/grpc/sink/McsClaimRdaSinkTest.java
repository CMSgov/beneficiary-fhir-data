package gov.cms.bfd.pipeline.rda.grpc.sink;

import static gov.cms.bfd.pipeline.rda.grpc.RdaPipelineTestUtils.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableList;
import com.zaxxer.hikari.HikariDataSource;
import gov.cms.bfd.model.rda.PreAdjMcsClaim;
import gov.cms.bfd.pipeline.rda.grpc.ProcessingException;
import gov.cms.bfd.pipeline.rda.grpc.RdaChange;
import gov.cms.bfd.pipeline.rda.grpc.source.McsClaimTransformer;
import gov.cms.bfd.pipeline.sharedutils.PipelineApplicationState;
import gov.cms.mpsm.rda.v1.McsClaimChange;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
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
public class McsClaimRdaSinkTest {
  private static final String VERSION = "version";

  private final Clock clock = Clock.fixed(Instant.ofEpochMilli(60_000L), ZoneOffset.UTC);
  @Mock private HikariDataSource dataSource;
  @Mock private EntityManagerFactory entityManagerFactory;
  @Mock private EntityManager entityManager;
  @Mock private EntityTransaction transaction;
  @Mock private McsClaimTransformer transformer;
  private MetricRegistry appMetrics;
  private McsClaimRdaSink sink;
  private long nextSeq = 0L;

  @Before
  public void setUp() {
    appMetrics = new MetricRegistry();
    doReturn(entityManager).when(entityManagerFactory).createEntityManager();
    doReturn(transaction).when(entityManager).getTransaction();
    doReturn(true).when(entityManager).isOpen();
    PipelineApplicationState appState =
        new PipelineApplicationState(appMetrics, dataSource, entityManagerFactory, clock);
    sink = new McsClaimRdaSink(appState, transformer, true);
    nextSeq = 0L;
  }

  @Test
  public void metricNames() {
    assertEquals(
        Arrays.asList(
            "McsClaimRdaSink.calls",
            "McsClaimRdaSink.change.latency.millis",
            "McsClaimRdaSink.failures",
            "McsClaimRdaSink.lastSeq",
            "McsClaimRdaSink.successes",
            "McsClaimRdaSink.writes.merged",
            "McsClaimRdaSink.writes.persisted",
            "McsClaimRdaSink.writes.total"),
        new ArrayList<>(appMetrics.getNames()));
  }

  @Test
  public void persistSuccessful() throws Exception {
    final List<RdaChange<PreAdjMcsClaim>> batch =
        ImmutableList.of(createClaim("1"), createClaim("2"), createClaim("3"));

    final int count = sink.writeMessages(VERSION, messagesForBatch(batch));
    assertEquals(3, count);

    for (RdaChange<PreAdjMcsClaim> change : batch) {
      verify(entityManager).persist(change.getClaim());
    }
    verify(entityManager, times(0))
        .merge(any()); // no calls made to merge since all the persist succeeded
    verify(transaction).commit();

    final AbstractClaimRdaSink.Metrics metrics = sink.getMetrics();
    assertMeterReading(1, "calls", metrics.getCalls());
    assertMeterReading(3, "persists", metrics.getObjectsPersisted());
    assertMeterReading(0, "merges", metrics.getObjectsMerged());
    assertMeterReading(3, "writes", metrics.getObjectsWritten());
    assertMeterReading(1, "successes", metrics.getSuccesses());
    assertMeterReading(0, "failures", metrics.getFailures());
    assertGaugeReading(2, "lastSeq", metrics.getLatestSequenceNumber());
  }

  @Test
  public void mergeSuccessful() throws Exception {
    final List<RdaChange<PreAdjMcsClaim>> batch =
        ImmutableList.of(createClaim("1"), createClaim("2"), createClaim("3"));
    doThrow(new EntityExistsException()).when(entityManager).persist(batch.get(1).getClaim());

    final int count = sink.writeMessages(VERSION, messagesForBatch(batch));
    assertEquals(3, count);

    verify(entityManager).persist(batch.get(0).getClaim());
    verify(entityManager).persist(batch.get(1).getClaim());
    verify(entityManager, times(0))
        .persist(batch.get(2).getClaim()); // not called once a persist fails
    for (RdaChange<PreAdjMcsClaim> change : batch) {
      verify(entityManager).merge(change.getClaim());
    }
    // the persist transaction will be rolled back
    verify(transaction).rollback();
    // the merge transaction will be committed
    verify(transaction).commit();

    final AbstractClaimRdaSink.Metrics metrics = sink.getMetrics();
    assertMeterReading(1, "calls", metrics.getCalls());
    assertMeterReading(0, "persists", metrics.getObjectsPersisted());
    assertMeterReading(3, "merges", metrics.getObjectsMerged());
    assertMeterReading(3, "writes", metrics.getObjectsWritten());
    assertMeterReading(1, "successes", metrics.getSuccesses());
    assertMeterReading(0, "failures", metrics.getFailures());
    assertGaugeReading(2, "lastSeq", metrics.getLatestSequenceNumber());
  }

  @Test
  public void persistAndMergeFail() {
    final List<RdaChange<PreAdjMcsClaim>> batch =
        ImmutableList.of(createClaim("1"), createClaim("2"), createClaim("3"));
    doThrow(new EntityExistsException()).when(entityManager).persist(batch.get(0).getClaim());
    doThrow(new EntityNotFoundException()).when(entityManager).merge(batch.get(1).getClaim());

    try {
      sink.writeMessages(VERSION, messagesForBatch(batch));
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

    final AbstractClaimRdaSink.Metrics metrics = sink.getMetrics();
    assertMeterReading(1, "calls", metrics.getCalls());
    assertMeterReading(0, "persists", metrics.getObjectsPersisted());
    assertMeterReading(0, "merges", metrics.getObjectsMerged());
    assertMeterReading(0, "writes", metrics.getObjectsWritten());
    assertMeterReading(0, "successes", metrics.getSuccesses());
    assertMeterReading(1, "failures", metrics.getFailures());
    assertGaugeReading(0, "lastSeq", metrics.getLatestSequenceNumber());
  }

  @Test
  public void persistFatalError() {
    final List<RdaChange<PreAdjMcsClaim>> batch =
        ImmutableList.of(createClaim("1"), createClaim("2"), createClaim("3"));
    doThrow(new RuntimeException("oops")).when(entityManager).persist(batch.get(1).getClaim());

    try {
      sink.writeMessages(VERSION, messagesForBatch(batch));
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

    final AbstractClaimRdaSink.Metrics metrics = sink.getMetrics();
    assertMeterReading(1, "calls", metrics.getCalls());
    assertMeterReading(0, "persists", metrics.getObjectsPersisted());
    assertMeterReading(0, "merges", metrics.getObjectsMerged());
    assertMeterReading(0, "writes", metrics.getObjectsWritten());
    assertMeterReading(0, "successes", metrics.getSuccesses());
    assertMeterReading(1, "failures", metrics.getFailures());
    assertGaugeReading(0, "lastSeq", metrics.getLatestSequenceNumber());
  }

  @Test
  public void closeMethodsAreCalled() throws Exception {
    sink.close();
    verify(entityManager).close();
  }

  private List<McsClaimChange> messagesForBatch(List<RdaChange<PreAdjMcsClaim>> batch) {
    final var messages = ImmutableList.<McsClaimChange>builder();
    for (RdaChange<PreAdjMcsClaim> preAdjMcsClaimRdaChange : batch) {
      var message = mock(McsClaimChange.class);
      doReturn(preAdjMcsClaimRdaChange).when(transformer).transformClaim(message);
      messages.add(message);
    }
    return messages.build();
  }

  private RdaChange<PreAdjMcsClaim> createClaim(String dcn) {
    PreAdjMcsClaim claim = new PreAdjMcsClaim();
    claim.setIdrClmHdIcn(dcn);
    return new RdaChange<>(
        nextSeq++, RdaChange.Type.INSERT, claim, clock.instant().minusMillis(12));
  }
}
