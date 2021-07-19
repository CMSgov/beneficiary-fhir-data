package gov.cms.bfd.server.war.r4.providers.preadj;

import static org.junit.Assert.assertEquals;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.google.common.collect.ImmutableMap;
import gov.cms.bfd.model.rda.PreAdjFissClaim;
import gov.cms.bfd.model.rda.PreAdjFissProcCode;
import gov.cms.bfd.model.rif.schema.DatabaseSchemaManager;
import gov.cms.bfd.model.rif.schema.DatabaseTestUtils;
import gov.cms.bfd.server.war.ServerTestUtils;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.function.Consumer;
import javax.persistence.EntityManager;
import javax.persistence.Persistence;
import javax.sql.DataSource;
import org.hl7.fhir.r4.model.Claim;
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
  }

  @AfterClass
  public static void tearDown() {
    doTransaction(
        em -> {
          em.createQuery("delete from PreAdjFissProcCode f").executeUpdate();
          em.createQuery("delete from PreAdjFissClaim f").executeUpdate();
        });
  }

  @Test
  public void shouldGetCorrectResource() {
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

    PreAdjFissProcCode code = new PreAdjFissProcCode();
    code.setDcn("123456");
    code.setPriority((short) 0);
    code.setProcCode("CODEABC");
    code.setProcFlag("FLAG");
    code.setProcDate(LocalDate.ofEpochDay(200));
    code.setLastUpdated(Instant.ofEpochMilli(0));

    claim.setProcCodes(Collections.singleton(code));

    doTransaction((em) -> em.persist(claim));

    IGenericClient fhirClient = ServerTestUtils.get().createFhirClientV2();

    Claim claimResult = fhirClient.read().resource(Claim.class).withId("f-123456").execute();

    claimResult.setCreated(TestResults.TEST_CREATED_DATE);

    String expected = TestResults.TEST_A;
    String actual = FhirContext.forR4().newJsonParser().encodeResourceToString(claimResult);

    assertEquals(expected, actual);
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

    public static final String TEST_A =
        "{\"resourceType\":\"Claim\",\"id\":\"f-123456\",\"meta\":{\"lastUpdated\":\"1970-01-01T00:00:00.000+00:00\"},\"contained\":[{\"resourceType\":\"Organization\",\"id\":\"provider-org\",\"meta\":{\"profile\":[\"http://hl7.org/fhir/us/carin-bb/StructureDefinition/C4BB-Organization\"]},\"identifier\":[{\"type\":{\"coding\":[{\"system\":\"http://terminology.hl7.org/CodeSystem/v2-0203\",\"code\":\"PRN\",\"display\":\"Provider number\"}]},\"value\":\"meda12345\"},{\"type\":{\"coding\":[{\"system\":\"http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBIdentifierType\",\"code\":\"npi\",\"display\":\"National Provider Identifier\"}]},\"system\":\"http://hl7.org/fhir/sid/us-npi\",\"value\":\"8876543211\"}]}],\"identifier\":[{\"type\":{\"coding\":[{\"system\":\"http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBIdentifierType\",\"code\":\"uc\",\"display\":\"Unique Claim ID\"}]},\"system\":\"https://dcgeo.cms.gov/resources/variables/dcn\",\"value\":\"123456\"}],\"status\":\"active\",\"type\":{\"coding\":[{\"system\":\"https://dcgeo.cms.gov/resources/codesystem/rda-type\",\"code\":\"FISS\"},{\"system\":\"http://terminology.hl7.org/CodeSystem/claim-type\",\"code\":\"institutional\",\"display\":\"Institutional\"}]},\"use\":\"claim\",\"patient\":{\"identifier\":{\"type\":{\"coding\":[{\"system\":\"http://terminology.hl7.org/CodeSystem/v2-0203\",\"code\":\"MC\",\"display\":\"Patient's Medicare number\"}]},\"system\":\"http://hl7.org/fhir/sid/us-mbi\",\"value\":\"123456MBI\"}},\"created\":\"1970-01-01T00:00:01+00:00\",\"provider\":{\"reference\":\"#provider-org\"},\"priority\":{\"coding\":[{\"system\":\"http://terminology.hl7.org/CodeSystem/processpriority\",\"code\":\"normal\",\"display\":\"Normal\"}]},\"diagnosis\":[{\"sequence\":1,\"diagnosisCodeableConcept\":{\"coding\":[{\"system\":\"http://hl7.org/fhir/sid/icd-10\",\"code\":\"princcd\"}]},\"type\":[{\"coding\":[{\"system\":\"http://terminology.hl7.org/CodeSystem/ex-diagnosistype\",\"code\":\"principal\",\"display\":\"Principal Diagnosis\"}]}]},{\"sequence\":2,\"diagnosisCodeableConcept\":{\"coding\":[{\"system\":\"http://hl7.org/fhir/sid/icd-10\",\"code\":\"admitcd\"}]},\"type\":[{\"coding\":[{\"system\":\"http://terminology.hl7.org/CodeSystem/ex-diagnosistype\",\"code\":\"admitting\",\"display\":\"Admitting Diagnosis\"}]}]}],\"procedure\":[{\"sequence\":1,\"date\":\"1970-07-20T00:00:00+00:00\",\"procedureCodeableConcept\":{\"coding\":[{\"system\":\"http://hl7.org/fhir/sid/icd-10\",\"code\":\"CODEABC\"}]}}],\"total\":{\"value\":1234.32,\"currency\":\"USD\"}}";
  }
}
