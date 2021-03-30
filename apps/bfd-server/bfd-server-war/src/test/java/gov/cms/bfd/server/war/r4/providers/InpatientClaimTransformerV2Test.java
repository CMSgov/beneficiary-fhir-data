package gov.cms.bfd.server.war.r4.providers;

import ca.uhn.fhir.context.FhirContext;
import com.codahale.metrics.MetricRegistry;
import gov.cms.bfd.model.rif.InpatientClaim;
import gov.cms.bfd.model.rif.samples.StaticRifResourceGroup;
import gov.cms.bfd.server.war.ServerTestUtils;
import gov.cms.bfd.server.war.commons.ProfileConstants;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.CareTeamComponent;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.ExplanationOfBenefitStatus;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.SupportingInformationComponent;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.Use;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Money;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public final class InpatientClaimTransformerV2Test {
  InpatientClaim claim;
  ExplanationOfBenefit eob;
  /**
   * Generates the Claim object to be used in multiple tests
   *
   * @return
   * @throws FHIRException
   */
  public InpatientClaim generateClaim() throws FHIRException {
    List<Object> parsedRecords =
        ServerTestUtils.parseData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));

    InpatientClaim claim =
        parsedRecords.stream()
            .filter(r -> r instanceof InpatientClaim)
            .map(r -> (InpatientClaim) r)
            .findFirst()
            .get();

    claim.setLastUpdated(new Date());

    return claim;
  }

  @Before
  public void before() {
    claim = generateClaim();
    eob = InpatientClaimTransformerV2.transform(new MetricRegistry(), claim);
  }

  private static final FhirContext fhirContext = FhirContext.forR4();

  /** Common top level EOB values */
  @Test
  public void shouldSetID() {
    Assert.assertEquals("inpatient-" + claim.getClaimId(), eob.getId());
  }

  @Test
  public void shouldSetLastUpdated() {
    Assert.assertNotNull(eob.getMeta().getLastUpdated());
  }

  @Test
  public void shouldSetCorrectProfile() {
    // The base CanonicalType doesn't seem to compare correctly so lets convert it to a string
    Assert.assertTrue(
        eob.getMeta().getProfile().stream()
            .map(ct -> ct.getValueAsString())
            .anyMatch(v -> v.equals(ProfileConstants.C4BB_EOB_INPATIENT_PROFILE_URL)));
  }

  @Test
  public void shouldSetUse() {
    Assert.assertEquals(Use.CLAIM, eob.getUse());
  }

  @Test
  public void shouldSetFinalAction() {
    Assert.assertEquals(ExplanationOfBenefitStatus.ACTIVE, eob.getStatus());
  }

  @Test
  public void shouldSetBillablePeriod() {
    Assert.assertNotNull(eob.getBillablePeriod());
    Assert.assertNotNull(eob.getBillablePeriod().getStart());
    Assert.assertNotNull(eob.getBillablePeriod().getEnd());
  }

  @Test
  public void shouldReferencePatient() {
    Assert.assertNotNull(eob.getPatient());
    Assert.assertEquals("Patient/567834", eob.getPatient().getReference());
  }

  @Test
  public void shouldHaveCreatedDate() {
    Assert.assertNotNull(eob.getCreated());
  }

  @Test
  public void shouldHaveFacilityTypeExtension() {
    Assert.assertNotNull(eob.getFacility());
    Assert.assertEquals(1, eob.getFacility().getExtension().size());

    Extension ex =
        TransformerTestUtilsV2.findExtensionByUrl(
            "https://bluebutton.cms.gov/resources/variables/clm_fac_type_cd",
            eob.getFacility().getExtension());

    Extension compare =
        new Extension(
            "https://bluebutton.cms.gov/resources/variables/clm_fac_type_cd",
            new Coding(
                "https://bluebutton.cms.gov/resources/variables/clm_fac_type_cd", "1", "Hospital"));

    Assert.assertTrue(compare.equalsDeep(ex));
  }

  /**
   * CareTeam list
   *
   * <p>Based on how the code currently works, we can assume that the same CareTeam members always
   * are added in the same order. This means we can look them up by sequence number.
   */
  @Test
  public void shouldHaveCareTeamList() {
    Assert.assertEquals(4, eob.getCareTeam().size());
  }

  /**
   * Testing all of these in one test, just because there isn't a distinct identifier really for
   * each
   */
  @Test
  public void shouldHaveCareTeamMembers() {
    // First member
    CareTeamComponent member1 = TransformerTestUtilsV2.findCareTeamBySequence(1, eob.getCareTeam());
    CareTeamComponent compare1 =
        TransformerTestUtilsV2.createNpiCareTeamMember(
            1,
            "161999999",
            "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBClaimCareTeamRole",
            "attending",
            "Attending");

    Assert.assertTrue(compare1.equalsDeep(member1));

    // Second member
    CareTeamComponent member2 = TransformerTestUtilsV2.findCareTeamBySequence(2, eob.getCareTeam());
    CareTeamComponent compare2 =
        TransformerTestUtilsV2.createNpiCareTeamMember(
            2,
            "3333444555",
            "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBClaimCareTeamRole",
            "operating",
            "Operating");

    Assert.assertTrue(compare2.equalsDeep(member2));

    // Third member
    CareTeamComponent member3 = TransformerTestUtilsV2.findCareTeamBySequence(3, eob.getCareTeam());
    CareTeamComponent compare3 =
        TransformerTestUtilsV2.createNpiCareTeamMember(
            3,
            "161943433",
            "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBClaimCareTeamRole",
            "otheroperating",
            "Other Operating");

    Assert.assertTrue(compare3.equalsDeep(member3));

    // Fourth member
    CareTeamComponent member4 = TransformerTestUtilsV2.findCareTeamBySequence(4, eob.getCareTeam());
    CareTeamComponent compare4 =
        TransformerTestUtilsV2.createNpiCareTeamMember(
            4,
            "345345345",
            "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBClaimCareTeamRole",
            "performing",
            "Performing provider");

    Assert.assertTrue(compare4.equalsDeep(member4));
  }

  /** SupportingInfo items */
  @Test
  public void shouldHaveNchPtntStusIndCdSupInfo() {
    SupportingInformationComponent sic =
        TransformerTestUtilsV2.findSupportingInfoByCode(
            "https://bluebutton.cms.gov/resources/variables/nch_ptnt_stus_ind_cd",
            eob.getSupportingInfo());

    SupportingInformationComponent compare =
        TransformerTestUtilsV2.createSupportingInfo(
            // We don't care what the sequence number is here
            sic.getSequence(),
            // Category
            Arrays.asList(
                new Coding(
                    "http://terminology.hl7.org/CodeSystem/claiminformationcategory",
                    "info",
                    "Information"),
                new Coding(
                    "https://bluebutton.cms.gov/resources/codesystem/information",
                    "https://bluebutton.cms.gov/resources/variables/nch_ptnt_stus_ind_cd",
                    "NCH Patient Status Indicator Code")),
            // Code
            new Coding(
                "https://bluebutton.cms.gov/resources/variables/nch_ptnt_stus_ind_cd",
                "A",
                "Discharged"));

    Assert.assertTrue(compare.equalsDeep(sic));
  }

  @Test
  public void shouldHaveClmIpAdmsnTypeCdSupInfo() {
    SupportingInformationComponent sic =
        TransformerTestUtilsV2.findSupportingInfoByCode(
            "https://bluebutton.cms.gov/resources/variables/clm_ip_admsn_type_cd",
            eob.getSupportingInfo());

    SupportingInformationComponent compare =
        TransformerTestUtilsV2.createSupportingInfo(
            // We don't care what the sequence number is here
            sic.getSequence(),
            // Category
            Arrays.asList(
                new Coding(
                    "http://terminology.hl7.org/CodeSystem/claiminformationcategory",
                    "info",
                    "Information"),
                new Coding(
                    "https://bluebutton.cms.gov/resources/codesystem/information",
                    "https://bluebutton.cms.gov/resources/variables/clm_ip_admsn_type_cd",
                    "Claim Inpatient Admission Type Code")),
            // Code
            new Coding(
                "https://bluebutton.cms.gov/resources/variables/clm_ip_admsn_type_cd",
                "1",
                "Emergency - The patient required immediate medical intervention as a result of severe, life threatening, or potentially disabling conditions. Generally, the patient was admitted through the emergency room."));

    Assert.assertTrue(compare.equalsDeep(sic));
  }

  /** Provider Local Reference */
  @Test
  public void shouldHaveLocalOrganizationReference() {
    Assert.assertNotNull(eob.getProvider());
    Assert.assertEquals("#provider-org", eob.getProvider().getReference());
  }

  /** Top level Extensions */
  @Test
  public void shouldHaveKnownExtensions() {
    Assert.assertEquals(5, eob.getExtension().size());
  }

  @Test
  public void shouldContainNchNearLineRecIdentCdExt() {
    Extension ex =
        TransformerTestUtilsV2.findExtensionByUrl(
            "https://bluebutton.cms.gov/resources/variables/nch_near_line_rec_ident_cd",
            eob.getExtension());

    Extension compare =
        new Extension(
            "https://bluebutton.cms.gov/resources/variables/nch_near_line_rec_ident_cd",
            new Coding(
                "https://bluebutton.cms.gov/resources/variables/nch_near_line_rec_ident_cd",
                "V",
                "Part A institutional claim record (inpatient [IP], skilled nursing facility [SNF], hospice [HOS], or home health agency [HHA])"));

    Assert.assertTrue(compare.equalsDeep(ex));
  }

  @Test
  public void shouldContainImeOpClmValAmtExt() {
    Extension ex =
        TransformerTestUtilsV2.findExtensionByUrl(
            "https://bluebutton.cms.gov/resources/variables/ime_op_clm_val_amt",
            eob.getExtension());

    Extension compare =
        new Extension(
            "https://bluebutton.cms.gov/resources/variables/ime_op_clm_val_amt",
            new Money().setValue(66125.51).setCurrency(TransformerConstants.CODED_MONEY_USD));

    Assert.assertTrue(compare.equalsDeep(ex));
  }

  @Test
  public void shouldContainDshOpClmValAmtExt() {
    Extension ex =
        TransformerTestUtilsV2.findExtensionByUrl(
            "https://bluebutton.cms.gov/resources/variables/dsh_op_clm_val_amt",
            eob.getExtension());

    Extension compare =
        new Extension(
            "https://bluebutton.cms.gov/resources/variables/dsh_op_clm_val_amt",
            new Money().setValue(25).setCurrency(TransformerConstants.CODED_MONEY_USD));

    Assert.assertTrue(compare.equalsDeep(ex));
  }

  @Test
  public void shouldContainClmMdcrNonPmtRsnCdExt() {
    Extension ex =
        TransformerTestUtilsV2.findExtensionByUrl(
            "https://bluebutton.cms.gov/resources/variables/clm_mdcr_non_pmt_rsn_cd",
            eob.getExtension());

    Extension compare =
        new Extension(
            "https://bluebutton.cms.gov/resources/variables/clm_mdcr_non_pmt_rsn_cd",
            new Coding(
                "https://bluebutton.cms.gov/resources/variables/clm_mdcr_non_pmt_rsn_cd",
                "A",
                "Covered worker's compensation (Obsolete)"));

    Assert.assertTrue(compare.equalsDeep(ex));
  }

  @Test
  public void shouldContainClmSrvcClsfctnTypeCdExt() {
    Extension ex =
        TransformerTestUtilsV2.findExtensionByUrl(
            "https://bluebutton.cms.gov/resources/variables/clm_srvc_clsfctn_type_cd",
            eob.getExtension());

    Extension compare =
        new Extension(
            "https://bluebutton.cms.gov/resources/variables/clm_srvc_clsfctn_type_cd",
            new Coding(
                "https://bluebutton.cms.gov/resources/variables/clm_srvc_clsfctn_type_cd",
                "1",
                null));

    Assert.assertTrue(compare.equalsDeep(ex));
  }

  /** Top level Identifiers */
  @Test
  public void shouldHaveKnownIdentifiers() {
    Assert.assertEquals(2, eob.getIdentifier().size());
  }

  @Test
  public void shouldIncludeClaimIdIdentifier() {
    Identifier clmId =
        TransformerTestUtilsV2.findIdentifierBySystem(
            "https://bluebutton.cms.gov/resources/variables/clm_id", eob.getIdentifier());

    Identifier compare =
        TransformerTestUtilsV2.createIdentifier(
            "https://bluebutton.cms.gov/resources/variables/clm_id",
            "333333222222",
            "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBIdentifierType",
            "uc",
            "Unique Claim ID");

    Assert.assertTrue(compare.equalsDeep(clmId));
  }

  @Test
  public void shouldIncludeClaimGroupIdentifier() {
    Identifier clmGrp =
        TransformerTestUtilsV2.findIdentifierBySystem(
            "https://bluebutton.cms.gov/resources/identifier/claim-group", eob.getIdentifier());

    Identifier compare =
        TransformerTestUtilsV2.createIdentifier(
            "https://bluebutton.cms.gov/resources/identifier/claim-group",
            "900",
            "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBIdentifierType",
            "uc",
            "Unique Claim ID");

    Assert.assertTrue(compare.equalsDeep(clmGrp));
  }

  /** Top level Type */
  @Test
  public void shouldHaveExpectedTypeCoding() {
    Assert.assertEquals(3, eob.getType().getCoding().size());
  }

  @Test
  public void shouldHaveExpectedCodingValues() {
    CodeableConcept compare =
        new CodeableConcept()
            .setCoding(
                Arrays.asList(
                    new Coding(
                        "https://bluebutton.cms.gov/resources/variables/nch_clm_type_cd",
                        "60",
                        "Inpatient claim"),
                    new Coding(
                        "https://bluebutton.cms.gov/resources/codesystem/eob-type",
                        "INPATIENT",
                        null),
                    new Coding(
                        "http://terminology.hl7.org/CodeSystem/claim-type",
                        "institutional",
                        "Institutional")));

    Assert.assertTrue(compare.equalsDeep(eob.getType()));
  }

  /**
   * Serializes the EOB and prints to the command line
   *
   * @throws FHIRException
   */
  @Ignore
  @Test
  public void serializeSampleARecord() throws FHIRException {
    ExplanationOfBenefit eob = InpatientClaimTransformerV2.transform(new MetricRegistry(), claim);
    System.out.println(fhirContext.newJsonParser().encodeResourceToString(eob));
  }
}
