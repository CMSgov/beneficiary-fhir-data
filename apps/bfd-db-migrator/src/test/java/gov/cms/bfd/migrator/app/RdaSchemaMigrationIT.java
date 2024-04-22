package gov.cms.bfd.migrator.app;

import static gov.cms.bfd.migrator.app.MigratorAppIT.LOG_FILE;
import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import com.google.common.collect.ImmutableMap;
import gov.cms.bfd.DataSourceComponents;
import gov.cms.bfd.DatabaseTestUtils;
import gov.cms.bfd.model.rda.Mbi;
import gov.cms.bfd.model.rda.RdaApiProgress;
import gov.cms.bfd.model.rda.RdaClaimMessageMetaData;
import gov.cms.bfd.model.rda.StringList;
import gov.cms.bfd.model.rda.entities.RdaFissClaim;
import gov.cms.bfd.model.rda.entities.RdaFissDiagnosisCode;
import gov.cms.bfd.model.rda.entities.RdaFissPayer;
import gov.cms.bfd.model.rda.entities.RdaFissProcCode;
import gov.cms.bfd.model.rda.entities.RdaMcsClaim;
import gov.cms.bfd.model.rda.entities.RdaMcsDetail;
import gov.cms.bfd.model.rda.entities.RdaMcsDiagnosisCode;
import gov.cms.bfd.sharedutils.config.ConfigLoader;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.sql.DataSource;
import org.awaitility.core.ConditionTimeoutException;
import org.hibernate.tool.schema.Action;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Integration tests to ensure basic functioning of the RDA API related JPA entity classes. */
public class RdaSchemaMigrationIT {

  /** The datasource used throughout the test. */
  private static DataSource dataSource;

  /** The name for persistence units. */
  public static final String PERSISTENCE_UNIT_NAME = "gov.cms.bfd.rda";

  /** The entity manager used for querying the database in the tests. */
  private static EntityManager entityManager;

  /**
   * Locks and truncates the log file so that each test case can read only its own messages from the
   * file when making assertions about log output.
   *
   * @throws Exception unable to lock or truncate the file
   */
  @BeforeEach
  public void lockLogFile() throws Exception {
    LOG_FILE.beginTest(true, 300);
  }

  /** Unlocks the log file. */
  @AfterEach
  public void releaseLogFile() {
    LOG_FILE.endTest();
  }

