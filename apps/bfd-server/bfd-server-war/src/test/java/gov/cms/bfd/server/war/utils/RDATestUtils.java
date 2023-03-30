package gov.cms.bfd.server.war.utils;

import gov.cms.bfd.DatabaseTestUtils;
import gov.cms.bfd.model.rda.Mbi;
import gov.cms.bfd.model.rda.RdaFissClaim;
import gov.cms.bfd.model.rda.RdaFissDiagnosisCode;
import gov.cms.bfd.model.rda.RdaFissPayer;
import gov.cms.bfd.model.rda.RdaFissProcCode;
import gov.cms.bfd.model.rda.RdaMcsClaim;
import gov.cms.bfd.model.rda.RdaMcsDetail;
import gov.cms.bfd.model.rda.RdaMcsDiagnosisCode;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.Persistence;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.sql.DataSource;

/** Supplies test data for the RDA based unit tests. */
public class RDATestUtils {

  /** Tracking entities (tables) so they can be cleaned after. */
  private static final List<Class<?>> TABLE_ENTITIES =
      List.of(
          RdaFissPayer.class,
          RdaFissDiagnosisCode.class,
          RdaFissProcCode.class,
          RdaFissClaim.class,
          RdaMcsDetail.class,
          RdaMcsDiagnosisCode.class,
          RdaMcsClaim.class,
          Mbi.class);

  /** Path to use for persistence units. */
  public static final String PERSISTENCE_UNIT_NAME = "gov.cms.bfd.rda";
  /** Test mbi. */
  public static final String MBI = "123456MBI";
  /** Test mbi hash. */
  public static final String MBI_HASH = "a7f8e93f09";
  /** Test mbi has (old). */
  public static final String MBI_OLD_HASH = "3816a4c752";
  /** Test fiss claim (DCN). */
  public static final String FISS_CLAIM_A_DCN = "123456d";
  /** Test fiss claim (DCN). */
  public static final String FISS_CLAIM_B_DCN = "123457d";
  /** Test fiss claim (ClaimId). */
  public static final String FISS_CLAIM_A_CLAIM_ID = "123456";
  /** Test fiss claim (ClaimId). */
  public static final String FISS_CLAIM_B_CLAIM_ID = "123457";

  /** The entity manager. */
  private EntityManager entityManager;

