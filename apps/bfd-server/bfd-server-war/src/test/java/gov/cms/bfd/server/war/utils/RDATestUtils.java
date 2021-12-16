package gov.cms.bfd.server.war.utils;

import gov.cms.bfd.model.rda.PreAdjFissClaim;
import gov.cms.bfd.model.rda.PreAdjFissDiagnosisCode;
import gov.cms.bfd.model.rda.PreAdjFissPayer;
import gov.cms.bfd.model.rda.PreAdjFissProcCode;
import gov.cms.bfd.model.rda.PreAdjMcsClaim;
import gov.cms.bfd.model.rda.PreAdjMcsDetail;
import gov.cms.bfd.model.rda.PreAdjMcsDiagnosisCode;
import gov.cms.bfd.model.rif.schema.DatabaseTestUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
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
          PreAdjFissPayer.class,
          PreAdjFissDiagnosisCode.class,
          PreAdjFissProcCode.class,
          PreAdjFissClaim.class,
          PreAdjMcsDetail.class,
          PreAdjMcsDiagnosisCode.class,
          PreAdjMcsClaim.class);

  public static final String PERSISTENCE_UNIT_NAME = "gov.cms.bfd.rda";

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

  /**
   * Seed data into the database for testing.
   *
   * @param entities The entities to seed into the db.
   */
  public void seedData(Collection<?> entities) {
    doTransaction(em -> entities.forEach(em::persist));
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
   * Provides a set of FISS related test data.
   *
   * @return The FISS related test data.
   */
  public List<?> fissTestData() {
    return List.of(fissTestDataA(), fissTestDataB());
  }

  /**
   * One FISS claim for testing
   *
   * @return The FISS test claim A
   */
  private PreAdjFissClaim fissTestDataA() {
    PreAdjFissClaim claim =
        PreAdjFissClaim.builder()
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
            .mbi("123456MBI")
            .mbiHash("a7f8e93f09")
            .fedTaxNumber("abc123")
            .lobCd("r")
            .lastUpdated(Instant.ofEpochMilli(0))
            .stmtCovFromDate(LocalDate.ofEpochDay(200))
            .stmtCovToDate(LocalDate.ofEpochDay(200))
            .servTypeCd("A")
            .freqCd("C")
            .build();

    Set<PreAdjFissProcCode> procCodes =
        Set.of(
            PreAdjFissProcCode.builder()
                .dcn("123456")
                .priority((short) 0)
                .procCode("CODEABC")
                .procFlag("FLAG")
                .procDate(LocalDate.ofEpochDay(200))
                .lastUpdated(Instant.ofEpochMilli(0))
                .build(),
            PreAdjFissProcCode.builder()
                .dcn("123456")
                .priority((short) 1)
                .procCode("CODECBA")
                .procFlag("FLA2")
                .lastUpdated(Instant.ofEpochMilli(0))
                .build());

    Set<PreAdjFissDiagnosisCode> diagnosisCodes =
        Set.of(
            PreAdjFissDiagnosisCode.builder()
                .dcn("123456")
                .priority((short) 0)
                .diagCd2("admitcd")
                .diagPoaInd("Z")
                .build(),
            PreAdjFissDiagnosisCode.builder()
                .dcn("123456")
                .priority((short) 1)
                .diagCd2("other")
                .diagPoaInd("U")
                .build(),
            PreAdjFissDiagnosisCode.builder()
                .dcn("123456")
                .priority((short) 2)
                .diagCd2("princcd")
                .diagPoaInd("n")
                .build());

    Set<PreAdjFissPayer> payers =
        Set.of(
            PreAdjFissPayer.builder()
                .dcn("123456")
                .priority((short) 0)
                .beneFirstName("jim")
                .beneMidInit("k")
                .beneLastName("baker")
                .beneSex("m")
                .beneDob(LocalDate.of(1975, 3, 1))
                .payerType(PreAdjFissPayer.PayerType.BeneZ)
                .payersName("MEDICARE")
                .build(),
            PreAdjFissPayer.builder()
                .dcn("123456")
                .priority((short) 1)
                .insuredName("BAKER  JIM  K")
                .payerType(PreAdjFissPayer.PayerType.Insured)
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
  private PreAdjFissClaim fissTestDataB() {
    PreAdjFissClaim claim =
        PreAdjFissClaim.builder()
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
            .mbi("123456MBI")
            .mbiHash("a7f8e93f09")
            .fedTaxNumber("abc124")
            .lobCd("k")
            .lastUpdated(Instant.ofEpochMilli(5000))
            .stmtCovFromDate(LocalDate.ofEpochDay(211))
            .stmtCovToDate(LocalDate.ofEpochDay(211))
            .servTypeCd("A")
            .freqCd("C")
            .build();

    Set<PreAdjFissProcCode> procCodes =
        Set.of(
            PreAdjFissProcCode.builder()
                .dcn("123457")
                .priority((short) 0)
                .procCode("CODEABD")
                .procFlag("FLAC")
                .procDate(LocalDate.ofEpochDay(211))
                .lastUpdated(Instant.ofEpochMilli(5000))
                .build());

    Set<PreAdjFissDiagnosisCode> diagnosisCodes =
        Set.of(
            PreAdjFissDiagnosisCode.builder()
                .dcn("123457")
                .priority((short) 0)
                .diagCd2("princcc")
                .diagPoaInd("Y")
                .build(),
            PreAdjFissDiagnosisCode.builder()
                .dcn("123457")
                .priority((short) 1)
                .diagCd2("other2")
                .diagPoaInd("w")
                .build(),
            PreAdjFissDiagnosisCode.builder()
                .dcn("123457")
                .priority((short) 2)
                .diagCd2("admitcc")
                .diagPoaInd("1")
                .build());

    Set<PreAdjFissPayer> payers =
        Set.of(
            PreAdjFissPayer.builder()
                .dcn("123457")
                .priority((short) 0)
                .beneFirstName("alice")
                .beneMidInit("r")
                .beneLastName("smith")
                .beneSex("f")
                .beneDob(LocalDate.of(1981, 8, 13))
                .payerType(PreAdjFissPayer.PayerType.BeneZ)
                .payersName("MEDICARE")
                .build(),
            PreAdjFissPayer.builder()
                .dcn("123457")
                .priority((short) 1)
                .insuredName("SMITH  ALICE  R")
                .payerType(PreAdjFissPayer.PayerType.Insured)
                .payersName("BCBS KC")
                .build());

    claim.setPayers(payers);
    claim.setProcCodes(procCodes);
    claim.setDiagCodes(diagnosisCodes);

    return claim;
  }

  /**
   * Provides a set of MCS related test data.
   *
   * @return The MCS related test data.
   */
  public List<PreAdjMcsClaim> mcsTestData() {
    return List.of(mcsTestDataA());
  }

  /**
   * One MCS claim for testing
   *
   * @return The MCS test claim A
   */
  private PreAdjMcsClaim mcsTestDataA() {
    PreAdjMcsClaim claim =
        PreAdjMcsClaim.builder()
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
            .idrClaimMbi("123456MBI")
            .idrClaimMbiHash("a7f8e93f09")
            .idrHdrFromDateOfSvc(LocalDate.ofEpochDay(210))
            .idrHdrToDateOfSvc(LocalDate.ofEpochDay(210))
            .lastUpdated(Instant.ofEpochMilli(4000))
            .build();

    Set<PreAdjMcsDetail> procCodes =
        Set.of(
            PreAdjMcsDetail.builder()
                .priority((short) 0)
                .idrClmHdIcn("654321")
                .idrDtlToDate(LocalDate.ofEpochDay(208))
                .idrProcCode("FDSAE")
                .idrModOne("A")
                .build(),
            PreAdjMcsDetail.builder()
                .priority((short) 1)
                .idrClmHdIcn("654321")
                .idrProcCode("FDAAA")
                .idrModTwo("B")
                .build());

    claim.setDetails(procCodes);

    claim.setDiagCodes(
        Set.of(
            new PreAdjMcsDiagnosisCode(
                "654321", (short) 0, "0", "HF3IJIF", Instant.ofEpochMilli(4000)),
            new PreAdjMcsDiagnosisCode(
                "654321", (short) 1, "1", "HF3IJIG", Instant.ofEpochMilli(4000))));

    return claim;
  }
}
