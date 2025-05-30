package gov.cms.bfd.server.war.r4.providers;

import static gov.cms.bfd.server.war.commons.CommonTransformerUtils.convertToDate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import com.codahale.metrics.MetricRegistry;
import gov.cms.bfd.model.codebook.data.CcwCodebookVariable;
import gov.cms.bfd.model.codebook.model.CcwCodebookInterface;
import gov.cms.bfd.model.rif.entities.CarrierClaim;
import gov.cms.bfd.model.rif.entities.CarrierClaimColumn;
import gov.cms.bfd.model.rif.entities.CarrierClaimLine;
import gov.cms.bfd.model.rif.entities.DMEClaim;
import gov.cms.bfd.model.rif.entities.DMEClaimColumn;
import gov.cms.bfd.model.rif.entities.DMEClaimLine;
import gov.cms.bfd.model.rif.entities.HHAClaim;
import gov.cms.bfd.model.rif.entities.HospiceClaim;
import gov.cms.bfd.model.rif.entities.InpatientClaim;
import gov.cms.bfd.model.rif.entities.OutpatientClaim;
import gov.cms.bfd.model.rif.entities.PartDEvent;
import gov.cms.bfd.model.rif.entities.SNFClaim;
import gov.cms.bfd.server.war.NPIOrgLookup;
import gov.cms.bfd.server.war.commons.CCWUtils;
import gov.cms.bfd.server.war.commons.ClaimType;
import gov.cms.bfd.server.war.commons.CommonTransformerUtils;
import gov.cms.bfd.server.war.commons.MedicareSegment;
import gov.cms.bfd.server.war.commons.SecurityTagManager;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import gov.cms.bfd.server.war.commons.carin.C4BBClaimProfessionalAndNonClinicianCareTeamRole;
import gov.cms.bfd.server.war.r4.providers.pac.common.ClaimWithSecurityTags;
import gov.cms.bfd.server.war.stu3.providers.ExplanationOfBenefitResourceProvider;
import gov.cms.bfd.server.war.utils.RDATestUtils;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IBaseHasExtensions;
import org.hl7.fhir.r4.model.BaseDateTimeType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.AdjudicationComponent;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.BenefitComponent;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.CareTeamComponent;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.DiagnosisComponent;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.ItemComponent;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.ProcedureComponent;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.SupportingInformationComponent;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Money;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.opentest4j.AssertionFailedError;

/** Test utilities helpful in setting up test data or running a test. */
public final class TransformerTestUtilsV2 {
  /** The fhir context for parsing the test file. Do this very slow operation once. */
  private static final FhirContext fhirContext = FhirContext.forR4();

  /** NPIOrgLookup. */
  private static final NPIOrgLookup npiOrgLookup;

  /** fake npi data. */
  private static final String ORG_FILE_NAME = "fakeOrgData.tsv";

  static {
    npiOrgLookup = RDATestUtils.createTestNpiOrgLookup();
    CommonTransformerUtils.setNpiOrgLookup(npiOrgLookup);
  }

  /**
   * Test the transformation of common group level header fields between all claim types.
   *
   * @param eob the {@link ExplanationOfBenefit} to modify
   * @param claimId CLM_ID
   * @param beneficiaryId BENE_ID
   * @param claimType {@link ClaimType} to process
   * @param claimGroupId CLM_GRP_ID
   * @param coverageType {@link MedicareSegment}
   * @param dateFrom CLM_FROM_DT
   * @param dateThrough CLM_THRU_DT
   * @param paymentAmount CLM_PMT_AMT
   * @param finalAction FINAL_ACTION
   */
  static void assertEobCommonClaimHeaderData(
      ExplanationOfBenefit eob,
      Long claimId,
      Long beneficiaryId,
      ClaimType claimType,
      String claimGroupId,
      MedicareSegment coverageType,
      Optional<LocalDate> dateFrom,
      Optional<LocalDate> dateThrough,
      Optional<BigDecimal> paymentAmount,
      char finalAction) {

    assertNoEncodedOptionals(eob);

    assertEquals(TransformerUtilsV2.buildEobId(claimType, claimId), eob.getIdElement().getIdPart());

    assertHasIdentifier(
        claimType.equals(ClaimType.PDE) ? CcwCodebookVariable.PDE_ID : CcwCodebookVariable.CLM_ID,
        String.valueOf(claimId),
        eob.getIdentifier());

    assertIdentifierExists(
        TransformerConstants.IDENTIFIER_SYSTEM_BBAPI_CLAIM_GROUP_ID,
        claimGroupId,
        eob.getIdentifier());

    assertEquals(
        TransformerUtilsV2.referencePatient(beneficiaryId).getReference(),
        eob.getPatient().getReference());

    assertEquals(
        TransformerUtilsV2.referenceCoverage(beneficiaryId, coverageType).getReference(),
        eob.getInsuranceFirstRep().getCoverage().getReference());

    switch (finalAction) {
      case 'F':
        assertEquals("active", eob.getStatus().toCode());
        break;
      case 'N':
        assertEquals("cancelled", eob.getStatus().toCode());
        break;
      default:
        throw new BadCodeMonkeyException();
    }

    if (paymentAmount.isPresent()) {
      assertEquals(paymentAmount.get(), eob.getPayment().getAmount().getValue());
    }
  }

  /**
   * Verifies that the specified FHIR {@link Resource} has no unwrapped {@link Optional} values.
   * This is important, as such values don't serialize to FHIR correctly.
   *
   * @param resource the FHIR {@link Resource} to check
   */
  static void assertNoEncodedOptionals(Resource resource) {
    String encodedResourceXml = fhirContext.newXmlParser().encodeResourceToString(resource);
    assertFalse(encodedResourceXml.contains("Optional"));
  }

  /**
   * Tests the provider number field is set as expected in the EOB. This field is common among these
   * claim types: Inpatient, Outpatient, Hospice, HHA and SNF.
   *
   * @param eob the {@link ExplanationOfBenefit} this method will test against
   * @param providerNumber a {@link String} PRVDR_NUM: representing the expected provider number for
   *     the claim
   */
  static void assertProviderNPI(ExplanationOfBenefit eob, Optional<String> providerNumber) {
    Reference reference = eob.getProvider();

    assertTrue(reference.hasIdentifier(), "Bad reference: " + reference);
    assertEquals(TransformerConstants.CODING_NPI_US, reference.getIdentifier().getSystem());
    assertEquals(providerNumber.get(), reference.getIdentifier().getValue());
  }

  /**
   * Asserts the expected identifier exists.
   *
   * @param expectedSystem the expected {@link Identifier#getSystem()} value
   * @param expectedId the expected {@link Identifier#getValue()} value
   * @param actuals the actual {@link Identifier} to verify
   */
  static void assertIdentifierExists(
      String expectedSystem, String expectedId, List<Identifier> actuals) {
    assertTrue(
        actuals.stream()
            .filter(i -> expectedSystem.equals(i.getSystem()))
            .anyMatch(i -> expectedId.equals(i.getValue())));
  }

  /**
   * Asserts the expected identifiers are equal.
   *
   * @param expectedIdentifierSystem the expected {@link Identifier#getSystem()} value
   * @param expectedIdentifierValue the expected {@link Identifier#getValue()} value
   * @param reference the actual {@link Reference} to verify
   */
  static void assertReferenceIdentifierEquals(
      String expectedIdentifierSystem, String expectedIdentifierValue, Reference reference) {
    assertTrue(reference.hasIdentifier(), "Bad reference: " + reference);
    assertEquals(expectedIdentifierSystem, reference.getIdentifier().getSystem());
    assertEquals(expectedIdentifierValue, reference.getIdentifier().getValue());
    assertEquals(
        CommonTransformerUtils.retrieveNpiCodeDisplay(expectedIdentifierValue),
        reference.getDisplay());
  }

  /**
   * Asserts the expected reference identifiers are equal.
   *
   * @param ccwVariable the {@link CcwCodebookVariable} that was mapped
   * @param expectedIdentifierValue the expected {@link Identifier#getValue()} of the {@link
   *     Reference#getIdentifier()}
   * @param actualReference the actual {@link Reference} to verify
   */
  private static void assertReferenceIdentifierEquals(
      CcwCodebookInterface ccwVariable, String expectedIdentifierValue, Reference actualReference) {
    assertTrue(actualReference.hasIdentifier(), "Bad reference: " + actualReference);
    assertEquals(
        CCWUtils.calculateVariableReferenceUrl(ccwVariable),
        actualReference.getIdentifier().getSystem());
    assertEquals(expectedIdentifierValue, actualReference.getIdentifier().getValue());
  }