  /** Initializes the test utility. */
  public void init() {
    final DataSource dataSource = DatabaseTestUtils.get().getUnpooledDataSource();

    final Map<String, Object> hibernateProperties =
        Map.of(org.hibernate.cfg.AvailableSettings.DATASOURCE, dataSource);

    entityManager =
        Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME, hibernateProperties)
            .createEntityManager();
  }

  /** Closes all resources. */
  public void destroy() {
    if (entityManager != null) {
      entityManager.close();
    }
  }

  /**
   * Gets the {@link #entityManager}.
   *
   * @return the entity manager
   */
  public EntityManager getEntityManager() {
    return entityManager;
  }

  /**
   * Seed data into the database for testing.
   *
   * @param includeOldHash whether to include the old hash
   */
  public void seedData(boolean includeOldHash) {
    doTransaction(
        em -> {
          String oldHash = includeOldHash ? MBI_OLD_HASH : null;
          Mbi mbi = em.merge(Mbi.builder().mbi(MBI).hash(MBI_HASH).oldHash(oldHash).build());
          em.merge(fissTestDataA(mbi));
          em.merge(fissTestDataB(mbi));
          em.merge(mcsTestDataA(mbi));
          em.merge(mcsTestDataB(mbi));
        });
  }

  /** Inserts an MBI cache record for use with test case claims. */
  public void seedMbiRecord() {
    final var mbi = Mbi.builder().mbi(MBI).hash(MBI_HASH).oldHash(MBI_OLD_HASH).build();
    doTransaction(em -> em.merge(mbi));
  }

  /** Delete all the test data from the db. */
  public void truncateTables() {
    doTransaction(
        em ->
            TABLE_ENTITIES.forEach(
                e -> em.createQuery("delete from " + e.getSimpleName() + " f").executeUpdate()));
  }

  /**
   * Helper method to perform transactions with the db.
   *
   * @param transaction Lambda containing the queries to commit to this transaction.
   */
  private void doTransaction(Consumer<EntityManager> transaction) {
    entityManager.getTransaction().begin();
    transaction.accept(entityManager);
    entityManager.getTransaction().commit();
  }

  /**
   * Fetches the expected response for a given requestId.
   *
   * <p>Each expected response is a dedicated json file in the test resources folder.
   *
   * @param requestId The ID associated with the request made.
   * @return The expected json string response associated to the given requestId.
   */
  public String expectedResponseFor(String requestId) {
    StringBuilder expectedResponse = new StringBuilder();
    InputStream fileStream =
        this.getClass()
            .getClassLoader()
            .getResourceAsStream("endpoint-responses/v2/" + requestId + ".json");

    if (fileStream != null) {
      try (BufferedReader br = new BufferedReader(new InputStreamReader(fileStream))) {

        String line;
        while ((line = br.readLine()) != null) {
          expectedResponse.append(line.trim());
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    return expectedResponse.toString();
  }

  /**
   * One FISS claim for testing.
   *
   * @param mbi the mbi
   * @return The FISS test claim A
   */
  private RdaFissClaim fissTestDataA(Mbi mbi) {
    final var lastUpdated = LocalDateTime.of(1970, 8, 1, 0, 0, 0).toInstant(ZoneOffset.UTC);
    RdaFissClaim claim =
        RdaFissClaim.builder()
            .sequenceNumber(1L)
            .claimId(FISS_CLAIM_A_CLAIM_ID)
            .dcn(FISS_CLAIM_A_DCN)
            .intermediaryNb("99999")
            .hicNo("hicnumber")
            .currStatus('a')
            .currLoc1('z')
            .currLoc2("Somda")
            .medaProvId("meda12345")
            .medaProv_6("meda12")
            .fedTaxNumber("tax12345")
            .totalChargeAmount(new BigDecimal("1234.32"))
            .receivedDate(LocalDate.of(1970, 1, 1))
            .currTranDate(LocalDate.of(1970, 1, 2))
            .admitDiagCode("admitcd")
            .principleDiag("princcd")
            .npiNumber("8876543211")
            .mbiRecord(mbi)
            .fedTaxNumber("abc123")
            .lobCd("r")
            .lastUpdated(lastUpdated)
            .stmtCovFromDate(LocalDate.of(1970, 7, 10))
            .stmtCovToDate(LocalDate.of(1970, 7, 20))
            .servTypeCd("A")
            .freqCd("C")
            .clmTypInd("4")
            .build();

    Set<RdaFissProcCode> procCodes =
        Set.of(
            RdaFissProcCode.builder()
                .claimId(claim.getClaimId())
                .rdaPosition((short) 1)
                .procCode("CODEABC")
                .procFlag("FLAG")
                .procDate(LocalDate.of(1970, 7, 20))
                .build(),
            RdaFissProcCode.builder()
                .claimId(claim.getClaimId())
                .rdaPosition((short) 2)
                .procCode("CODECBA")
                .procFlag("FLA2")
                .build());

    Set<RdaFissDiagnosisCode> diagnosisCodes =
        Set.of(
            RdaFissDiagnosisCode.builder()
                .claimId(claim.getClaimId())
                .rdaPosition((short) 1)
                .diagCd2("admitcd")
                .diagPoaInd("Z")
                .build(),
            RdaFissDiagnosisCode.builder()
                .claimId(claim.getClaimId())
                .rdaPosition((short) 2)
                .diagCd2("other")
                .diagPoaInd("U")
                .build(),
            RdaFissDiagnosisCode.builder()
                .claimId(claim.getClaimId())
                .rdaPosition((short) 3)
                .diagCd2("princcd")
                .diagPoaInd("n")
                .build());

    Set<RdaFissPayer> payers =
        Set.of(
            RdaFissPayer.builder()
                .claimId(claim.getClaimId())
                .rdaPosition((short) 1)
                .beneFirstName("jim")
                .beneMidInit("k")
                .beneLastName("baker")
                .beneSex("m")
                .beneDob(LocalDate.of(1975, 3, 1))
                .payerType(RdaFissPayer.PayerType.BeneZ)
                .payersName("MEDICARE")
                .build(),
            RdaFissPayer.builder()
                .claimId(claim.getClaimId())
                .rdaPosition((short) 2)
                .insuredName("BAKER  JIM  K")
                .payerType(RdaFissPayer.PayerType.Insured)
                .payersName("BCBS KC")
                .build());

    claim.setPayers(payers);
    claim.setProcCodes(procCodes);
    claim.setDiagCodes(diagnosisCodes);

    return claim;
  }

  /**
   * One FISS claim for testing.
   *
   * @param mbi the mbi
   * @return The FISS test claim B
   */
  private RdaFissClaim fissTestDataB(Mbi mbi) {
    final var lastUpdated = LocalDateTime.of(1970, 8, 7, 0, 0, 0).toInstant(ZoneOffset.UTC);
    RdaFissClaim claim =
        RdaFissClaim.builder()
            .sequenceNumber(2L)
            .claimId(FISS_CLAIM_B_CLAIM_ID)
            .dcn(FISS_CLAIM_B_DCN)
            .intermediaryNb("99999")
            .hicNo("hicnumbe2")
            .currStatus('0')
            .currLoc1('r')
            .currLoc2("Somdb")
            .medaProvId("meda12346")
            .fedTaxNumber("tax12345")
            .totalChargeAmount(new BigDecimal("1235.32"))
            .receivedDate(LocalDate.of(1970, 1, 9))
            .currTranDate(LocalDate.of(1970, 1, 13))
            .admitDiagCode("admitcc")
            .principleDiag("princcc")
            .npiNumber("8876543212")
            .mbiRecord(mbi)
            .fedTaxNumber("abc124")
            .lobCd("k")
            .lastUpdated(lastUpdated)
            .stmtCovFromDate(LocalDate.of(1970, 7, 30))
            .stmtCovToDate(LocalDate.of(1970, 8, 3))
            .servTypeCd("A")
            .freqCd("C")
            .clmTypInd("1")
            .build();

    Set<RdaFissProcCode> procCodes =
        Set.of(
            RdaFissProcCode.builder()
                .claimId(claim.getClaimId())
                .rdaPosition((short) 1)
                .procCode("CODEABD")
                .procFlag("FLAC")
                .procDate(LocalDate.of(1970, 7, 31))
                .build());

    Set<RdaFissDiagnosisCode> diagnosisCodes =
        Set.of(
            RdaFissDiagnosisCode.builder()
                .claimId(claim.getClaimId())
                .rdaPosition((short) 1)
                .diagCd2("princcc")
                .diagPoaInd("Y")
                .build(),
            RdaFissDiagnosisCode.builder()
                .claimId(claim.getClaimId())
                .rdaPosition((short) 2)
                .diagCd2("other2")
                .diagPoaInd("w")
                .build(),
            RdaFissDiagnosisCode.builder()
                .claimId(claim.getClaimId())
                .rdaPosition((short) 3)
                .diagCd2("admitcc")
                .diagPoaInd("1")
                .build());

    Set<RdaFissPayer> payers =
        Set.of(
            RdaFissPayer.builder()
                .claimId(claim.getClaimId())
                .rdaPosition((short) 1)
                .beneFirstName("alice")
                .beneMidInit("r")
                .beneLastName("smith")
                .beneSex("f")
                .beneDob(LocalDate.of(1981, 8, 13))
                .payerType(RdaFissPayer.PayerType.BeneZ)
                .payersName("MEDICARE")
                .build(),
            RdaFissPayer.builder()
                .claimId(claim.getClaimId())
                .rdaPosition((short) 2)
                .insuredName("SMITH  ALICE  R")
                .payerType(RdaFissPayer.PayerType.Insured)
                .payersName("BCBS KC")
                .build());

    claim.setPayers(payers);
    claim.setProcCodes(procCodes);
    claim.setDiagCodes(diagnosisCodes);

    return claim;
  }

  /**
   * One MCS claim for testing.
   *
   * @param mbi the mbi
   * @return The MCS test claim A
   */
  private RdaMcsClaim mcsTestDataA(Mbi mbi) {
    RdaMcsClaim claim =
        RdaMcsClaim.builder()
            .sequenceNumber(1L)
            .idrClmHdIcn("654321")
            .idrContrId("contr")
            .idrHic("HicValue")
            .idrClaimType("R")
            .idrDtlCnt(56)
            .idrBeneLast_1_6("SMITH")
            .idrBeneFirstInit("J")
            .idrBeneMidInit("D")
            .idrBeneSex("M")
            .idrStatusCode("5")
            .idrStatusDate(LocalDate.of(1970, 7, 11))
            .idrBillProvNpi("9876789102")
            .idrBillProvNum("4444422222")
            .idrBillProvEin("1231231231")
            .idrBillProvType("AB")
            .idrBillProvSpec("BA")
            .idrBillProvGroupInd("A")
            .idrBillProvPriceSpec("FF")
            .idrBillProvCounty("GG")
            .idrBillProvLoc("HH")
            .idrTotAllowed(new BigDecimal("323.45"))
            .idrCoinsurance(new BigDecimal("300.45"))
            .idrDeductible(new BigDecimal("23.00"))
            .idrBillProvStatusCd("Z")
            .idrTotBilledAmt(new BigDecimal("23.00"))
            .idrClaimReceiptDate(LocalDate.of(1970, 2, 24))
            .mbiRecord(mbi)
            .idrHdrFromDateOfSvc(LocalDate.of(1970, 7, 28))
            .idrHdrToDateOfSvc(LocalDate.of(1970, 7, 30))
            .lastUpdated(Instant.ofEpochMilli(4000))
            .build();

    Set<RdaMcsDetail> details =
        Set.of(
            RdaMcsDetail.builder()
                .idrDtlNumber((short) 1)
                .idrClmHdIcn("654321")
                .idrDtlToDate(LocalDate.of(1970, 7, 30))
                .idrProcCode("FDSAE")
                .idrDtlPrimaryDiagCode("HF3IJIF")
                .idrModOne("A")
                .build(),
            RdaMcsDetail.builder()
                .idrDtlNumber((short) 2)
                .idrClmHdIcn("654321")
                .idrModTwo("B")
                .build());

    claim.setDetails(details);

    claim.setDiagCodes(
        Set.of(
            new RdaMcsDiagnosisCode("654321", (short) 1, "0", "HF3IJIF"),
            new RdaMcsDiagnosisCode("654321", (short) 2, "9", "HF3IJIG")));

    return claim;
  }

  /**
   * One MCS claim for testing.
   *
   * @param mbi the mbi
   * @return The MCS test claim B
   */
  private RdaMcsClaim mcsTestDataB(Mbi mbi) {
    final String NOT_VALID_CODE = "?";
    RdaMcsClaim claim =
        RdaMcsClaim.builder()
            .sequenceNumber(1L)
            .idrClmHdIcn("654323")
            .idrContrId("contr")
            .idrHic("HicValue")
            .idrClaimType("R")
            .idrDtlCnt(56)
            .idrBeneLast_1_6("SMITH")
            .idrBeneFirstInit("J")
            .idrBeneMidInit("D")
            .idrBeneSex("M")
            .idrStatusCode(NOT_VALID_CODE)
            .idrStatusDate(LocalDate.of(1970, 1, 3))
            .idrBillProvNpi("9876789102")
            .idrBillProvNum("4444422222")
            .idrBillProvEin("1231231231")
            .idrBillProvType("AB")
            .idrBillProvSpec("BA")
            .idrBillProvGroupInd("A")
            .idrBillProvPriceSpec("FF")
            .idrBillProvCounty("GG")
            .idrBillProvLoc("HH")
            .idrTotAllowed(new BigDecimal("224.41"))
            .idrCoinsurance(new BigDecimal("14.32"))
            .idrDeductible(new BigDecimal("11.00"))
            .idrBillProvStatusCd(null)
            .idrTotBilledAmt(new BigDecimal("23.00"))
            .idrClaimReceiptDate(LocalDate.of(1970, 2, 24))
            .mbiRecord(mbi)
            .idrHdrFromDateOfSvc(LocalDate.of(1970, 7, 18))
            .idrHdrToDateOfSvc(LocalDate.of(1970, 7, 20))
            .lastUpdated(Instant.ofEpochMilli(4000))
            .build();

    Set<RdaMcsDetail> details =
        Set.of(
            RdaMcsDetail.builder()
                .idrDtlNumber((short) 1)
                .idrClmHdIcn("654323")
                .idrDtlToDate(LocalDate.of(1970, 7, 28))
                .idrProcCode("FDSAE")
                .idrModOne("A")
                .build(),
            RdaMcsDetail.builder()
                .idrDtlNumber((short) 2)
                .idrClmHdIcn("654323")
                .idrProcCode("FDAAA")
                .idrModTwo("B")
                .build());

    claim.setDetails(details);

    claim.setDiagCodes(
        Set.of(
            new RdaMcsDiagnosisCode("654323", (short) 1, "0", "HF3IJIF"),
            new RdaMcsDiagnosisCode("654323", (short) 2, "9", "HF3IJIG")));

    return claim;
  }

  /**
   * Writes a single {@link RdaMcsClaim} record and associated details based on the given criteria.
   *
   * @param claimId value for {@link RdaMcsClaim#getIdrClmHdIcn}
   * @param lastUpdated value for {@link RdaMcsClaim#getLastUpdated} as a date
   * @param claimServiceDate value for {@link RdaMcsClaim#getIdrHdrToDateOfSvc}
   * @param detailServiceDates values for {@link RdaMcsDetail} record {@link
   *     RdaMcsDetail#getIdrDtlToDate}
   */
  public void seedMcsClaimForServiceIdTest(
      String claimId,
      LocalDate lastUpdated,
      LocalDate claimServiceDate,
      List<LocalDate> detailServiceDates) {
    final String NOT_VALID_CODE = "?";
    RdaMcsClaim claim =
        RdaMcsClaim.builder()
            .sequenceNumber(1L)
            .idrClmHdIcn(claimId)
            .idrContrId("contr")
            .idrHic("HicValue")
            .idrClaimType("R")
            .idrDtlCnt(56)
            .idrBeneLast_1_6("SMITH")
            .idrBeneFirstInit("J")
            .idrBeneMidInit("D")
            .idrBeneSex("M")
            .idrStatusCode(NOT_VALID_CODE)
            .idrStatusDate(LocalDate.of(1970, 1, 3))
            .idrBillProvNpi("9876789102")
            .idrBillProvNum("4444422222")
            .idrBillProvEin("1231231231")
            .idrBillProvType("AB")
            .idrBillProvSpec("BA")
            .idrBillProvGroupInd("A")
            .idrBillProvPriceSpec("FF")
            .idrBillProvCounty("GG")
            .idrBillProvLoc("HH")
            .idrTotAllowed(new BigDecimal("224.41"))
            .idrCoinsurance(new BigDecimal("14.32"))
            .idrDeductible(new BigDecimal("11.00"))
            .idrBillProvStatusCd(null)
            .idrTotBilledAmt(new BigDecimal("23.00"))
            .idrClaimReceiptDate(LocalDate.of(1970, 2, 24))
            .lastUpdated(lastUpdated.atTime(12, 0, 0).toInstant(ZoneOffset.UTC))
            .idrHdrToDateOfSvc(claimServiceDate)
            .build();

    var index = new AtomicInteger(1);
    Set<RdaMcsDetail> details =
        detailServiceDates.stream()
            .map(
                serviceDate ->
                    RdaMcsDetail.builder()
                        .idrDtlNumber((short) index.getAndIncrement())
                        .idrClmHdIcn(claimId)
                        .idrDtlToDate(serviceDate)
                        .idrProcCode("FDSAE")
                        .idrModOne("A")
                        .build())
            .collect(Collectors.toSet());

    claim.setDetails(details);

    claim.setDiagCodes(
        Set.of(
            new RdaMcsDiagnosisCode(claimId, (short) 1, "0", "HF3IJIF"),
            new RdaMcsDiagnosisCode(claimId, (short) 2, "1", "HF3IJIG")));

    doTransaction(
        em -> {
          Mbi mbi = lookupTestMbiRecord(em);
          claim.setMbiRecord(mbi);
          em.merge(claim);
        });
  }

  /**
   * Used to look up the {@link Mbi} value that we wrote to the database in an earlier transaction.
   *
   * @param em our {@link EntityManager}
   * @return the {@link Mbi}
   */
  private Mbi lookupTestMbiRecord(EntityManager em) {
    final CriteriaBuilder builder = em.getCriteriaBuilder();
    final CriteriaQuery<Mbi> criteria = builder.createQuery(Mbi.class);
    final Root<Mbi> root = criteria.from(Mbi.class);
    criteria.select(root).where(builder.equal(root.get(Mbi.Fields.mbi), MBI));
    return em.createQuery(criteria).getSingleResult();
  }
}
