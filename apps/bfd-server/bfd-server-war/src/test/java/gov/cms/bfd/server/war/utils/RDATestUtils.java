package gov.cms.bfd.server.war.utils;

import gov.cms.bfd.model.rda.Mbi;
import gov.cms.bfd.model.rda.RdaFissClaim;
import gov.cms.bfd.model.rda.RdaFissDiagnosisCode;
import gov.cms.bfd.model.rda.RdaFissPayer;
import gov.cms.bfd.model.rda.RdaFissProcCode;
import gov.cms.bfd.model.rda.RdaMcsClaim;
import gov.cms.bfd.model.rda.RdaMcsDetail;
import gov.cms.bfd.model.rda.RdaMcsDiagnosisCode;
import gov.cms.bfd.model.rif.schema.DatabaseTestUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import javax.persistence.EntityManager;
import javax.persistence.Persistence;
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

  public static final String PERSISTENCE_UNIT_NAME = "gov.cms.bfd.rda";
  public static final String MBI = "123456MBI";
  public static final String MBI_HASH = "a7f8e93f09";
  public static final String MBI_OLD_HASH = "3816a4c752";

  private EntityManager entityManager;

  public void init() {
    final DataSource dataSource = DatabaseTestUtils.get().getUnpooledDataSource();

    final Map<String, Object> hibernateProperties =
        Map.of(org.hibernate.cfg.AvailableSettings.DATASOURCE, dataSource);

    entityManager =
        Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME, hibernateProperties)
            .createEntityManager();
  }

  public void destroy() {
    if (entityManager != null) {
      entityManager.close();
    }
  }

  public EntityManager getEntityManager() {
    return entityManager;
  }

  /** Seed data into the database for testing. */
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
   * One FISS claim for testing
   *
   * @return The FISS test claim A
   */
  private RdaFissClaim fissTestDataA(Mbi mbi) {
    RdaFissClaim claim =
        RdaFissClaim.builder()
            .sequenceNumber(1L)
            .dcn("123456")
            .hicNo("hicnumber")
            .currStatus('a')
            .currLoc1('z')
            .currLoc2("Somda")
            .medaProvId("meda12345")
            .fedTaxNumber("tax12345")
            .totalChargeAmount(new BigDecimal("1234.32"))
            .receivedDate(LocalDate.ofEpochDay(0))
            .currTranDate(LocalDate.ofEpochDay(1))
            .admitDiagCode("admitcd")
            .principleDiag("princcd")
            .npiNumber("8876543211")
            .mbiRecord(mbi)
            .fedTaxNumber("abc123")
            .lobCd("r")
            .lastUpdated(Instant.ofEpochMilli(0))
            .stmtCovFromDate(LocalDate.ofEpochDay(190))
            .stmtCovToDate(LocalDate.ofEpochDay(200))
            .servTypeCd("A")
            .freqCd("C")
            .build();

    Set<RdaFissProcCode> procCodes =
        Set.of(
            RdaFissProcCode.builder()
                .dcn("123456")
                .priority((short) 0)
                .procCode("CODEABC")
                .procFlag("FLAG")
                .procDate(LocalDate.ofEpochDay(200))
                .lastUpdated(Instant.ofEpochMilli(0))
                .build(),
            RdaFissProcCode.builder()
                .dcn("123456")
                .priority((short) 1)
                .procCode("CODECBA")
                .procFlag("FLA2")
                .lastUpdated(Instant.ofEpochMilli(0))
                .build());

    Set<RdaFissDiagnosisCode> diagnosisCodes =
        Set.of(
            RdaFissDiagnosisCode.builder()
                .dcn("123456")
                .priority((short) 0)
                .diagCd2("admitcd")
                .diagPoaInd("Z")
                .build(),
            RdaFissDiagnosisCode.builder()
                .dcn("123456")
                .priority((short) 1)
                .diagCd2("other")
                .diagPoaInd("U")
                .build(),
            RdaFissDiagnosisCode.builder()
                .dcn("123456")
                .priority((short) 2)
                .diagCd2("princcd")
                .diagPoaInd("n")
                .build());

    Set<RdaFissPayer> payers =
        Set.of(
            RdaFissPayer.builder()
                .dcn("123456")
                .priority((short) 0)
                .beneFirstName("jim")
                .beneMidInit("k")
                .beneLastName("baker")
                .beneSex("m")
                .beneDob(LocalDate.of(1975, 3, 1))
                .payerType(RdaFissPayer.PayerType.BeneZ)
                .payersName("MEDICARE")
                .build(),
            RdaFissPayer.builder()
                .dcn("123456")
                .priority((short) 1)
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
   * One FISS claim for testing
   *
   * @return The FISS test claim B
   */
  private RdaFissClaim fissTestDataB(Mbi mbi) {
    RdaFissClaim claim =
        RdaFissClaim.builder()
            .sequenceNumber(2L)
            .dcn("123457")
            .hicNo("hicnumbe2")
            .currStatus('t')
            .currLoc1('r')
            .currLoc2("Somdb")
            .medaProvId("meda12346")
            .fedTaxNumber("tax12345")
            .totalChargeAmount(new BigDecimal("1235.32"))
            .receivedDate(LocalDate.ofEpochDay(8))
            .currTranDate(LocalDate.ofEpochDay(12))
            .admitDiagCode("admitcc")
            .principleDiag("princcc")
            .npiNumber("8876543212")
            .mbiRecord(mbi)
            .fedTaxNumber("abc124")
            .lobCd("k")
            .lastUpdated(Instant.ofEpochMilli(5000))
            .stmtCovFromDate(LocalDate.ofEpochDay(209))
            .stmtCovToDate(LocalDate.ofEpochDay(211))
            .servTypeCd("A")
            .freqCd("C")
            .build();

    Set<RdaFissProcCode> procCodes =
        Set.of(
            RdaFissProcCode.builder()
                .dcn("123457")
                .priority((short) 0)
                .procCode("CODEABD")
                .procFlag("FLAC")
                .procDate(LocalDate.ofEpochDay(211))
                .lastUpdated(Instant.ofEpochMilli(5000))
                .build());

    Set<RdaFissDiagnosisCode> diagnosisCodes =
        Set.of(
            RdaFissDiagnosisCode.builder()
                .dcn("123457")
                .priority((short) 0)
                .diagCd2("princcc")
                .diagPoaInd("Y")
                .build(),
            RdaFissDiagnosisCode.builder()
                .dcn("123457")
                .priority((short) 1)
                .diagCd2("other2")
                .diagPoaInd("w")
                .build(),
            RdaFissDiagnosisCode.builder()
                .dcn("123457")
                .priority((short) 2)
                .diagCd2("admitcc")
                .diagPoaInd("1")
                .build());

    Set<RdaFissPayer> payers =
        Set.of(
            RdaFissPayer.builder()
                .dcn("123457")
                .priority((short) 0)
                .beneFirstName("alice")
                .beneMidInit("r")
                .beneLastName("smith")
                .beneSex("f")
                .beneDob(LocalDate.of(1981, 8, 13))
                .payerType(RdaFissPayer.PayerType.BeneZ)
                .payersName("MEDICARE")
                .build(),
            RdaFissPayer.builder()
                .dcn("123457")
                .priority((short) 1)
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
   * One MCS claim for testing
   *
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
            .idrStatusDate(LocalDate.ofEpochDay(191))
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
            .idrClaimReceiptDate(LocalDate.ofEpochDay(54))
            .mbiRecord(mbi)
            .idrHdrFromDateOfSvc(LocalDate.ofEpochDay(208))
            .idrHdrToDateOfSvc(LocalDate.ofEpochDay(210))
            .lastUpdated(Instant.ofEpochMilli(4000))
            .build();

    Set<RdaMcsDetail> procCodes =
        Set.of(
            RdaMcsDetail.builder()
                .priority((short) 0)
                .idrClmHdIcn("654321")
                .idrDtlToDate(LocalDate.ofEpochDay(208))
                .idrProcCode("FDSAE")
                .idrModOne("A")
                .build(),
            RdaMcsDetail.builder()
                .priority((short) 1)
                .idrClmHdIcn("654321")
                .idrProcCode("FDAAA")
                .idrModTwo("B")
                .build());

    claim.setDetails(procCodes);

    claim.setDiagCodes(
        Set.of(
            new RdaMcsDiagnosisCode(
                "654321", (short) 0, "0", "HF3IJIF", Instant.ofEpochMilli(4000)),
            new RdaMcsDiagnosisCode(
                "654321", (short) 1, "1", "HF3IJIG", Instant.ofEpochMilli(4000))));

    return claim;
  }

  /**
   * One MCS claim for testing
   *
   * @return The MCS test claim B
   */
  private RdaMcsClaim mcsTestDataB(Mbi mbi) {
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
            .idrStatusCode(null)
            .idrStatusDate(LocalDate.ofEpochDay(2))
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
            .idrClaimReceiptDate(LocalDate.ofEpochDay(54))
            .mbiRecord(mbi)
            .idrHdrFromDateOfSvc(LocalDate.ofEpochDay(198))
            .idrHdrToDateOfSvc(LocalDate.ofEpochDay(200))
            .lastUpdated(Instant.ofEpochMilli(4000))
            .build();

    Set<RdaMcsDetail> procCodes =
        Set.of(
            RdaMcsDetail.builder()
                .priority((short) 0)
                .idrClmHdIcn("654323")
                .idrDtlToDate(LocalDate.ofEpochDay(208))
                .idrProcCode("FDSAE")
                .idrModOne("A")
                .build(),
            RdaMcsDetail.builder()
                .priority((short) 1)
                .idrClmHdIcn("654323")
                .idrProcCode("FDAAA")
                .idrModTwo("B")
                .build());

    claim.setDetails(procCodes);

    claim.setDiagCodes(
        Set.of(
            new RdaMcsDiagnosisCode(
                "654323", (short) 0, "0", "HF3IJIF", Instant.ofEpochMilli(4000)),
            new RdaMcsDiagnosisCode(
                "654323", (short) 1, "1", "HF3IJIG", Instant.ofEpochMilli(4000))));

    return claim;
  }
}