  /**
   * Asserts the expected extension coding exists.
   *
   * @param ccwVariable the {@link CcwCodebookVariable} that was mapped
   * @param expectedCode the expected {@link Coding#getCode()}
   * @param actualElement the FHIR element to find and verify the {@link Extension} of
   */
  static void assertExtensionCodingEquals(
      CcwCodebookInterface ccwVariable, Object expectedCode, IBaseHasExtensions actualElement) {
    // Jumping through hoops to cope with overloaded method:
    Optional<?> expectedCodeCast =
        expectedCode instanceof Optional ? (Optional<?>) expectedCode : Optional.of(expectedCode);
    assertExtensionCodingEquals(ccwVariable, expectedCodeCast, actualElement);
  }

  /**
   * Asserts the expected extension coding exists.
   *
   * <p>FIXME change name of this and related methods to assertHasExtensionCoding(...)
   *
   * @param ccwVariable the {@link CcwCodebookVariable} that the expected {@link Extension} / {@link
   *     Coding} are for
   * @param expectedCode the expected {@link Coding#getCode()}
   * @param actualElement the FHIR element to find and verify the {@link Extension} of
   */
  static void assertExtensionCodingEquals(
      CcwCodebookInterface ccwVariable,
      Optional<?> expectedCode,
      IBaseHasExtensions actualElement) {
    String expectedExtensionUrl = CCWUtils.calculateVariableReferenceUrl(ccwVariable);
    String expectedCodingSystem = expectedExtensionUrl;
    Optional<? extends IBaseExtension<?, ?>> extensionForUrl =
        actualElement.getExtension().stream()
            .filter(e -> e.getUrl().equals(expectedExtensionUrl))
            .findFirst();

    assertEquals(expectedCode.isPresent(), extensionForUrl.isPresent());
    if (expectedCode.isPresent())
      assertCodingEquals(
          expectedCodingSystem, expectedCode.get(), (Coding) extensionForUrl.get().getValue());
  }

  /**
   * Asserts the expected extension coding exists.
   *
   * @param fhirElement the FHIR element to check the extension of
   * @param expectedExtensionUrl the expected {@link Extension#getUrl()} of the {@link Extension} to
   *     look for
   * @param expectedCodingSystem the expected {@link Coding#getSystem()}
   * @param expectedCode the expected {@link Coding#getCode()}
   */
  static void assertExtensionCodingEquals(
      IBaseHasExtensions fhirElement,
      String expectedExtensionUrl,
      String expectedCodingSystem,
      String expectedCode) {
    IBaseExtension<?, ?> extensionForUrl =
        fhirElement.getExtension().stream()
            .filter(e -> e.getUrl().equals(expectedExtensionUrl))
            .findFirst()
            .get();
    assertHasCoding(expectedCodingSystem, expectedCode, (Coding) extensionForUrl.getValue());
  }

  /**
   * Asserts the expected identifier exists.
   *
   * @param ccwVariable the {@link CcwCodebookVariable} that was mapped
   * @param expectedValue the expected {@link Identifier#getValue()} value
   * @param actualIdentifiers the actual {@link Identifier}s to verify a match can be found within
   */
  private static void assertHasIdentifier(
      CcwCodebookInterface ccwVariable, String expectedValue, List<Identifier> actualIdentifiers) {
    if (expectedValue == null) throw new IllegalArgumentException();

    assertNotNull(actualIdentifiers);

    String expectedSystem = CCWUtils.calculateVariableReferenceUrl(ccwVariable);
    Optional<Identifier> matchingIdentifier =
        actualIdentifiers.stream()
            .filter(i -> expectedSystem.equals(i.getSystem()))
            .filter(i -> expectedValue.equals(i.getValue()))
            .findAny();
    assertTrue(matchingIdentifier.isPresent());
  }

  /**
   * Asserts the expected coding exists.
   *
   * @param expectedSystem the expected {@link Coding#getSystem()} value
   * @param expectedCode the expected {@link Coding#getCode()} value
   * @param actualCode the actual {@link Coding} to verify
   */
  static void assertHasCoding(String expectedSystem, String expectedCode, Coding actualCode) {
    assertHasCoding(expectedSystem, null, null, expectedCode, Arrays.asList(actualCode));
  }

  /**
   * Asserts the expected coding exists.
   *
   * @param expectedSystem the expected {@link Coding#getSystem()} value
   * @param expectedCode the expected {@link Coding#getCode()} value
   * @param actualCode the actual {@link List}&lt;{@link Coding}&gt; to verify
   */
  static void assertHasCoding(String expectedSystem, String expectedCode, List<Coding> actualCode) {
    assertHasCoding(expectedSystem, null, null, expectedCode, actualCode);
  }

  /**
   * Asserts the expected coding exists.
   *
   * @param expectedSystem the expected {@link Coding#getSystem()} value
   * @param expectedVersion the expected {@link Coding#getVersion()} value
   * @param expectedDisplay the expected {@link Coding#getDisplay()} value
   * @param expectedCode the expected {@link Coding#getCode()} value
   * @param actualCode the actual {@link List}&lt;{@link Coding}&gt; to verify
   */
  static void assertHasCoding(
      String expectedSystem,
      String expectedVersion,
      String expectedDisplay,
      String expectedCode,
      List<Coding> actualCode) {
    assertTrue(
        actualCode.stream()
            .anyMatch(
                c -> {
                  return expectedSystem.equals(c.getSystem())
                      && (expectedVersion == null || expectedVersion.equals(c.getVersion()))
                      && (expectedDisplay == null || expectedDisplay.equals(c.getDisplay()))
                      && expectedCode.equals(c.getCode());
                }),
        "No matching Coding found: " + actualCode);
  }

  /**
   * Asserts the expected identifiers match the expected value.
   *
   * @param ccwVariable the {@link CcwCodebookVariable} that was mapped
   * @param expectedValue the expected {@link Identifier#getValue()} value
   * @param actual the actual {@link Identifier} to verify
   */
  static void assertIdentifierEquals(
      CcwCodebookInterface ccwVariable, String expectedValue, Identifier actual) {
    if (expectedValue == null) throw new IllegalArgumentException();

    assertNotNull(actual);
    assertEquals(CCWUtils.calculateVariableReferenceUrl(ccwVariable), actual.getSystem());
    assertEquals(expectedValue, actual.getValue());
  }

  /**
   * Asserts the expected dates are equal.
   *
   * @param expected the expected {@link LocalDate}
   * @param actual the actual {@link BaseDateTimeType} to verify
   */
  static void assertDateEquals(LocalDate expected, BaseDateTimeType actual) {
    assertEquals(convertToDate(expected), actual.getValue());
    assertEquals(TemporalPrecisionEnum.DAY, actual.getPrecision());
  }

  /**
   * Finds a care team entry for the specified provider identifier.
   *
   * @param expectedProviderNpi the {@link Identifier#getValue()} of the provider to find a matching
   *     {@link CareTeamComponent} for
   * @param careTeam the {@link List} of {@link CareTeamComponent}s to search
   * @return the {@link CareTeamComponent} whose {@link CareTeamComponent#getProvider()} is an
   *     {@link Identifier} with the specified provider NPI, or else <code>null</code> if no such
   *     {@link CareTeamComponent} was found
   */
  static CareTeamComponent findCareTeamEntryForProviderIdentifier(
      String expectedProviderNpi, List<CareTeamComponent> careTeam) {
    return findCareTeamEntryForProviderIdentifier(
        Optional.of(TransformerConstants.CODING_NPI_US), expectedProviderNpi, null, careTeam);
  }

  /**
   * Finds a care team entry for the specified provider tax number.
   *
   * @param expectedProviderTaxNumber the {@link Identifier#getValue()} of the provider to find a
   *     matching {@link CareTeamComponent} for
   * @param careTeam the {@link List} of {@link CareTeamComponent}s to search
   * @return the {@link CareTeamComponent} whose {@link CareTeamComponent#getProvider()} is an
   *     {@link Identifier} with the specified provider tax number, or else <code>null</code> if no
   *     such {@link CareTeamComponent} was found
   */
  static CareTeamComponent findCareTeamEntryForProviderTaxNumber(
      String expectedProviderTaxNumber, List<CareTeamComponent> careTeam) {
    return findCareTeamEntryForProviderIdentifier(
        Optional.empty(),
        expectedProviderTaxNumber,
        C4BBClaimProfessionalAndNonClinicianCareTeamRole.OTHER,
        careTeam);
  }

