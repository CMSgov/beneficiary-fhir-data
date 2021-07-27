package gov.cms.bfd.pipeline.rda.grpc.sink;

import static org.junit.Assert.assertEquals;

import com.codahale.metrics.MetricRegistry;
import com.zaxxer.hikari.HikariDataSource;
import gov.cms.bfd.model.rda.PreAdjFissClaim;
import gov.cms.bfd.model.rda.PreAdjFissDiagnosisCode;
import gov.cms.bfd.model.rda.PreAdjFissProcCode;
import gov.cms.bfd.model.rda.PreAdjMcsClaim;
import gov.cms.bfd.model.rda.PreAdjMcsDetail;
import gov.cms.bfd.model.rda.PreAdjMcsDiagnosisCode;
import gov.cms.bfd.model.rif.schema.DatabaseSchemaManager;
import gov.cms.bfd.pipeline.rda.grpc.RdaChange;
import gov.cms.bfd.pipeline.sharedutils.DatabaseOptions;
import gov.cms.bfd.pipeline.sharedutils.PipelineApplicationState;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import javax.persistence.EntityManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class JpaClaimRdaSinkIT {
  public static final String PERSISTENCE_UNIT_NAME = "gov.cms.bfd.rda";

  private PipelineApplicationState appState;
  private EntityManager entityManager;

  @Before
  public void setUp() {
    final DatabaseOptions dbOptiona = new DatabaseOptions("jdbc:hsqldb:mem:unit-tests", "", "", 10);
    final MetricRegistry appMetrics = new MetricRegistry();
    final HikariDataSource pooledDataSource =
        PipelineApplicationState.createPooledDataSource(dbOptiona, appMetrics);
    DatabaseSchemaManager.createOrUpdateSchema(pooledDataSource);
    appState = new PipelineApplicationState(appMetrics, pooledDataSource, PERSISTENCE_UNIT_NAME);
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
    final JpaClaimRdaSink<PreAdjFissClaim> sink = new JpaClaimRdaSink<>("fiss", appState);

    final PreAdjFissClaim claim = new PreAdjFissClaim();
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

    int count = sink.writeObject(new RdaChange<>(RdaChange.Type.INSERT, claim));
    assertEquals(1, count);

    List<PreAdjFissClaim> claims =
        entityManager
            .createQuery("select c from PreAdjFissClaim c", PreAdjFissClaim.class)
            .getResultList();
    assertEquals(1, claims.size());
    PreAdjFissClaim resultClaim = claims.get(0);
    assertEquals("h1", resultClaim.getHicNo());
    assertEquals("city name can be very long indeed", resultClaim.getPracLocCity());
    assertEquals(1, resultClaim.getProcCodes().size());
    assertEquals(1, resultClaim.getDiagCodes().size());
  }

  @Test
  public void mcsClaim() throws Exception {
    final JpaClaimRdaSink<PreAdjMcsClaim> sink = new JpaClaimRdaSink<>("fiss", appState);

    final PreAdjMcsClaim claim = new PreAdjMcsClaim();
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

    int count = sink.writeObject(new RdaChange<>(RdaChange.Type.INSERT, claim));
    assertEquals(1, count);

    List<PreAdjMcsClaim> resultClaims =
        entityManager
            .createQuery("select c from PreAdjMcsClaim c", PreAdjMcsClaim.class)
            .getResultList();
    assertEquals(1, resultClaims.size());
    PreAdjMcsClaim resultClaim = resultClaims.get(0);
    assertEquals("hc", resultClaim.getIdrHic());
    assertEquals(1, resultClaim.getDetails().size());
    assertEquals(1, resultClaim.getDiagCodes().size());
  }
}
