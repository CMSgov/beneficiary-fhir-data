package gov.cms.bfd.server.ng;

import static org.junit.jupiter.api.Assertions.assertTrue;

import au.com.origin.snapshots.Expect;
import au.com.origin.snapshots.junit5.SnapshotExtension;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ch.qos.logback.classic.spi.ILoggingEvent;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Coverage;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;

@Import(IntegrationTestConfiguration.class)
@ExtendWith({SnapshotExtension.class})
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class IntegrationTestBase {
  @LocalServerPort protected int port;
  protected Expect expect;
  @Autowired protected EntityManager entityManager;

  protected static final String HISTORICAL_MERGED_BENE_SK = "848484848";
  protected static final String HISTORICAL_MERGED_BENE_SK2 = "121212121";
  protected static final String CURRENT_MERGED_BENE_SK = "517782585";
  protected static final String HISTORICAL_MERGED_BENE_SK_KILL_CREDIT = "232323232";
  protected static final String HISTORICAL_MERGED_BENE_SK_MULTIPLE_HISTORICAL_MBIS = "617782589";
  protected static final String HISTORICAL_MERGED_MBI_KILL_CREDIT = "2B19C89AA37";
  protected static final String HISTORICAL_MERGED_MBI = "2B19C89AA36";
  protected static final String HISTORICAL_AND_CURRENT_MBI = "2B19C89AA35";
  protected static final String OVERSHARE_MBI = "2B19C89AA38";
  protected static final String OVERSHARE_BENE_SK1 = "617782586";
  protected static final String OVERSHARE_BENE_SK2 = "617782587";

  protected static final String BENE_ID_PART_A_ONLY = "178083966";
  protected static final String BENE_ID_PART_B_ONLY = "365359727";
  protected static final String BENE_ID_PART_C_ONLY = "441149422";
  protected static final String BENE_ID_PART_D_ONLY = "547437476";
  protected static final String BENE_ID_PART_C_AND_D_ONLY_SAME_PROGRAM_TYPE_CODE = "353816020";
  protected static final String BENE_ID_PART_C_AND_D_ONLY_DIFF_PROGRAM_TYPE_CODE = "319785798";
  protected static final String BENE_ID_PART_C_AND_D_ONLY_DIFF_PROGRAM_TYPE_CODE_DIFF_ACTIVE_DATES =
      "657388415";
  protected static final String BENE_ID_DUAL_ONLY = "184043356";
  protected static final String BENE_ID_DUAL_ONLY_EXPIRED = "184043357";
  protected static final String BENE_ID_ALL_PARTS_WITH_XREF = "405764107";
  protected static final String BENE_ID_NO_TP = "451482106";
  protected static final String BENE_ID_EXPIRED_COVERAGE = "421056595";
  protected static final String BENE_ID_FUTURE_COVERAGE = "971050241";
  protected static final String BENE_ID_NON_CURRENT = "181968400";
  protected static final String BENE_ID_NO_COVERAGE = "289169129";

  protected static final String CLAIM_ID_ADJUDICATED_ICD_9 = "1071939711294";
  protected static final String CLAIM_ID_ADJUDICATED = "1071939711295";
  protected static final String CLAIM_ID_PHASE_1 = "1071939711297";
  protected static final String CLAIM_ID_PHASE_2 = "1071939711298";
  protected static final String CLAIM_ID_RX = "5213363418309";
  protected static final String CLAIM_ID_PROFESSIONAL = "3351266481401";
  protected static final String CLAIM_ID_PROFESSIONAL_ORG = "3351266481402";
  protected static final String CLAIM_ID_PROFESSIONAL_MCS = "3351266481403";
  protected static final String CLAIM_ID_RX_ORGANIZATION = "1409088853940";
  protected static final String CLM_CNTL_NUM_DUPE = "31646182683546TDF";

  protected static final String DUAL_ONLY_BENE_COVERAGE_STATUS_CODE = "XX";

  private static final String FHIR_JSON = "fhir+json";

  protected String getServerBaseUrl() {
    return "http://localhost:" + port;
  }

  protected String getServerUrl() {
    return getServerBaseUrl() + "/v3/fhir";
  }

  protected IGenericClient getFhirClient() {
    FhirContext ctx = FhirContext.forR4Cached();
    return ctx.newRestfulGenericClient(getServerUrl());
  }

  protected Expect expectFhir() {
    return expect.serializer(FHIR_JSON);
  }

  protected List<Patient> getPatientsFromBundle(Bundle patientBundle) {
    return patientBundle.getEntry().stream()
        .map(Bundle.BundleEntryComponent::getResource)
        // We don't check the type here because the types should be homogeneous
        .map(Patient.class::cast)
        .toList();
  }

  protected List<Coverage> getCoverageFromBundle(Bundle coverageBundle) {
    return coverageBundle.getEntry().stream()
        .map(Bundle.BundleEntryComponent::getResource)
        .map(Coverage.class::cast)
        .toList();
  }

  protected List<ExplanationOfBenefit> getEobFromBundle(Bundle eobBundle) {
    return eobBundle.getEntry().stream()
        .map(Bundle.BundleEntryComponent::getResource)
        .map(ExplanationOfBenefit.class::cast)
        .toList();
  }

  protected List<Extension> getExtensionByUrl(DomainResource resource, String url) {
    return resource.getExtension().stream().filter(e -> e.getUrl().equals(url)).toList();
  }

  protected long queryCount(List<ILoggingEvent> events) {
    // SQL queries are logged under the org.hibernate.SQL logger
    return events.stream().filter(l -> l.getLoggerName().equals("org.hibernate.SQL")).count();
  }

  protected void validateCodings(IBaseResource resource) {
    var ctx = FhirContext.forR4Cached();
    var codings = ctx.newTerser().getAllPopulatedChildElementsOfType(resource, Coding.class);
    for (Coding coding : codings) {
      assertTrue(coding.hasSystem());
      assertTrue(coding.hasCode());
    }
  }
}