  /**
   * Finds a care team entry for the specified provider identifier.
   *
   * @param expectedIdentifierSystem the {@link Identifier#getSystem()} of the provider to find a
   *     matching {@link CareTeamComponent} for
   * @param expectedIdentifierValue the {@link Identifier#getValue()} of the provider to find a
   *     matching {@link CareTeamComponent} for
   * @param expectedRole the expected role
   * @param careTeam the {@link List} of {@link CareTeamComponent}s to search
   * @return the {@link CareTeamComponent} whose {@link CareTeamComponent#getProvider()} is an
   *     {@link Identifier} with the specified provider NPI, or else <code>null</code> if no such
   *     {@link CareTeamComponent} was found
   */
  public static CareTeamComponent findCareTeamEntryForProviderIdentifier(
      Optional<String> expectedIdentifierSystem,
      String expectedIdentifierValue,
      C4BBClaimProfessionalAndNonClinicianCareTeamRole expectedRole,
      List<CareTeamComponent> careTeam) {
    Optional<CareTeamComponent> careTeamEntry =
        careTeam.stream()
            .filter(
                ctc ->
                    doesReferenceMatchIdentifier(
                        expectedIdentifierSystem, expectedIdentifierValue, ctc.getProvider()))
            .filter(ctc -> doesCareTeamComponentMatchRole(expectedRole, ctc))
            .findFirst();
    return careTeamEntry.orElse(null);
  }

  /**
   * Validates if the reference matches the identifier.
   *
   * @param expectedIdentifierSystem the expected {@link Identifier#getSystem()} to match
   * @param expectedIdentifierValue the expected {@link Identifier#getValue()} to match
   * @param actualReference the {@link Reference} to check
   * @return <code>true</code> if the specified {@link Reference} matches the expected {@link
   *     Identifier}
   */
  private static boolean doesReferenceMatchIdentifier(
      Optional<String> expectedIdentifierSystem,
      String expectedIdentifierValue,
      Reference actualReference) {
    if (!actualReference.hasIdentifier()) return false;

    if (expectedIdentifierSystem.isPresent()) {
      return expectedIdentifierSystem.get().equals(actualReference.getIdentifier().getSystem())
          && expectedIdentifierValue.equals(actualReference.getIdentifier().getValue());
    } else {
      return expectedIdentifierValue.equals(actualReference.getIdentifier().getValue());
    }
  }

  /**
   * Does the role of the care team component match what is expected.
   *
   * @param expectedRole expected role; maybe empty
   * @param actualComponent the Care Team Component to test
   * @return iff it matches or expected role is empty
   */
  private static boolean doesCareTeamComponentMatchRole(
      C4BBClaimProfessionalAndNonClinicianCareTeamRole expectedRole,
      CareTeamComponent actualComponent) {
    if (expectedRole == null) return true;
    if (!actualComponent.hasRole()) return false;
    final Coding actualRole = actualComponent.getRole().getCodingFirstRep();
    return actualRole.getCode().equals(expectedRole.toCode());
    // TODO - fix this
    // && actualRole.getSystem().equals(ClaimCareteamrole.NULL.getSystem());
  }

  /**
   * Asserts that the provided EOB has the specified supporting information details.
   *
   * @param categoryVariable the {@link CcwCodebookVariable} matching the {@link
   *     SupportingInformationComponent#getCategory()} to find
   * @param codeSystemVariable the {@link CcwCodebookVariable} that should have been mapped to
   *     {@link SupportingInformationComponent#getCode()}'s {@link Coding#getSystem()}
   * @param codeValue the value that should have been mapped to {@link
   *     SupportingInformationComponent#getCode()}'s {@link Coding#getCode()}
   * @param eob the actual {@link ExplanationOfBenefit} to search
   */
  static void assertInfoWithCodeEquals(
      CcwCodebookVariable categoryVariable,
      CcwCodebookVariable codeSystemVariable,
      Optional<?> codeValue,
      ExplanationOfBenefit eob) {
    SupportingInformationComponent info = assertHasInfo(categoryVariable, eob);
    assertHasCoding(codeSystemVariable, codeValue, info.getCode());
  }

  /**
   * Tests that the hcpcs code and hcpcs modifier codes are set as expected in the item component.
   * The hcpcsCode field is common among these claim types: Carrier, Inpatient, Outpatient, DME,
   * Hospice, HHA and SNF. The modifier fields are common among these claim types: Carrier,
   * Outpatient, DME, Hospice and HHA.
   *
   * @param item the {@link ItemComponent} this method will test against
   * @param hcpcsCode the {@link Optional}&lt;{@link String}&gt; HCPCS_CD: representing the hcpcs
   *     code for the claim
   * @param hcpcsInitialModifierCode the {@link Optional}&lt;{@link String}&gt; HCPCS_1ST_MDFR_CD:
   *     representing the expected hcpcs initial modifier code for the claim
   * @param hcpcsSecondModifierCode the {@link Optional}&lt;{@link String}&gt; HCPCS_2ND_MDFR_CD:
   *     representing the expected hcpcs second modifier code for the claim
   * @param hcpcsYearCode the {@link Optional}&lt;{@link Character}&gt; CARR_CLM_HCPCS_YR_CD:
   *     representing the hcpcs year code for the claim
   * @param index the {@link int} modifier index in the item containing the expected code
   */
  static void assertHcpcsCodes(
      ItemComponent item,
      Optional<String> hcpcsCode,
      Optional<String> hcpcsInitialModifierCode,
      Optional<String> hcpcsSecondModifierCode,
      Optional<Character> hcpcsYearCode,
      int index) {
    // TODO - fix this
    /*
    if (hcpcsYearCode.isPresent()) { // some claim types have a year code...
      assertHasCoding(
          TransformerConstants.CODING_SYSTEM_HCPCS,
          "" + hcpcsYearCode.get(),
          null,
          hcpcsInitialModifierCode.get(),
          item.getModifier().get(index).getCoding());
      assertHasCoding(
          TransformerConstants.CODING_SYSTEM_HCPCS,
          "" + hcpcsYearCode.get(),
          null,
          hcpcsCode.get(),
          item.getService().getCoding());
    } else { // while others do not...
      if (hcpcsInitialModifierCode.isPresent()) {
        assertHasCoding(
            TransformerConstants.CODING_SYSTEM_HCPCS,
            hcpcsInitialModifierCode.get(),
            item.getModifier().get(index).getCoding());
      }
      if (hcpcsCode.isPresent()) {
        assertHasCoding(
            TransformerConstants.CODING_SYSTEM_HCPCS,
            hcpcsCode.get(),
            item.getService().getCoding());
      }
    }
    */
    assertFalse(hcpcsSecondModifierCode.isPresent());
  }

