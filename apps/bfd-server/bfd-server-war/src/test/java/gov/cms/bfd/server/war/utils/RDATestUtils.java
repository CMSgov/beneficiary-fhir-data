package gov.cms.bfd.server.war.utils;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import gov.cms.bfd.model.rda.PreAdjFissClaim;
import gov.cms.bfd.model.rda.PreAdjFissProcCode;
import gov.cms.bfd.model.rda.PreAdjMcsClaim;
import gov.cms.bfd.model.rda.PreAdjMcsDetail;
import gov.cms.bfd.model.rda.PreAdjMcsDiagnosisCode;
import gov.cms.bfd.model.rif.schema.DatabaseSchemaManager;
import gov.cms.bfd.model.rif.schema.DatabaseTestUtils;
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

  public static final String PERSISTENCE_UNIT_NAME = "gov.cms.bfd.rda";

  private EntityManager entityManager;

  public void init() {
    final DataSource dataSource = DatabaseTestUtils.get().getUnpooledDataSource();
    DatabaseSchemaManager.createOrUpdateSchema(dataSource);

    final Map<String, Object> hibernateProperties =
        ImmutableMap.of(org.hibernate.cfg.AvailableSettings.DATASOURCE, dataSource);

    entityManager =
        Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME, hibernateProperties)
            .createEntityManager();
  }

  public void seedData(Collection<?> entities) {
    doTransaction(em -> entities.forEach(em::persist));
  }

  public void truncate(Class<?> entityClass) {
    doTransaction(
        em -> {
          em.createQuery("delete from " + entityClass.getSimpleName() + " f").executeUpdate();
        });
  }

  public void doTransaction(Consumer<EntityManager> transaction) {
    entityManager.getTransaction().begin();
    transaction.accept(entityManager);
    entityManager.getTransaction().commit();
  }

  public List<PreAdjFissClaim> fissTestData() {
    return Arrays.asList(fissTestDataA(), fissTestDataB());
  }

  private PreAdjFissClaim fissTestDataA() {
    PreAdjFissClaim claim = new PreAdjFissClaim();
    claim.setDcn("123456");
    claim.setHicNo("hicnumber");
    claim.setCurrStatus('a');
    claim.setCurrLoc1('z');
    claim.setCurrLoc2("Somda");
    claim.setMedaProvId("meda12345");
    claim.setTotalChargeAmount(new BigDecimal("1234.32"));
    claim.setReceivedDate(LocalDate.ofEpochDay(0));
    claim.setCurrTranDate(LocalDate.ofEpochDay(1));
    claim.setAdmitDiagCode("admitcd");
    claim.setPrincipleDiag("princcd");
    claim.setNpiNumber("8876543211");
    claim.setMbi("123456MBI");
    claim.setMbiHash("a7f8e93f09");
    claim.setFedTaxNumber("abc123");
    claim.setLastUpdated(Instant.ofEpochMilli(0));
    claim.setStmtCovFromDate(LocalDate.ofEpochDay(200));
    claim.setStmtCovToDate(LocalDate.ofEpochDay(200));

    PreAdjFissProcCode code = new PreAdjFissProcCode();
    code.setDcn("123456");
    code.setPriority((short) 0);
    code.setProcCode("CODEABC");
    code.setProcFlag("FLAG");
    code.setProcDate(LocalDate.ofEpochDay(200));
    code.setLastUpdated(Instant.ofEpochMilli(0));

    claim.setProcCodes(Collections.singleton(code));

    return claim;
  }

  private PreAdjFissClaim fissTestDataB() {
    PreAdjFissClaim claim = new PreAdjFissClaim();
    claim.setDcn("123457");
    claim.setHicNo("hicnumbe2");
    claim.setCurrStatus('t');
    claim.setCurrLoc1('r');
    claim.setCurrLoc2("Somdb");
    claim.setMedaProvId("meda12346");
    claim.setTotalChargeAmount(new BigDecimal("1235.32"));
    claim.setReceivedDate(LocalDate.ofEpochDay(8));
    claim.setCurrTranDate(LocalDate.ofEpochDay(12));
    claim.setAdmitDiagCode("admitcc");
    claim.setPrincipleDiag("princcc");
    claim.setNpiNumber("8876543212");
    claim.setMbi("123456MBI");
    claim.setMbiHash("a7f8e93f09");
    claim.setFedTaxNumber("abc124");
    claim.setLastUpdated(Instant.ofEpochMilli(5000));
    claim.setStmtCovFromDate(LocalDate.ofEpochDay(211));
    claim.setStmtCovToDate(LocalDate.ofEpochDay(211));

    PreAdjFissProcCode code = new PreAdjFissProcCode();
    code.setDcn("123457");
    code.setPriority((short) 0);
    code.setProcCode("CODEABD");
    code.setProcFlag("FLAC");
    code.setProcDate(LocalDate.ofEpochDay(211));
    code.setLastUpdated(Instant.ofEpochMilli(5000));

    claim.setProcCodes(Collections.singleton(code));

    return claim;
  }

  public List<PreAdjMcsClaim> mcsTestData() {
    return Collections.singletonList(mcsTestDataA());
  }

  private PreAdjMcsClaim mcsTestDataA() {
    PreAdjMcsClaim claim = new PreAdjMcsClaim();
    claim.setIdrClmHdIcn("654321");
    claim.setIdrContrId("contr");
    claim.setIdrHic("HicValue");
    claim.setIdrClaimType("R");
    claim.setIdrDtlCnt(56);
    claim.setIdrBeneLast_1_6("SMITH");
    claim.setIdrBeneFirstInit("J");
    claim.setIdrBeneMidInit("D");
    claim.setIdrBeneSex("M");
    claim.setIdrStatusCode("5");
    claim.setIdrStatusDate(LocalDate.ofEpochDay(191));
    claim.setIdrBillProvNpi("9876789102");
    claim.setIdrBillProvNum("4444422222");
    claim.setIdrBillProvEin("1231231231");
    claim.setIdrBillProvType("AB");
    claim.setIdrBillProvSpec("BA");
    claim.setIdrBillProvGroupInd("A");
    claim.setIdrBillProvPriceSpec("FF");
    claim.setIdrBillProvCounty("GG");
    claim.setIdrBillProvLoc("HH");
    claim.setIdrTotAllowed(new BigDecimal("323.45"));
    claim.setIdrCoinsurance(new BigDecimal("300.45"));
    claim.setIdrDeductible(new BigDecimal("23.00"));
    claim.setIdrBillProvStatusCd("Z");
    claim.setIdrTotBilledAmt(new BigDecimal("23.00"));
    claim.setIdrClaimReceiptDate(LocalDate.ofEpochDay(54));
    claim.setIdrClaimMbi("123456MBI");
    claim.setIdrClaimMbiHash("a7f8e93f09");
    claim.setIdrHdrFromDateOfSvc(LocalDate.ofEpochDay(210));
    claim.setIdrHdrToDateOfSvc(LocalDate.ofEpochDay(210));
    claim.setLastUpdated(Instant.ofEpochMilli(4000));

    PreAdjMcsDetail proc = new PreAdjMcsDetail();
    proc.setIdrClmHdIcn("654321");
    proc.setIdrDtlToDate(LocalDate.ofEpochDay(208));
    proc.setIdrProcCode("FDSAE");

    claim.setDetails(Collections.singleton(proc));

    claim.setDiagCodes(
        Sets.newHashSet(
            new PreAdjMcsDiagnosisCode(
                "654321", (short) 0, "0", "HF3IJIF", Instant.ofEpochMilli(4000)),
            new PreAdjMcsDiagnosisCode(
                "654321", (short) 1, "1", "HF3IJIG", Instant.ofEpochMilli(4000))));

    return claim;
  }
}
