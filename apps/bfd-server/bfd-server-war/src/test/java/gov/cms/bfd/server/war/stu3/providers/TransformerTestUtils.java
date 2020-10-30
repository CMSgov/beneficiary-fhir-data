package gov.cms.bfd.server.war.stu3.providers;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import com.justdavis.karl.misc.exceptions.BadCodeMonkeyException;
import gov.cms.bfd.model.codebook.data.CcwCodebookInterface;
import gov.cms.bfd.model.codebook.data.CcwCodebookVariable;
import gov.cms.bfd.model.rif.CarrierClaim;
import gov.cms.bfd.model.rif.CarrierClaimColumn;
import gov.cms.bfd.model.rif.CarrierClaimLine;
import gov.cms.bfd.model.rif.DMEClaim;
import gov.cms.bfd.model.rif.DMEClaimColumn;
import gov.cms.bfd.model.rif.DMEClaimLine;
import gov.cms.bfd.model.rif.HHAClaim;
import gov.cms.bfd.model.rif.HHAClaimColumn;
import gov.cms.bfd.model.rif.HHAClaimLine;
import gov.cms.bfd.model.rif.HospiceClaim;
import gov.cms.bfd.model.rif.HospiceClaimColumn;
import gov.cms.bfd.model.rif.HospiceClaimLine;
import gov.cms.bfd.model.rif.InpatientClaim;
import gov.cms.bfd.model.rif.InpatientClaimColumn;
import gov.cms.bfd.model.rif.InpatientClaimLine;
import gov.cms.bfd.model.rif.OutpatientClaim;
import gov.cms.bfd.model.rif.OutpatientClaimColumn;
import gov.cms.bfd.model.rif.OutpatientClaimLine;
import gov.cms.bfd.model.rif.SNFClaim;
import gov.cms.bfd.model.rif.SNFClaimColumn;
import gov.cms.bfd.model.rif.SNFClaimLine;
import gov.cms.bfd.server.war.commons.Diagnosis;
import gov.cms.bfd.server.war.commons.MedicareSegment;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import java.io.IOException;
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
import junit.framework.AssertionFailedError;
import org.hl7.fhir.dstu3.model.BaseDateTimeType;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.DateTimeType;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.AdjudicationComponent;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.BenefitBalanceComponent;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.BenefitComponent;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.CareTeamComponent;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.DiagnosisComponent;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.ItemComponent;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.SupportingInformationComponent;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Money;
import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.dstu3.model.Period;
import org.hl7.fhir.dstu3.model.Quantity;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.ReferralRequest;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.dstu3.model.codesystems.BenefitCategory;
import org.hl7.fhir.dstu3.model.codesystems.ClaimCareteamrole;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IBaseHasExtensions;
import org.junit.Assert;

/**
 * Contains utility methods useful for testing the transformers (e.g. {@link
 * gov.cms.bfd.server.war.stu3.providers.BeneficiaryTransformer}).
 */