  /**
   * Test the transformation of the item level data elements between the {@link CarrierClaimLine}
   * and {@link DMEClaimLine} claim types to FHIR. The method parameter fields from {@link
   * CarrierClaimLine} and {@link DMEClaimLine} are listed below and their corresponding RIF CCW
   * fields (denoted in all CAPS below from {@link CarrierClaimColumn} and {@link DMEClaimColumn}).
   *
   * @param item the {@link ItemComponent} to test
   * @param eob the {@link ExplanationOfBenefit} to test
   * @param serviceCount LINE_SRVC_CNT,
   * @param placeOfServiceCode LINE_PLACE_OF_SRVC_CD,
   * @param firstExpenseDate LINE_1ST_EXPNS_DT,
   * @param lastExpenseDate LINE_LAST_EXPNS_DT,
   * @param beneficiaryPaymentAmount LINE_BENE_PMT_AMT,
   * @param providerPaymentAmount LINE_PRVDR_PMT_AMT,
   * @param beneficiaryPartBDeductAmount LINE_BENE_PTB_DDCTBL_AMT,
   * @param primaryPayerCode LINE_BENE_PRMRY_PYR_CD,
   * @param primaryPayerPaidAmount LINE_BENE_PRMRY_PYR_PD_AMT,
   * @param betosCode BETOS_CD,
   * @param paymentAmount LINE_NCH_PMT_AMT,
   * @param paymentCode LINE_PMT_80_100_CD,
   * @param coinsuranceAmount LINE_COINSRNC_AMT,
   * @param submittedChargeAmount LINE_SBMTD_CHRG_AMT,
   * @param allowedChargeAmount LINE_ALOWD_CHRG_AMT,
   * @param processingIndicatorCode LINE_PRCSG_IND_CD,
   * @param serviceDeductibleCode LINE_SERVICE_DEDUCTIBLE,
   * @param diagnosisCode LINE_ICD_DGNS_CD,
   * @param diagnosisCodeVersion LINE_ICD_DGNS_VRSN_CD,
   * @param hctHgbTestTypeCode LINE_HCT_HGB_TYPE_CD
   * @param hctHgbTestResult LINE_HCT_HGB_RSLT_NUM,
   * @param cmsServiceTypeCode LINE_CMS_TYPE_SRVC_CD,
   * @param nationalDrugCode LINE_NDC_CD
   * @throws FHIRException if there is a transformation issue
   */
  static void assertEobCommonItemCarrierDMEEquals(
      ItemComponent item,
      ExplanationOfBenefit eob,
      BigDecimal serviceCount,
      String placeOfServiceCode,
      Optional<LocalDate> firstExpenseDate,
      Optional<LocalDate> lastExpenseDate,
      BigDecimal beneficiaryPaymentAmount,
      BigDecimal providerPaymentAmount,
      BigDecimal beneficiaryPartBDeductAmount,
      Optional<Character> primaryPayerCode,
      BigDecimal primaryPayerPaidAmount,
      Optional<String> betosCode,
      BigDecimal paymentAmount,
      Optional<Character> paymentCode,
      BigDecimal coinsuranceAmount,
      BigDecimal submittedChargeAmount,
      BigDecimal allowedChargeAmount,
      Optional<String> processingIndicatorCode,
      Optional<Character> serviceDeductibleCode,
      Optional<String> diagnosisCode,
      Optional<Character> diagnosisCodeVersion,
      Optional<String> hctHgbTestTypeCode,
      BigDecimal hctHgbTestResult,
      char cmsServiceTypeCode,
      Optional<String> nationalDrugCode)
      throws FHIRException {

    assertEquals(serviceCount, item.getQuantity().getValue());

    assertHasCoding(
        CcwCodebookVariable.LINE_CMS_TYPE_SRVC_CD, cmsServiceTypeCode, item.getCategory());
    assertHasCoding(
        CcwCodebookVariable.LINE_PLACE_OF_SRVC_CD,
        placeOfServiceCode,
        item.getLocationCodeableConcept());
    assertExtensionCodingEquals(CcwCodebookVariable.BETOS_CD, betosCode, item);
    assertDateEquals(firstExpenseDate.get(), item.getServicedPeriod().getStartElement());
    assertDateEquals(lastExpenseDate.get(), item.getServicedPeriod().getEndElement());

    // TODO - fix this
    /*
    AdjudicationComponent adjudicationForPayment =
        assertAdjudicationAmountEquals(
            CcwCodebookVariable.LINE_NCH_PMT_AMT, paymentAmount, item.getAdjudication());
    assertExtensionCodingEquals(
        CcwCodebookVariable.LINE_PMT_80_100_CD, paymentCode, adjudicationForPayment);
    assertAdjudicationAmountEquals(
        CcwCodebookVariable.LINE_BENE_PMT_AMT, beneficiaryPaymentAmount, item.getAdjudication());
    assertAdjudicationAmountEquals(
        CcwCodebookVariable.LINE_PRVDR_PMT_AMT, providerPaymentAmount, item.getAdjudication());
    assertAdjudicationAmountEquals(
        CcwCodebookVariable.LINE_BENE_PTB_DDCTBL_AMT,
        beneficiaryPartBDeductAmount,
        item.getAdjudication());
    assertExtensionCodingEquals(CcwCodebookVariable.LINE_BENE_PRMRY_PYR_CD, primaryPayerCode, item);
    assertAdjudicationAmountEquals(
        CcwCodebookVariable.LINE_BENE_PRMRY_PYR_PD_AMT,
        primaryPayerPaidAmount,
        item.getAdjudication());
    assertAdjudicationAmountEquals(
        CcwCodebookVariable.LINE_COINSRNC_AMT, coinsuranceAmount, item.getAdjudication());
    assertAdjudicationAmountEquals(
        CcwCodebookVariable.LINE_SBMTD_CHRG_AMT, submittedChargeAmount, item.getAdjudication());
    assertAdjudicationAmountEquals(
        CcwCodebookVariable.LINE_ALOWD_CHRG_AMT, allowedChargeAmount, item.getAdjudication());
    assertAdjudicationReasonEquals(
        CcwCodebookVariable.LINE_PRCSG_IND_CD, processingIndicatorCode, item.getAdjudication());
    assertExtensionCodingEquals(
        CcwCodebookVariable.LINE_SERVICE_DEDUCTIBLE, serviceDeductibleCode, item);

    assertDiagnosisLinkPresent(Diagnosis.from(diagnosisCode, diagnosisCodeVersion), eob, item);

    List<Extension> hctHgbObservationExtension =
        item.getExtensionsByUrl(
            TransformerUtils.calculateVariableReferenceUrl(
                CcwCodebookVariable.LINE_HCT_HGB_RSLT_NUM));
    assertEquals(1, hctHgbObservationExtension.size());
    assertTrue(hctHgbObservationExtension.get(0).getValue() instanceof Reference);
    Reference hctHgbReference = (Reference) hctHgbObservationExtension.get(0).getValue();
    assertTrue(hctHgbReference.getResource() instanceof Observation);
    Observation hctHgbObservation = (Observation) hctHgbReference.getResource();
    assertHasCoding(
        CcwCodebookVariable.LINE_HCT_HGB_TYPE_CD, hctHgbTestTypeCode, hctHgbObservation.getCode());
    assertEquals(hctHgbTestResult, hctHgbObservation.getValueQuantity().getValue());

    assertExtensionCodingEquals(
        item,
        TransformerConstants.CODING_NDC,
        TransformerConstants.CODING_NDC,
        nationalDrugCode.get());
        */
  }

  /**
   * Asserts the EOB has the specified supporting information.
   *
   * @param categoryVariable the {@link CcwCodebookVariable} matching the {@link
   *     SupportingInformationComponent#getCategory()} to find
   * @param eob the {@link ExplanationOfBenefit} to search
   * @return the {@link SupportingInformationComponent} that was found (if one wasn't, the method
   *     will instead fail with an {@link AssertionFailedError})
   */
  static SupportingInformationComponent assertHasInfo(
      CcwCodebookVariable categoryVariable, ExplanationOfBenefit eob) {
    Optional<SupportingInformationComponent> info =
        eob.getSupportingInfo().stream()
            .filter(
                i ->
                    isCodeInConcept(
                        i.getCategory(),
                        TransformerConstants.CODING_BBAPI_INFORMATION_CATEGORY,
                        CCWUtils.calculateVariableReferenceUrl(categoryVariable)))
            .findFirst();
    assertTrue(info.isPresent());

    return info.get();
  }

  /**
   * Verifies a {@link Coding} exists within the specified {@link CodeableConcept}.
   *
   * @param concept the {@link CodeableConcept} to check
   * @param codingSystem the {@link Coding#getSystem()} to match
   * @param codingCode the {@link Coding#getCode()} to match
   * @return <code>true</code> if the specified {@link CodeableConcept} contains the specified
   *     {@link Coding}, <code>false</code> if it does not
   */
  static boolean isCodeInConcept(CodeableConcept concept, String codingSystem, String codingCode) {
    return isCodeInConcept(concept, codingSystem, null, codingCode);
  }

  /**
   * Verifies a @link Coding} exists within the specified {@link CodeableConcept}.
   *
   * @param concept the {@link CodeableConcept} to check
   * @param codingSystem the {@link Coding#getSystem()} to match
   * @param codingVersion the {@link Coding#getVersion()} to match
   * @param codingCode the {@link Coding#getCode()} to match
   * @return <code>true</code> if the specified {@link CodeableConcept} contains the specified
   *     {@link Coding}, <code>false</code> if it does not
   */
  static boolean isCodeInConcept(
      CodeableConcept concept, String codingSystem, String codingVersion, String codingCode) {
    return concept.getCoding().stream()
        .anyMatch(
            c -> {
              if (!codingSystem.equals(c.getSystem())) return false;
              if (codingVersion != null && !codingVersion.equals(c.getVersion())) return false;
              return codingCode.equals(c.getCode());
            });
  }

  /**
   * Asserts an extension date year equals the provided value.
   *
   * @param ccwVariable the {@link CcwCodebookVariable} that the expected {@link Extension} / {@link
   *     Coding} are for
   * @param expectedDateYear the expected {@link Coding#getCode()}
   * @param actualElement the FHIR element to find and verify the {@link Extension} of
   */
  static void assertExtensionDateYearEquals(
      CcwCodebookInterface ccwVariable,
      Optional<?> expectedDateYear,
      IBaseHasExtensions actualElement) {
    String expectedExtensionUrl = CCWUtils.calculateVariableReferenceUrl(ccwVariable);
    String expectedCodingSystem = expectedExtensionUrl;
    Optional<? extends IBaseExtension<?, ?>> extensionForUrl =
        actualElement.getExtension().stream()
            .filter(e -> e.getUrl().equals(expectedExtensionUrl))
            .findFirst();

    assertEquals(expectedDateYear.isPresent(), extensionForUrl.isPresent());
  }

