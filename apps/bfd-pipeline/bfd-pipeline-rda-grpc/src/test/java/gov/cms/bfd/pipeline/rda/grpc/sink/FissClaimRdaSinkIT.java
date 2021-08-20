package gov.cms.bfd.pipeline.rda.grpc.sink;

import static gov.cms.bfd.pipeline.sharedutils.PipelineApplicationState.RDA_PERSISTENCE_UNIT_NAME;
import static org.junit.Assert.assertEquals;

import com.codahale.metrics.MetricRegistry;
import com.zaxxer.hikari.HikariDataSource;
import gov.cms.bfd.model.rda.PreAdjFissClaim;
import gov.cms.bfd.model.rda.PreAdjFissDiagnosisCode;
import gov.cms.bfd.model.rda.PreAdjFissProcCode;
import gov.cms.bfd.model.rif.schema.DatabaseSchemaManager;
import gov.cms.bfd.pipeline.rda.grpc.RdaChange;
import gov.cms.bfd.pipeline.sharedutils.DatabaseOptions;
import gov.cms.bfd.pipeline.sharedutils.PipelineApplicationState;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import javax.persistence.EntityManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class FissClaimRdaSinkIT {
  private PipelineApplicationState appState;
  private EntityManager entityManager;

  @Before
  public void setUp() {
    final DatabaseOptions dbOptiona =
        new DatabaseOptions("jdbc:hsqldb:mem:FissClaimRdaSinkIT", "", "", 10);
    final MetricRegistry appMetrics = new MetricRegistry();
    final HikariDataSource pooledDataSource =
        PipelineApplicationState.createPooledDataSource(dbOptiona, appMetrics);
    DatabaseSchemaManager.createOrUpdateSchema(pooledDataSource);
    appState =
        new PipelineApplicationState(
            appMetrics, pooledDataSource, RDA_PERSISTENCE_UNIT_NAME, Clock.systemUTC());
    entityManager = appState.getEntityManagerFactory().createEntityManager();
  }

  @After
  public void tearDown() throws Exception {
    if (entityManager != null) {
      entityManager.close();
      entityManager = null;
    }
    if (appState != null) {
      appState.close();
      appState = null;
    }
  }

  @Test
  public void fissClaim() throws Exception {
    final FissClaimRdaSink sink = new FissClaimRdaSink(appState);

    assertEquals(Optional.empty(), sink.readMaxExistingSequenceNumber());

    final PreAdjFissClaim claim = new PreAdjFissClaim();
    claim.setSequenceNumber(3L);
    claim.setDcn("1");
    claim.setHicNo("h1");
    claim.setCurrStatus('1');
    claim.setCurrLoc1('A');
    claim.setCurrLoc2("1A");
    claim.setPracLocCity("city name can be very long indeed");

    final PreAdjFissProcCode procCode0 = new PreAdjFissProcCode();
    procCode0.setDcn(claim.getDcn());
    procCode0.setPriority((short) 0);
    procCode0.setProcCode("P");
    procCode0.setProcFlag("F");
    procCode0.setProcDate(LocalDate.now());
    procCode0.setLastUpdated(Instant.now());
    claim.getProcCodes().add(procCode0);

    final PreAdjFissDiagnosisCode diagCode0 = new PreAdjFissDiagnosisCode();
    diagCode0.setDcn(claim.getDcn());
    diagCode0.setPriority((short) 0);
    diagCode0.setDiagCd2("cd2");
    diagCode0.setDiagPoaInd("Q");
    claim.getDiagCodes().add(diagCode0);

    int count =
        sink.writeObject(
            new RdaChange<>(
                claim.getSequenceNumber(), RdaChange.Type.INSERT, claim, Instant.now()));
    assertEquals(1, count);

    List<PreAdjFissClaim> claims =
        entityManager
            .createQuery("select c from PreAdjFissClaim c", PreAdjFissClaim.class)
            .getResultList();
    assertEquals(1, claims.size());
    PreAdjFissClaim resultClaim = claims.get(0);
    assertEquals(Long.valueOf(3), resultClaim.getSequenceNumber());
    assertEquals("h1", resultClaim.getHicNo());
    assertEquals("city name can be very long indeed", resultClaim.getPracLocCity());
    assertEquals(1, resultClaim.getProcCodes().size());
    assertEquals(1, resultClaim.getDiagCodes().size());

    assertEquals(Optional.of(3L), sink.readMaxExistingSequenceNumber());
  }
}