final class TransformerTestUtils {
  /* Do this very slow operation once */
  private static final FhirContext fhirContext = FhirContext.forDstu3();

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
    String expectedExtensionUrl = TransformerUtils.calculateVariableReferenceUrl(categoryVariable);
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
    Assert.assertEquals(TransformerConstants.CODING_MONEY, actualValue.getSystem());
    Assert.assertEquals(TransformerConstants.CODED_MONEY_USD, actualValue.getCode());
    assertEquivalent(expectedAmountValue, actualValue.getValue());
  }

  /**
   * @param ccwVariable the {@link CcwCodebookVariable} for the {@link
   *     AdjudicationComponent#getCategory()} to find and verify
   * @param expectedAmount the expected {@link AdjudicationComponent#getAmount()}
   * @param actuals the actual {@link AdjudicationComponent}s to verify
   * @return the {@link AdjudicationComponent} that was found and verified
   */
  static AdjudicationComponent assertAdjudicationAmountEquals(
      CcwCodebookInterface ccwVariable,
      BigDecimal expectedAmount,
      List<AdjudicationComponent> actuals) {
    CodeableConcept expectedCategory = TransformerUtils.createAdjudicationCategory(ccwVariable);
    Optional<AdjudicationComponent> adjudication =
        actuals.stream()
            .filter(
                a ->
                    isCodeInConcept(
                        a.getCategory(),
                        expectedCategory.getCodingFirstRep().getSystem(),
                        expectedCategory.getCodingFirstRep().getCode()))
            .findAny();
    Assert.assertTrue(adjudication.isPresent());
    assertEquivalent(expectedAmount, adjudication.get().getAmount().getValue());

    return adjudication.get();
  }

  /**
   * @param expectedCategoryCode the {@link CcwCodebookVariable} for the {@link
   *     AdjudicationComponent#getCategory()} to find and verify
   * @param expectedReasonCode the expected {@link Coding#getCode()} of the {@link
   *     AdjudicationComponent#getReason()} to find and verify
   * @param actuals the actual {@link AdjudicationComponent}s to verify
   */
  static void assertAdjudicationReasonEquals(
      CcwCodebookInterface ccwVariable,
      Optional<?> expectedReasonCode,
      List<AdjudicationComponent> actuals) {
    CodeableConcept expectedCategory = TransformerUtils.createAdjudicationCategory(ccwVariable);
    Optional<AdjudicationComponent> adjudication =
        actuals.stream()
            .filter(
                a ->
                    isCodeInConcept(
                        a.getCategory(),
                        expectedCategory.getCodingFirstRep().getSystem(),
                        expectedCategory.getCodingFirstRep().getCode()))
            .findAny();
    Assert.assertEquals(expectedReasonCode.isPresent(), adjudication.isPresent());

    if (expectedReasonCode.isPresent())
      assertHasCoding(ccwVariable, expectedReasonCode, adjudication.get().getReason());
  }

  /**
   * @param expectedCategoryCode the {@link CcwCodebookVariable} for the {@link
   *     AdjudicationComponent#getCategory()} to find and verify
   * @param expectedReasonCode the expected {@link Coding#getCode()} of the {@link
   *     AdjudicationComponent#getReason()} to find and verify
   * @param actuals the actual {@link AdjudicationComponent}s to verify
   */
  static void assertAdjudicationReasonEquals(
      CcwCodebookInterface ccwVariable,
      Object expectedReasonCode,
      List<AdjudicationComponent> actuals) {
    // Jumping through hoops to cope with overloaded method:
    Optional<?> expectedReasonCodeCast =
        expectedReasonCode instanceof Optional
            ? (Optional<?>) expectedReasonCode
            : Optional.of(expectedReasonCode);
    assertAdjudicationReasonEquals(ccwVariable, expectedReasonCodeCast, actuals);
  }

  /**
   * FIXME add allowed to method name
   *
   * @param expectedFinancialTypeSystem the expected {@link Coding#getSystem()} of the {@link
   *     BenefitComponent#getCode)} to find and verify
   * @param expectedFinancialTypeCode the expected {@link Coding#getCode()} of the {@link
   *     BenefitComponent#getCode)} to find and verify
   * @param expectedAmount the expected {@link BenefitComponent#getBenefitMoney}
   * @param actuals the actual {@link BenefitComponent}s to verify
   */
  static void assertBenefitBalanceEquals(
      String expectedFinancialTypeSystem,
      String expectedFinancialTypeCode,
      BigDecimal expectedAmount,
      List<BenefitComponent> actuals) {
    Optional<BenefitComponent> benefitComponent =
        actuals.stream()
            .filter(
                a ->
                    isCodeInConcept(
                        a.getType(), expectedFinancialTypeSystem, expectedFinancialTypeCode))
            .findFirst();
    Assert.assertTrue(benefitComponent.isPresent());
    try {
      assertEquivalent(expectedAmount, benefitComponent.get().getAllowedMoney().getValue());
    } catch (FHIRException e) {
      throw new BadCodeMonkeyException(e);
    }
  }

  /**
   * @param expectedFinancialTypeSystem the expected {@link Coding#getSystem()} of the {@link
   *     BenefitComponent#getCode)} to find and verify
   * @param expectedFinancialTypeCode the expected {@link Coding#getCode()} of the {@link
   *     BenefitComponent#getCode)} to find and verify
   * @param expectedAmount the expected {@link BenefitComponent#getBenefitUnsignedIntType()}
   * @param actuals the actual {@link BenefitComponent}s to verify
   */
  static void assertBenefitBalanceEquals(
      String expectedFinancialTypeSystem,
      String expectedFinancialTypeCode,
      Integer expectedAmount,
      List<BenefitComponent> actuals) {
    Optional<BenefitComponent> benefitComponent =
        actuals.stream()
            .filter(
                a ->
                    isCodeInConcept(
                        a.getType(), expectedFinancialTypeSystem, expectedFinancialTypeCode))
            .findFirst();
    Assert.assertTrue(benefitComponent.isPresent());
    try {
      Assert.assertEquals(
          expectedAmount, benefitComponent.get().getAllowedUnsignedIntType().getValue());
    } catch (FHIRException e) {
      throw new BadCodeMonkeyException(e);
    }
  }

  /**
   * @param expectedFinancialTypeSystem the expected {@link Coding#getSystem()} of the {@link
   *     BenefitComponent#getCode)} to find and verify
   * @param expectedFinancialTypeCode the expected {@link Coding#getCode()} of the {@link
   *     BenefitComponent#getCode)} to find and verify
   * @param expectedAmount the expected {@link BenefitComponent#getBenefitUsedUnsignedIntType}
   * @param actuals the actual {@link BenefitComponent}s to verify
   */
  static void assertBenefitBalanceUsedEquals(
      String expectedFinancialTypeSystem,
      String expectedFinancialTypeCode,
      Integer expectedAmount,
      List<BenefitComponent> actuals) {
    Optional<BenefitComponent> benefitComponent =
        actuals.stream()
            .filter(
                a ->
                    isCodeInConcept(
                        a.getType(), expectedFinancialTypeSystem, expectedFinancialTypeCode))
            .findFirst();
    Assert.assertTrue(benefitComponent.isPresent());
    try {
      Assert.assertEquals(
          expectedAmount, benefitComponent.get().getUsedUnsignedIntType().getValue());
    } catch (FHIRException e) {
      throw new BadCodeMonkeyException(e);
    }
  }

  /**
   * @param expectedBenefitCategory the {@link BenefitCategory} for the expected {@link
   *     BenefitBalanceComponent#getCategory()}
   * @param expectedFinancialType the {@link CcwCodebookVariable} for the expected {@link
   *     BenefitComponent#getType()}
   * @param expectedUsedInt the expected {@link BenefitComponent#getUsedUnsignedIntType()} value
   * @param eob the {@link ExplanationOfBenefit} to verify the actual {@link BenefitComponent}
   *     within
   */
  static void assertBenefitBalanceUsedIntEquals(
      BenefitCategory expectedBenefitCategory,
      CcwCodebookVariable expectedFinancialType,
      Integer expectedUsedInt,
      ExplanationOfBenefit eob) {
    Optional<BenefitBalanceComponent> benefitBalanceComponent =
        eob.getBenefitBalance().stream()
            .filter(
                bb ->
                    isCodeInConcept(
                        bb.getCategory(),
                        expectedBenefitCategory.getSystem(),
                        expectedBenefitCategory.toCode()))
            .findAny();
    Assert.assertTrue(benefitBalanceComponent.isPresent());

    Optional<BenefitComponent> benefitBalanceFinancialEntry =
        benefitBalanceComponent.get().getFinancial().stream()
            .filter(
                f ->
                    isCodeInConcept(
                        f.getType(),
                        TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
                        TransformerUtils.calculateVariableReferenceUrl(expectedFinancialType)))
            .findAny();
    Assert.assertTrue(benefitBalanceFinancialEntry.isPresent());

    try {
      Assert.assertEquals(
          expectedUsedInt, benefitBalanceFinancialEntry.get().getUsedUnsignedIntType().getValue());
    } catch (FHIRException e) {
      throw new BadCodeMonkeyException(e);
    }
  }

  /**
   * @param expectedPractitioner {@link CareTeamComponent#getProviderIdentifier)} to find and verify
   * @param expectedCareTeamRole the {@link ClaimCareteamrole} for {@link
   *     CareTeamComponent#getRole)} to find and verify
   * @param eob the actual {@link ExplanationOfBenefit}s to verify
   */
  static void assertCareTeamEquals(
      String expectedPractitioner,
      ClaimCareteamrole expectedCareTeamRole,
      ExplanationOfBenefit eob) {
    CareTeamComponent careTeamEntry =
        findCareTeamEntryForProviderIdentifier(
            TransformerConstants.CODING_NPI_US,
            expectedPractitioner,
            expectedCareTeamRole,
            eob.getCareTeam());
    Assert.assertNotNull(careTeamEntry);
    assertCodingEquals(
        expectedCareTeamRole.getSystem(),
        expectedCareTeamRole.toCode(),
        careTeamEntry.getRole().getCodingFirstRep());
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
    if (expectedCode instanceof Character)
      Assert.assertEquals(((Character) expectedCode).toString(), actual.getCode());
    else if (expectedCode instanceof String)
      Assert.assertEquals(((String) expectedCode).trim(), actual.getCode());
    else throw new BadCodeMonkeyException();
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
   * @param expectedDiagnosis the expected {@link gov.cms.bfd.server.war.stu3.providers.IcdCode} to
   *     verify the presence of in the {@link ItemComponent}
   * @param eob the {@link ExplanationOfBenefit} to verify
   * @param eobItem the {@link ItemComponent} to verify
   */
  static void assertDiagnosisLinkPresent(
      Optional<Diagnosis> diagnosis, ExplanationOfBenefit eob, ItemComponent eobItem) {
    if (!diagnosis.isPresent()) return;

    Optional<DiagnosisComponent> eobDiagnosis =
        eob.getDiagnosis().stream()
            .filter(d -> d.getDiagnosis() instanceof CodeableConcept)
            .filter(d -> diagnosis.get().isContainedIn((CodeableConcept) d.getDiagnosis()))
            .findAny();
    Assert.assertTrue(eobDiagnosis.isPresent());
    Assert.assertTrue(
        eobItem.getDiagnosisLinkId().stream()
            .filter(l -> eobDiagnosis.get().getSequence() == l.getValue())
            .findAny()
            .isPresent());
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
    String expectedCodingSystem = TransformerUtils.calculateVariableReferenceUrl(ccwVariable);
    Optional<Coding> codingForSystem =
        actualConcept.getCoding().stream()
            .filter(c -> c.getSystem().equals(expectedCodingSystem))
            .findFirst();

    Assert.assertEquals(expectedCode.isPresent(), codingForSystem.isPresent());
    if (expectedCode.isPresent())
      assertCodingEquals(expectedCodingSystem, expectedCode.get(), codingForSystem.get());
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
    String expectedExtensionUrl = TransformerUtils.calculateVariableReferenceUrl(ccwVariable);
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
    String expectedExtensionUrl = TransformerUtils.calculateVariableReferenceUrl(ccwVariable);
    Optional<? extends IBaseExtension<?, ?>> extensionForUrl =
        actualElement.getExtension().stream()
            .filter(e -> e.getUrl().equals(expectedExtensionUrl))
            .findFirst();

    Assert.assertEquals(expectedValue.isPresent(), extensionForUrl.isPresent());
    if (expectedValue.isPresent())
      assertQuantityEquals(expectedValue.get(), (Quantity) extensionForUrl.get().getValue());
  }

  /**
   * @param ccwVariableForQuantity the {@link CcwCodebookVariable} that was mapped to a {@link
   *     Quantity} {@link Extension}
   * @param ccwVariableForUnit the {@link CcwCodebookVariable} that was mapped to a {@link Quantity}
   *     unit
   * @param expectedUnitCode the expected {@link Quantity#getCode()} value
   * @param actualElement the FHIR element to find and verify the {@link Extension} of
   */
  static void assertQuantityUnitInfoEquals(
      CcwCodebookInterface ccwVariableForQuantity,
      CcwCodebookInterface ccwVariableForUnit,
      Object expectedUnitCode,
      IBaseHasExtensions actualElement) {
    String expectedExtensionUrl =
        TransformerUtils.calculateVariableReferenceUrl(ccwVariableForQuantity);
    Optional<? extends IBaseExtension<?, ?>> actualExtension =
        actualElement.getExtension().stream()
            .filter(e -> e.getUrl().equals(expectedExtensionUrl))
            .findFirst();
    Assert.assertTrue(actualExtension.isPresent());
    Assert.assertTrue(actualExtension.get().getValue() instanceof Quantity);
    Quantity actualQuantity = (Quantity) actualExtension.get().getValue();

    String expectedUnitCodeString;
    if (expectedUnitCode instanceof String) expectedUnitCodeString = (String) expectedUnitCode;
    else if (expectedUnitCode instanceof Character)
      expectedUnitCodeString = ((Character) expectedUnitCode).toString();
    else throw new BadCodeMonkeyException("Unsupported: " + expectedUnitCode);

    Assert.assertEquals(expectedUnitCodeString, actualQuantity.getCode());
    Assert.assertEquals(
        TransformerUtils.calculateVariableReferenceUrl(ccwVariableForUnit),
        actualQuantity.getSystem());
  }

  /**
   * @param ccwVariableForQuantity the {@link CcwCodebookVariable} that was mapped to a {@link
   *     Quantity} {@link Extension}
   * @param ccwVariableForUnit the {@link CcwCodebookVariable} that was mapped to a {@link Quantity}
   *     unit
   * @param expectedUnitCode the expected {@link Quantity#getCode()} value
   * @param actualElement the FHIR element to find and verify the {@link Extension} of
   */
  static void assertQuantityUnitInfoEquals(
      CcwCodebookInterface ccwVariableForQuantity,
      CcwCodebookInterface ccwVariableForUnit,
      Optional<?> expectedUnitCode,
      IBaseHasExtensions actualElement) {
    String expectedExtensionUrl =
        TransformerUtils.calculateVariableReferenceUrl(ccwVariableForQuantity);
    Optional<? extends IBaseExtension<?, ?>> actualExtension =
        actualElement.getExtension().stream()
            .filter(e -> e.getUrl().equals(expectedExtensionUrl))
            .findFirst();
    Assert.assertEquals(expectedUnitCode.isPresent(), actualExtension.isPresent());

    if (expectedUnitCode.isPresent())
      assertQuantityUnitInfoEquals(
          ccwVariableForQuantity, ccwVariableForUnit, expectedUnitCode.get(), actualElement);
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
    String expectedExtensionUrl = TransformerUtils.calculateVariableReferenceUrl(ccwVariable);
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
   * @param ccwVariable the {@link CcwCodebookVariable} that the expected {@link Extension} / {@link
   *     Coding} are for
   * @param expectedDateYear the expected {@link Coding#getCode()}
   * @param actualElement the FHIR element to find and verify the {@link Extension} of
   */
  static void assertExtensionDateYearEquals(
      CcwCodebookInterface ccwVariable,
      Optional<?> expectedDateYear,
      IBaseHasExtensions actualElement) {
    String expectedExtensionUrl = TransformerUtils.calculateVariableReferenceUrl(ccwVariable);
    String expectedCodingSystem = expectedExtensionUrl;
    Optional<? extends IBaseExtension<?, ?>> extensionForUrl =
        actualElement.getExtension().stream()
            .filter(e -> e.getUrl().equals(expectedExtensionUrl))
            .findFirst();

    Assert.assertEquals(expectedDateYear.isPresent(), extensionForUrl.isPresent());
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
        TransformerUtils.calculateVariableReferenceUrl(ccwVariable), actual.getSystem());
    Assert.assertEquals(expectedValue, actual.getValue());
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
   * @param ccwVariable the {@link CcwCodebookVariable} that was mapped
   * @param expectedValue the expected {@link Identifier#getValue()} value
   * @param actualIdentifiers the actual {@link Identifier}s to verify a match can be found within
   */
  private static void assertHasIdentifier(
      CcwCodebookInterface ccwVariable, String expectedValue, List<Identifier> actualIdentifiers) {
    if (expectedValue == null) throw new IllegalArgumentException();

    Assert.assertNotNull(actualIdentifiers);

    String expectedSystem = TransformerUtils.calculateVariableReferenceUrl(ccwVariable);
    Optional<Identifier> matchingIdentifier =
        actualIdentifiers.stream()
            .filter(i -> expectedSystem.equals(i.getSystem()))
            .filter(i -> expectedValue.equals(i.getValue()))
            .findAny();
    Assert.assertTrue(matchingIdentifier.isPresent());
  }

  /**
   * Tests that the specified extension list contains a single Identifier with the expected string
   * value
   *
   * @param extension a {@link List}&lt;{@link Extension}&gt; containing an Identifier
   * @param expected a {@link String} containing the expected value of the Identifier
   */
  static void assertExtensionIdentifierEqualsString(List<Extension> extension, String expected) {
    Assert.assertEquals(1, extension.size());
    Assert.assertTrue(extension.get(0).getValue() instanceof Identifier);
    Identifier identifier = (Identifier) extension.get(0).getValue();
    Assert.assertEquals(expected, identifier.getValue());
  }

  /** Test */
  static void assertExtensionValueQuantityEquals(
      List<Extension> extension,
      String expectedExtensionUrl,
      String expectedSystem,
      BigDecimal expectedValue) {
    Assert.assertEquals(1, extension.size());
    Assert.assertEquals(extension.get(0).getUrl(), expectedExtensionUrl);
    Assert.assertTrue(extension.get(0).getValue() instanceof Quantity);
    Quantity quantity = (Quantity) extension.get(0).getValue();
    Assert.assertEquals(expectedValue, quantity.getValue());
    Assert.assertEquals(expectedSystem, quantity.getSystem());
  }

  /**
   * @param expectedSystem the expected {@link Coding#getSystem()} of the {@link
   *     SupportingInformationComponent#getCategory()} to find and verify
   * @param expectedCode the expected {@link Coding#getCoding()} of the {@link
   *     SupportingInformationComponent#getCategory()} to find and verify
   * @param expectedDate the expected {@link
   *     SupportingInformationComponent#getTiming().primitiveValue()}
   * @param actuals the actual {@link SupportingInformationComponent}s to verify
   */
  static void assertInformationDateEquals(
      String expectedSystem,
      String expectedCode,
      LocalDate expectedDate,
      List<SupportingInformationComponent> actuals) {
    Optional<SupportingInformationComponent> supportingInformationComponent =
        actuals.stream()
            .filter(a -> isCodeInConcept(a.getCategory(), expectedSystem, expectedCode))
            .findAny();
    Assert.assertTrue(supportingInformationComponent.isPresent());
    Assert.assertEquals(
        expectedDate.toString(), supportingInformationComponent.get().getTiming().primitiveValue());
  }

  /**
   * @param expectedCategorySystem the expected value for {@link
   *     SupportingInformationComponent#getCategory()}'s {@link Coding#getSystem()}
   * @param expectedCategoryCodeVariable the expected {@link CcwCodebookVariable} for {@link
   *     SupportingInformationComponent#getCategory()}'s {@link Coding#getCode()}
   * @param expectedDate the expected {@link
   *     SupportingInformationComponent#getTiming().primitiveValue()}
   * @param actuals the actual {@link SupportingInformationComponent}s to verify
   */
  static void assertInformationDateEquals(
      String expectedCategorySystem,
      CcwCodebookVariable expectedCategoryCodeVariable,
      LocalDate expectedDate,
      List<SupportingInformationComponent> actuals) {
    String expectedCategoryCode =
        TransformerUtils.calculateVariableReferenceUrl(expectedCategoryCodeVariable);
    Optional<SupportingInformationComponent> supportingInformationComponent =
        actuals.stream()
            .filter(
                a -> isCodeInConcept(a.getCategory(), expectedCategorySystem, expectedCategoryCode))
            .findAny();
    Assert.assertTrue(supportingInformationComponent.isPresent());
    Assert.assertEquals(
        expectedDate.toString(), supportingInformationComponent.get().getTiming().primitiveValue());
  }

  /**
   * @param expectedSystem the expected {@link Coding#getSystem()} of the {@link
   *     SupportingInformationComponent#getCategory()} to find and verify
   * @param expectedCode the expected {@link Coding#getCoding()} of the {@link
   *     SupportingInformationComponent#getCategory()} to find and verify
   * @param expectedFromDate the expected {@link
   *     SupportingInformationComponent#getTimingPeriod().getStartElement()}
   * @param expectedThruDate the expected {@link
   *     SupportingInformationComponent#getTimingPeriod().getEndElement()}
   * @param actuals the actual {@link SupportingInformationComponent}s to verify
   */
  static void assertInformationPeriodEquals(
      String expectedSystem,
      String expectedCode,
      LocalDate expectedFromDate,
      LocalDate expectedThruDate,
      List<SupportingInformationComponent> actuals) {
    Optional<SupportingInformationComponent> supportingInformationComponent =
        actuals.stream()
            .filter(a -> isCodeInConcept(a.getCategory(), expectedSystem, expectedCode))
            .findAny();
    Assert.assertTrue(supportingInformationComponent.isPresent());
    try {
      assertDateEquals(
          expectedFromDate,
          supportingInformationComponent.get().getTimingPeriod().getStartElement());
      assertDateEquals(
          expectedThruDate, supportingInformationComponent.get().getTimingPeriod().getEndElement());
    } catch (FHIRException e) {
      throw new BadCodeMonkeyException(e);
    }
  }

  /**
   * @param expectedCategorySystem the expected value for {@link
   *     SupportingInformationComponent#getCategory()}'s {@link Coding#getSystem()}
   * @param expectedCategoryCodeVariable the expected {@link CcwCodebookVariable} for {@link
   *     SupportingInformationComponent#getCategory()}'s {@link Coding#getCode()}
   * @param expectedFromDate the expected {@link
   *     SupportingInformationComponent#getTimingPeriod().getStartElement()}
   * @param expectedThruDate the expected {@link
   *     SupportingInformationComponent#getTimingPeriod().getEndElement()}
   * @param actuals the actual {@link SupportingInformationComponent}s to verify
   */
  static void assertInformationPeriodEquals(
      String expectedCategorySystem,
      CcwCodebookVariable expectedCategoryCodeVariable,
      LocalDate expectedFromDate,
      LocalDate expectedThruDate,
      List<SupportingInformationComponent> actuals) {
    String expectedCategoryCode =
        TransformerUtils.calculateVariableReferenceUrl(expectedCategoryCodeVariable);
    Optional<SupportingInformationComponent> supportingInformationComponent =
        actuals.stream()
            .filter(
                a -> isCodeInConcept(a.getCategory(), expectedCategorySystem, expectedCategoryCode))
            .findAny();
    Assert.assertTrue(supportingInformationComponent.isPresent());
    try {
      assertDateEquals(
          expectedFromDate,
          supportingInformationComponent.get().getTimingPeriod().getStartElement());
      assertDateEquals(
          expectedThruDate, supportingInformationComponent.get().getTimingPeriod().getEndElement());
    } catch (FHIRException e) {
      throw new BadCodeMonkeyException(e);
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
   * @param expectedIdentifierSystem the expected {@link Identifier#getSystem()} to match
   * @param expectedIdentifierValue the expected {@link Identifier#getValue()} to match
   * @param actualReference the {@link Reference} to check
   */
  static void assertReferenceEquals(
      String expectedIdentifierSystem, String expectedIdentifierValue, Reference actualReference) {
    Assert.assertTrue(
        "Reference doesn't match: " + actualReference,
        doesReferenceMatchIdentifier(
            expectedIdentifierSystem, expectedIdentifierValue, actualReference));
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
        TransformerUtils.retrieveNpiCodeDisplay(expectedIdentifierValue), reference.getDisplay());
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
        TransformerUtils.calculateVariableReferenceUrl(ccwVariable),
        actualReference.getIdentifier().getSystem());
    Assert.assertEquals(expectedIdentifierValue, actualReference.getIdentifier().getValue());
  }

  /**
   * @param eobType the eobType {@link CodeableConcept} we are testing against for expected values
   * @param blueButtonClaimType expected blue button {@link
   *     gov.cms.bfd.server.war.stu3.providers.ClaimType} value
   * @param fhirClaimType optional expected fhir {@link
   *     org.hl7.fhir.dstu3.model.codesystems.ClaimType} value
   * @param ccwNearLineRecordIdCode optional expected ccw near line record id code {@link
   *     Optional}&lt;{@link Character}&gt;
   * @param ccwClaimTypeCode optional expected ccw claim type code {@link Optional}&lt;{@link
   *     String}&gt;
   */
  static void assertMapEobType(
      CodeableConcept eobType,
      ClaimType blueButtonClaimType,
      Optional<org.hl7.fhir.dstu3.model.codesystems.ClaimType> fhirClaimType,
      Optional<Character> ccwNearLineRecordIdCode,
      Optional<String> ccwClaimTypeCode) {
    assertHasCoding(
        TransformerConstants.CODING_SYSTEM_BBAPI_EOB_TYPE,
        blueButtonClaimType.name(),
        eobType.getCoding());

    if (fhirClaimType.isPresent()) {
      assertHasCoding(
          org.hl7.fhir.dstu3.model.codesystems.ClaimType.INSTITUTIONAL.getSystem(),
          fhirClaimType.get().toCode(),
          eobType.getCoding());
    }

    if (ccwNearLineRecordIdCode.isPresent()) {
      assertHasCoding(
          CcwCodebookVariable.NCH_NEAR_LINE_REC_IDENT_CD, ccwNearLineRecordIdCode, eobType);
    }

    if (ccwClaimTypeCode.isPresent()) {
      assertHasCoding(CcwCodebookVariable.NCH_CLM_TYPE_CD, ccwClaimTypeCode, eobType);
    }
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
      ClaimCareteamrole expectedRole, CareTeamComponent actualComponent) {
    if (expectedRole == null) return true;
    if (!actualComponent.hasRole()) return false;
    final Coding actualRole = actualComponent.getRole().getCodingFirstRep();
    return actualRole.getCode().equals(expectedRole.toCode())
        && actualRole.getSystem().equals(ClaimCareteamrole.NULL.getSystem());
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
      ClaimCareteamrole expectedRole,
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
   * @param ccwVariable the {@link CcwCodebookVariable} that was mapped
   * @param expectedCode the {@link Coding#getCode()} to match
   * @param actualConcept the {@link CodeableConcept} to check
   * @return <code>true</code> if the specified {@link CodeableConcept} contains the specified
   *     {@link Coding}, <code>false</code> if it does not
   */
  static boolean isCodeInConcept(
      CcwCodebookInterface ccwVariable, Object expectedCode, CodeableConcept actualConcept) {
    String expectedCodeString;
    if (expectedCode instanceof String) expectedCodeString = (String) expectedCode;
    else if (expectedCode instanceof Character)
      expectedCodeString = ((Character) expectedCode).toString();
    else throw new BadCodeMonkeyException();

    return actualConcept.getCoding().stream()
        .anyMatch(
            c -> {
              if (!TransformerUtils.calculateVariableReferenceUrl(ccwVariable)
                  .equals(c.getSystem())) return false;
              if (!expectedCodeString.equals(c.getCode())) return false;

              return true;
            });
  }

  /**
   * @param ccwVariable the {@link CcwCodebookVariable} that was mapped
   * @param expectedCode the {@link Coding#getCode()} to match
   * @param actualConcept the {@link CodeableConcept} to check
   * @return <code>true</code> if the specified {@link CodeableConcept} contains the specified
   *     {@link Coding}, <code>false</code> if it does not
   */
  static boolean isCodeInConcept(
      CcwCodebookInterface ccwVariable, Optional<?> expectedCode, CodeableConcept actualConcept) {
    if (!expectedCode.isPresent()) throw new IllegalArgumentException();
    return isCodeInConcept(ccwVariable, expectedCode.get(), actualConcept);
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
   * Tests field values of an eob's benefit balance component that are common between the Inpatient
   * and SNF claim types.
   *
   * @param eob the {@link ExplanationOfBenefit} that will be tested by this method
   * @param coinsuranceDayCount BENE_TOT_COINSRNC_DAYS_CNT: a {@link BigDecimal} shared field
   *     representing the coinsurance day count for the claim
   * @param nonUtilizationDayCount CLM_NON_UTLZTN_DAYS_CNT: a {@link BigDecimal} shared field
   *     representing the non-utilization day count for the claim
   * @param deductibleAmount NCH_BENE_IP_DDCTBL_AMT: a {@link BigDecimal} shared field representing
   *     the deductible amount for the claim
   * @param partACoinsuranceLiabilityAmount NCH_BENE_PTA_COINSRNC_LBLTY_AM: a {@link BigDecimal}
   *     shared field representing the part A coinsurance amount for the claim
   * @param bloodPintsFurnishedQty NCH_BLOOD_PNTS_FRNSHD_QTY: a {@link BigDecimal} shared field
   *     representing the blood pints furnished quantity for the claim
   * @param noncoveredCharge NCH_IP_NCVRD_CHRG_AMT: a {@link BigDecimal} shared field representing
   *     the non-covered charge for the claim
   * @param totalDeductionAmount NCH_IP_TOT_DDCTN_AMT: a {@link BigDecimal} shared field
   *     representing the total deduction amount for the claim
   * @param claimPPSCapitalDisproportionateShareAmt CLM_PPS_CPTL_DSPRPRTNT_SHR_AMT: an {@link
   *     Optional}&lt;{@link BigDecimal}&gt; shared field representing the claim PPS capital
   *     disproportionate share amount for the claim
   * @param claimPPSCapitalExceptionAmount CLM_PPS_CPTL_EXCPTN_AMT: an {@link Optional}&lt;{@link
   *     BigDecimal}&gt; shared field representing the claim PPS capital exception amount for the
   *     claim
   * @param claimPPSCapitalFSPAmount CLM_PPS_CPTL_FSP_AMT: an {@link Optional}&lt;{@link
   *     BigDecimal}&gt; shared field representing the claim PPS capital FSP amount for the claim
   * @param claimPPSCapitalIMEAmount CLM_PPS_CPTL_IME_AMT: an {@link Optional}&lt;{@link
   *     BigDecimal}&gt; shared field representing the claim PPS capital IME amount for the claim
   * @param claimPPSCapitalOutlierAmount CLM_PPS_CPTL_OUTLIER_AMT: an {@link Optional}&lt;{@link
   *     BigDecimal}&gt; shared field representing the claim PPS capital outlier amount for the
   *     claim
   * @param claimPPSOldCapitalHoldHarmlessAmount CLM_PPS_OLD_CPTL_HLD_HRMLS_AMT: an {@link
   *     Optional}&lt;{@link BigDecimal}&gt; shared field representing the claim PPS old capital
   *     hold harmless amount for the claim
   * @throws FHIRException Indicates a test failure.
   */
  static void assertCommonGroupInpatientSNF(
      ExplanationOfBenefit eob,
      BigDecimal coinsuranceDayCount,
      BigDecimal nonUtilizationDayCount,
      BigDecimal deductibleAmount,
      BigDecimal partACoinsuranceLiabilityAmount,
      BigDecimal bloodPintsFurnishedQty,
      BigDecimal noncoveredCharge,
      BigDecimal totalDeductionAmount,
      Optional<BigDecimal> claimPPSCapitalDisproportionateShareAmt,
      Optional<BigDecimal> claimPPSCapitalExceptionAmount,
      Optional<BigDecimal> claimPPSCapitalFSPAmount,
      Optional<BigDecimal> claimPPSCapitalIMEAmount,
      Optional<BigDecimal> claimPPSCapitalOutlierAmount,
      Optional<BigDecimal> claimPPSOldCapitalHoldHarmlessAmount)
      throws FHIRException {
    BenefitComponent benefit_BENE_TOT_COINSRNC_DAYS_CNT =
        assertHasBenefitComponent(CcwCodebookVariable.BENE_TOT_COINSRNC_DAYS_CNT, eob);
    Assert.assertEquals(
        coinsuranceDayCount.intValue(),
        benefit_BENE_TOT_COINSRNC_DAYS_CNT.getUsedUnsignedIntType().getValue().intValue());

    BenefitComponent benefit_CLM_NON_UTLZTN_DAYS_CNT =
        assertHasBenefitComponent(CcwCodebookVariable.CLM_NON_UTLZTN_DAYS_CNT, eob);
    Assert.assertEquals(
        nonUtilizationDayCount.intValue(),
        benefit_CLM_NON_UTLZTN_DAYS_CNT.getUsedUnsignedIntType().getValue().intValue());

    TransformerTestUtils.assertAdjudicationTotalAmountEquals(
        CcwCodebookVariable.NCH_BENE_IP_DDCTBL_AMT, deductibleAmount, eob);

    TransformerTestUtils.assertAdjudicationTotalAmountEquals(
        CcwCodebookVariable.NCH_BENE_PTA_COINSRNC_LBLTY_AMT, partACoinsuranceLiabilityAmount, eob);

    SupportingInformationComponent nchBloodPntsFrnshdQtyInfo =
        TransformerTestUtils.assertHasInfo(CcwCodebookVariable.NCH_BLOOD_PNTS_FRNSHD_QTY, eob);
    Assert.assertEquals(
        bloodPintsFurnishedQty.intValueExact(),
        nchBloodPntsFrnshdQtyInfo.getValueQuantity().getValue().intValueExact());

    TransformerTestUtils.assertAdjudicationTotalAmountEquals(
        CcwCodebookVariable.NCH_IP_NCVRD_CHRG_AMT, noncoveredCharge, eob);

    TransformerTestUtils.assertAdjudicationTotalAmountEquals(
        CcwCodebookVariable.NCH_IP_TOT_DDCTN_AMT, totalDeductionAmount, eob);

    if (claimPPSCapitalDisproportionateShareAmt.isPresent()) {
      TransformerTestUtils.assertAdjudicationTotalAmountEquals(
          CcwCodebookVariable.CLM_PPS_CPTL_DSPRPRTNT_SHR_AMT,
          claimPPSCapitalDisproportionateShareAmt,
          eob);
    }

    if (claimPPSCapitalExceptionAmount.isPresent()) {
      TransformerTestUtils.assertAdjudicationTotalAmountEquals(
          CcwCodebookVariable.CLM_PPS_CPTL_EXCPTN_AMT, claimPPSCapitalExceptionAmount, eob);
    }

    if (claimPPSCapitalFSPAmount.isPresent()) {
      TransformerTestUtils.assertAdjudicationTotalAmountEquals(
          CcwCodebookVariable.CLM_PPS_CPTL_FSP_AMT, claimPPSCapitalFSPAmount, eob);
    }

    if (claimPPSCapitalIMEAmount.isPresent()) {
      TransformerTestUtils.assertAdjudicationTotalAmountEquals(
          CcwCodebookVariable.CLM_PPS_CPTL_IME_AMT, claimPPSCapitalIMEAmount, eob);
    }

    if (claimPPSCapitalOutlierAmount.isPresent()) {
      TransformerTestUtils.assertAdjudicationTotalAmountEquals(
          CcwCodebookVariable.CLM_PPS_CPTL_OUTLIER_AMT, claimPPSCapitalOutlierAmount, eob);
    }

    if (claimPPSOldCapitalHoldHarmlessAmount.isPresent()) {
      TransformerTestUtils.assertAdjudicationTotalAmountEquals(
          CcwCodebookVariable.CLM_PPS_OLD_CPTL_HLD_HRMLS_AMT,
          claimPPSOldCapitalHoldHarmlessAmount,
          eob);
    }
  }

  /**
   * @param ccwVariable the {@link CcwCodebookVariable} matching the {@link
   *     BenefitComponent#getType()} to find
   * @param eob the {@link ExplanationOfBenefit} to search
   * @return the {@link BenefitComponent} that was found (if one wasn't, the method will instead
   *     fail with an {@link AssertionFailedError})
   */
  private static BenefitComponent assertHasBenefitComponent(
      CcwCodebookInterface ccwVariable, ExplanationOfBenefit eob) {
    // We only ever map one root EOB.benefitBalance.
    BenefitBalanceComponent benefitBalanceComponent = eob.getBenefitBalanceFirstRep();
    Assert.assertNotNull(benefitBalanceComponent);

    Optional<BenefitComponent> benefitOptional =
        benefitBalanceComponent.getFinancial().stream()
            .filter(
                bc ->
                    isCodeInConcept(
                        bc.getType(),
                        TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
                        TransformerUtils.calculateVariableReferenceUrl(ccwVariable)))
            .findFirst();
    Assert.assertTrue(benefitOptional.isPresent());

    return benefitOptional.get();
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
        eob.getInformation().stream()
            .filter(
                i ->
                    isCodeInConcept(
                        i.getCategory(),
                        TransformerConstants.CODING_BBAPI_INFORMATION_CATEGORY,
                        TransformerUtils.calculateVariableReferenceUrl(categoryVariable)))
            .findFirst();
    Assert.assertTrue(info.isPresent());

    return info.get();
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
      Object codeValue,
      ExplanationOfBenefit eob) {
    assertInfoWithCodeEquals(categoryVariable, codeSystemVariable, Optional.of(codeValue), eob);
  }

  /**
   * Tests EOB information fields that are common between the Inpatient and SNF claim types.
   *
   * @param eob the {@link ExplanationOfBenefit} that will be tested by this method
   * @param noncoveredStayFromDate NCH_VRFD_NCVRD_STAY_FROM_DT: an {@link Optional}&lt;{@link
   *     LocalDate}&gt; shared field representing the non-covered stay from date for the claim
   * @param noncoveredStayThroughDate NCH_VRFD_NCVRD_STAY_THRU_DT: an {@link Optional}&lt;{@link
   *     LocalDate}&gt; shared field representing the non-covered stay through date for the claim
   * @param coveredCareThroughDate NCH_ACTV_OR_CVRD_LVL_CARE_THRU: an {@link Optional}&lt;{@link
   *     LocalDate}&gt; shared field representing the covered stay through date for the claim
   * @param medicareBenefitsExhaustedDate NCH_BENE_MDCR_BNFTS_EXHTD_DT_I: an {@link
   *     Optional}&lt;{@link LocalDate}&gt; shared field representing the medicare benefits
   *     exhausted date for the claim
   * @param diagnosisRelatedGroupCd CLM_DRG_CD: an {@link Optional}&lt;{@link String}&gt; shared
   *     field representing the non-covered stay from date for the claim
   */
  static void assertCommonEobInformationInpatientSNF(
      ExplanationOfBenefit eob,
      Optional<LocalDate> noncoveredStayFromDate,
      Optional<LocalDate> noncoveredStayThroughDate,
      Optional<LocalDate> coveredCareThroughDate,
      Optional<LocalDate> medicareBenefitsExhaustedDate,
      Optional<String> diagnosisRelatedGroupCd) {
    /*
     * TODO missing tests for: admissionTypeCd, sourceAdmissionCd,
     * diagnosisAdmittingCode, diagnosisAdmittingCodeVersion
     */

    // noncoveredStayFromDate & noncoveredStayThroughDate
    if (noncoveredStayFromDate.isPresent() || noncoveredStayThroughDate.isPresent()) {
      SupportingInformationComponent nchVrfdNcvrdStayInfo =
          TransformerTestUtils.assertHasInfo(CcwCodebookVariable.NCH_VRFD_NCVRD_STAY_FROM_DT, eob);
      TransformerTestUtils.assertPeriodEquals(
          noncoveredStayFromDate,
          noncoveredStayThroughDate,
          (Period) nchVrfdNcvrdStayInfo.getTiming());
    }

    // coveredCareThroughDate
    if (coveredCareThroughDate.isPresent()) {
      SupportingInformationComponent nchActvOrCvrdLvlCareThruInfo =
          TransformerTestUtils.assertHasInfo(
              CcwCodebookVariable.NCH_ACTV_OR_CVRD_LVL_CARE_THRU, eob);
      TransformerTestUtils.assertDateEquals(
          coveredCareThroughDate.get(), (DateTimeType) nchActvOrCvrdLvlCareThruInfo.getTiming());
    }

    // medicareBenefitsExhaustedDate
    if (medicareBenefitsExhaustedDate.isPresent()) {
      SupportingInformationComponent nchBeneMdcrBnftsExhtdDtIInfo =
          TransformerTestUtils.assertHasInfo(
              CcwCodebookVariable.NCH_BENE_MDCR_BNFTS_EXHTD_DT_I, eob);
      TransformerTestUtils.assertDateEquals(
          medicareBenefitsExhaustedDate.get(),
          (BaseDateTimeType) nchBeneMdcrBnftsExhtdDtIInfo.getTiming());
    }

    // diagnosisRelatedGroupCd
    assertHasCoding(
        CcwCodebookVariable.CLM_DRG_CD,
        diagnosisRelatedGroupCd,
        eob.getDiagnosisFirstRep().getPackageCode());
  }

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
      ClaimType claimType,
      String claimGroupId,
      MedicareSegment coverageType,
      Optional<LocalDate> dateFrom,
      Optional<LocalDate> dateThrough,
      Optional<BigDecimal> paymentAmount,
      char finalAction) {

    assertNoEncodedOptionals(eob);

    Assert.assertEquals(
        TransformerUtils.buildEobId(claimType, claimId), eob.getIdElement().getIdPart());

    if (claimType.equals(ClaimType.PDE))
      assertHasIdentifier(CcwCodebookVariable.PDE_ID, claimId, eob.getIdentifier());
    else assertHasIdentifier(CcwCodebookVariable.CLM_ID, claimId, eob.getIdentifier());

    assertIdentifierExists(
        TransformerConstants.IDENTIFIER_SYSTEM_BBAPI_CLAIM_GROUP_ID,
        claimGroupId,
        eob.getIdentifier());
    Assert.assertEquals(
        TransformerUtils.referencePatient(beneficiaryId).getReference(),
        eob.getPatient().getReference());
    Assert.assertEquals(
        TransformerUtils.referenceCoverage(beneficiaryId, coverageType).getReference(),
        eob.getInsurance().getCoverage().getReference());

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

    if (dateFrom.isPresent()) {
      assertDateEquals(dateFrom.get(), eob.getBillablePeriod().getStartElement());
      assertDateEquals(dateThrough.get(), eob.getBillablePeriod().getEndElement());
    }

    if (paymentAmount.isPresent()) {
      Assert.assertEquals(paymentAmount.get(), eob.getPayment().getAmount().getValue());
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

    ReferralRequest referral = (ReferralRequest) eob.getReferral().getResource();
    Assert.assertEquals(
        TransformerUtils.referencePatient(beneficiaryId).getReference(),
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
  }

  /**
   * Test the transformation of the item level data elements between the {@link InpatientClaim}
   * {@link OutpatientClaim} {@link HospiceClaim} {@link HHAClaim}and {@link SNFClaim} claim types
   * to FHIR. The method parameter fields from {@link InpatientClaim} {@link OutpatientClaim} {@link
   * HospiceClaim} {@link HHAClaim}and {@link SNFClaim} are listed below and their corresponding RIF
   * CCW fields (denoted in all CAPS below from {@link InpatientClaimColumn} {@link
   * OutpatientClaimColumn} {@link HopsiceClaimColumn} {@link HHAClaimColumn} and {@link
   * SNFClaimColumn}).
   *
   * @param eob the {@ ExplanationOfBenefit} to test
   * @param organizationNpi ORG_NPI_NUM,
   * @param claimFacilityTypeCode CLM_FAC_TYPE_CD,
   * @param claimFrequencyCode CLM_FREQ_CD,
   * @param claimNonPaymentReasonCode CLM_MDCR_NON_PMT_RSN_CD,
   * @param patientDischargeStatusCode PTNT_DSCHRG_STUS_CD,
   * @param claimServiceClassificationTypeCode CLM_SRVC_CLSFCTN_TYPE_CD,
   * @param claimPrimaryPayerCode NCH_PRMRY_PYR_CD,
   * @param attendingPhysicianNpi AT_PHYSN_NPI,
   * @param totalChargeAmount CLM_TOT_CHRG_AMT,
   * @param primaryPayerPaidAmount NCH_PRMRY_PYR_CLM_PD_AMT,
   * @param fiscalIntermediaryNumber FI_NUMBER
   */
  static void assertEobCommonGroupInpOutHHAHospiceSNFEquals(
      ExplanationOfBenefit eob,
      Optional<String> organizationNpi,
      char claimFacilityTypeCode,
      char claimFrequencyCode,
      Optional<String> claimNonPaymentReasonCode,
      String patientDischargeStatusCode,
      char claimServiceClassificationTypeCode,
      Optional<Character> claimPrimaryPayerCode,
      Optional<String> attendingPhysicianNpi,
      BigDecimal totalChargeAmount,
      BigDecimal primaryPayerPaidAmount,
      Optional<String> fiscalIntermediaryNumber,
      Optional<String> fiDocumentClaimControlNumber,
      Optional<String> fiOriginalClaimControlNumber) {

    TransformerTestUtils.assertReferenceIdentifierEquals(
        TransformerConstants.CODING_NPI_US, organizationNpi.get(), eob.getOrganization());
    TransformerTestUtils.assertReferenceIdentifierEquals(
        TransformerConstants.CODING_NPI_US, organizationNpi.get(), eob.getFacility());

    assertExtensionCodingEquals(
        CcwCodebookVariable.CLM_FAC_TYPE_CD, claimFacilityTypeCode, eob.getFacility());

    // TODO add tests for claimFrequencyCode, patientDischargeStatusCode and
    // claimPrimaryPayerCode

    assertExtensionCodingEquals(
        CcwCodebookVariable.CLM_MDCR_NON_PMT_RSN_CD, claimNonPaymentReasonCode, eob);

    assertHasCoding(
        CcwCodebookVariable.CLM_SRVC_CLSFCTN_TYPE_CD,
        claimServiceClassificationTypeCode,
        eob.getType());

    TransformerTestUtils.assertCareTeamEquals(
        attendingPhysicianNpi.get(), ClaimCareteamrole.PRIMARY, eob);

    Assert.assertEquals(totalChargeAmount, eob.getTotalCost().getValue());
    TransformerTestUtils.assertAdjudicationTotalAmountEquals(
        CcwCodebookVariable.PRPAYAMT, primaryPayerPaidAmount, eob);

    if (fiscalIntermediaryNumber.isPresent()) {
      assertExtensionIdentifierEquals(CcwCodebookVariable.FI_NUM, fiscalIntermediaryNumber, eob);
    }
  }

  /**
   * Test the transformation of the item level data elements between the {@link InpatientClaimLine}
   * {@link OutpatientClaimLine} {@link HospiceClaimLine} {@link HHAClaimLine}and {@link
   * SNFClaimLine} claim types to FHIR. The method parameter fields from {@link InpatientClaimLine}
   * {@link OutpatientClaimLine} {@link HospiceClaimLine} {@link HHAClaimLine}and {@link
   * SNFClaimLine} are listed below and their corresponding RIF CCW fields (denoted in all CAPS
   * below from {@link InpatientClaimColumn} {@link OutpatientClaimColumn} {@link
   * HopsiceClaimColumn} {@link HHAClaimColumn} and {@link SNFClaimColumn}).
   *
   * @param item the {@ ItemComponent} to test
   * @param eob the {@ ExplanationOfBenefit} to test
   * @param revenueCenterCode REV_CNTR,
   * @param rateAmount REV_CNTR_RATE_AMT,
   * @param totalChargeAmount REV_CNTR_TOT_CHRG_AMT,
   * @param nonCoveredChargeAmount REV_CNTR_NCVRD_CHRG_AMT,
   * @param unitCount REV_CNTR_UNIT_CNT,
   * @param nationalDrugCodeQuantity REV_CNTR_NDC_QTY,
   * @param nationalDrugCodeQualifierCode REV_CNTR_NDC_QTY_QLFR_CD,
   * @param revenueCenterRenderingPhysicianNPI RNDRNG_PHYSN_NPI
   */
  static void assertEobCommonItemRevenueEquals(
      ItemComponent item,
      ExplanationOfBenefit eob,
      String revenueCenterCode,
      BigDecimal rateAmount,
      BigDecimal totalChargeAmount,
      BigDecimal nonCoveredChargeAmount,
      BigDecimal unitCount,
      String claimControlNum,
      Optional<BigDecimal> nationalDrugCodeQuantity,
      Optional<String> nationalDrugCodeQualifierCode,
      Optional<String> revenueCenterRenderingPhysicianNPI,
      int index) {

    assertHasCoding(CcwCodebookVariable.REV_CNTR, revenueCenterCode, item.getRevenue());

    TransformerTestUtils.assertAdjudicationAmountEquals(
        CcwCodebookVariable.REV_CNTR_RATE_AMT, rateAmount, item.getAdjudication());

    TransformerTestUtils.assertAdjudicationAmountEquals(
        CcwCodebookVariable.REV_CNTR_NCVRD_CHRG_AMT,
        nonCoveredChargeAmount,
        item.getAdjudication());
    TransformerTestUtils.assertAdjudicationAmountEquals(
        CcwCodebookVariable.REV_CNTR_TOT_CHRG_AMT, totalChargeAmount, item.getAdjudication());

    Assert.assertEquals(unitCount, item.getQuantity().getValue());

    if (nationalDrugCodeQualifierCode.isPresent()) {
      assertExtensionQuantityEquals(
          CcwCodebookVariable.REV_CNTR_NDC_QTY, nationalDrugCodeQuantity, item);
    }

    TransformerTestUtils.assertCareTeamEquals(
        revenueCenterRenderingPhysicianNPI.get(), ClaimCareteamrole.PRIMARY, eob);
  }

  /**
   * Test the transformation of the common group level data elements between the {@link
   * OutpatientClaim} {@link HospiceClaim} and {@link HHAClaim} claim types to FHIR. The method
   * parameter fields from {@link OutpatientClaim} {@link HospiceClaim} and {@link HHAClaim} are
   * listed below and their corresponding RIF CCW fields (denoted in all CAPS below from {@link
   * OutpatientClaimColumn} {@link HospiceClaimColumn} and {@link HHAClaimColumn}).
   *
   * @param item the {@link ItemComponent} to test
   * @param revenueCenterDate REV_CNTR_DT,
   * @param paymentAmount REV_CNTR_PMT_AMT_AMT
   */
  static void assertEobCommonItemRevenueOutHHAHospice(
      ItemComponent item, Optional<LocalDate> revenueCenterDate, BigDecimal paymentAmount)
      throws FHIRException {

    if (revenueCenterDate.isPresent()) {
      // Convert both LocalDate and Date type to millisconds to compare.
      Assert.assertEquals(
          java.sql.Date.valueOf(revenueCenterDate.get()).getTime(),
          item.getServicedDateType().getValue().getTime());
    }

    assertAdjudicationAmountEquals(
        CcwCodebookVariable.REV_CNTR_PMT_AMT_AMT, paymentAmount, item.getAdjudication());
  }

  /**
   * Test the transformation of the common group level data elements between the {@link
   * InpatientClaim} {@link OutpatientClaim} and {@link SNFClaim} claim types to FHIR. The method
   * parameter fields from {@link InpatientClaim} {@link OutpatientClaim} and {@link SNFClaim} are
   * listed below and their corresponding RIF CCW fields (denoted in all CAPS below from {@link
   * InpatientClaimColumn} {@link OutpatientClaimColumn} and {@link SNFClaimColumn}).
   *
   * @param eob the {@link ExplanationOfBenefit} to test
   */
  static void assertEobCommonGroupInpOutSNFEquals(
      ExplanationOfBenefit eob,
      BigDecimal bloodDeductibleLiabilityAmount,
      Optional<String> operatingPhysicianNpi,
      Optional<String> otherPhysicianNpi,
      char claimQueryCode,
      Optional<Character> mcoPaidSw) {
    TransformerTestUtils.assertAdjudicationTotalAmountEquals(
        CcwCodebookVariable.NCH_BENE_BLOOD_DDCTBL_LBLTY_AM, bloodDeductibleLiabilityAmount, eob);

    TransformerTestUtils.assertCareTeamEquals(
        operatingPhysicianNpi.get(), ClaimCareteamrole.ASSIST, eob);

    TransformerTestUtils.assertCareTeamEquals(
        otherPhysicianNpi.get(), ClaimCareteamrole.OTHER, eob);

    assertExtensionCodingEquals(
        CcwCodebookVariable.CLAIM_QUERY_CD, claimQueryCode, eob.getBillablePeriod());
  }

  /**
   * Test the transformation of the common group level data elements between the {@link
   * InpatientClaim} {@link HHAClaim} {@link HospiceClaim} and {@link SNFClaim} claim types to FHIR.
   * The method parameter fields from {@link InpatientClaim} {@link HHAClaim} {@link HospiceClaim}
   * and {@link SNFClaim} are listed below and their corresponding RIF CCW fields (denoted in all
   * CAPS below from {@link InpatientClaimColumn} {@link HospiceClaimColumn} {@link HHAClaimColumn}
   * and {@link SNFClaimColumn}).
   *
   * @param eob the {@link ExplanationOfBenefit} to test
   */
  static void assertEobCommonGroupInpHHAHospiceSNFEquals(
      ExplanationOfBenefit eob,
      Optional<LocalDate> claimAdmissionDate,
      Optional<LocalDate> beneficiaryDischargeDate,
      Optional<BigDecimal> utilizedDays) {

    TransformerTestUtils.assertDateEquals(
        claimAdmissionDate.get(), eob.getHospitalization().getStartElement());

    if (beneficiaryDischargeDate.isPresent()) {
      TransformerTestUtils.assertDateEquals(
          beneficiaryDischargeDate.get(), eob.getHospitalization().getEndElement());
    }

    if (utilizedDays.isPresent()) {
      TransformerTestUtils.assertBenefitBalanceUsedIntEquals(
          BenefitCategory.MEDICAL,
          CcwCodebookVariable.CLM_UTLZTN_DAY_CNT,
          utilizedDays.get().intValue(),
          eob);
    }
  }

  /**
   * Test the transformation of the common group level data elements between the {@link
   * InpatientClaim} {@link HHAClaim} {@link HospiceClaim} and {@link SNFClaim} claim types to FHIR.
   * The method parameter fields from {@link InpatientClaim} {@link HHAClaim} {@link HospiceClaim}
   * and {@link SNFClaim} are listed below and their corresponding RIF CCW fields (denoted in all
   * CAPS below from {@link InpatientClaimColumn} {@link HHAClaimColumn} {@link HospiceColumn} and
   * {@link SNFClaimColumn}).
   *
   * @param item the {@link ItemComponent} to test
   */
  static void assertEobCommonGroupInpHHAHospiceSNFCoinsuranceEquals(
      ItemComponent item, Optional<Character> deductibleCoinsuranceCd) {
    assertExtensionCodingEquals(
        CcwCodebookVariable.REV_CNTR_DDCTBL_COINSRNC_CD,
        deductibleCoinsuranceCd,
        item.getRevenue());
  }

  /**
   * Tests the provider number field is set as expected in the EOB. This field is common among these
   * claim types: Inpatient, Outpatient, Hospice, HHA and SNF.
   *
   * @param eob the {@link ExplanationOfBenefit} this method will test against
   * @param providerNumber a {@link String} PRVDR_NUM: representing the expected provider number for
   *     the claim
   */
  static void assertProviderNumber(ExplanationOfBenefit eob, String providerNumber) {
    assertReferenceIdentifierEquals(
        CcwCodebookVariable.PRVDR_NUM, providerNumber, eob.getProvider());
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
    if (hcpcsYearCode.isPresent()) { // some claim types have a year code...
      assertHasCoding(
          TransformerConstants.CODING_SYSTEM_HCPCS,
          "" + hcpcsYearCode.get(),
          null,
          hcpcsInitialModifierCode.get(),
          item.getModifier().get(index).getCoding());
      TransformerTestUtils.assertHasCoding(
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
        TransformerTestUtils.assertHasCoding(
            TransformerConstants.CODING_SYSTEM_HCPCS,
            hcpcsCode.get(),
            item.getService().getCoding());
      }
    }

    Assert.assertFalse(hcpcsSecondModifierCode.isPresent());
  }

  /**
   * @param expectedStartDate the expected value for {@link Period#getStart()}
   * @param expectedEndDate the expected value for {@link Period#getEnd()}
   * @param actualPeriod the {@link Period} to verify
   */
  static void assertPeriodEquals(
      Optional<LocalDate> expectedStartDate,
      Optional<LocalDate> expectedEndDate,
      Period actualPeriod) {
    Assert.assertTrue(expectedStartDate.isPresent() || expectedEndDate.isPresent());
    if (expectedStartDate.isPresent())
      assertDateEquals(expectedStartDate.get(), actualPeriod.getStartElement());
    if (expectedEndDate.isPresent())
      assertDateEquals(expectedEndDate.get(), actualPeriod.getEndElement());
  }

  /** @throws IOException */
  static void assertFDADrugCodeDisplayEquals(
      String nationalDrugCode, String nationalDrugCodeDisplayValue) throws IOException {
    String nationalDrugCodeDisplayValueActual =
        TransformerUtils.retrieveFDADrugCodeDisplay(nationalDrugCode);
    Assert.assertEquals(
        String.format("NDC code '%s' display value mismatch: ", nationalDrugCode),
        nationalDrugCodeDisplayValueActual,
        nationalDrugCodeDisplayValue);
  }

  /**
   * Tests that the NPI code display is set correctly
   *
   * @throws IOException
   */
  static void assertNPICodeDisplayEquals(String npiCode, String npiCodeDisplayValue)
      throws IOException {
    Assert.assertEquals(TransformerUtils.retrieveNpiCodeDisplay(npiCode), npiCodeDisplayValue);
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
}