  /**
   * Asserts a {@link CodeableConcept} has an expected {@link Coding}.
   *
   * @param ccwVariable the {@link CcwCodebookVariable} that was mapped
   * @param expectedCode the expected {@link Coding#getCode()}
   * @param actualConcept the FHIR {@link CodeableConcept} to verify
   */
  static void assertHasCoding(
      CcwCodebookInterface ccwVariable, Object expectedCode, CodeableConcept actualConcept) {
    // Jumping through hoops to cope with overloaded method:
    Optional<?> expectedCodeCast =
        expectedCode instanceof Optional ? (Optional<?>) expectedCode : Optional.of(expectedCode);
    assertHasCoding(ccwVariable, expectedCodeCast, actualConcept);
  }

  /**
   * Asserts a {@link CodeableConcept} has an expected {@link Coding}.
   *
   * @param ccwVariable the {@link CcwCodebookVariable} that was mapped
   * @param expectedCode the expected {@link Coding#getCode()}
   * @param actualConcept the FHIR {@link CodeableConcept} to verify
   */
  static void assertHasCoding(
      CcwCodebookInterface ccwVariable, Optional<?> expectedCode, CodeableConcept actualConcept) {
    String expectedCodingSystem = CCWUtils.calculateVariableReferenceUrl(ccwVariable);
    Optional<Coding> codingForSystem =
        actualConcept.getCoding().stream()
            .filter(c -> c.getSystem().equals(expectedCodingSystem))
            .findFirst();

    assertEquals(expectedCode.isPresent(), codingForSystem.isPresent());
    if (expectedCode.isPresent()) {
      assertCodingEquals(expectedCodingSystem, expectedCode.get(), codingForSystem.get());
    }
  }

  /**
   * Asserts a coding has the expected values.
   *
   * @param expectedSystem the expected {@link Coding#getSystem()} value
   * @param expectedCode the expected {@link Coding#getCode()} value
   * @param actual the actual {@link Coding} to verify
   */
  static void assertCodingEquals(String expectedSystem, Object expectedCode, Coding actual) {
    assertCodingEquals(expectedSystem, null, expectedCode, actual);
  }

  /**
   * Asserts a coding has the expected values.
   *
   * @param expectedSystem the expected {@link Coding#getSystem()} value
   * @param expectedVersion the expected {@link Coding#getVersion()} value
   * @param expectedCode the expected {@link Coding#getCode()} value
   * @param actual the actual {@link Coding} to verify
   */
  private static void assertCodingEquals(
      String expectedSystem, String expectedVersion, Object expectedCode, Coding actual) {
    assertEquals(expectedSystem, actual.getSystem());
    assertEquals(expectedVersion, actual.getVersion());

    /*
     * The code parameter is an Object to avoid needing multiple copies of this and
     * related methods. This if-else block is the price to be paid for that, though.
     */
    if (expectedCode instanceof Character) {
      assertEquals(((Character) expectedCode).toString(), actual.getCode());
    } else if (expectedCode instanceof String) {
      assertEquals(((String) expectedCode).trim(), actual.getCode());
    } else {
      throw new BadCodeMonkeyException();
    }
  }

  /**
   * Asserts a quantity has the expected value.
   *
   * @param expectedValue the expected {@link Quantity#getValue()}
   * @param actual the actual {@link Quantity} to verify
   */
  static void assertQuantityEquals(Number expectedValue, Quantity actual) {
    assertNotNull(actual);

    if (expectedValue instanceof BigDecimal) assertEquals(expectedValue, actual.getValue());
    else throw new BadCodeMonkeyException();
  }

  /**
   * Asserts that the adjudication total in an EOB matches the expected value.
   *
   * @param categoryVariable the {@link CcwCodebookVariable} for the {@link Extension#getUrl()} to
   *     find and verify
   * @param expectedAmountValue the expected {@link Extension#getValue()} {@link Money#getValue()}
   *     value to verify
   * @param eob the actual {@link ExplanationOfBenefit} to verify the adjudication total in
   */
  static void assertAdjudicationTotalAmountEquals(
      CcwCodebookVariable categoryVariable,
      Optional<BigDecimal> expectedAmountValue,
      ExplanationOfBenefit eob) {
    String expectedExtensionUrl = CCWUtils.calculateVariableReferenceUrl(categoryVariable);
    Optional<Extension> adjudicationTotalExtension =
        eob.getExtension().stream().filter(e -> expectedExtensionUrl.equals(e.getUrl())).findAny();
    assertEquals(expectedAmountValue.isPresent(), adjudicationTotalExtension.isPresent());

    if (expectedAmountValue.isPresent()) {
      assertNotNull(adjudicationTotalExtension.get().getValue());
      assertMoneyValue(
          expectedAmountValue.get(), (Money) adjudicationTotalExtension.get().getValue());
    }
  }

  /**
   * Asserts that the adjudication total in an EOB matches the expected value.
   *
   * @param categoryVariable the {@link CcwCodebookVariable} for the {@link Extension#getUrl()} to
   *     find and verify
   * @param expectedAmountValue the expected {@link Extension#getValue()} {@link Money#getValue()}
   *     value to verify
   * @param eob the actual {@link ExplanationOfBenefit} to verify the adjudication total in
   */
  static void assertAdjudicationTotalAmountEquals(
      CcwCodebookVariable categoryVariable,
      BigDecimal expectedAmountValue,
      ExplanationOfBenefit eob) {
    assertAdjudicationTotalAmountEquals(categoryVariable, Optional.of(expectedAmountValue), eob);
  }

  /**
   * Asserts the value in a {@link Money} matches the expected value.
   *
   * @param expectedAmountValue the expected {@link Money#getValue()}
   * @param actualValue the actual {@link Money} to verify
   */
  private static void assertMoneyValue(BigDecimal expectedAmountValue, Money actualValue) {
    /*
     * TODO: Money coding? assertEquals(TransformerConstants.CODING_MONEY,
     * actualValue.getSystem());
     * assertEquals(TransformerConstants.CODED_MONEY_USD, actualValue.getCode());
     */
    assertEquivalent(expectedAmountValue, actualValue.getValue());
  }

  /**
   * Verifies that the specific "actual" {@link BigDecimal} value is equivalent to the "expected"
   * value, with no loss of precision or scale.
   *
   * @param expected the "expected" {@link BigDecimal} value
   * @param actual the "actual" {@link BigDecimal} value
   */
  static void assertEquivalent(BigDecimal expected, BigDecimal actual) {
    assertTrue(actual.precision() >= expected.precision());
    assertTrue(actual.scale() >= expected.scale());
    assertEquals(0, expected.compareTo(actual));
  }

  /**
   * Asserts that an extension identifier matches the expected values.
   *
   * @param ccwVariable the {@link CcwCodebookVariable} that was mapped
   * @param expectedValue the expected {@link Identifier#getValue()}
   * @param actualElement the FHIR element to find and verify the {@link Extension} of
   */
  static void assertExtensionIdentifierEquals(
      CcwCodebookInterface ccwVariable, String expectedValue, IBaseHasExtensions actualElement) {
    assertExtensionIdentifierEquals(ccwVariable, Optional.of(expectedValue), actualElement);
  }

  /**
   * Asserts that an extension identifier matches the expected values.
   *
   * @param ccwVariable the {@link CcwCodebookVariable} that the expected {@link Extension} / {@link
   *     Coding} are for
   * @param expectedValue the expected {@link Quantity#getValue()}
   * @param actualElement the FHIR element to find and verify the {@link Extension} of
   */
  static void assertExtensionIdentifierEquals(
      CcwCodebookInterface ccwVariable,
      Optional<String> expectedValue,
      IBaseHasExtensions actualElement) {
    String expectedExtensionUrl = CCWUtils.calculateVariableReferenceUrl(ccwVariable);
    Optional<? extends IBaseExtension<?, ?>> extensionForUrl =
        actualElement.getExtension().stream()
            .filter(e -> e.getUrl().equals(expectedExtensionUrl))
            .findFirst();

    assertEquals(expectedValue.isPresent(), extensionForUrl.isPresent());
    if (expectedValue.isPresent())
      assertIdentifierEquals(
          ccwVariable, expectedValue.get(), (Identifier) extensionForUrl.get().getValue());
  }

