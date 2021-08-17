package gov.cms.bfd.server.war.r4.providers.preadj;

import static org.junit.Assert.assertEquals;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import com.google.common.collect.ImmutableMap;
import gov.cms.bfd.model.rda.PreAdjFissClaim;
import gov.cms.bfd.model.rda.PreAdjFissProcCode;
import gov.cms.bfd.model.rda.PreAdjMcsClaim;
import gov.cms.bfd.model.rda.PreAdjMcsDetail;
import gov.cms.bfd.model.rda.PreAdjMcsDiagnosisCode;
import gov.cms.bfd.server.war.ServerTestUtils;
import gov.cms.bfd.server.war.utils.RDATestUtils;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Claim;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Resource;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class R4ClaimResourceProviderIT {

  private static final RDATestUtils testUtils = new RDATestUtils();

  @BeforeClass
  public static void init() {
    testUtils.init();

    testUtils.seedData(testUtils.fissTestData());
    testUtils.seedData(testUtils.mcsTestData());
  }

  @AfterClass
  public static void tearDown() {
    testUtils.truncate(PreAdjFissProcCode.class);
    testUtils.truncate(PreAdjFissClaim.class);
    testUtils.truncate(PreAdjMcsDetail.class);
    testUtils.truncate(PreAdjMcsDiagnosisCode.class);
    testUtils.truncate(PreAdjMcsClaim.class);
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

  private static class TestResults {

    public static final Date TEST_CREATED_DATE = Date.from(Instant.ofEpochMilli(1000));
    public static final String TEST_BUNDLE_ID = "1856fb88-119e-46b6-a775-ab78ae0f61e9";

    public static final String TEST_FISS =
        "{\"resourceType\":\"Claim\",\"id\":\"f-123456\",\"meta\":{\"lastUpdated\":\"1970-01-01T00:00:00.000+00:00\"},\"contained\":[{\"resourceType\":\"Organization\",\"id\":\"provider-org\",\"meta\":{\"profile\":[\"http://hl7.org/fhir/us/carin-bb/StructureDefinition/C4BB-Organization\"]},\"identifier\":[{\"type\":{\"coding\":[{\"system\":\"http://terminology.hl7.org/CodeSystem/v2-0203\",\"code\":\"PRN\",\"display\":\"Provider number\"}]},\"value\":\"meda12345\"},{\"type\":{\"coding\":[{\"system\":\"http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBIdentifierType\",\"code\":\"npi\",\"display\":\"National Provider Identifier\"}]},\"system\":\"http://hl7.org/fhir/sid/us-npi\",\"value\":\"8876543211\"}]}],\"identifier\":[{\"type\":{\"coding\":[{\"system\":\"http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBIdentifierType\",\"code\":\"uc\",\"display\":\"Unique Claim ID\"}]},\"system\":\"https://dcgeo.cms.gov/resources/variables/dcn\",\"value\":\"123456\"}],\"status\":\"active\",\"type\":{\"coding\":[{\"system\":\"https://dcgeo.cms.gov/resources/codesystem/rda-type\",\"code\":\"FISS\"},{\"system\":\"http://terminology.hl7.org/CodeSystem/claim-type\",\"code\":\"institutional\",\"display\":\"Institutional\"}]},\"use\":\"claim\",\"patient\":{\"identifier\":{\"type\":{\"coding\":[{\"system\":\"http://terminology.hl7.org/CodeSystem/v2-0203\",\"code\":\"MC\",\"display\":\"Patient's Medicare number\"}]},\"system\":\"http://hl7.org/fhir/sid/us-mbi\",\"value\":\"123456MBI\"}},\"created\":\"1970-01-01T00:00:01+00:00\",\"provider\":{\"reference\":\"#provider-org\"},\"priority\":{\"coding\":[{\"system\":\"http://terminology.hl7.org/CodeSystem/processpriority\",\"code\":\"normal\",\"display\":\"Normal\"}]},\"diagnosis\":[{\"sequence\":1,\"diagnosisCodeableConcept\":{\"coding\":[{\"system\":\"http://hl7.org/fhir/sid/icd-10\",\"code\":\"princcd\"}]},\"type\":[{\"coding\":[{\"system\":\"http://terminology.hl7.org/CodeSystem/ex-diagnosistype\",\"code\":\"principal\",\"display\":\"Principal Diagnosis\"}]}]},{\"sequence\":2,\"diagnosisCodeableConcept\":{\"coding\":[{\"system\":\"http://hl7.org/fhir/sid/icd-10\",\"code\":\"admitcd\"}]},\"type\":[{\"coding\":[{\"system\":\"http://terminology.hl7.org/CodeSystem/ex-diagnosistype\",\"code\":\"admitting\",\"display\":\"Admitting Diagnosis\"}]}]}],\"procedure\":[{\"sequence\":1,\"date\":\"1970-07-20T00:00:00+00:00\",\"procedureCodeableConcept\":{\"coding\":[{\"system\":\"http://hl7.org/fhir/sid/icd-10\",\"code\":\"CODEABC\"}]}}],\"total\":{\"value\":1234.32,\"currency\":\"USD\"}}";

    public static final String TEST_MCS =
        "{\"resourceType\":\"Claim\",\"id\":\"m-654321\",\"meta\":{\"lastUpdated\":\"1970-01-01T00:00:04.000+00:00\"},\"contained\":[{\"resourceType\":\"Organization\",\"id\":\"provider-org\",\"meta\":{\"profile\":[\"http://hl7.org/fhir/us/carin-bb/StructureDefinition/C4BB-Organization\"]},\"identifier\":[{\"type\":{\"coding\":[{\"system\":\"http://terminology.hl7.org/CodeSystem/v2-0203\",\"code\":\"PRN\",\"display\":\"Provider number\"}]},\"value\":\"4444422222\"},{\"type\":{\"coding\":[{\"system\":\"http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBIdentifierType\",\"code\":\"npi\",\"display\":\"National Provider Identifier\"}]},\"system\":\"http://hl7.org/fhir/sid/us-npi\",\"value\":\"9876789102\"}]}],\"identifier\":[{\"type\":{\"coding\":[{\"system\":\"http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBIdentifierType\",\"code\":\"uc\",\"display\":\"Unique Claim ID\"}]},\"system\":\"https://dcgeo.cms.gov/resources/variables/icn\",\"value\":\"654321\"}],\"status\":\"active\",\"type\":{\"coding\":[{\"system\":\"https://dcgeo.cms.gov/resources/codesystem/rda-type\",\"code\":\"MCS\"},{\"system\":\"http://terminology.hl7.org/CodeSystem/claim-type\",\"code\":\"professional\",\"display\":\"Professional\"}]},\"use\":\"claim\",\"patient\":{\"identifier\":{\"type\":{\"coding\":[{\"system\":\"http://terminology.hl7.org/CodeSystem/v2-0203\",\"code\":\"MC\",\"display\":\"Patient's Medicare number\"}]},\"system\":\"http://hl7.org/fhir/sid/us-mbi\",\"value\":\"123456MBI\"}},\"created\":\"1970-01-01T00:00:01+00:00\",\"provider\":{\"reference\":\"#provider-org\"},\"priority\":{\"coding\":[{\"system\":\"http://terminology.hl7.org/CodeSystem/processpriority\",\"code\":\"normal\",\"display\":\"Normal\"}]},\"diagnosis\":[{\"sequence\":1,\"diagnosisCodeableConcept\":{\"coding\":[{\"system\":\"http://hl7.org/fhir/sid/icd-10\",\"code\":\"HF3IJIF\"}]}},{\"sequence\":2,\"diagnosisCodeableConcept\":{\"coding\":[{\"system\":\"http://hl7.org/fhir/sid/icd-9\",\"code\":\"HF3IJIG\"}]}}],\"procedure\":[{\"sequence\":1,\"date\":\"1970-07-28T00:00:00+00:00\",\"procedureCodeableConcept\":{\"coding\":[{\"system\":\"http://www.ama-assn.org/go/cpt\",\"code\":\"FDSAE\"}]}}],\"total\":{\"value\":23.00,\"currency\":\"USD\"}}";

    // TODO: This will get updated when DCGEO-117 comes in
    public static final String TEST_SEARCH =
        "{\"resourceType\":\"Bundle\",\"id\":\"1856fb88-119e-46b6-a775-ab78ae0f61e9\",\"meta\":{\"lastUpdated\":\"1970-01-01T00:00:01.000+00:00\"},\"type\":\"searchset\",\"link\":[{\"relation\":\"self\",\"url\":\"https://localhost:6500/v2/fhir/Claim?mbi=a7f8e93f09&service-date=gt1970-07-18&service-date=lt1970-07-30\"}],\"entry\":[{\"resource\":{\"resourceType\":\"Claim\",\"id\":\"f-123456\",\"meta\":{\"lastUpdated\":\"1970-01-01T00:00:00.000+00:00\"},\"contained\":[{\"resourceType\":\"Organization\",\"id\":\"provider-org\",\"meta\":{\"profile\":[\"http://hl7.org/fhir/us/carin-bb/StructureDefinition/C4BB-Organization\"]},\"identifier\":[{\"type\":{\"coding\":[{\"system\":\"http://terminology.hl7.org/CodeSystem/v2-0203\",\"code\":\"PRN\",\"display\":\"Provider number\"}]},\"value\":\"meda12345\"},{\"type\":{\"coding\":[{\"system\":\"http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBIdentifierType\",\"code\":\"npi\",\"display\":\"National Provider Identifier\"}]},\"system\":\"http://hl7.org/fhir/sid/us-npi\",\"value\":\"8876543211\"}]}],\"identifier\":[{\"type\":{\"coding\":[{\"system\":\"http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBIdentifierType\",\"code\":\"uc\",\"display\":\"Unique Claim ID\"}]},\"system\":\"https://dcgeo.cms.gov/resources/variables/dcn\",\"value\":\"123456\"}],\"status\":\"active\",\"type\":{\"coding\":[{\"system\":\"https://dcgeo.cms.gov/resources/codesystem/rda-type\",\"code\":\"FISS\"},{\"system\":\"http://terminology.hl7.org/CodeSystem/claim-type\",\"code\":\"institutional\",\"display\":\"Institutional\"}]},\"use\":\"claim\",\"patient\":{\"identifier\":{\"type\":{\"coding\":[{\"system\":\"http://terminology.hl7.org/CodeSystem/v2-0203\",\"code\":\"MC\",\"display\":\"Patient's Medicare number\"}]},\"system\":\"http://hl7.org/fhir/sid/us-mbi\",\"value\":\"123456MBI\"}},\"created\":\"1970-01-01T00:00:01+00:00\",\"provider\":{\"reference\":\"#provider-org\"},\"priority\":{\"coding\":[{\"system\":\"http://terminology.hl7.org/CodeSystem/processpriority\",\"code\":\"normal\",\"display\":\"Normal\"}]},\"diagnosis\":[{\"sequence\":1,\"diagnosisCodeableConcept\":{\"coding\":[{\"system\":\"http://hl7.org/fhir/sid/icd-10\",\"code\":\"princcd\"}]},\"type\":[{\"coding\":[{\"system\":\"http://terminology.hl7.org/CodeSystem/ex-diagnosistype\",\"code\":\"principal\",\"display\":\"Principal Diagnosis\"}]}]},{\"sequence\":2,\"diagnosisCodeableConcept\":{\"coding\":[{\"system\":\"http://hl7.org/fhir/sid/icd-10\",\"code\":\"admitcd\"}]},\"type\":[{\"coding\":[{\"system\":\"http://terminology.hl7.org/CodeSystem/ex-diagnosistype\",\"code\":\"admitting\",\"display\":\"Admitting Diagnosis\"}]}]}],\"procedure\":[{\"sequence\":1,\"date\":\"1970-07-20T00:00:00+00:00\",\"procedureCodeableConcept\":{\"coding\":[{\"system\":\"http://hl7.org/fhir/sid/icd-10\",\"code\":\"CODEABC\"}]}}],\"total\":{\"value\":1234.32,\"currency\":\"USD\"}}}]}";
  }
}
