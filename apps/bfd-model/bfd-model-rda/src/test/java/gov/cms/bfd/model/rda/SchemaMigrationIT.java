package gov.cms.bfd.model.rda;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.google.common.collect.ImmutableMap;
import gov.cms.bfd.model.rif.schema.DatabaseSchemaManager;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.persistence.EntityManager;
import javax.persistence.Persistence;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import org.hibernate.tool.schema.Action;
import org.hsqldb.jdbc.JDBCDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SchemaMigrationIT {
  public static final String PERSISTENCE_UNIT_NAME = "gov.cms.bfd.rda";

  private Connection dbLifetimeConnection;
  private EntityManager entityManager;

  @BeforeEach
  public void setUp() throws SQLException {
    final String dbUrl = "jdbc:hsqldb:mem:" + getClass().getSimpleName();

    // the HSQLDB database will be destroyed when this connection is closed
    dbLifetimeConnection = DriverManager.getConnection(dbUrl + ";shutdown=true", "", "");

    final JDBCDataSource dataSource = new JDBCDataSource();
    dataSource.setUrl(dbUrl);
    dataSource.setUser("");
    dataSource.setPassword("");
    DatabaseSchemaManager.createOrUpdateSchema(dataSource);
    entityManager = createEntityManager(dataSource);
  }

  @AfterEach
  public void tearDown() throws SQLException {
    if (entityManager != null && entityManager.isOpen()) {
      entityManager.close();
      entityManager = null;
    }
    if (dbLifetimeConnection != null) {
      dbLifetimeConnection.close();
      dbLifetimeConnection = null;
    }
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

  @Test
  public void verifyFissMbiQueries() {
    // populate a schema with a bunch of claims
    final List<String> mbis = new ArrayList<>();
    final String hashSuffix = "-hash";
    long seqNo = 1;
    for (int mbiNumber = 1; mbiNumber <= 10; mbiNumber += 1) {
      final String mbi = format("%05d", mbiNumber);
      mbis.add(mbi);
      entityManager.getTransaction().begin();
      Mbi mbiRecord = entityManager.merge(new Mbi(mbi, mbi + hashSuffix));
      for (int claimNumber = 1; claimNumber <= 3; ++claimNumber) {
        final PreAdjFissClaim claim =
            PreAdjFissClaim.builder()
                .dcn(mbi + "d" + claimNumber)
                .hicNo(mbi + "h" + claimNumber)
                .currStatus('1')
                .currLoc1('A')
                .currLoc2("1A")
                .sequenceNumber(seqNo++)
                .mbiRecord(mbiRecord)
                .build();
        entityManager.merge(claim);
      }
      entityManager.getTransaction().commit();
      entityManager.clear();
    }

    // verify the mbis were written to the MbiCache table
    entityManager.getTransaction().begin();
    for (String mbi : mbis) {
      CriteriaBuilder builder = entityManager.getCriteriaBuilder();
      CriteriaQuery<Mbi> criteria = builder.createQuery(Mbi.class);
      Root<Mbi> root = criteria.from(Mbi.class);
      criteria.select(root);
      criteria.where(builder.equal(root.get(Mbi.Fields.mbi), mbi));
      var record = entityManager.createQuery(criteria).getSingleResult();
      assertNotNull(record);
    }
    entityManager.getTransaction().commit();

    // verify we can find the claims using their MBI hash through the mbiRecord
    entityManager.getTransaction().begin();
    for (String mbi : mbis) {
      CriteriaBuilder builder = entityManager.getCriteriaBuilder();
      CriteriaQuery<PreAdjFissClaim> criteria = builder.createQuery(PreAdjFissClaim.class);
      Root<PreAdjFissClaim> root = criteria.from(PreAdjFissClaim.class);
      criteria.select(root);
      criteria.where(
          builder.equal(
              root.get(PreAdjFissClaim.Fields.mbiRecord).get(Mbi.Fields.hash), mbi + hashSuffix));
      var claims = entityManager.createQuery(criteria).getResultList();
      assertEquals(3, claims.size());
      for (PreAdjFissClaim claim : claims) {
        assertEquals(mbi, claim.getDcn().substring(0, mbi.length()));
      }
    }
    entityManager.getTransaction().commit();
  }

  @Test
  public void verifyMcsMbiQueries() {
    // populate a schema with a bunch of claims
    final List<String> mbis = new ArrayList<>();
    final String hashSuffix = "-hash";
    long seqNo = 1;
    for (int mbiNumber = 1; mbiNumber <= 10; mbiNumber += 1) {
      final String mbi = format("%05d", mbiNumber);
      mbis.add(mbi);
      entityManager.getTransaction().begin();
      Mbi mbiRecord = entityManager.merge(new Mbi(mbi, mbi + hashSuffix));
      for (int claimNumber = 1; claimNumber <= 3; ++claimNumber) {
        final PreAdjMcsClaim claim =
            PreAdjMcsClaim.builder()
                .sequenceNumber(7L)
                .idrClmHdIcn(mbi + "i" + claimNumber)
                .idrContrId("c1")
                .idrHic("hc")
                .idrClaimType("c")
                .sequenceNumber(seqNo++)
                .mbiRecord(mbiRecord)
                .build();
        entityManager.merge(claim);
      }
      entityManager.getTransaction().commit();
      entityManager.clear();
    }

    // verify the mbis were written to the MbiCache table
    entityManager.getTransaction().begin();
    for (String mbi : mbis) {
      CriteriaBuilder builder = entityManager.getCriteriaBuilder();
      CriteriaQuery<Mbi> criteria = builder.createQuery(Mbi.class);
      Root<Mbi> root = criteria.from(Mbi.class);
      criteria.select(root);
      criteria.where(builder.equal(root.get(Mbi.Fields.mbi), mbi));
      var record = entityManager.createQuery(criteria).getSingleResult();
      assertNotNull(record);
    }
    entityManager.getTransaction().commit();

    // verify we can find the claims using their MBI hash through the mbiRecord
    entityManager.getTransaction().begin();
    for (String mbi : mbis) {
      CriteriaBuilder builder = entityManager.getCriteriaBuilder();
      CriteriaQuery<PreAdjMcsClaim> criteria = builder.createQuery(PreAdjMcsClaim.class);
      Root<PreAdjMcsClaim> root = criteria.from(PreAdjMcsClaim.class);
      criteria.select(root);
      criteria.where(
          builder.equal(
              root.get(PreAdjFissClaim.Fields.mbiRecord).get(Mbi.Fields.hash), mbi + hashSuffix));
      var claims = entityManager.createQuery(criteria).getResultList();
      assertEquals(3, claims.size());
      for (PreAdjMcsClaim claim : claims) {
        assertEquals(mbi, claim.getIdrClmHdIcn().substring(0, mbi.length()));
      }
    }
    entityManager.getTransaction().commit();
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
}
