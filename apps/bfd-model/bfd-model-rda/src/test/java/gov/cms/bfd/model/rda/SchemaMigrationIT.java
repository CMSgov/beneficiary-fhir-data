package gov.cms.bfd.model.rda;

import static java.lang.String.format;
import static org.junit.Assert.assertEquals;

import com.google.common.collect.ImmutableMap;
import gov.cms.bfd.model.rif.schema.DatabaseSchemaManager;
import java.math.BigDecimal;
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
    final PreAdjFissClaim claim =
        PreAdjFissClaim.builder()
            .dcn("1")
            .hicNo("h1")
            .currStatus('1')
            .currLoc1('A')
            .currLoc2("1A")
            .pracLocCity("city name can be very long indeed")
            .sequenceNumber(3L)
            .build();

    final PreAdjFissProcCode procCode0 =
        PreAdjFissProcCode.builder()
            .dcn(claim.getDcn())
            .priority((short) 0)
            .procCode("P")
            .procFlag("F")
            .procDate(LocalDate.now())
            .lastUpdated(Instant.now())
            .build();
    claim.getProcCodes().add(procCode0);

    final PreAdjFissProcCode procCode1 =
        PreAdjFissProcCode.builder()
            .dcn(claim.getDcn())
            .priority((short) 1)
            .procCode("P")
            .procFlag("G")
            .procDate(LocalDate.now())
            .lastUpdated(Instant.now())
            .build();
    claim.getProcCodes().add(procCode1);

    final PreAdjFissDiagnosisCode diagCode0 =
        PreAdjFissDiagnosisCode.builder()
            .dcn(claim.getDcn())
            .priority((short) 0)
            .diagCd2("cd2")
            .diagPoaInd("Q")
            .build();
    claim.getDiagCodes().add(diagCode0);

    final PreAdjFissDiagnosisCode diagCode1 =
        PreAdjFissDiagnosisCode.builder()
            .dcn(claim.getDcn())
            .priority((short) 1)
            .diagCd2("cd2")
            .diagPoaInd("R")
            .build();
    claim.getDiagCodes().add(diagCode1);

    final PreAdjFissPayer payer0 =
        PreAdjFissPayer.builder()
            .dcn(claim.getDcn())
            .priority((short) 0)
            .payerType(PreAdjFissPayer.PayerType.BeneZ)
            .estAmtDue(new BigDecimal("1.23"))
            .build();
    claim.getPayers().add(payer0);

    final PreAdjFissPayer payer1 =
        PreAdjFissPayer.builder()
            .dcn(claim.getDcn())
            .priority((short) 1)
            .payerType(PreAdjFissPayer.PayerType.Insured)
            .estAmtDue(new BigDecimal("4.56"))
            .build();
    claim.getPayers().add(payer1);

    // Insert a record and read it back to verify some columns and that the detail records were
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
    assertEquals(Long.valueOf(3), resultClaim.getSequenceNumber());
    assertEquals("city name can be very long indeed", resultClaim.getPracLocCity());

    assertEquals("0:F,1:G", summarizeFissProcCodes(resultClaim));
    assertEquals("0:Q,1:R", summarizeFissDiagCodes(resultClaim));
    assertEquals("0:BeneZ:1.23,1:Insured:4.56", summarizeFissPayers(resultClaim));

    // Remove a procCode and diagCode and modify the remaining ones, update, and read back to verify
    // all records updated correctly.
    claim.getProcCodes().remove(procCode1);
    claim.getDiagCodes().remove(diagCode0);
    claim.getPayers().remove(payer0);
    procCode0.setProcFlag("H");
    diagCode1.setDiagPoaInd("S");
    payer1.setEstAmtDue(new BigDecimal("7.89"));
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
    assertEquals("1:Insured:7.89", summarizeFissPayers(resultClaim));
  }

  /**
   * Quick persist and query to verify the entities are compatible with hibernate. Also verify the
   * cascaded updates work correctly.
   */
  @Test
  public void mcsClaimEntities() {
    final PreAdjMcsClaim claim =
        PreAdjMcsClaim.builder()
            .idrClmHdIcn("3")
            .idrContrId("c1")
            .idrHic("hc")
            .idrClaimType("c")
            .sequenceNumber(3L)
            .build();

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

    // Insert a record and read it back to verify some columns and that the detail records were
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
    assertEquals("0:T:0,1:U:1,2:V:2", summarizeMcsDiagCodes(resultClaim));

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
    assertEquals(Long.valueOf(3), resultClaim.getSequenceNumber());
    assertEquals("0:P,2:S", summarizeMcsDetails(resultClaim));
    assertEquals("0:W:0,1:U:1", summarizeMcsDiagCodes(resultClaim));
  }

  private PreAdjMcsDetail quickMcsDetail(PreAdjMcsClaim claim, int priority, String dtlStatus) {
    return PreAdjMcsDetail.builder()
        .idrClmHdIcn(claim.getIdrClmHdIcn())
        .priority((short) priority)
        .idrDtlStatus(dtlStatus)
        .build();
  }

  private PreAdjMcsDiagnosisCode quickMcsDiagCode(
      PreAdjMcsClaim claim, int priority, String icdType) {
    return PreAdjMcsDiagnosisCode.builder()
        .idrClmHdIcn(claim.getIdrClmHdIcn())
        .priority((short) priority)
        .idrDiagIcdType(icdType)
        .idrDiagCode(String.valueOf(priority))
        .build();
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

  private String summarizeFissPayers(PreAdjFissClaim resultClaim) {
    return summarizeObjects(
        resultClaim.getPayers().stream(),
        d -> format("%d:%s:%s", d.getPriority(), d.getPayerType(), d.getEstAmtDue()));
  }

  private String summarizeMcsDetails(PreAdjMcsClaim resultClaim) {
    return summarizeObjects(
        resultClaim.getDetails().stream(),
        d -> format("%d:%s", d.getPriority(), d.getIdrDtlStatus()));
  }

  private String summarizeMcsDiagCodes(PreAdjMcsClaim resultClaim) {
    return summarizeObjects(
        resultClaim.getDiagCodes().stream(),
        d -> format("%d:%s:%s", d.getPriority(), d.getIdrDiagIcdType(), d.getIdrDiagCode()));
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
    dataSource.setUrl("jdbc:hsqldb:mem:" + getClass().getSimpleName());
    return dataSource;
  }
}
