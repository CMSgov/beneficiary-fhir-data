package gov.cms.bfd.server.war.utils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import gov.cms.bfd.model.rda.PreAdjFissClaim;
import gov.cms.bfd.model.rda.PreAdjFissClaimJson;
import gov.cms.bfd.model.rda.PreAdjFissProcCode;
import gov.cms.bfd.model.rda.PreAdjMcsClaim;
import gov.cms.bfd.model.rda.PreAdjMcsClaimJson;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.persistence.EntityManager;
import javax.persistence.Persistence;
import javax.sql.DataSource;

public class RDATestUtils {

  private static final List<Class<?>> TABLE_ENTITIES =
      ImmutableList.of(PreAdjFissClaimJson.class, PreAdjMcsClaimJson.class);

  public static final String PERSISTENCE_UNIT_NAME = "gov.cms.bfd.rda";

  private EntityManager entityManager;

  public void init() {
    final DataSource dataSource = DatabaseTestUtils.get().getUnpooledDataSource();

    final Map<String, Object> hibernateProperties =
        ImmutableMap.of(org.hibernate.cfg.AvailableSettings.DATASOURCE, dataSource);

    entityManager =
        Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME, hibernateProperties)
            .createEntityManager();
  }

  public void destroy() {
    if (entityManager != null) {
      entityManager.close();
    }
  }

  public void seedData(Collection<?> entities) {
    doTransaction(em -> entities.forEach(em::persist));
  }

  public void truncateTables() {
    doTransaction(
        em -> {
          TABLE_ENTITIES.forEach(
              e -> em.createQuery("delete from " + e.getSimpleName() + " f").executeUpdate());
        });
  }

  public void doTransaction(Consumer<EntityManager> transaction) {
    entityManager.getTransaction().begin();
    transaction.accept(entityManager);
    entityManager.getTransaction().commit();
  }

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

  public List<PreAdjFissClaimJson> fissTestData() {
    return Arrays.asList(fissTestDataA(), fissTestDataB());
  }

  private PreAdjFissClaimJson fissTestDataA() {
    PreAdjFissClaim claim =
        PreAdjFissClaim.builder()
            .sequenceNumber(1L)
            .dcn("123456")
            .hicNo("hicnumber")
            .currStatus('a')
            .currLoc1('z')
            .currLoc2("Somda")
            .medaProvId("meda12345")
            .totalChargeAmount(new BigDecimal("1234.32"))
            .receivedDate(LocalDate.ofEpochDay(0))
            .currTranDate(LocalDate.ofEpochDay(1))
            .admitDiagCode("admitcd")
            .principleDiag("princcd")
            .npiNumber("8876543211")
            .mbi("123456MBI")
            .mbiHash("a7f8e93f09")
            .fedTaxNumber("abc123")
            .lastUpdated(Instant.ofEpochMilli(0))
            .stmtCovFromDate(LocalDate.ofEpochDay(200))
            .stmtCovToDate(LocalDate.ofEpochDay(200))
            .build();

    List<PreAdjFissProcCode> codes =
        Lists.newArrayList(
            PreAdjFissProcCode.builder()
                .procCode("CODEABC")
                .procFlag("FLAG")
                .procDate(LocalDate.ofEpochDay(200))
                .lastUpdated(Instant.ofEpochMilli(0))
                .build(),
            PreAdjFissProcCode.builder()
                .procCode("CODECBA")
                .procFlag("FLA2")
                .lastUpdated(Instant.ofEpochMilli(0))
                .build());

    claim.setProcCodes(codes);

    return new PreAdjFissClaimJson(claim);
  }

  private PreAdjFissClaimJson fissTestDataB() {
    PreAdjFissClaim claim =
        PreAdjFissClaim.builder()
            .sequenceNumber(2L)
            .dcn("123457")
            .hicNo("hicnumbe2")
            .currStatus('t')
            .currLoc1('r')
            .currLoc2("Somdb")
            .medaProvId("meda12346")
            .totalChargeAmount(new BigDecimal("1235.32"))
            .receivedDate(LocalDate.ofEpochDay(8))
            .currTranDate(LocalDate.ofEpochDay(12))
            .admitDiagCode("admitcc")
            .principleDiag("princcc")
            .npiNumber("8876543212")
            .mbi("123456MBI")
            .mbiHash("a7f8e93f09")
            .fedTaxNumber("abc124")
            .lastUpdated(Instant.ofEpochMilli(5000))
            .stmtCovFromDate(LocalDate.ofEpochDay(211))
            .stmtCovToDate(LocalDate.ofEpochDay(211))
            .build();

    PreAdjFissProcCode code =
        PreAdjFissProcCode.builder()
            .procCode("CODEABD")
            .procFlag("FLAC")
            .procDate(LocalDate.ofEpochDay(211))
            .lastUpdated(Instant.ofEpochMilli(5000))
            .build();

    claim.setProcCodes(Collections.singletonList(code));

    return new PreAdjFissClaimJson(claim);
  }

  public List<PreAdjMcsClaimJson> mcsTestData() {
    return Collections.singletonList(mcsTestDataA());
  }

  private PreAdjMcsClaimJson mcsTestDataA() {
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

    List<PreAdjMcsDetail> procCodes =
        Lists.newArrayList(
            PreAdjMcsDetail.builder()
                .idrDtlToDate(LocalDate.ofEpochDay(208))
                .idrProcCode("FDSAE")
                .build(),
            PreAdjMcsDetail.builder().idrProcCode("FDAAA").build());

    claim.setDetails(procCodes);

    claim.setDiagCodes(
        Lists.newArrayList(
            new PreAdjMcsDiagnosisCode("0", "HF3IJIF", Instant.ofEpochMilli(4000)),
            new PreAdjMcsDiagnosisCode("1", "HF3IJIG", Instant.ofEpochMilli(4000))));

    return new PreAdjMcsClaimJson(claim);
  }
}
