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
            .lastUpdated(Instant.now())
            .sequenceNumber(3L)
            .build();

    final PreAdjFissProcCode procCode0 =
        PreAdjFissProcCode.builder()
            .procCode("P")
            .procFlag("F")
            .procDate(LocalDate.now())
            .lastUpdated(Instant.now())
            .build();
    claim.getProcCodes().add(procCode0);

    final PreAdjFissProcCode procCode1 =
        PreAdjFissProcCode.builder()
            .procCode("P")
            .procFlag("G")
            .procDate(LocalDate.now())
            .lastUpdated(Instant.now())
            .build();
    claim.getProcCodes().add(procCode1);

    final PreAdjFissDiagnosisCode diagCode0 =
        PreAdjFissDiagnosisCode.builder()
            .diagCd2("cd2")
            .diagPoaInd("Q")
            .lastUpdated(Instant.now())
            .build();
    claim.getDiagCodes().add(diagCode0);

    final PreAdjFissDiagnosisCode diagCode1 =
        PreAdjFissDiagnosisCode.builder()
            .diagCd2("cd2")
            .diagPoaInd("R")
            .lastUpdated(Instant.now())
            .build();
    claim.getDiagCodes().add(diagCode1);

    final PreAdjFissPayer payer0 =
        PreAdjFissPayer.builder()
            .payerType(PreAdjFissPayer.PayerType.BeneZ)
            .estAmtDue(new BigDecimal("1.23"))
            .lastUpdated(Instant.now())
            .build();
    claim.getPayers().add(payer0);

    final PreAdjFissPayer payer1 =
        PreAdjFissPayer.builder()
            .payerType(PreAdjFissPayer.PayerType.Insured)
            .estAmtDue(new BigDecimal("4.56"))
            .lastUpdated(Instant.now())
            .build();
    claim.getPayers().add(payer1);

    final PreAdjFissClaimJson container = new PreAdjFissClaimJson(claim);

    // Insert a record and read it back to verify some columns and that the detail records were
    // written
    entityManager.getTransaction().begin();
    entityManager.persist(container);
    entityManager.getTransaction().commit();

    List<PreAdjFissClaimJson> claims =
        entityManager
            .createQuery("select c from PreAdjFissClaimJson c", PreAdjFissClaimJson.class)
            .getResultList();
    assertEquals(1, claims.size());

    PreAdjFissClaim resultClaim = claims.get(0).getClaim();
    assertEquals("h1", resultClaim.getHicNo());
    assertEquals(Long.valueOf(3), resultClaim.getSequenceNumber());
    assertEquals("city name can be very long indeed", resultClaim.getPracLocCity());

    assertEquals("F,G", summarizeFissProcCodes(resultClaim));
    assertEquals("Q,R", summarizeFissDiagCodes(resultClaim));
    assertEquals("BeneZ:1.23,Insured:4.56", summarizeFissPayers(resultClaim));

    // Remove a procCode and diagCode and modify the remaining ones, update, and read back to verify
    // all records updated correctly.
    claim.getProcCodes().remove(procCode1);
    claim.getDiagCodes().remove(diagCode0);
    claim.getPayers().remove(payer0);
    procCode0.setProcFlag("H");
    diagCode1.setDiagPoaInd("S");
    payer1.setEstAmtDue(new BigDecimal("7.89"));
    entityManager.getTransaction().begin();
    entityManager.persist(container);
    entityManager.getTransaction().commit();
    resultClaim =
        entityManager
            .createQuery("select c from PreAdjFissClaimJson c", PreAdjFissClaimJson.class)
            .getResultList()
            .get(0)
            .getClaim();
    assertEquals("H", summarizeFissProcCodes(resultClaim));
    assertEquals("S", summarizeFissDiagCodes(resultClaim));
    assertEquals("Insured:7.89", summarizeFissPayers(resultClaim));
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
            .lastUpdated(Instant.now())
            .build();

    claim.getDetails().add(quickMcsDetail("P"));
    PreAdjMcsDetail detail1 = quickMcsDetail("Q");
    claim.getDetails().add(detail1);
    PreAdjMcsDetail detail2 = quickMcsDetail("R");
    claim.getDetails().add(detail2);

    PreAdjMcsDiagnosisCode diag0 = quickMcsDiagCode("T");
    claim.getDiagCodes().add(diag0);
    claim.getDiagCodes().add(quickMcsDiagCode("U"));
    PreAdjMcsDiagnosisCode diag2 = quickMcsDiagCode("V");
    claim.getDiagCodes().add(diag2);

    final PreAdjMcsClaimJson container = new PreAdjMcsClaimJson(claim);

    // Insert a record and read it back to verify some columns and that the detail records were
    // written
    entityManager.getTransaction().begin();
    entityManager.persist(container);
    entityManager.getTransaction().commit();

    List<PreAdjMcsClaimJson> resultClaims =
        entityManager
            .createQuery("select c from PreAdjMcsClaimJson c", PreAdjMcsClaimJson.class)
            .getResultList();
    assertEquals(1, resultClaims.size());
    PreAdjMcsClaim resultClaim = resultClaims.get(0).getClaim();
    assertEquals("P,Q,R", summarizeMcsDetails(resultClaim));
    assertEquals("T,U,V", summarizeMcsDiagCodes(resultClaim));

    // Remove a detail and diagCode and modify the remaining ones, update, and read back to verify
    // all records updated correctly.
    claim.getDetails().remove(detail1);
    detail2.setIdrDtlStatus("S");
    claim.getDiagCodes().remove(diag2);
    diag0.setIdrDiagIcdType("W");

    entityManager.getTransaction().begin();
    entityManager.persist(container);
    entityManager.getTransaction().commit();

    resultClaims =
        entityManager
            .createQuery("select c from PreAdjMcsClaimJson c", PreAdjMcsClaimJson.class)
            .getResultList();
    assertEquals(1, resultClaims.size());
    resultClaim = resultClaims.get(0).getClaim();
    assertEquals(Long.valueOf(3), resultClaim.getSequenceNumber());
    assertEquals("P,S", summarizeMcsDetails(resultClaim));
    assertEquals("W,U", summarizeMcsDiagCodes(resultClaim));
  }

  private PreAdjMcsDetail quickMcsDetail(String dtlStatus) {
    return PreAdjMcsDetail.builder().idrDtlStatus(dtlStatus).build();
  }

  private PreAdjMcsDiagnosisCode quickMcsDiagCode(String icdType) {
    return PreAdjMcsDiagnosisCode.builder().idrDiagIcdType(icdType).build();
  }

  private String summarizeFissProcCodes(PreAdjFissClaim resultClaim) {
    return summarizeObjects(resultClaim.getProcCodes().stream(), PreAdjFissProcCode::getProcFlag);
  }

  private String summarizeFissDiagCodes(PreAdjFissClaim resultClaim) {
    return summarizeObjects(
        resultClaim.getDiagCodes().stream(), PreAdjFissDiagnosisCode::getDiagPoaInd);
  }

  private String summarizeFissPayers(PreAdjFissClaim resultClaim) {
    return summarizeObjects(
        resultClaim.getPayers().stream(), d -> format("%s:%s", d.getPayerType(), d.getEstAmtDue()));
  }

  private String summarizeMcsDetails(PreAdjMcsClaim resultClaim) {
    return summarizeObjects(resultClaim.getDetails().stream(), PreAdjMcsDetail::getIdrDtlStatus);
  }

  private String summarizeMcsDiagCodes(PreAdjMcsClaim resultClaim) {
    return summarizeObjects(
        resultClaim.getDiagCodes().stream(), PreAdjMcsDiagnosisCode::getIdrDiagIcdType);
  }

  private <T> String summarizeObjects(Stream<T> objects, Function<T, String> mapping) {
    return objects.map(mapping).collect(Collectors.joining(","));
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
