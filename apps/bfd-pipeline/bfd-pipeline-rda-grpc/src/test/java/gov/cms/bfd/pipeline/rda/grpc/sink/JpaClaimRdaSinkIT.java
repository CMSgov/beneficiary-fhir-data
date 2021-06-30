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
import gov.cms.bfd.pipeline.sharedutils.DatabaseUtils;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import org.hsqldb.jdbc.JDBCDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class JpaClaimRdaSinkIT {
  private HikariDataSource dataSource;
  private EntityManagerFactory entityManagerFactory;
  private EntityManager entityManager;
  private JpaClaimRdaSink<PreAdjFissClaim> sink;

  @Before
  public void setUp() {
    //      sink = new JpaClaimRdaSink<>(dataSource, entityManagerFactory, entityManager, new
    // MetricRegistry());
    JDBCDataSource dataSource1 = new JDBCDataSource();
    dataSource1.setUrl("jdbc:hsqldb:mem:unit-tests");
    final HikariDataSource dataSource =
        DatabaseUtils.createDataSource(dataSource1, new MetricRegistry(), 10);
    DatabaseSchemaManager.createOrUpdateSchema(dataSource);
    entityManagerFactory =
        DatabaseUtils.createEntityManagerFactory(
            dataSource, DatabaseUtils.RDA_PERSISTENCE_UNIT_NAME);
    entityManager = entityManagerFactory.createEntityManager();
  }

  @After
  public void tearDown() {
    if (entityManager != null && entityManager.isOpen()) {
      entityManager.close();
    }
    entityManager = null;
  }

  @Test
  public void fissClaim() throws Exception {
    final JpaClaimRdaSink<PreAdjFissClaim> sink =
        new JpaClaimRdaSink<>(
            "fiss", dataSource, entityManagerFactory, entityManager, new MetricRegistry());

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
    final JpaClaimRdaSink<PreAdjMcsClaim> sink =
        new JpaClaimRdaSink<>(
            "fiss", dataSource, entityManagerFactory, entityManager, new MetricRegistry());

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
