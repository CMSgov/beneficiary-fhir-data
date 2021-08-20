package gov.cms.bfd.pipeline.rda.grpc.sink;

import static gov.cms.bfd.pipeline.rda.grpc.RdaChange.MIN_SEQUENCE_NUM;
import static gov.cms.bfd.pipeline.sharedutils.PipelineApplicationState.RDA_PERSISTENCE_UNIT_NAME;
import static org.junit.Assert.assertEquals;

import com.codahale.metrics.MetricRegistry;
import com.zaxxer.hikari.HikariDataSource;
import gov.cms.bfd.model.rda.PreAdjMcsClaim;
import gov.cms.bfd.model.rda.PreAdjMcsDetail;
import gov.cms.bfd.model.rda.PreAdjMcsDiagnosisCode;
import gov.cms.bfd.model.rif.schema.DatabaseSchemaManager;
import gov.cms.bfd.pipeline.rda.grpc.RdaChange;
import gov.cms.bfd.pipeline.sharedutils.DatabaseOptions;
import gov.cms.bfd.pipeline.sharedutils.PipelineApplicationState;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import javax.persistence.EntityManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class McsClaimRdaSinkIT {
  private PipelineApplicationState appState;
  private EntityManager entityManager;

  @Before
  public void setUp() {
    final DatabaseOptions dbOptiona =
        new DatabaseOptions("jdbc:hsqldb:mem:McsClaimRdaSinkIT", "", "", 10);
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
  public void mcsClaim() throws Exception {
    final McsClaimRdaSink sink = new McsClaimRdaSink(appState);

    assertEquals(Optional.empty(), sink.readMaxExistingSequenceNumber());

    final PreAdjMcsClaim claim = new PreAdjMcsClaim();
    claim.setSequenceNumber(7L);
    claim.setIdrClmHdIcn("3");
    claim.setIdrContrId("c1");
    claim.setIdrHic("hc");
    claim.setIdrClaimType("c");

    final PreAdjMcsDetail detail = new PreAdjMcsDetail();
    detail.setIdrClmHdIcn(claim.getIdrClmHdIcn());
    detail.setPriority((short) 0);
    detail.setIdrDtlStatus("P");
    claim.getDetails().add(detail);

    final PreAdjMcsDiagnosisCode diagCode = new PreAdjMcsDiagnosisCode();
    diagCode.setIdrClmHdIcn(claim.getIdrClmHdIcn());
    diagCode.setPriority((short) 0);
    diagCode.setIdrDiagIcdType("T");
    claim.getDiagCodes().add(diagCode);

    int count =
        sink.writeObject(
            new RdaChange<>(MIN_SEQUENCE_NUM, RdaChange.Type.INSERT, claim, Instant.now()));
    assertEquals(1, count);

    List<PreAdjMcsClaim> resultClaims =
        entityManager
            .createQuery("select c from PreAdjMcsClaim c", PreAdjMcsClaim.class)
            .getResultList();
    assertEquals(1, resultClaims.size());
    PreAdjMcsClaim resultClaim = resultClaims.get(0);
    assertEquals(Long.valueOf(7), resultClaim.getSequenceNumber());
    assertEquals("hc", resultClaim.getIdrHic());
    assertEquals(1, resultClaim.getDetails().size());
    assertEquals(1, resultClaim.getDiagCodes().size());

    assertEquals(Optional.of(7L), sink.readMaxExistingSequenceNumber());
  }
}