  /**
   * Asserts that an extension quantity matches the expected values.
   *
   * @param ccwVariable the {@link CcwCodebookVariable} that was mapped
   * @param expectedValue the expected {@link Quantity#getValue()}
   * @param actualElement the FHIR element to find and verify the {@link Extension} of
   */
  // FIXME rename this and friends to include "Value"
  static void assertExtensionQuantityEquals(
      CcwCodebookInterface ccwVariable, Number expectedValue, IBaseHasExtensions actualElement) {
    assertExtensionQuantityEquals(ccwVariable, Optional.of(expectedValue), actualElement);
  }

  /**
   * Asserts that an extension quantity matches the expected value.
   *
   * @param ccwVariable the {@link CcwCodebookVariable} that the expected {@link Extension} / {@link
   *     Coding} are for
   * @param expectedValue the expected {@link Quantity#getValue()}
   * @param actualElement the FHIR element to find and verify the {@link Extension} of
   */
  static void assertExtensionQuantityEquals(
      CcwCodebookInterface ccwVariable,
      Optional<? extends Number> expectedValue,
      IBaseHasExtensions actualElement) {
    String expectedExtensionUrl = CCWUtils.calculateVariableReferenceUrl(ccwVariable);
    Optional<? extends IBaseExtension<?, ?>> extensionForUrl =
        actualElement.getExtension().stream()
            .filter(e -> e.getUrl().equals(expectedExtensionUrl))
            .findFirst();

    assertEquals(expectedValue.isPresent(), extensionForUrl.isPresent());
    if (expectedValue.isPresent())
      assertQuantityEquals(expectedValue.get(), (Quantity) extensionForUrl.get().getValue());
  }

  /**
   * Verifies that the Item Component has an extension with the extension url that is passed in.
   *
   * @param ccwVariable the expected {@link CcwCodebookInterface}
   * @param expectedValue the expected {@link BigDecimal}
   * @param itemComponents the FHIR element to find and verify the {@link ItemComponent} list of
   */
  static void assertExtensionQuantityEquals(
      CcwCodebookInterface ccwVariable,
      BigDecimal expectedValue,
      List<ItemComponent> itemComponents) {

    String expectedExtensionUrl = CCWUtils.calculateVariableReferenceUrl(ccwVariable);
    Optional<Extension> returnExtension =
        itemComponents.stream()
            .flatMap(ic -> ic.getExtension().stream())
            .filter(ext -> ext.getUrl().equals(expectedExtensionUrl))
            .findFirst();

    if (returnExtension.isPresent()) {
      Quantity quantity = (Quantity) returnExtension.get().getValue();
      assertEquals(expectedValue, quantity.getValue());
    }
  }

