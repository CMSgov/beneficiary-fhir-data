package gov.cms.bfd.server.war.r4.providers;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

import ca.uhn.fhir.context.FhirContext;
import com.codahale.metrics.MetricRegistry;
import gov.cms.bfd.model.rif.Beneficiary;
import gov.cms.bfd.model.rif.BeneficiaryHistory;
import gov.cms.bfd.model.rif.MedicareBeneficiaryIdHistory;
import gov.cms.bfd.model.rif.samples.StaticRifResource;
import gov.cms.bfd.model.rif.samples.StaticRifResourceGroup;
import gov.cms.bfd.server.war.ServerTestUtils;
import gov.cms.bfd.server.war.commons.RequestHeaders;
import java.util.*;
import java.util.stream.Collectors;
import org.hamcrest.collection.IsEmptyCollection;
import org.hl7.fhir.r4.model.Patient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** Unit tests for {@link gov.cms.bfd.server.war.r4.providers.BeneficiaryTransformerV2}. */
public final class BeneficiaryTransformerV2Test {

  private static final FhirContext fhirContext = FhirContext.forR4();
  private static Beneficiary beneficiary = null;

  @Before
  public void setup() {
    List<Object> parsedRecords =
        ServerTestUtils.parseData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));

    // Pull out the base Beneficiary record and fix its HICN and MBI-HASH fields.
    beneficiary =
        parsedRecords.stream()
            .filter(r -> r instanceof Beneficiary)
            .map(r -> (Beneficiary) r)
            .findFirst()
            .get();

    beneficiary.setHicn("someHICNhash");
    beneficiary.setMbiHash(Optional.of("someMBIhash"));

    // Add the HICN history records to the Beneficiary, and fix their HICN fields.
    Set<BeneficiaryHistory> beneficiaryHistories =
        parsedRecords.stream()
            .filter(r -> r instanceof BeneficiaryHistory)
            .map(r -> (BeneficiaryHistory) r)
            .filter(r -> beneficiary.getBeneficiaryId().equals(r.getBeneficiaryId()))
            .collect(Collectors.toSet());

    beneficiary.getBeneficiaryHistories().addAll(beneficiaryHistories);
    for (BeneficiaryHistory beneficiaryHistory : beneficiary.getBeneficiaryHistories()) {
      beneficiaryHistory.setHicnUnhashed(Optional.of(beneficiaryHistory.getHicn()));
      beneficiaryHistory.setHicn("someHICNhash");
    }

    // Add the MBI history records to the Beneficiary.
    Set<MedicareBeneficiaryIdHistory> beneficiaryMbis =
        parsedRecords.stream()
            .filter(r -> r instanceof MedicareBeneficiaryIdHistory)
            .map(r -> (MedicareBeneficiaryIdHistory) r)
            .filter(r -> beneficiary.getBeneficiaryId().equals(r.getBeneficiaryId().orElse(null)))
            .collect(Collectors.toSet());
    beneficiary.getMedicareBeneficiaryIdHistories().addAll(beneficiaryMbis);
    assertThat(beneficiary, is(notNullValue()));
  }

  @After
  public void tearDown() {
    beneficiary = null;
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.r4.providers.BeneficiaryTransformerV2#transform(Beneficiary)} works as
   * expected when run against the {@link StaticRifResource#SAMPLE_A_BENES} {@link Beneficiary}.
   */
  @Test
  public void transformSampleARecord() {

    RequestHeaders requestHeader = getRHwithIncldIdentityHdr("true");

    Patient patient =
        BeneficiaryTransformerV2.transform(new MetricRegistry(), beneficiary, requestHeader);

    System.out.println(fhirContext.newJsonParser().encodeResourceToString(patient));
    assertThat(patient.getIdentifier(), not(IsEmptyCollection.empty()));

    // assertMatches(beneficiary, patient, requestHeader);
    // Assert.assertEquals("Number of identifiers should be 2", 2, patient.getIdentifier().size());

    // Verify identifiers and values match.
    /*
    assertValuesInPatientIdentifiers(
        patient,
        TransformerUtils.calculateVariableReferenceUrl(CcwCodebookVariable.BENE_ID),
        "567834");
    assertValuesInPatientIdentifiers(
        patient, TransformerConstants.CODING_BBAPI_BENE_MBI_HASH, "someMBIhash");
        */
  }

  /**
   * @return the {@link StaticRifResourceGroup#SAMPLE_A} {@link Beneficiary} record, with the {@link
   *     Beneficiary#getBeneficiaryHistories()} and {@link
   *     Beneficiary#getMedicareBeneficiaryIdHistories()} fields populated.
   */

  /**
   * test helper
   *
   * @param value of all include identifier values
   * @return RequestHeaders instance derived from value
   */
  public static RequestHeaders getRHwithIncldIdentityHdr(String value) {
    return RequestHeaders.getHeaderWrapper(
        R4PatientResourceProvider.HEADER_NAME_INCLUDE_IDENTIFIERS, value);
  }

  /**
   * test helper
   *
   * @param value of all include address fields values
   * @return RequestHeaders instance derived from value
   */
  public static RequestHeaders getRHwithIncldAddrFldHdr(String value) {
    return RequestHeaders.getHeaderWrapper(
        R4PatientResourceProvider.HEADER_NAME_INCLUDE_ADDRESS_FIELDS, value);
  }
}
