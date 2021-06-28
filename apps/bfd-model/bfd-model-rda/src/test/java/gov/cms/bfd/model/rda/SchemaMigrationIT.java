package gov.cms.bfd.model.rda;

import static java.lang.String.format;
import static org.junit.Assert.assertEquals;

import com.google.common.collect.ImmutableMap;
import gov.cms.bfd.model.rif.schema.DatabaseSchemaManager;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.persistence.EntityManager;
import javax.persistence.Persistence;
import org.hibernate.tool.schema.Action;
import org.hsqldb.jdbc.JDBCDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SchemaMigrationIT {
  public static final String PERSISTENCE_UNIT_NAME = "gov.cms.bfd.rda";

  private EntityManager entityManager;

  @Before
  public void setUp() {
    final JDBCDataSource dataSource = createInMemoryDataSource();
    DatabaseSchemaManager.createOrUpdateSchema(dataSource);
    entityManager = createEntityManager(dataSource);
  }

  @After
  public void tearDown() {
    if (entityManager != null && entityManager.isOpen()) {
      entityManager.close();
    }
    entityManager = null;
  }

  /**
   * Quick persist and query to verify the entities are compatible with hibernate. Also verify the
   * cascaded updates work correctly.
   */
  @Test
  public void fissClaimEntities() {
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

    final PreAdjFissProcCode procCode1 = new PreAdjFissProcCode();
    procCode1.setDcn(claim.getDcn());
    procCode1.setPriority((short) 1);
    procCode1.setProcCode("P");
    procCode1.setProcFlag("G");
    procCode1.setProcDate(LocalDate.now());
    procCode1.setLastUpdated(Instant.now());
    claim.getProcCodes().add(procCode1);

    final PreAdjFissDiagnosisCode diagCode0 = new PreAdjFissDiagnosisCode();
    diagCode0.setDcn(claim.getDcn());
    diagCode0.setPriority((short) 0);
    diagCode0.setDiagCd2("cd2");
    diagCode0.setDiagPoaInd("Q");
    claim.getDiagCodes().add(diagCode0);

    final PreAdjFissDiagnosisCode diagCode1 = new PreAdjFissDiagnosisCode();
    diagCode1.setDcn(claim.getDcn());
    diagCode1.setPriority((short) 1);
    diagCode1.setDiagCd2("cd2");
    diagCode1.setDiagPoaInd("R");
    claim.getDiagCodes().add(diagCode1);

    // Insert a record and ready back to verify some columns and that the detail records were
    // written
    entityManager.getTransaction().begin();
    entityManager.persist(claim);
    entityManager.getTransaction().commit();

    List<PreAdjFissClaim> claims =
        entityManager
            .createQuery("select c from PreAdjFissClaim c", PreAdjFissClaim.class)
            .getResultList();
    assertEquals(1, claims.size());

    PreAdjFissClaim resultClaim = claims.get(0);
    assertEquals("h1", resultClaim.getHicNo());
    assertEquals("city name can be very long indeed", resultClaim.getPracLocCity());

    assertEquals("0:F,1:G", summarizeFissProcCodes(resultClaim));
    assertEquals("0:Q,1:R", summarizeFissDiagCodes(resultClaim));

    // Remove a procCode and diagCode and modify the remaining ones, update, and read back to verify
    // all records updated correctly.
    claim.getProcCodes().remove(procCode1);
    claim.getDiagCodes().remove(diagCode0);
    procCode0.setProcFlag("H");
    diagCode1.setDiagPoaInd("S");
    entityManager.getTransaction().begin();
    entityManager.persist(claim);
    entityManager.getTransaction().commit();
    resultClaim =
        entityManager
            .createQuery("select c from PreAdjFissClaim c", PreAdjFissClaim.class)
            .getResultList()
            .get(0);
    assertEquals("0:H", summarizeFissProcCodes(resultClaim));
    assertEquals("1:S", summarizeFissDiagCodes(resultClaim));
  }

  /**
   * Quick persist and query to verify the entities are compatible with hibernate. Also verify the
   * cascaded updates work correctly.
   */
  @Test
  public void mcsClaimEntities() {
    final PreAdjMcsClaim claim = new PreAdjMcsClaim();
    claim.setIdrClmHdIcn("3");
    claim.setIdrContrId("c1");
    claim.setIdrHic("hc");
    claim.setIdrClaimType("c");

    claim.getDetails().add(quickMcsDetail(claim, 0, "P"));
    PreAdjMcsDetail detail1 = quickMcsDetail(claim, 1, "Q");
    claim.getDetails().add(detail1);
    PreAdjMcsDetail detail2 = quickMcsDetail(claim, 2, "R");
    claim.getDetails().add(detail2);

    PreAdjMcsDiagnosisCode diag0 = quickMcsDiagCode(claim, 0, "T");
    claim.getDiagCodes().add(diag0);
    claim.getDiagCodes().add(quickMcsDiagCode(claim, 1, "U"));
    PreAdjMcsDiagnosisCode diag2 = quickMcsDiagCode(claim, 2, "V");
    claim.getDiagCodes().add(diag2);

    // Insert a record and ready back to verify some columns and that the detail records were
    // written
    entityManager.getTransaction().begin();
    entityManager.persist(claim);
    entityManager.getTransaction().commit();

    List<PreAdjMcsClaim> resultClaims =
        entityManager
            .createQuery("select c from PreAdjMcsClaim c", PreAdjMcsClaim.class)
            .getResultList();
    assertEquals(1, resultClaims.size());
    PreAdjMcsClaim resultClaim = resultClaims.get(0);
    assertEquals("0:P,1:Q,2:R", summarizeMcsDetails(resultClaim));
    assertEquals("0:T,1:U,2:V", summarizeMcsDiagCodes(resultClaim));

    // Remove a detail and diagCode and modify the remaining ones, update, and read back to verify
    // all records updated correctly.
    claim.getDetails().remove(detail1);
    detail2.setIdrDtlStatus("S");
    claim.getDiagCodes().remove(diag2);
    diag0.setIdrDiagIcdType("W");

    entityManager.getTransaction().begin();
    entityManager.persist(claim);
    entityManager.getTransaction().commit();

    resultClaims =
        entityManager
            .createQuery("select c from PreAdjMcsClaim c", PreAdjMcsClaim.class)
            .getResultList();
    assertEquals(1, resultClaims.size());
    resultClaim = resultClaims.get(0);
    assertEquals("0:P,2:S", summarizeMcsDetails(resultClaim));
    assertEquals("0:W,1:U", summarizeMcsDiagCodes(resultClaim));
  }

  private PreAdjMcsDetail quickMcsDetail(PreAdjMcsClaim claim, int priority, String dtlStatus) {
    final PreAdjMcsDetail detail = new PreAdjMcsDetail();
    detail.setIdrClmHdIcn(claim.getIdrClmHdIcn());
    detail.setPriority((short) priority);
    detail.setIdrDtlStatus(dtlStatus);
    return detail;
  }

  private PreAdjMcsDiagnosisCode quickMcsDiagCode(
      PreAdjMcsClaim claim, int priority, String icdType) {
    final PreAdjMcsDiagnosisCode diagCode = new PreAdjMcsDiagnosisCode();
    diagCode.setIdrClmHdIcn(claim.getIdrClmHdIcn());
    diagCode.setPriority((short) priority);
    diagCode.setIdrDiagIcdType(icdType);
    return diagCode;
  }

  private String summarizeFissProcCodes(PreAdjFissClaim resultClaim) {
    return summarizeObjects(
        resultClaim.getProcCodes().stream(),
        d -> format("%d:%s", d.getPriority(), d.getProcFlag()));
  }

  private String summarizeFissDiagCodes(PreAdjFissClaim resultClaim) {
    return summarizeObjects(
        resultClaim.getDiagCodes().stream(),
        d -> format("%d:%s", d.getPriority(), d.getDiagPoaInd()));
  }

  private String summarizeMcsDetails(PreAdjMcsClaim resultClaim) {
    return summarizeObjects(
        resultClaim.getDetails().stream(),
        d -> format("%d:%s", d.getPriority(), d.getIdrDtlStatus()));
  }

  private String summarizeMcsDiagCodes(PreAdjMcsClaim resultClaim) {
    return summarizeObjects(
        resultClaim.getDiagCodes().stream(),
        d -> format("%d:%s", d.getPriority(), d.getIdrDiagIcdType()));
  }

  private <T> String summarizeObjects(Stream<T> objects, Function<T, String> mapping) {
    return objects.map(mapping).sorted().collect(Collectors.joining(","));
  }

  private EntityManager createEntityManager(JDBCDataSource dataSource) {
    final Map<String, Object> hibernateProperties =
        ImmutableMap.of(
            org.hibernate.cfg.AvailableSettings.DATASOURCE,
            dataSource,
            org.hibernate.cfg.AvailableSettings.HBM2DDL_AUTO,
            Action.VALIDATE,
            org.hibernate.cfg.AvailableSettings.STATEMENT_BATCH_SIZE,
            10);

    return Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME, hibernateProperties)
        .createEntityManager();
  }

  private JDBCDataSource createInMemoryDataSource() {
    JDBCDataSource dataSource = new JDBCDataSource();
    dataSource.setUrl("jdbc:hsqldb:mem:unit-tests");
    return dataSource;
  }
}