  /**
   * Running the migrator once cuts the test time down significantly; however
   * the @BeforeAll/@AfterAll annotation requires static methods, thus the static entityManager and
   * datasource as well.
   */
  @BeforeAll
  public static void setUp() {
    MigratorApp app = spy(new MigratorApp());
    ConfigLoader configLoader = createConfigLoader();
    doReturn(configLoader).when(app).createConfigLoader();

    // Await start/finish of application
    try {
      final int exitCode = app.performMigrationsAndHandleExceptions();
      final String logOutput = LOG_FILE.readFileAsString();
      assertEquals(0, exitCode, "Migration failed during test setup. \nOUTPUT:\n" + logOutput);

      final Map<String, Object> hibernateProperties =
          ImmutableMap.of(
              org.hibernate.cfg.AvailableSettings.DATASOURCE,
              dataSource,
              org.hibernate.cfg.AvailableSettings.HBM2DDL_AUTO,
              Action.VALIDATE,
              org.hibernate.cfg.AvailableSettings.STATEMENT_BATCH_SIZE,
              10);

      EntityManagerFactory entityManagerFactory =
          Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME, hibernateProperties);
      entityManager = entityManagerFactory.createEntityManager();
    } catch (ConditionTimeoutException e) {
      final String logOutput = LOG_FILE.readFileAsString();
      fail("Migration application threw exception, OUTPUT:\n" + logOutput, e);
    }
  }

  /** Cleans up the resources after all tests have run. */
  @AfterAll
  public static void tearDown() {
    DatabaseTestUtils.get().dropSchemaForDataSource();
    if (entityManager != null && entityManager.isOpen()) {
      entityManager.close();
      entityManager = null;
    }
    dataSource = null;
  }

  /**
   * Creates a {@link ConfigLoader} for the migrator tests to be run with.
   *
   * @return config with common values set
   */
  private static ConfigLoader createConfigLoader() {
    dataSource = DatabaseTestUtils.get().getUnpooledDataSource();
    DataSourceComponents dataSourceComponents = new DataSourceComponents(dataSource);

    ImmutableMap.Builder<String, String> environment = ImmutableMap.builder();
    environment.put(AppConfiguration.SSM_PATH_DATABASE_URL, dataSourceComponents.getUrl());
    environment.put(
        AppConfiguration.SSM_PATH_DATABASE_USERNAME, dataSourceComponents.getUsername());
    environment.put(
        AppConfiguration.SSM_PATH_DATABASE_PASSWORD, dataSourceComponents.getPassword());
    environment.put(AppConfiguration.SSM_PATH_DATABASE_MAX_POOL_SIZE, "2");

    return ConfigLoader.builder().addMap(environment.build()).build();
  }

  /**
   * Quick persist and query to verify the entities are compatible with hibernate. Also verify the
   * cascaded updates work correctly.
   */
  @Test
  public void fissClaimEntities() {
    final RdaFissClaim claim =
        RdaFissClaim.builder()
            .claimId("1")
            .dcn("d1")
            .intermediaryNb("i1")
            .hicNo("h1")
            .currStatus('1')
            .currLoc1('A')
            .currLoc2("1A")
            .pracLocCity("city name can be very long indeed")
            .sequenceNumber(3L)
            .clmTypInd("1")
            .build();

    final RdaFissProcCode procCode0 =
        RdaFissProcCode.builder()
            .claimId(claim.getClaimId())
            .rdaPosition((short) 1)
            .procCode("P")
            .procFlag("F")
            .procDate(LocalDate.now())
            .build();
    claim.getProcCodes().add(procCode0);

    final RdaFissProcCode procCode1 =
        RdaFissProcCode.builder()
            .claimId(claim.getClaimId())
            .rdaPosition((short) 2)
            .procCode("P")
            .procFlag("G")
            .procDate(LocalDate.now())
            .build();
    claim.getProcCodes().add(procCode1);

    final RdaFissDiagnosisCode diagCode0 =
        RdaFissDiagnosisCode.builder()
            .claimId(claim.getClaimId())
            .rdaPosition((short) 1)
            .diagCd2("cd2")
            .diagPoaInd("Q")
            .build();
    claim.getDiagCodes().add(diagCode0);

    final RdaFissDiagnosisCode diagCode1 =
        RdaFissDiagnosisCode.builder()
            .claimId(claim.getClaimId())
            .rdaPosition((short) 2)
            .diagCd2("cd2")
            .diagPoaInd("R")
            .build();
    claim.getDiagCodes().add(diagCode1);

    final RdaFissPayer payer0 =
        RdaFissPayer.builder()
            .claimId(claim.getClaimId())
            .rdaPosition((short) 1)
            .payerType(RdaFissPayer.PayerType.BeneZ)
            .estAmtDue(new BigDecimal("1.23"))
            .build();
    claim.getPayers().add(payer0);

    final RdaFissPayer payer1 =
        RdaFissPayer.builder()
            .claimId(claim.getClaimId())
            .rdaPosition((short) 2)
            .payerType(RdaFissPayer.PayerType.Insured)
            .estAmtDue(new BigDecimal("4.56"))
            .build();
    claim.getPayers().add(payer1);

    // Insert a record and read it back to verify some columns and that the detail records were
    // written
    entityManager.getTransaction().begin();
    entityManager.persist(claim);
    entityManager.getTransaction().commit();

    List<RdaFissClaim> claims =
        entityManager
            .createQuery("select c from RdaFissClaim c where c.claimId = '1'", RdaFissClaim.class)
            .getResultList();
    assertEquals(1, claims.size());

    RdaFissClaim resultClaim = claims.get(0);
    assertEquals("h1", resultClaim.getHicNo());
    assertEquals(Long.valueOf(3), resultClaim.getSequenceNumber());
    assertEquals("city name can be very long indeed", resultClaim.getPracLocCity());

    assertEquals("1:F,2:G", summarizeFissProcCodes(resultClaim));
    assertEquals("1:Q,2:R", summarizeFissDiagCodes(resultClaim));
    assertEquals("1:BeneZ:1.23,2:Insured:4.56", summarizeFissPayers(resultClaim));

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
            .createQuery("select c from RdaFissClaim c where c.claimId = '1'", RdaFissClaim.class)
            .getResultList()
            .get(0);
    assertEquals("1:H", summarizeFissProcCodes(resultClaim));
    assertEquals("2:S", summarizeFissDiagCodes(resultClaim));
    assertEquals("2:Insured:7.89", summarizeFissPayers(resultClaim));
  }

  /**
   * Quick persist and query to verify the entities are compatible with hibernate. Also verify the
   * cascaded updates work correctly.
   */
  @Test
  public void mcsClaimEntities() {
    final RdaMcsClaim claim =
        RdaMcsClaim.builder()
            .idrClmHdIcn("3")
            .idrContrId("c1")
            .idrHic("hc")
            .idrClaimType("c")
            .sequenceNumber(3L)
            .build();

    claim.getDetails().add(quickMcsDetail(claim, 1, "P"));
    RdaMcsDetail detail1 = quickMcsDetail(claim, 2, "Q");
    claim.getDetails().add(detail1);
    RdaMcsDetail detail2 = quickMcsDetail(claim, 3, "R");
    claim.getDetails().add(detail2);

    RdaMcsDiagnosisCode diag0 = quickMcsDiagCode(claim, 1, "T");
    claim.getDiagCodes().add(diag0);
    claim.getDiagCodes().add(quickMcsDiagCode(claim, 2, "U"));
    RdaMcsDiagnosisCode diag2 = quickMcsDiagCode(claim, 3, "V");
    claim.getDiagCodes().add(diag2);

    // Insert a record and read it back to verify some columns and that the detail records were
    // written
    entityManager.getTransaction().begin();
    entityManager.persist(claim);
    entityManager.getTransaction().commit();

    List<RdaMcsClaim> resultClaims =
        entityManager.createQuery("select c from RdaMcsClaim c", RdaMcsClaim.class).getResultList();
    assertEquals(1, resultClaims.size());
    RdaMcsClaim resultClaim = resultClaims.get(0);
    assertEquals("1:P,2:Q,3:R", summarizeMcsDetails(resultClaim));
    assertEquals("1:T:1,2:U:2,3:V:3", summarizeMcsDiagCodes(resultClaim));

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
        entityManager.createQuery("select c from RdaMcsClaim c", RdaMcsClaim.class).getResultList();
    assertEquals(1, resultClaims.size());
    resultClaim = resultClaims.get(0);
    assertEquals(Long.valueOf(3), resultClaim.getSequenceNumber());
    assertEquals("1:P,3:S", summarizeMcsDetails(resultClaim));
    assertEquals("1:W:1,2:U:2", summarizeMcsDiagCodes(resultClaim));
  }

  /** Ensure that the MBI cache relationships work properly in FISS claim entities. */
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
        final RdaFissClaim claim =
            RdaFissClaim.builder()
                .claimId(mbi + "id" + claimNumber)
                .dcn(mbi + "d" + claimNumber)
                .intermediaryNb(String.format("%03d%02d", mbiNumber, claimNumber))
                .hicNo(mbi + "h" + claimNumber)
                .currStatus('1')
                .currLoc1('A')
                .currLoc2("1A")
                .clmTypInd("1")
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
      CriteriaQuery<RdaFissClaim> criteria = builder.createQuery(RdaFissClaim.class);
      Root<RdaFissClaim> root = criteria.from(RdaFissClaim.class);
      criteria.select(root);
      criteria.where(
          builder.equal(
              root.get(RdaFissClaim.Fields.mbiRecord).get(Mbi.Fields.hash), mbi + hashSuffix));
      var claims = entityManager.createQuery(criteria).getResultList();
      assertEquals(3, claims.size());
      for (RdaFissClaim claim : claims) {
        assertEquals(mbi, claim.getDcn().substring(0, mbi.length()));
      }
    }
    entityManager.getTransaction().commit();
  }

  /** Ensure that the MBI cache relationships work properly in MCS claim entities. */
  @Test
  public void verifyMcsMbiQueries() {
    // populate a schema with a bunch of claims
    final List<String> mbis = new ArrayList<>();
    final String hashSuffix = "-hash";
    long seqNo = 1;
    for (int mbiNumber = 11; mbiNumber <= 20; mbiNumber += 1) {
      final String mbi = format("%05d", mbiNumber);
      mbis.add(mbi);
      entityManager.getTransaction().begin();
      Mbi mbiRecord = entityManager.merge(new Mbi(mbi, mbi + hashSuffix));
      for (int claimNumber = 1; claimNumber <= 3; ++claimNumber) {
        final RdaMcsClaim claim =
            RdaMcsClaim.builder()
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
      CriteriaQuery<RdaMcsClaim> criteria = builder.createQuery(RdaMcsClaim.class);
      Root<RdaMcsClaim> root = criteria.from(RdaMcsClaim.class);
      criteria.select(root);
      criteria.where(
          builder.equal(
              root.get(RdaFissClaim.Fields.mbiRecord).get(Mbi.Fields.hash), mbi + hashSuffix));
      var claims = entityManager.createQuery(criteria).getResultList();
      assertEquals(3, claims.size());
      for (RdaMcsClaim claim : claims) {
        assertEquals(mbi, claim.getIdrClmHdIcn().substring(0, mbi.length()));
      }
    }
    entityManager.getTransaction().commit();
  }

  /**
   * Verifies that claim meta data records can be written to the database and that their {@code
   * metaDataId} fields are properly updated from the sequence when {@code persist()} is called.
   */
  @Test
  public void verifyClaimMetaData() {
    var metaDataList =
        IntStream.of(1, 2, 3)
            .mapToObj(
                i ->
                    RdaClaimMessageMetaData.builder()
                        .sequenceNumber(i)
                        .claimState("A")
                        .claimId(String.valueOf(i))
                        .lastUpdated(Instant.now())
                        .transactionDate(LocalDate.now())
                        .claimType(RdaApiProgress.ClaimType.FISS)
                        .locations(StringList.ofNonEmpty(String.valueOf(i)))
                        .build())
            .collect(Collectors.toList());
    entityManager.getTransaction().begin();
    metaDataList.forEach(entityManager::persist);
    entityManager.getTransaction().commit();
    CriteriaQuery<RdaClaimMessageMetaData> criteria =
        entityManager.getCriteriaBuilder().createQuery(RdaClaimMessageMetaData.class);
    Root<RdaClaimMessageMetaData> root = criteria.from(RdaClaimMessageMetaData.class);
    criteria.select(root);
    var claims = entityManager.createQuery(criteria).getResultList();
    claims.sort(Comparator.comparing(RdaClaimMessageMetaData::getSequenceNumber));
    assertEquals(3, claims.size());
    assertEquals(metaDataList.get(0).getLocations(), claims.get(0).getLocations());
    assertEquals(metaDataList.get(0).getTransactionDate(), claims.get(0).getTransactionDate());
  }

  /**
   * Create a minimally populated {@link RdaMcsDetail} object for use in tests.
   *
   * @param claim parent claim
   * @param idrDtlNumber column value
   * @param dtlStatus column value
   * @return the object
   */
  private RdaMcsDetail quickMcsDetail(RdaMcsClaim claim, int idrDtlNumber, String dtlStatus) {
    return RdaMcsDetail.builder()
        .idrClmHdIcn(claim.getIdrClmHdIcn())
        .idrDtlNumber((short) idrDtlNumber)
        .idrDtlStatus(dtlStatus)
        .build();
  }

  /**
   * Create a minimally populated {@link RdaMcsDiagnosisCode} object for use in tests.
   *
   * @param claim parent claim
   * @param rdaPosition column value
   * @param icdType column value
   * @return the object
   */
  private RdaMcsDiagnosisCode quickMcsDiagCode(RdaMcsClaim claim, int rdaPosition, String icdType) {
    return RdaMcsDiagnosisCode.builder()
        .idrClmHdIcn(claim.getIdrClmHdIcn())
        .rdaPosition((short) rdaPosition)
        .idrDiagIcdType(icdType)
        .idrDiagCode(String.valueOf(rdaPosition))
        .build();
  }

  /**
   * Produce a string that can be used as a quick check that claim was loaded correctly. Combines
   * fields from all of the claim's {@link RdaFissProcCode} objects to produce the string.
   *
   * @param resultClaim claim to summary
   * @return summary of key fields
   */
  private String summarizeFissProcCodes(RdaFissClaim resultClaim) {
    return summarizeObjects(
        resultClaim.getProcCodes().stream(),
        d -> format("%d:%s", d.getRdaPosition(), d.getProcFlag()));
  }

  /**
   * Produce a string that can be used as a quick check that claim was loaded correctly. Combines
   * fields from all of the claim's {@link RdaFissDiagnosisCode} objects to produce the string.
   *
   * @param resultClaim claim to summary
   * @return summary of key fields
   */
  private String summarizeFissDiagCodes(RdaFissClaim resultClaim) {
    return summarizeObjects(
        resultClaim.getDiagCodes().stream(),
        d -> format("%d:%s", d.getRdaPosition(), d.getDiagPoaInd()));
  }

  /**
   * Produce a string that can be used as a quick check that claim was loaded correctly. Combines
   * fields from all of the claim's {@link RdaFissPayer} objects to produce the string.
   *
   * @param resultClaim claim to summary
   * @return summary of key fields
   */
  private String summarizeFissPayers(RdaFissClaim resultClaim) {
    return summarizeObjects(
        resultClaim.getPayers().stream(),
        d -> format("%d:%s:%s", d.getRdaPosition(), d.getPayerType(), d.getEstAmtDue()));
  }

  /**
   * Produce a string that can be used as a quick check that claim was loaded correctly. Combines
   * fields from all of the claim's {@link RdaMcsDetail} objects to produce the string.
   *
   * @param resultClaim claim to summary
   * @return summary of key fields
   */
  private String summarizeMcsDetails(RdaMcsClaim resultClaim) {
    return summarizeObjects(
        resultClaim.getDetails().stream(),
        d -> format("%d:%s", d.getIdrDtlNumber(), d.getIdrDtlStatus()));
  }

  /**
   * Produce a string that can be used as a quick check that claim was loaded correctly. Combines
   * fields from all of the claim's {@link RdaMcsDiagnosisCode} objects to produce the string.
   *
   * @param resultClaim claim to summary
   * @return summary of key fields
   */
  private String summarizeMcsDiagCodes(RdaMcsClaim resultClaim) {
    return summarizeObjects(
        resultClaim.getDiagCodes().stream(),
        d -> format("%d:%s:%s", d.getRdaPosition(), d.getIdrDiagIcdType(), d.getIdrDiagCode()));
  }

  /**
   * Calls a function to extract a string from each object in the stream and then joins them to
   * produce a single summary string.
   *
   * @param objects objects to summary
   * @param mapping function to summarize each object
   * @return string combining all of the summaries
   * @param <T> type of the objects being summarized
   */
  private <T> String summarizeObjects(Stream<T> objects, Function<T, String> mapping) {
    return objects.map(mapping).sorted().collect(Collectors.joining(","));
  }
}
