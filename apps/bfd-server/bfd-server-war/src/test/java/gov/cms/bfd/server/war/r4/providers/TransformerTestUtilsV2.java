package gov.cms.bfd.server.war.r4.providers;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import gov.cms.bfd.model.codebook.data.CcwCodebookVariable;
import gov.cms.bfd.model.codebook.model.CcwCodebookInterface;
import gov.cms.bfd.server.war.commons.MedicareSegment;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import gov.cms.bfd.server.war.commons.carin.C4BBClaimProfessionalAndNonClinicianCareTeamRole;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
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
import org.hl7.fhir.r4.model.ExplanationOfBenefit.CareTeamComponent;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.ItemComponent;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.SupportingInformationComponent;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Money;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.junit.Assert;

public final class TransformerTestUtilsV2 {
  /* Do this very slow operation once */
  private static final FhirContext fhirContext = FhirContext.forR4();
  /**
   * Test the transformation of common group level header fields between all claim types
   *
   * @param eob the {@link ExplanationOfBenefit} to modify
   * @param claimId CLM_ID
   * @param beneficiaryId BENE_ID
   * @param claimType {@link gov.cms.bfd.server.war.stu3.providers.ClaimType} to process
   * @param claimGroupId CLM_GRP_ID
   * @param coverageType {@link gov.cms.bfd.server.war.stu3.providers.MedicareSegment}
   * @param dateFrom CLM_FROM_DT
   * @param dateThrough CLM_THRU_DT
   * @param paymentAmount CLM_PMT_AMT
   * @param finalAction FINAL_ACTION
   */
  static void assertEobCommonClaimHeaderData(
      ExplanationOfBenefit eob,
      String claimId,
      String beneficiaryId,
      ClaimTypeV2 claimType,
      String claimGroupId,
      MedicareSegment coverageType,
      Optional<LocalDate> dateFrom,
      Optional<LocalDate> dateThrough,
      Optional<BigDecimal> paymentAmount,
      char finalAction) {

    assertNoEncodedOptionals(eob);

    Assert.assertEquals(
        TransformerUtilsV2.buildEobId(claimType, claimId), eob.getIdElement().getIdPart());

    if (claimType.equals(ClaimTypeV2.PDE)) {
      assertHasIdentifier(CcwCodebookVariable.PDE_ID, claimId, eob.getIdentifier());
    } else {
      assertHasIdentifier(CcwCodebookVariable.CLM_ID, claimId, eob.getIdentifier());
    }

    assertIdentifierExists(
        TransformerConstants.IDENTIFIER_SYSTEM_BBAPI_CLAIM_GROUP_ID,
        claimGroupId,
        eob.getIdentifier());

    Assert.assertEquals(
        TransformerUtilsV2.referencePatient(beneficiaryId).getReference(),
        eob.getPatient().getReference());

    Assert.assertEquals(
        TransformerUtilsV2.referenceCoverage(beneficiaryId, coverageType).getReference(),
        eob.getInsuranceFirstRep().getCoverage().getReference());

    switch (finalAction) {
      case 'F':
        Assert.assertEquals("active", eob.getStatus().toCode());
        break;
      case 'N':
        Assert.assertEquals("cancelled", eob.getStatus().toCode());
        break;
      default:
        throw new BadCodeMonkeyException();
    }

    if (paymentAmount.isPresent()) {
      Assert.assertEquals(paymentAmount.get(), eob.getPayment().getAmount().getValue());
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
    Assert.assertFalse(encodedResourceXml.contains("Optional"));
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

    Assert.assertTrue("Bad reference: " + reference, reference.hasIdentifier());
    Assert.assertEquals(TransformerConstants.CODING_NPI_US, reference.getIdentifier().getSystem());
    Assert.assertEquals(providerNumber.get(), reference.getIdentifier().getValue());
  }

  /**
   * @param expectedSystem the expected {@link Identifier#getSystem()} value
   * @param expectedId the expected {@link Identifier#getValue()} value
   * @param actuals the actual {@link Identifier} to verify
   */
  static void assertIdentifierExists(
      String expectedSystem, String expectedId, List<Identifier> actuals) {
    Assert.assertTrue(
        actuals.stream()
            .filter(i -> expectedSystem.equals(i.getSystem()))
            .anyMatch(i -> expectedId.equals(i.getValue())));
  }

  /**
   * @param expectedIdentifierSystem the expected {@link Identifier#getSystem()} value
   * @param expectedIdentifierValue the expected {@link Identifier#getValue()} value
   * @param reference the actual {@link Reference} to verify
   */
  static void assertReferenceIdentifierEquals(
      String expectedIdentifierSystem, String expectedIdentifierValue, Reference reference) {
    Assert.assertTrue("Bad reference: " + reference, reference.hasIdentifier());
    Assert.assertEquals(expectedIdentifierSystem, reference.getIdentifier().getSystem());
    Assert.assertEquals(expectedIdentifierValue, reference.getIdentifier().getValue());
    Assert.assertEquals(
        TransformerUtilsV2.retrieveNpiCodeDisplay(expectedIdentifierValue), reference.getDisplay());
  }

  /**
   * @param ccwVariable the {@link CcwCodebookVariable} that was mapped
   * @param expectedIdentifierValue the expected {@link Identifier#getValue()} of the {@link
   *     Reference#getIdentifier()}
   * @param actualReference the actual {@link Reference} to verify
   */
  private static void assertReferenceIdentifierEquals(
      CcwCodebookInterface ccwVariable, String expectedIdentifierValue, Reference actualReference) {
    Assert.assertTrue("Bad reference: " + actualReference, actualReference.hasIdentifier());
    Assert.assertEquals(
        TransformerUtilsV2.calculateVariableReferenceUrl(ccwVariable),
        actualReference.getIdentifier().getSystem());
    Assert.assertEquals(expectedIdentifierValue, actualReference.getIdentifier().getValue());
  }

  /**
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
   * FIXME change name of this and related methods to assertHasExtensionCoding(...)
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
    String expectedExtensionUrl = TransformerUtilsV2.calculateVariableReferenceUrl(ccwVariable);
    String expectedCodingSystem = expectedExtensionUrl;
    Optional<? extends IBaseExtension<?, ?>> extensionForUrl =
        actualElement.getExtension().stream()
            .filter(e -> e.getUrl().equals(expectedExtensionUrl))
            .findFirst();

    Assert.assertEquals(expectedCode.isPresent(), extensionForUrl.isPresent());
    if (expectedCode.isPresent())
      assertCodingEquals(
          expectedCodingSystem, expectedCode.get(), (Coding) extensionForUrl.get().getValue());
  }

  /**
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
   * @param ccwVariable the {@link CcwCodebookVariable} that was mapped
   * @param expectedValue the expected {@link Identifier#getValue()} value
   * @param actualIdentifiers the actual {@link Identifier}s to verify a match can be found within
   */
  private static void assertHasIdentifier(
      CcwCodebookInterface ccwVariable, String expectedValue, List<Identifier> actualIdentifiers) {
    if (expectedValue == null) throw new IllegalArgumentException();

    Assert.assertNotNull(actualIdentifiers);

    String expectedSystem = TransformerUtilsV2.calculateVariableReferenceUrl(ccwVariable);
    Optional<Identifier> matchingIdentifier =
        actualIdentifiers.stream()
            .filter(i -> expectedSystem.equals(i.getSystem()))
            .filter(i -> expectedValue.equals(i.getValue()))
            .findAny();
    Assert.assertTrue(matchingIdentifier.isPresent());
  }

  /**
   * @param expectedSystem the expected {@link Coding#getSystem()} value
   * @param expectedCode the expected {@link Coding#getCode()} value
   * @param actualCode the actual {@link Coding} to verify
   */
  static void assertHasCoding(String expectedSystem, String expectedCode, Coding actualCode) {
    assertHasCoding(expectedSystem, null, null, expectedCode, Arrays.asList(actualCode));
  }

  /**
   * @param expectedSystem the expected {@link Coding#getSystem()} value
   * @param expectedCode the expected {@link Coding#getCode()} value
   * @param actualCode the actual {@link List}&lt;{@link Coding}&gt; to verify
   */
  static void assertHasCoding(String expectedSystem, String expectedCode, List<Coding> actualCode) {
    assertHasCoding(expectedSystem, null, null, expectedCode, actualCode);
  }

  /**
   * @param expectedSystem the expected {@link Coding#getSystem()} value
   * @param expectedVersion the expected {@link Coding#getVersion()} value
   * @param expectedVersion the expected {@link Coding#getDisplay()} value
   * @param expectedCode the expected {@link Coding#getCode()} value
   * @param actualCode the actual {@link List}&lt;{@link Coding}&gt; to verify
   */
  static void assertHasCoding(
      String expectedSystem,
      String expectedVersion,
      String expectedDisplay,
      String expectedCode,
      List<Coding> actualCode) {
    Assert.assertTrue(
        "No matching Coding found: " + actualCode.toString(),
        actualCode.stream()
            .anyMatch(
                c -> {
                  if (!expectedSystem.equals(c.getSystem())
                      || (expectedVersion != null && !expectedVersion.equals(c.getVersion()))
                      || (expectedDisplay != null && !expectedDisplay.equals(c.getDisplay()))
                      || !expectedCode.equals(c.getCode())) {
                    return false;
                  }
                  return true;
                }));
  }

  /**
   * @param ccwVariable the {@link CcwCodebookVariable} that was mapped
   * @param expectedValue the expected {@link Identifier#getValue()} value
   * @param actual the actual {@link Identifier} to verify
   */
  static void assertIdentifierEquals(
      CcwCodebookInterface ccwVariable, String expectedValue, Identifier actual) {
    if (expectedValue == null) throw new IllegalArgumentException();

    Assert.assertNotNull(actual);
    Assert.assertEquals(
        TransformerUtilsV2.calculateVariableReferenceUrl(ccwVariable), actual.getSystem());
    Assert.assertEquals(expectedValue, actual.getValue());
  }

  /**
   * @param expected the expected {@link LocalDate}
   * @param actual the actual {@link BaseDateTimeType} to verify
   */
  static void assertDateEquals(LocalDate expected, BaseDateTimeType actual) {
    Assert.assertEquals(
        Date.from(expected.atStartOfDay(ZoneId.systemDefault()).toInstant()), actual.getValue());
    Assert.assertEquals(TemporalPrecisionEnum.DAY, actual.getPrecision());
  }

  /**
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
        TransformerConstants.CODING_NPI_US, expectedProviderNpi, null, careTeam);
  }

  /**
   * @param expectedIdentifierSystem the {@link Identifier#getSystem()} of the provider to find a
   *     matching {@link CareTeamComponent} for
   * @param expectedIdentifierValue the {@link Identifier#getValue()} of the provider to find a
   *     matching {@link CareTeamComponent} for
   * @param careTeam the {@link List} of {@link CareTeamComponent}s to search
   * @return the {@link CareTeamComponent} whose {@link CareTeamComponent#getProvider()} is an
   *     {@link Identifier} with the specified provider NPI, or else <code>null</code> if no such
   *     {@link CareTeamComponent} was found
   */
  private static CareTeamComponent findCareTeamEntryForProviderIdentifier(
      String expectedIdentifierSystem,
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
   * @param expectedIdentifierSystem the expected {@link Identifier#getSystem()} to match
   * @param expectedIdentifierValue the expected {@link Identifier#getValue()} to match
   * @param actualReference the {@link Reference} to check
   * @return <code>true</code> if the specified {@link Reference} matches the expected {@link
   *     Identifier}
   */
  private static boolean doesReferenceMatchIdentifier(
      String expectedIdentifierSystem, String expectedIdentifierValue, Reference actualReference) {
    if (!actualReference.hasIdentifier()) return false;
    return expectedIdentifierSystem.equals(actualReference.getIdentifier().getSystem())
        && expectedIdentifierValue.equals(actualReference.getIdentifier().getValue());
  }

  /**
   * Does the role of the care team component match what is expected
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
   * @param hcpcCode the {@link Optional}&lt;{@link String}&gt; HCPCS_CD: representing the hcpcs
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

    Assert.assertFalse(hcpcsSecondModifierCode.isPresent());
  }

  /**
   * Test the transformation of the item level data elements between the {@link CarrierClaimLine}
   * and {@link DMEClaimLine} claim types to FHIR. The method parameter fields from {@link
   * CarrierClaimLine} and {@link DMEClaimLine} are listed below and their corresponding RIF CCW
   * fields (denoted in all CAPS below from {@link CarrierClaimColumn} and {@link DMEClaimColumn}).
   *
   * @param item the {@ ItemComponent} to test
   * @param eob the {@ ExplanationOfBenefit} to test
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
   * @throws FHIRException
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

    Assert.assertEquals(serviceCount, item.getQuantity().getValue());

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
    Assert.assertEquals(1, hctHgbObservationExtension.size());
    Assert.assertTrue(hctHgbObservationExtension.get(0).getValue() instanceof Reference);
    Reference hctHgbReference = (Reference) hctHgbObservationExtension.get(0).getValue();
    Assert.assertTrue(hctHgbReference.getResource() instanceof Observation);
    Observation hctHgbObservation = (Observation) hctHgbReference.getResource();
    assertHasCoding(
        CcwCodebookVariable.LINE_HCT_HGB_TYPE_CD, hctHgbTestTypeCode, hctHgbObservation.getCode());
    Assert.assertEquals(hctHgbTestResult, hctHgbObservation.getValueQuantity().getValue());

    assertExtensionCodingEquals(
        item,
        TransformerConstants.CODING_NDC,
        TransformerConstants.CODING_NDC,
        nationalDrugCode.get());
        */
  }

  /**
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
                        TransformerUtilsV2.calculateVariableReferenceUrl(categoryVariable)))
            .findFirst();
    Assert.assertTrue(info.isPresent());

    return info.get();
  }

  /**
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
   * @param concept the {@link CodeableConcept} to check
   * @param codingSystem the {@link Coding#getSystem()} to match
   * @param codingSystem the {@link Coding#getVersion()} to match
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
              if (!codingCode.equals(c.getCode())) return false;

              return true;
            });
  }

  /**
   * @param ccwVariable the {@link CcwCodebookVariable} that the expected {@link Extension} / {@link
   *     Coding} are for
   * @param expectedDateYear the expected {@link Coding#getCode()}
   * @param actualElement the FHIR element to find and verify the {@link Extension} of
   */
  static void assertExtensionDateYearEquals(
      CcwCodebookInterface ccwVariable,
      Optional<?> expectedDateYear,
      IBaseHasExtensions actualElement) {
    String expectedExtensionUrl = TransformerUtilsV2.calculateVariableReferenceUrl(ccwVariable);
    String expectedCodingSystem = expectedExtensionUrl;
    Optional<? extends IBaseExtension<?, ?>> extensionForUrl =
        actualElement.getExtension().stream()
            .filter(e -> e.getUrl().equals(expectedExtensionUrl))
            .findFirst();

    Assert.assertEquals(expectedDateYear.isPresent(), extensionForUrl.isPresent());
  }

  /**
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
   * @param ccwVariable the {@link CcwCodebookVariable} that was mapped
   * @param expectedCode the expected {@link Coding#getCode()}
   * @param actualConcept the FHIR {@link CodeableConcept} to verify
   */
  static void assertHasCoding(
      CcwCodebookInterface ccwVariable, Optional<?> expectedCode, CodeableConcept actualConcept) {
    String expectedCodingSystem = TransformerUtilsV2.calculateVariableReferenceUrl(ccwVariable);
    Optional<Coding> codingForSystem =
        actualConcept.getCoding().stream()
            .filter(c -> c.getSystem().equals(expectedCodingSystem))
            .findFirst();

    Assert.assertEquals(expectedCode.isPresent(), codingForSystem.isPresent());
    if (expectedCode.isPresent()) {
      assertCodingEquals(expectedCodingSystem, expectedCode.get(), codingForSystem.get());
    }
  }

  /**
   * @param expectedSystem the expected {@link Coding#getSystem()} value
   * @param expectedCode the expected {@link Coding#getCode()} value
   * @param actual the actual {@link Coding} to verify
   */
  static void assertCodingEquals(String expectedSystem, Object expectedCode, Coding actual) {
    assertCodingEquals(expectedSystem, null, expectedCode, actual);
  }

  /**
   * @param expectedSystem the expected {@link Coding#getSystem()} value
   * @param expectedVersion the expected {@link Coding#getVersion()} value
   * @param expectedCode the expected {@link Coding#getCode()} value
   * @param actual the actual {@link Coding} to verify
   */
  private static void assertCodingEquals(
      String expectedSystem, String expectedVersion, Object expectedCode, Coding actual) {
    Assert.assertEquals(expectedSystem, actual.getSystem());
    Assert.assertEquals(expectedVersion, actual.getVersion());

    /*
     * The code parameter is an Object to avoid needing multiple copies of this and
     * related methods. This if-else block is the price to be paid for that, though.
     */
    if (expectedCode instanceof Character) {
      Assert.assertEquals(((Character) expectedCode).toString(), actual.getCode());
    } else if (expectedCode instanceof String) {
      Assert.assertEquals(((String) expectedCode).trim(), actual.getCode());
    } else {
      throw new BadCodeMonkeyException();
    }
  }

  /**
   * @param expectedValue the expected {@link Quantity#getValue()}
   * @param actual the actual {@link Quantity} to verify
   */
  static void assertQuantityEquals(Number expectedValue, Quantity actual) {
    Assert.assertNotNull(actual);

    if (expectedValue instanceof BigDecimal) Assert.assertEquals(expectedValue, actual.getValue());
    else throw new BadCodeMonkeyException();
  }

  /**
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
    String expectedExtensionUrl =
        TransformerUtilsV2.calculateVariableReferenceUrl(categoryVariable);
    Optional<Extension> adjudicationTotalExtension =
        eob.getExtension().stream().filter(e -> expectedExtensionUrl.equals(e.getUrl())).findAny();
    Assert.assertEquals(expectedAmountValue.isPresent(), adjudicationTotalExtension.isPresent());

    if (expectedAmountValue.isPresent()) {
      Assert.assertNotNull(adjudicationTotalExtension.get().getValue());
      assertMoneyValue(
          expectedAmountValue.get(), (Money) adjudicationTotalExtension.get().getValue());
    }
  }

  /**
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
   * @param expectedAmountValue the expected {@link Money#getValue()}
   * @param actualValue the actual {@link Money} to verify
   */
  private static void assertMoneyValue(BigDecimal expectedAmountValue, Money actualValue) {
    /**
     * TODO: Money coding? Assert.assertEquals(TransformerConstants.CODING_MONEY,
     * actualValue.getSystem()); Assert.assertEquals(TransformerConstants.CODED_MONEY_USD,
     * actualValue.getCode());
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
    Assert.assertTrue(actual.precision() >= expected.precision());
    Assert.assertTrue(actual.scale() >= expected.scale());
    Assert.assertEquals(0, expected.compareTo(actual));
  }

  /**
   * @param ccwVariable the {@link CcwCodebookVariable} that was mapped
   * @param expectedValue the expected {@link Identifier#getValue()}
   * @param actualElement the FHIR element to find and verify the {@link Extension} of
   */
  static void assertExtensionIdentifierEquals(
      CcwCodebookInterface ccwVariable, String expectedValue, IBaseHasExtensions actualElement) {
    assertExtensionIdentifierEquals(ccwVariable, Optional.of(expectedValue), actualElement);
  }

  /**
   * @param ccwVariable the {@link CcwCodebookVariable} that the expected {@link Extension} / {@link
   *     Coding} are for
   * @param expectedValue the expected {@link Quantity#getValue()}
   * @param actualElement the FHIR element to find and verify the {@link Extension} of
   */
  static void assertExtensionIdentifierEquals(
      CcwCodebookInterface ccwVariable,
      Optional<String> expectedValue,
      IBaseHasExtensions actualElement) {
    String expectedExtensionUrl = TransformerUtilsV2.calculateVariableReferenceUrl(ccwVariable);
    Optional<? extends IBaseExtension<?, ?>> extensionForUrl =
        actualElement.getExtension().stream()
            .filter(e -> e.getUrl().equals(expectedExtensionUrl))
            .findFirst();

    Assert.assertEquals(expectedValue.isPresent(), extensionForUrl.isPresent());
    if (expectedValue.isPresent())
      assertIdentifierEquals(
          ccwVariable, expectedValue.get(), (Identifier) extensionForUrl.get().getValue());
  }

  /**
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
   * @param ccwVariable the {@link CcwCodebookVariable} that the expected {@link Extension} / {@link
   *     Coding} are for
   * @param expectedValue the expected {@link Quantity#getValue()}
   * @param actualElement the FHIR element to find and verify the {@link Extension} of
   */
  static void assertExtensionQuantityEquals(
      CcwCodebookInterface ccwVariable,
      Optional<? extends Number> expectedValue,
      IBaseHasExtensions actualElement) {
    String expectedExtensionUrl = TransformerUtilsV2.calculateVariableReferenceUrl(ccwVariable);
    Optional<? extends IBaseExtension<?, ?>> extensionForUrl =
        actualElement.getExtension().stream()
            .filter(e -> e.getUrl().equals(expectedExtensionUrl))
            .findFirst();

    Assert.assertEquals(expectedValue.isPresent(), extensionForUrl.isPresent());
    if (expectedValue.isPresent())
      assertQuantityEquals(expectedValue.get(), (Quantity) extensionForUrl.get().getValue());
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
   * Test that the resource being tested has a matching lastUpdated
   *
   * @param expectedDateTime from the entity
   * @param actualResource that is being created by the transform
   */
  static void assertLastUpdatedEquals(
      Optional<Date> expectedDateTime, IAnyResource actualResource) {
    if (expectedDateTime.isPresent()) {
      /* Dev Note: We often run our tests in parallel, so there is subtle race condition because we
       * use one instance of an IT DB with the same resources for most tests.
       * The actual resources a test finds may have a lastUpdated value slightly after the time the test wrote it
       * because another test over wrote the same resource.
       * To handle this case, dates that are within a second of each other match.
       */
      final Instant expectedLastUpdated = expectedDateTime.get().toInstant();
      final Instant actualLastUpdated = actualResource.getMeta().getLastUpdated().toInstant();
      final Duration diff = Duration.between(expectedLastUpdated, actualLastUpdated);
      Assert.assertTrue(
          "Expect the actual lastUpdated to be equal or after the loaded resources",
          diff.compareTo(Duration.ofSeconds(1)) <= 0);
    } else {
      Assert.assertEquals(
          "Expect lastUpdated to be the fallback value",
          TransformerConstants.FALLBACK_LAST_UPDATED,
          actualResource.getMeta().getLastUpdated());
    }
  }

  /**
   * Test the transformation of the common group level data elements between the {@link
   * CarrierClaim} and {@link DMEClaim} claim types to FHIR. The method parameter fields from {@link
   * CarrierClaim} and {@link DMEClaim} are listed below and their corresponding RIF CCW fields
   * (denoted in all CAPS below from {@link CarrierClaimColumn} and {@link DMEClaimColumn}).
   *
   * @param eob the {@link ExplanationOfBenefit} to test
   * @param benficiaryId BENE_ID, *
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
        ReferralRequest referral = (ReferralRequest) eob.getReferral().getResource();
        Assert.assertEquals(
            TransformerUtilsV2.referencePatient(beneficiaryId).getReference(),
            referral.getSubject().getReference());
        assertReferenceIdentifierEquals(
            TransformerConstants.CODING_NPI_US,
            referringPhysicianNpi.get(),
            referral.getRequester().getAgent());
        Assert.assertEquals(1, referral.getRecipient().size());
        assertReferenceIdentifierEquals(
            TransformerConstants.CODING_NPI_US,
            referringPhysicianNpi.get(),
            referral.getRecipientFirstRep());
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
}
