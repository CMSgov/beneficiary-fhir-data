package gov.cms.bfd.server.war.r4.providers.preadj;

import static org.junit.Assert.assertEquals;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import gov.cms.bfd.model.rda.PreAdjFissClaim;
import gov.cms.bfd.model.rda.PreAdjFissProcCode;
import gov.cms.bfd.model.rda.PreAdjMcsClaim;
import gov.cms.bfd.model.rda.PreAdjMcsDetail;
import gov.cms.bfd.model.rda.PreAdjMcsDiagnosisCode;
import gov.cms.bfd.model.rif.schema.DatabaseSchemaManager;
import gov.cms.bfd.model.rif.schema.DatabaseTestUtils;
import gov.cms.bfd.server.war.ServerTestUtils;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.persistence.EntityManager;
import javax.persistence.Persistence;
import javax.sql.DataSource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Claim;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Resource;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class R4ClaimResourceProviderIT {

  public static final String PERSISTENCE_UNIT_NAME = "gov.cms.bfd.rda";

  private static EntityManager entityManager;

  @BeforeClass
  public static void init() {
    final DataSource dataSource = DatabaseTestUtils.get().getUnpooledDataSource();
    DatabaseSchemaManager.createOrUpdateSchema(dataSource);

    entityManager = createEntityManager(dataSource);

    List<PreAdjFissClaim> fissClaims = fissTestData();

    for (PreAdjFissClaim claim : fissClaims) {
      doTransaction((em) -> em.persist(claim));
    }

    List<PreAdjMcsClaim> mcsClaims = mcsTestData();

    for (PreAdjMcsClaim claim : mcsClaims) {
      doTransaction((em) -> em.persist(claim));
    }
  }

  @AfterClass
  public static void tearDown() {
    doTransaction(
        em -> {
          em.createQuery("delete from PreAdjFissProcCode f").executeUpdate();
          em.createQuery("delete from PreAdjFissClaim f").executeUpdate();
        });

    doTransaction(
        em -> {
          em.createQuery("delete from PreAdjMcsDetail m").executeUpdate();
          em.createQuery("delete from PreAdjMcsDiagnosisCode m").executeUpdate();
          em.createQuery("delete from PreAdjMcsClaim m").executeUpdate();
        });
  }

  @Test
  public void shouldGetCorrectFissClaimResourceById() {
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClientV2();

    Claim claimResult = fhirClient.read().resource(Claim.class).withId("f-123456").execute();

    // Created date changes every run, replace it
    claimResult.setCreated(TestResults.TEST_CREATED_DATE);

    String expected = TestResults.TEST_FISS;
    String actual = FhirContext.forR4().newJsonParser().encodeResourceToString(claimResult);

    assertEquals(expected, actual);
  }

  @Test
  public void shouldGetCorrectMcsClaimResourceById() {
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClientV2();

    Claim claimResult = fhirClient.read().resource(Claim.class).withId("m-654321").execute();

    // Created date changes every run, replace it
    claimResult.setCreated(TestResults.TEST_CREATED_DATE);

    String expected = TestResults.TEST_MCS;
    String actual = FhirContext.forR4().newJsonParser().encodeResourceToString(claimResult);

    assertEquals(expected, actual);
  }

  @Test
  public void shouldGetCorrectClaimResourcesByMbiHash() {
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClientV2();

    Bundle claimResult =
        fhirClient
            .search()
            .forResource(Claim.class)
            .where(
                ImmutableMap.of(
                    "mbi", Collections.singletonList(new ReferenceParam("a7f8e93f09")),
                    "service-date",
                        Arrays.asList(
                            new DateParam("gt1970-07-18"), new DateParam("lt1970-07-30"))))
            .returnBundle(Bundle.class)
            .execute();

    // Created date changes every run, replace it
    claimResult
        .getEntry()
        .forEach(
            e -> {
              Resource r = e.getResource();
              if (r instanceof Claim) {
                ((Claim) r).setCreated(TestResults.TEST_CREATED_DATE);
              }
            });

    // Sort entries for consistent testing results
    claimResult.getEntry().sort(Comparator.comparing(a -> a.getResource().getId()));

    // Set bundle ID for consistent testing results
    claimResult.setId(TestResults.TEST_BUNDLE_ID);

    // Set bundle lastUpdated for consistent testing results
    claimResult.setMeta(new Meta().setLastUpdated(TestResults.TEST_CREATED_DATE));

    String expected = TestResults.TEST_SEARCH;
    String actual = FhirContext.forR4().newJsonParser().encodeResourceToString(claimResult);

    // Change random port value for consistent testing
    actual = actual.replaceAll("localhost:\\d{4}", "localhost:6500");

    assertEquals(expected, actual);
  }

  private static List<PreAdjFissClaim> fissTestData() {
    return Arrays.asList(fissTestDataA(), fissTestDataB());
  }

  private static PreAdjFissClaim fissTestDataA() {
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

  private static PreAdjFissClaim fissTestDataB() {
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

  private static List<PreAdjMcsClaim> mcsTestData() {
    return Collections.singletonList(mcsTestDataA());
  }

  private static PreAdjMcsClaim mcsTestDataA() {
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

  private static void doTransaction(Consumer<EntityManager> transaction) {
    entityManager.getTransaction().begin();
    transaction.accept(entityManager);
    entityManager.getTransaction().commit();
  }

  private static EntityManager createEntityManager(DataSource dataSource) {
    final Map<String, Object> hibernateProperties =
        ImmutableMap.of(org.hibernate.cfg.AvailableSettings.DATASOURCE, dataSource);

    return Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME, hibernateProperties)
        .createEntityManager();
  }

  private static class TestResults {

    public static final Date TEST_CREATED_DATE = Date.from(Instant.ofEpochMilli(1000));
    public static final String TEST_BUNDLE_ID = "1856fb88-119e-46b6-a775-ab78ae0f61e9";

    public static final String TEST_FISS =
        "{\"resourceType\":\"Claim\",\"id\":\"f-123456\",\"meta\":{\"lastUpdated\":\"1970-01-01T00:00:00.000+00:00\"},\"contained\":[{\"resourceType\":\"Organization\",\"id\":\"provider-org\",\"meta\":{\"profile\":[\"http://hl7.org/fhir/us/carin-bb/StructureDefinition/C4BB-Organization\"]},\"identifier\":[{\"type\":{\"coding\":[{\"system\":\"http://terminology.hl7.org/CodeSystem/v2-0203\",\"code\":\"PRN\",\"display\":\"Provider number\"}]},\"value\":\"meda12345\"},{\"type\":{\"coding\":[{\"system\":\"http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBIdentifierType\",\"code\":\"npi\",\"display\":\"National Provider Identifier\"}]},\"system\":\"http://hl7.org/fhir/sid/us-npi\",\"value\":\"8876543211\"}]}],\"identifier\":[{\"type\":{\"coding\":[{\"system\":\"http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBIdentifierType\",\"code\":\"uc\",\"display\":\"Unique Claim ID\"}]},\"system\":\"https://dcgeo.cms.gov/resources/variables/dcn\",\"value\":\"123456\"}],\"status\":\"active\",\"type\":{\"coding\":[{\"system\":\"https://dcgeo.cms.gov/resources/codesystem/rda-type\",\"code\":\"FISS\"},{\"system\":\"http://terminology.hl7.org/CodeSystem/claim-type\",\"code\":\"institutional\",\"display\":\"Institutional\"}]},\"use\":\"claim\",\"patient\":{\"identifier\":{\"type\":{\"coding\":[{\"system\":\"http://terminology.hl7.org/CodeSystem/v2-0203\",\"code\":\"MC\",\"display\":\"Patient's Medicare number\"}]},\"system\":\"http://hl7.org/fhir/sid/us-mbi\",\"value\":\"123456MBI\"}},\"created\":\"1970-01-01T00:00:01+00:00\",\"provider\":{\"reference\":\"#provider-org\"},\"priority\":{\"coding\":[{\"system\":\"http://terminology.hl7.org/CodeSystem/processpriority\",\"code\":\"normal\",\"display\":\"Normal\"}]},\"diagnosis\":[{\"sequence\":1,\"diagnosisCodeableConcept\":{\"coding\":[{\"system\":\"http://hl7.org/fhir/sid/icd-10\",\"code\":\"princcd\"}]},\"type\":[{\"coding\":[{\"system\":\"http://terminology.hl7.org/CodeSystem/ex-diagnosistype\",\"code\":\"principal\",\"display\":\"Principal Diagnosis\"}]}]},{\"sequence\":2,\"diagnosisCodeableConcept\":{\"coding\":[{\"system\":\"http://hl7.org/fhir/sid/icd-10\",\"code\":\"admitcd\"}]},\"type\":[{\"coding\":[{\"system\":\"http://terminology.hl7.org/CodeSystem/ex-diagnosistype\",\"code\":\"admitting\",\"display\":\"Admitting Diagnosis\"}]}]}],\"procedure\":[{\"sequence\":1,\"date\":\"1970-07-20T00:00:00+00:00\",\"procedureCodeableConcept\":{\"coding\":[{\"system\":\"http://hl7.org/fhir/sid/icd-10\",\"code\":\"CODEABC\"}]}}],\"total\":{\"value\":1234.32,\"currency\":\"USD\"}}";

    public static final String TEST_MCS =
        "{\"resourceType\":\"Claim\",\"id\":\"m-654321\",\"meta\":{\"lastUpdated\":\"1970-01-01T00:00:04.000+00:00\"},\"contained\":[{\"resourceType\":\"Organization\",\"id\":\"provider-org\",\"meta\":{\"profile\":[\"http://hl7.org/fhir/us/carin-bb/StructureDefinition/C4BB-Organization\"]},\"identifier\":[{\"type\":{\"coding\":[{\"system\":\"http://terminology.hl7.org/CodeSystem/v2-0203\",\"code\":\"PRN\",\"display\":\"Provider number\"}]},\"value\":\"4444422222\"},{\"type\":{\"coding\":[{\"system\":\"http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBIdentifierType\",\"code\":\"npi\",\"display\":\"National Provider Identifier\"}]},\"system\":\"http://hl7.org/fhir/sid/us-npi\",\"value\":\"9876789102\"}]}],\"identifier\":[{\"type\":{\"coding\":[{\"system\":\"http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBIdentifierType\",\"code\":\"uc\",\"display\":\"Unique Claim ID\"}]},\"system\":\"https://dcgeo.cms.gov/resources/variables/icn\",\"value\":\"654321\"}],\"status\":\"active\",\"type\":{\"coding\":[{\"system\":\"https://dcgeo.cms.gov/resources/codesystem/rda-type\",\"code\":\"MCS\"},{\"system\":\"http://terminology.hl7.org/CodeSystem/claim-type\",\"code\":\"professional\",\"display\":\"Professional\"}]},\"use\":\"claim\",\"patient\":{\"identifier\":{\"type\":{\"coding\":[{\"system\":\"http://terminology.hl7.org/CodeSystem/v2-0203\",\"code\":\"MC\",\"display\":\"Patient's Medicare number\"}]},\"system\":\"http://hl7.org/fhir/sid/us-mbi\",\"value\":\"123456MBI\"}},\"created\":\"1970-01-01T00:00:01+00:00\",\"provider\":{\"reference\":\"#provider-org\"},\"priority\":{\"coding\":[{\"system\":\"http://terminology.hl7.org/CodeSystem/processpriority\",\"code\":\"normal\",\"display\":\"Normal\"}]},\"diagnosis\":[{\"sequence\":1,\"diagnosisCodeableConcept\":{\"coding\":[{\"system\":\"http://hl7.org/fhir/sid/icd-10\",\"code\":\"HF3IJIF\"}]}},{\"sequence\":2,\"diagnosisCodeableConcept\":{\"coding\":[{\"system\":\"http://hl7.org/fhir/sid/icd-9\",\"code\":\"HF3IJIG\"}]}}],\"procedure\":[{\"sequence\":1,\"date\":\"1970-07-28T00:00:00+00:00\",\"procedureCodeableConcept\":{\"coding\":[{\"system\":\"http://www.ama-assn.org/go/cpt\",\"code\":\"FDSAE\"}]}}],\"total\":{\"value\":23.00,\"currency\":\"USD\"}}";

    // TODO: This will get updated when DCGEO-117 comes in
    public static final String TEST_SEARCH =
        "{\"resourceType\":\"Bundle\",\"id\":\"1856fb88-119e-46b6-a775-ab78ae0f61e9\",\"meta\":{\"lastUpdated\":\"1970-01-01T00:00:01.000+00:00\"},\"type\":\"searchset\",\"link\":[{\"relation\":\"self\",\"url\":\"https://localhost:6500/v2/fhir/Claim?mbi=a7f8e93f09&service-date=gt1970-07-18&service-date=lt1970-07-30\"}],\"entry\":[{\"resource\":{\"resourceType\":\"Claim\",\"id\":\"f-123456\",\"meta\":{\"lastUpdated\":\"1970-01-01T00:00:00.000+00:00\"},\"contained\":[{\"resourceType\":\"Organization\",\"id\":\"provider-org\",\"meta\":{\"profile\":[\"http://hl7.org/fhir/us/carin-bb/StructureDefinition/C4BB-Organization\"]},\"identifier\":[{\"type\":{\"coding\":[{\"system\":\"http://terminology.hl7.org/CodeSystem/v2-0203\",\"code\":\"PRN\",\"display\":\"Provider number\"}]},\"value\":\"meda12345\"},{\"type\":{\"coding\":[{\"system\":\"http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBIdentifierType\",\"code\":\"npi\",\"display\":\"National Provider Identifier\"}]},\"system\":\"http://hl7.org/fhir/sid/us-npi\",\"value\":\"8876543211\"}]}],\"identifier\":[{\"type\":{\"coding\":[{\"system\":\"http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBIdentifierType\",\"code\":\"uc\",\"display\":\"Unique Claim ID\"}]},\"system\":\"https://dcgeo.cms.gov/resources/variables/dcn\",\"value\":\"123456\"}],\"status\":\"active\",\"type\":{\"coding\":[{\"system\":\"https://dcgeo.cms.gov/resources/codesystem/rda-type\",\"code\":\"FISS\"},{\"system\":\"http://terminology.hl7.org/CodeSystem/claim-type\",\"code\":\"institutional\",\"display\":\"Institutional\"}]},\"use\":\"claim\",\"patient\":{\"identifier\":{\"type\":{\"coding\":[{\"system\":\"http://terminology.hl7.org/CodeSystem/v2-0203\",\"code\":\"MC\",\"display\":\"Patient's Medicare number\"}]},\"system\":\"http://hl7.org/fhir/sid/us-mbi\",\"value\":\"123456MBI\"}},\"created\":\"1970-01-01T00:00:01+00:00\",\"provider\":{\"reference\":\"#provider-org\"},\"priority\":{\"coding\":[{\"system\":\"http://terminology.hl7.org/CodeSystem/processpriority\",\"code\":\"normal\",\"display\":\"Normal\"}]},\"diagnosis\":[{\"sequence\":1,\"diagnosisCodeableConcept\":{\"coding\":[{\"system\":\"http://hl7.org/fhir/sid/icd-10\",\"code\":\"princcd\"}]},\"type\":[{\"coding\":[{\"system\":\"http://terminology.hl7.org/CodeSystem/ex-diagnosistype\",\"code\":\"principal\",\"display\":\"Principal Diagnosis\"}]}]},{\"sequence\":2,\"diagnosisCodeableConcept\":{\"coding\":[{\"system\":\"http://hl7.org/fhir/sid/icd-10\",\"code\":\"admitcd\"}]},\"type\":[{\"coding\":[{\"system\":\"http://terminology.hl7.org/CodeSystem/ex-diagnosistype\",\"code\":\"admitting\",\"display\":\"Admitting Diagnosis\"}]}]}],\"procedure\":[{\"sequence\":1,\"date\":\"1970-07-20T00:00:00+00:00\",\"procedureCodeableConcept\":{\"coding\":[{\"system\":\"http://hl7.org/fhir/sid/icd-10\",\"code\":\"CODEABC\"}]}}],\"total\":{\"value\":1234.32,\"currency\":\"USD\"}}},{\"resource\":{\"resourceType\":\"Claim\",\"id\":\"f-123457\",\"meta\":{\"lastUpdated\":\"1970-01-01T00:00:05.000+00:00\"},\"contained\":[{\"resourceType\":\"Organization\",\"id\":\"provider-org\",\"meta\":{\"profile\":[\"http://hl7.org/fhir/us/carin-bb/StructureDefinition/C4BB-Organization\"]},\"identifier\":[{\"type\":{\"coding\":[{\"system\":\"http://terminology.hl7.org/CodeSystem/v2-0203\",\"code\":\"PRN\",\"display\":\"Provider number\"}]},\"value\":\"meda12346\"},{\"type\":{\"coding\":[{\"system\":\"http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBIdentifierType\",\"code\":\"npi\",\"display\":\"National Provider Identifier\"}]},\"system\":\"http://hl7.org/fhir/sid/us-npi\",\"value\":\"8876543212\"}]}],\"identifier\":[{\"type\":{\"coding\":[{\"system\":\"http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBIdentifierType\",\"code\":\"uc\",\"display\":\"Unique Claim ID\"}]},\"system\":\"https://dcgeo.cms.gov/resources/variables/dcn\",\"value\":\"123457\"}],\"status\":\"active\",\"type\":{\"coding\":[{\"system\":\"https://dcgeo.cms.gov/resources/codesystem/rda-type\",\"code\":\"FISS\"},{\"system\":\"http://terminology.hl7.org/CodeSystem/claim-type\",\"code\":\"institutional\",\"display\":\"Institutional\"}]},\"use\":\"claim\",\"patient\":{\"identifier\":{\"type\":{\"coding\":[{\"system\":\"http://terminology.hl7.org/CodeSystem/v2-0203\",\"code\":\"MC\",\"display\":\"Patient's Medicare number\"}]},\"system\":\"http://hl7.org/fhir/sid/us-mbi\",\"value\":\"123456MBI\"}},\"created\":\"1970-01-01T00:00:01+00:00\",\"provider\":{\"reference\":\"#provider-org\"},\"priority\":{\"coding\":[{\"system\":\"http://terminology.hl7.org/CodeSystem/processpriority\",\"code\":\"normal\",\"display\":\"Normal\"}]},\"diagnosis\":[{\"sequence\":1,\"diagnosisCodeableConcept\":{\"coding\":[{\"system\":\"http://hl7.org/fhir/sid/icd-10\",\"code\":\"princcc\"}]},\"type\":[{\"coding\":[{\"system\":\"http://terminology.hl7.org/CodeSystem/ex-diagnosistype\",\"code\":\"principal\",\"display\":\"Principal Diagnosis\"}]}]},{\"sequence\":2,\"diagnosisCodeableConcept\":{\"coding\":[{\"system\":\"http://hl7.org/fhir/sid/icd-10\",\"code\":\"admitcc\"}]},\"type\":[{\"coding\":[{\"system\":\"http://terminology.hl7.org/CodeSystem/ex-diagnosistype\",\"code\":\"admitting\",\"display\":\"Admitting Diagnosis\"}]}]}],\"procedure\":[{\"sequence\":1,\"date\":\"1970-07-31T00:00:00+00:00\",\"procedureCodeableConcept\":{\"coding\":[{\"system\":\"http://hl7.org/fhir/sid/icd-10\",\"code\":\"CODEABD\"}]}}],\"total\":{\"value\":1235.32,\"currency\":\"USD\"}}},{\"resource\":{\"resourceType\":\"Claim\",\"id\":\"m-654321\",\"meta\":{\"lastUpdated\":\"1970-01-01T00:00:04.000+00:00\"},\"contained\":[{\"resourceType\":\"Organization\",\"id\":\"provider-org\",\"meta\":{\"profile\":[\"http://hl7.org/fhir/us/carin-bb/StructureDefinition/C4BB-Organization\"]},\"identifier\":[{\"type\":{\"coding\":[{\"system\":\"http://terminology.hl7.org/CodeSystem/v2-0203\",\"code\":\"PRN\",\"display\":\"Provider number\"}]},\"value\":\"4444422222\"},{\"type\":{\"coding\":[{\"system\":\"http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBIdentifierType\",\"code\":\"npi\",\"display\":\"National Provider Identifier\"}]},\"system\":\"http://hl7.org/fhir/sid/us-npi\",\"value\":\"9876789102\"}]}],\"identifier\":[{\"type\":{\"coding\":[{\"system\":\"http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBIdentifierType\",\"code\":\"uc\",\"display\":\"Unique Claim ID\"}]},\"system\":\"https://dcgeo.cms.gov/resources/variables/icn\",\"value\":\"654321\"}],\"status\":\"active\",\"type\":{\"coding\":[{\"system\":\"https://dcgeo.cms.gov/resources/codesystem/rda-type\",\"code\":\"MCS\"},{\"system\":\"http://terminology.hl7.org/CodeSystem/claim-type\",\"code\":\"professional\",\"display\":\"Professional\"}]},\"use\":\"claim\",\"patient\":{\"identifier\":{\"type\":{\"coding\":[{\"system\":\"http://terminology.hl7.org/CodeSystem/v2-0203\",\"code\":\"MC\",\"display\":\"Patient's Medicare number\"}]},\"system\":\"http://hl7.org/fhir/sid/us-mbi\",\"value\":\"123456MBI\"}},\"created\":\"1970-01-01T00:00:01+00:00\",\"provider\":{\"reference\":\"#provider-org\"},\"priority\":{\"coding\":[{\"system\":\"http://terminology.hl7.org/CodeSystem/processpriority\",\"code\":\"normal\",\"display\":\"Normal\"}]},\"diagnosis\":[{\"sequence\":1,\"diagnosisCodeableConcept\":{\"coding\":[{\"system\":\"http://hl7.org/fhir/sid/icd-10\",\"code\":\"HF3IJIF\"}]}},{\"sequence\":2,\"diagnosisCodeableConcept\":{\"coding\":[{\"system\":\"http://hl7.org/fhir/sid/icd-9\",\"code\":\"HF3IJIG\"}]}}],\"procedure\":[{\"sequence\":1,\"date\":\"1970-07-28T00:00:00+00:00\",\"procedureCodeableConcept\":{\"coding\":[{\"system\":\"http://www.ama-assn.org/go/cpt\",\"code\":\"FDSAE\"}]}}],\"total\":{\"value\":23.00,\"currency\":\"USD\"}}}]}";
  }
}