  /**
   * Uses the setters of the specified record to set all {@link Optional} fields to {@link
   * Optional#empty()}.
   *
   * @param record the record to modify
   */
  static void setAllOptionalsToEmpty(Object record) {
    try {
      for (Method method : record.getClass().getMethods()) {
        if (!method.getName().startsWith("set")) continue;
        if (method.getParameterTypes().length != 1) continue;
        if (!method.getParameterTypes()[0].equals(Optional.class)) continue;

        method.invoke(record, Optional.empty());
      }
    } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Test that the resource being tested has a matching lastUpdated.
   *
   * @param expectedDateTime from the entity
   * @param actualResource that is being created by the transform
   */
  static void assertLastUpdatedEquals(
      Optional<Instant> expectedDateTime, IAnyResource actualResource) {
    if (expectedDateTime.isPresent()) {
      /*
       * Dev Note: We often run our tests in parallel, so there is subtle race
       * condition because we
       * use one instance of an IT DB with the same resources for most tests.
       * The actual resources a test finds may have a lastUpdated value slightly after
       * the time the test wrote it
       * because another test over wrote the same resource.
       * To handle this case, dates that are within a second of each other match.
       */
      final Instant expectedLastUpdated = expectedDateTime.get();
      final Instant actualLastUpdated = actualResource.getMeta().getLastUpdated().toInstant();
      final Duration diff = Duration.between(expectedLastUpdated, actualLastUpdated);
      assertTrue(
          diff.compareTo(Duration.ofSeconds(10)) <= 0,
          "Expect the actual lastUpdated to be equal or after the loaded resources");
    } else {
      assertEquals(
          TransformerConstants.FALLBACK_LAST_UPDATED,
          actualResource.getMeta().getLastUpdated().toInstant(),
          "Expect lastUpdated to be the fallback value");
    }
  }

  /**
   * Test the transformation of the common group level data elements between the {@link
   * CarrierClaim} and {@link DMEClaim} claim types to FHIR. The method parameter fields from {@link
   * CarrierClaim} and {@link DMEClaim} are listed below and their corresponding RIF CCW fields
   * (denoted in all CAPS below from {@link CarrierClaimColumn} and {@link DMEClaimColumn}).
   *
   * @param eob the {@link ExplanationOfBenefit} to test
   * @param beneficiaryId BENE_ID,
   * @param carrierNumber CARR_NUM,
   * @param clinicalTrialNumber CLM_CLNCL_TRIL_NUM,
   * @param beneficiaryPartBDeductAmount CARR_CLM_CASH_DDCTBL_APLD_AMT,
   * @param paymentDenialCode CARR_CLM_PMT_DNL_CD,
   * @param referringPhysicianNpi RFR_PHYSN_NPI,
   * @param providerAssignmentIndicator CARR_CLM_PRVDR_ASGNMT_IND_SW,
   * @param providerPaymentAmount NCH_CLM_PRVDR_PMT_AMT,
   * @param beneficiaryPaymentAmount NCH_CLM_BENE_PMT_AMT,
   * @param submittedChargeAmount NCH_CARR_CLM_SBMTD_CHRG_AMT,
   * @param allowedChargeAmount NCH_CARR_CLM_ALOWD_AMT,
   */
  static void assertEobCommonGroupCarrierDMEEquals(
      ExplanationOfBenefit eob,
      String beneficiaryId,
      String carrierNumber,
      Optional<String> clinicalTrialNumber,
      BigDecimal beneficiaryPartBDeductAmount,
      String paymentDenialCode,
      Optional<String> referringPhysicianNpi,
      Optional<Character> providerAssignmentIndicator,
      BigDecimal providerPaymentAmount,
      BigDecimal beneficiaryPaymentAmount,
      BigDecimal submittedChargeAmount,
      BigDecimal allowedChargeAmount) {

    assertExtensionCodingEquals(CcwCodebookVariable.CARR_CLM_PMT_DNL_CD, paymentDenialCode, eob);

    /*
     * ReferralRequest referral = (ReferralRequest) eob.getReferral().getResource();
     * assertEquals(
     * TransformerUtilsV2.referencePatient(beneficiaryId).getReference(),
     * referral.getSubject().getReference());
     * assertReferenceIdentifierEquals(
     * TransformerConstants.CODING_NPI_US,
     * referringPhysicianNpi.get(),
     * referral.getRequester().getAgent());
     * assertEquals(1, referral.getRecipient().size());
     * assertReferenceIdentifierEquals(
     * TransformerConstants.CODING_NPI_US,
     * referringPhysicianNpi.get(),
     * referral.getRecipientFirstRep());
     */
    assertExtensionCodingEquals(CcwCodebookVariable.ASGMNTCD, providerAssignmentIndicator, eob);

    assertExtensionIdentifierEquals(CcwCodebookVariable.CARR_NUM, carrierNumber, eob);
    assertExtensionIdentifierEquals(
        CcwCodebookVariable.CLM_CLNCL_TRIL_NUM, clinicalTrialNumber, eob);
    assertAdjudicationTotalAmountEquals(
        CcwCodebookVariable.CARR_CLM_CASH_DDCTBL_APLD_AMT, beneficiaryPartBDeductAmount, eob);
    assertAdjudicationTotalAmountEquals(
        CcwCodebookVariable.NCH_CLM_PRVDR_PMT_AMT, providerPaymentAmount, eob);
    assertAdjudicationTotalAmountEquals(
        CcwCodebookVariable.NCH_CLM_BENE_PMT_AMT, beneficiaryPaymentAmount, eob);
    assertAdjudicationTotalAmountEquals(
        CcwCodebookVariable.NCH_CARR_CLM_SBMTD_CHRG_AMT, submittedChargeAmount, eob);
    assertAdjudicationTotalAmountEquals(
        CcwCodebookVariable.NCH_CARR_CLM_ALOWD_AMT, allowedChargeAmount, eob);
  }

  /**
   * Assert that two periods are equal.
   *
   * @param expectedStartDate the expected value for {@link Period#getStart()}
   * @param expectedEndDate the expected value for {@link Period#getEnd()}
   * @param actualPeriod the {@link Period} to verify
   */
  static void assertPeriodEquals(
      Optional<LocalDate> expectedStartDate,
      Optional<LocalDate> expectedEndDate,
      Period actualPeriod) {
    assertTrue(expectedStartDate.isPresent() || expectedEndDate.isPresent());
    if (expectedStartDate.isPresent())
      assertDateEquals(expectedStartDate.get(), actualPeriod.getStartElement());
    if (expectedEndDate.isPresent())
      assertDateEquals(expectedEndDate.get(), actualPeriod.getEndElement());
  }

  /**
   * Finds an {@link Identifier} in a list based on the System URL.
   *
   * @param system the system
   * @param identifiers the identifiers
   * @return the identifier
   */
  static Identifier findIdentifierBySystem(String system, List<Identifier> identifiers) {
    Optional<Identifier> id =
        identifiers.stream().filter(i -> system.equals(i.getSystem())).findFirst();

    assertTrue(id.isPresent());

    return id.get();
  }

  /**
   * Creates an {@link Identifier} to be used in tests.
   *
   * @param system the system
   * @param value the value
   * @param codeSystem the code system
   * @param code the code
   * @param codeDisplay the code display
   * @return the identifier
   */
  static Identifier createIdentifier(
      String system, String value, String codeSystem, String code, String codeDisplay) {
    return new Identifier()
        .setType(
            new CodeableConcept()
                .setCoding(Arrays.asList(new Coding(codeSystem, code, codeDisplay))))
        .setSystem(system)
        .setValue(value);
  }

  /**
   * Finds an {@link Extension} in a list based on the Extension URL.
   *
   * @param url the url
   * @param extensions the extensions
   * @return the extension
   */
  static Extension findExtensionByUrl(String url, List<Extension> extensions) {
    Optional<Extension> ex = extensions.stream().filter(e -> url.equals(e.getUrl())).findFirst();

    assertTrue(ex.isPresent());

    return ex.get();
  }

  /**
   * Finds an {@link Extension} in a list based on the Extension URL and System URL.
   *
   * @param url the expected {@link Extension#getUrl()} of the {@link Extension} to look for
   * @param system the expected {@link Coding#getSystem()} value
   * @param extensions the list of extensions to filter through
   * @return the extension
   */
  static Extension findExtensionByUrlAndSystem(
      String url, String system, List<Extension> extensions) {

    Coding cod =
        extensions.stream()
            .filter(e -> e.getValue() instanceof Coding)
            .map(e -> (Coding) e.getValue())
            .filter(e -> system.equals(e.getSystem()))
            .findFirst()
            .get();

    Optional<Extension> ex =
        extensions.stream()
            .filter(e -> url.equals(e.getUrl()) && e.getValue().equalsDeep(cod))
            .findFirst();

    assertTrue(ex.isPresent());

    return ex.get();
  }

  /**
   * Finds a specific {@link Coding} in a list given the system.
   *
   * @param system the system
   * @param codings the codings
   * @return the coding
   */
  static Coding findCodingBySystem(String system, List<Coding> codings) {
    Optional<Coding> coding =
        codings.stream().filter(c -> system.equals(c.getSystem())).findFirst();

    assertTrue(coding.isPresent());

    return coding.get();
  }

  /**
   * Finds a Care Team member by Sequence value.
   *
   * @param seq the seq
   * @param team the team
   * @return the care team component
   */
  static CareTeamComponent findCareTeamBySequence(int seq, List<CareTeamComponent> team) {
    Optional<CareTeamComponent> ctc = team.stream().filter(c -> c.getSequence() == seq).findFirst();

    assertTrue(ctc.isPresent());

    return ctc.get();
  }

  /**
   * Helper that creates a {@link CareTeamComponent} to be used in unit tests.
   *
   * @param sequence The sequence to set
   * @param npi The NPI for the member
   * @param system System defining the type of member
   * @param code Code defining the type of member
   * @param display Display for the type of member
   * @return {@link CareTeamComponent}
   */
  static CareTeamComponent createNpiCareTeamMember(
      int sequence, String npi, String system, String code, String display) {
    return createNpiCareTeamMember(sequence, npi, system, code, display, null, null);
  }

  /**
   * Helper that creates a {@link CareTeamComponent} to be used in unit tests.
   *
   * @param sequence The sequence to set
   * @param npi The NPI for the member
   * @param system System defining the type of member
   * @param code Code defining the type of member
   * @param display Display for the type of member
   * @param taxonomyCode The taxonomy code.
   * @return {@link CareTeamComponent}
   */
  static CareTeamComponent createNpiCareTeamMember(
      int sequence,
      String npi,
      String system,
      String code,
      String display,
      String taxonomyCode,
      String taxonomyDisplay) {
    CareTeamComponent component = new CareTeamComponent();
    component
        .setSequence(sequence)
        .setProvider(
            new Reference()
                .setIdentifier(
                    createIdentifier(
                        null,
                        npi,
                        "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBIdentifierType",
                        "npi",
                        "National Provider Identifier")))
        .setRole(new CodeableConcept().setCoding(Arrays.asList(new Coding(system, code, display))));
    if (taxonomyCode != null) {
      component.setQualification(
          new CodeableConcept(
              new Coding()
                  .setCode(taxonomyCode)
                  .setDisplay(taxonomyDisplay)
                  .setSystem("http://nucc.org/provider-taxonomy")));
    }
    return component;
  }

  /**
   * Finds a {@link SupportingInformationComponent} based on the value of a Code of the Category.
   *
   * @param code the code
   * @param components the components
   * @return supporting information component
   */
  static SupportingInformationComponent findSupportingInfoByCode(
      String code, List<SupportingInformationComponent> components) {
    Optional<SupportingInformationComponent> si =
        components.stream()
            .filter(
                cmp ->
                    cmp.getCategory().getCoding().stream()
                            .filter(c -> code.equals(c.getCode()))
                            .count()
                        > 0)
            .findFirst();

    assertTrue(si.isPresent());

    return si.get();
  }

  /**
   * Helper to create a {@link SupportingInformationComponent}.
   *
   * @param sequence The sequence number to set
   * @param category A list of {@link Coding} elements to use for Category
   * @param code A Coding to use for Code
   * @return supporting information component
   */
  static SupportingInformationComponent createSupportingInfo(
      int sequence, List<Coding> category, Coding code) {
    return new SupportingInformationComponent()
        .setSequence(sequence)
        .setCategory(new CodeableConcept().setCoding(category))
        .setCode(new CodeableConcept().setCoding(Arrays.asList(code)));
  }

  /**
   * Helper to create a {@link SupportingInformationComponent}.
   *
   * @param sequence The sequence number to set
   * @param category A list of {@link Coding} elements to use for Category
   * @return supporting information component
   */
  static SupportingInformationComponent createSupportingInfo(int sequence, List<Coding> category) {
    return new SupportingInformationComponent()
        .setSequence(sequence)
        .setCategory(new CodeableConcept().setCoding(category));
  }

  /**
   * Finds a {@link DiagnosisComponent} in a list based on the coding of the diagnosis.
   *
   * @param code the code
   * @param components the components
   * @return diagnosis component
   */
  static DiagnosisComponent findDiagnosisByCode(String code, List<DiagnosisComponent> components) {
    Optional<DiagnosisComponent> diag =
        components.stream()
            .filter(
                cmp ->
                    cmp
                            .getDiagnosis()
                            .castToCodeableConcept(cmp.getDiagnosis())
                            .getCoding()
                            .stream()
                            .filter(c -> code.equals(c.getCode()))
                            .count()
                        > 0)
            .findFirst();

    assertTrue(diag.isPresent());

    return diag.get();
  }

  /**
   * Helper that creates a {@link DiagnosisComponent} for testing.
   *
   * @param seq The sequence number
   * @param codes A list of codings to use for the Diagnosis CodeableConcept
   * @param type A coding to use for the Diagnosis Type
   * @param poasw Nullable - The increment for the "Present on Admission" extension
   * @param poaval Nullable - The type for the "Present on Admission" extension
   * @param poa Nullable - The Code to set for "Present on Admission" ("Y" or "N")
   * @return diagnosis component
   */
  static DiagnosisComponent createDiagnosis(
      int seq, List<Coding> codes, Coding type, Integer poasw, String poaval, String poa) {
    DiagnosisComponent diag =
        new DiagnosisComponent()
            .setSequence(seq)
            .setDiagnosis(new CodeableConcept().setCoding(codes))
            .setType(Arrays.asList(new CodeableConcept().setCoding(Arrays.asList(type))));

    if (poasw != null) {
      diag.addExtension()
          .setUrl("https://bluebutton.cms.gov/resources/variables/" + poa + poasw)
          .setValue(
              new Coding(
                  "https://bluebutton.cms.gov/resources/variables/" + poa + poasw,
                  poaval,
                  "Y".equals(poaval)
                      ? "Diagnosis was present at the time of admission (POA)"
                      : "Diagnosis was not present at the time of admission"));
    }

    return diag;
  }

  /**
   * Creates a {@link DiagnosisComponent} using the "clm_poa_ind_sw" type.
   *
   * @param seq the seq
   * @param codes the codes
   * @param type the type
   * @param poasw the poasw
   * @param poaval the poaval
   * @return the diagnosis component
   */
  static DiagnosisComponent createDiagnosis(
      int seq, List<Coding> codes, Coding type, Integer poasw, String poaval) {
    return createDiagnosis(seq, codes, type, poasw, poaval, "clm_poa_ind_sw");
  }

  /**
   * Creates a {@link DiagnosisComponent} using the "clm_e_poa_ind_sw" (external) type.
   *
   * @param seq the seq
   * @param codes the codes
   * @param type the type
   * @param poasw the poasw
   * @param poaval the poaval
   * @return the diagnosis component
   */
  static DiagnosisComponent createExDiagnosis(
      int seq, List<Coding> codes, Coding type, Integer poasw, String poaval) {
    return createDiagnosis(seq, codes, type, poasw, poaval, "clm_e_poa_ind_sw");
  }

  /**
   * Finds a {@link ProcedureComponent} in a list, based on a code in the Procedure's
   * CodeableConcept.
   *
   * @param code the code
   * @param components the components
   * @return procedure component
   */
  static ProcedureComponent findProcedureByCode(String code, List<ProcedureComponent> components) {
    Optional<ProcedureComponent> proc =
        components.stream()
            .filter(
                cmp ->
                    cmp.getProcedureCodeableConcept().getCoding().stream()
                            .filter(c -> code.equals(c.getCode()))
                            .count()
                        > 0)
            .findFirst();

    assertTrue(proc.isPresent());

    return proc.get();
  }

  /**
   * Creates a {@link ProcedureComponent} for use in testing.
   *
   * @param seq The sequence number to set
   * @param codes A List of {@link Coding}s to set to the procedureCodeableConcept
   * @param date A String date when the procedure was performed
   * @return procedure component
   */
  static ProcedureComponent createProcedure(int seq, List<Coding> codes, String date) {
    // The CCW Procedure extraction uses a LocalDate and converts it to Date
    LocalDate ldate =
        LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX"));

    return new ProcedureComponent()
        .setSequence(seq)
        .setProcedure(new CodeableConcept().setCoding(codes))
        .setDate(convertToDate(ldate));
  }

  /**
   * Finds an {@link AdjudicationComponent} using a code in the category.
   *
   * @param code the code
   * @param components the components
   * @return adjudication component
   */
  static AdjudicationComponent findAdjudicationByCategory(
      String code, List<AdjudicationComponent> components) {
    Optional<AdjudicationComponent> adjudication =
        components.stream()
            .filter(
                cmp ->
                    cmp.getCategory().getCoding().stream()
                            .filter(c -> code.equals(c.getCode()))
                            .count()
                        > 0)
            .findFirst();

    assertTrue(adjudication.isPresent());

    return adjudication.get();
  }

  /**
   * Finds an {@link AdjudicationComponent} using a code in the category and value in amount.
   *
   * @param code the code
   * @param amount the amount
   * @param components the components
   * @return adjudication component
   */
  static AdjudicationComponent findAdjudicationByCategoryAndAmount(
      String code, BigDecimal amount, List<AdjudicationComponent> components) {
    final BigDecimal amt = amount.setScale(2, RoundingMode.HALF_DOWN);

    Optional<AdjudicationComponent> adjudication =
        components.stream()
            .filter(cmp -> (amt.equals(cmp.getAmount().getValue())))
            .filter(
                cmp ->
                    cmp.getCategory().getCoding().stream()
                            .filter(c -> code.equals(c.getCode()))
                            .count()
                        > 0)
            .findFirst();

    assertTrue(adjudication.isPresent());

    return adjudication.get();
  }

  /**
   * Finds an {@link AdjudicationComponent} using a code in the reason.
   *
   * @param code the code
   * @param components the components
   * @return adjudication component
   */
  static AdjudicationComponent findAdjudicationByReason(
      String code, List<AdjudicationComponent> components) {
    Optional<AdjudicationComponent> adjudication =
        components.stream()
            .filter(
                cmp ->
                    cmp.getReason().getCoding().stream()
                            .filter(c -> code.equals(c.getCode()))
                            .count()
                        > 0)
            .findFirst();

    assertTrue(adjudication.isPresent());

    return adjudication.get();
  }

  /**
   * Finds a {@link BenefitComponent} in a list based on a Code in the component's Type.
   *
   * @param code the code
   * @param components the components
   * @return benefit component
   */
  static BenefitComponent findFinancial(String code, List<BenefitComponent> components) {
    Optional<BenefitComponent> benefit =
        components.stream()
            .filter(
                cmp ->
                    cmp.getType().getCoding().stream().filter(c -> code.equals(c.getCode())).count()
                        > 0)
            .findFirst();

    assertTrue(benefit.isPresent());

    return benefit.get();
  }

  /**
   * Assert extension coding does not exist.
   *
   * @param ccwVariable the {@link CcwCodebookVariable} that the expected {@link Extension} / {@link
   *     Coding} are for
   * @param actualElement the FHIR element to find and verify the {@link Extension} of
   */
  static void assertExtensionCodingDoesNotExist(
      CcwCodebookInterface ccwVariable, IBaseHasExtensions actualElement) {
    String expectedExtensionUrl = CCWUtils.calculateVariableReferenceUrl(ccwVariable);
    Optional<? extends IBaseExtension<?, ?>> extensionForUrl =
        actualElement.getExtension().stream()
            .filter(e -> e.getUrl().equals(expectedExtensionUrl))
            .findFirst();

    assertEquals(false, extensionForUrl.isPresent());
  }

  /**
   * Transform rif record to eob explanation of benefit.
   *
   * @param metricRegistry the {@link MetricRegistry} to use
   * @param rifRecord the RIF record (e.g. a {@link CarrierClaim} instance) to transform@param
   *     includeTaxNumbers whether to include tax numbers in the result (see {@link
   *     ExplanationOfBenefitResourceProvider#HEADER_NAME_INCLUDE_TAX_NUMBERS}, defaults to <code>
   *          false</code> )
   * @param includeTaxNumbers if tax numbers should be included in the response
   * @param securityTagManager SamhsaSecurityTags lookup
   * @return the transformed {@link ExplanationOfBenefit} for the specified RIF record
   */
  static ExplanationOfBenefit transformRifRecordToEob(
      ClaimWithSecurityTags<?> rifRecord,
      MetricRegistry metricRegistry,
      boolean includeTaxNumbers,
      SecurityTagManager securityTagManager) {

    ClaimTransformerInterfaceV2 claimTransformerInterface = null;
    Object entity = rifRecord.getClaimEntity();
    if (entity instanceof CarrierClaim) {
      claimTransformerInterface =
          new CarrierClaimTransformerV2(metricRegistry, securityTagManager, false);
    } else if (entity instanceof DMEClaim) {
      claimTransformerInterface =
          new DMEClaimTransformerV2(metricRegistry, securityTagManager, false);
    } else if (entity instanceof HHAClaim) {
      claimTransformerInterface =
          new HHAClaimTransformerV2(metricRegistry, securityTagManager, false);
    } else if (entity instanceof HospiceClaim) {
      claimTransformerInterface =
          new HospiceClaimTransformerV2(metricRegistry, securityTagManager, false);
    } else if (entity instanceof InpatientClaim) {
      claimTransformerInterface =
          new InpatientClaimTransformerV2(metricRegistry, securityTagManager, false);
    } else if (entity instanceof OutpatientClaim) {
      claimTransformerInterface =
          new OutpatientClaimTransformerV2(metricRegistry, securityTagManager, false);
    } else if (entity instanceof PartDEvent) {
      claimTransformerInterface = new PartDEventTransformerV2(metricRegistry);
    } else if (entity instanceof SNFClaim) {
      claimTransformerInterface =
          new SNFClaimTransformerV2(metricRegistry, securityTagManager, false);
    } else {
      throw new BadCodeMonkeyException("Unhandled RifRecord type!");
    }
    return claimTransformerInterface.transform(rifRecord, includeTaxNumbers);
  }
}
