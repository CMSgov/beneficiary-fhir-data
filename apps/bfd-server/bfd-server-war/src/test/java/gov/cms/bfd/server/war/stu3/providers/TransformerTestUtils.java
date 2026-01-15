package gov.cms.bfd.server.war.stu3.providers;

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
import gov.cms.bfd.model.rif.entities.HHAClaimColumn;
import gov.cms.bfd.model.rif.entities.HHAClaimLine;
import gov.cms.bfd.model.rif.entities.HospiceClaim;
import gov.cms.bfd.model.rif.entities.HospiceClaimColumn;
import gov.cms.bfd.model.rif.entities.HospiceClaimLine;
import gov.cms.bfd.model.rif.entities.InpatientClaim;
import gov.cms.bfd.model.rif.entities.InpatientClaimColumn;
import gov.cms.bfd.model.rif.entities.InpatientClaimLine;
import gov.cms.bfd.model.rif.entities.OutpatientClaim;
import gov.cms.bfd.model.rif.entities.OutpatientClaimColumn;
import gov.cms.bfd.model.rif.entities.OutpatientClaimLine;
import gov.cms.bfd.model.rif.entities.PartDEvent;
import gov.cms.bfd.model.rif.entities.SNFClaim;
import gov.cms.bfd.model.rif.entities.SNFClaimColumn;
import gov.cms.bfd.model.rif.entities.SNFClaimLine;
import gov.cms.bfd.server.war.FDADrugCodeDisplayLookup;
import gov.cms.bfd.server.war.NPIOrgLookup;
import gov.cms.bfd.server.war.commons.CCWUtils;
import gov.cms.bfd.server.war.commons.ClaimType;
import gov.cms.bfd.server.war.commons.CommonTransformerUtils;
import gov.cms.bfd.server.war.commons.Diagnosis;
import gov.cms.bfd.server.war.commons.IdentifierType;
import gov.cms.bfd.server.war.commons.MedicareSegment;
import gov.cms.bfd.server.war.commons.SecurityTagManager;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import gov.cms.bfd.server.war.r4.providers.pac.common.ClaimWithSecurityTags;
import gov.cms.bfd.server.war.utils.RDATestUtils;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
import org.opentest4j.AssertionFailedError;

/**
 * Contains utility methods useful for testing the transformers (e.g. {@link
 * gov.cms.bfd.server.war.stu3.providers.BeneficiaryTransformer}).
 */
final class TransformerTestUtils {
  /** The fhir context for parsing the test file. Do this very slow operation once. */
  private static final FhirContext fhirContext = FhirContext.forDstu3();

  /** fake npi data. */
  private static final String ORG_FILE_NAME = "fakeOrgData.tsv";

  static {
    NPIOrgLookup npiOrgLookup = RDATestUtils.createTestNpiOrgLookup();
    CommonTransformerUtils.setNpiOrgLookup(npiOrgLookup);
  }

  /** Empty method used to trigger execution of the static initializer. */
  public static void touch() {
    // NOOP
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
   * Asserts the value in a {@link org.hl7.fhir.r4.model.Money} matches the expected value.
   *
   * @param expectedAmountValue the expected {@link Money#getValue()}
   * @param actualValue the actual {@link Money} to verify
   */
  private static void assertMoneyValue(BigDecimal expectedAmountValue, Money actualValue) {
    assertEquals(TransformerConstants.CODING_MONEY, actualValue.getSystem());
    assertEquals(TransformerConstants.CODED_MONEY_USD, actualValue.getCode());
    assertEquivalent(expectedAmountValue, actualValue.getValue());
  }

  /**
   * Assert adjudication component exists and matches the expected amount for its value.
   *
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
    assertTrue(adjudication.isPresent());
    assertEquivalent(expectedAmount, adjudication.get().getAmount().getValue());

    return adjudication.get();
  }

  /**
   * Assert the adjudication reason code equals the expected value.
   *
   * @param ccwVariable the {@link CcwCodebookVariable} for the {@link
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
    assertEquals(expectedReasonCode.isPresent(), adjudication.isPresent());

    if (expectedReasonCode.isPresent())
      assertHasCoding(ccwVariable, expectedReasonCode, adjudication.get().getReason());
  }

  /**
   * Assert the adjudication reason code equals the expected value.
   *
   * @param ccwVariable the {@link CcwCodebookVariable} for the {@link
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
   * Asserts the benefit balance equals the expected amount.
   *
   * <p>FIXME add allowed to method name
   *
   * @param expectedFinancialTypeSystem the expected {@link Coding#getSystem()} of the component to
   *     find and verify
   * @param expectedFinancialTypeCode the expected {@link Coding#getCode()} of the component to find
   *     and verify
   * @param expectedAmount the expected balance
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
    assertTrue(benefitComponent.isPresent());
    try {
      assertEquivalent(expectedAmount, benefitComponent.get().getAllowedMoney().getValue());
    } catch (FHIRException e) {
      throw new BadCodeMonkeyException(e);
    }
  }

  /**
   * Asserts the benefit balance equals the expected amount.
   *
   * @param expectedFinancialTypeSystem the expected {@link Coding#getSystem()} of the component to
   *     find and verify
   * @param expectedFinancialTypeCode the expected {@link Coding#getCode()} of the component to find
   *     and verify
   * @param expectedAmount the expected balance
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
    assertTrue(benefitComponent.isPresent());
    try {
      assertEquals(expectedAmount, benefitComponent.get().getAllowedUnsignedIntType().getValue());
    } catch (FHIRException e) {
      throw new BadCodeMonkeyException(e);
    }
  }

  /**
   * Assert benefit balance used amount equals the expected amount.
   *
   * @param expectedFinancialTypeSystem the expected {@link Coding#getSystem()} of the component to
   *     find and verify
   * @param expectedFinancialTypeCode the expected {@link Coding#getCode()} of the component to find
   *     and verify
   * @param expectedAmount the expected {@link BenefitComponent#getUsedMoney} amount
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
    assertTrue(benefitComponent.isPresent());
    try {
      assertEquals(expectedAmount, benefitComponent.get().getUsedUnsignedIntType().getValue());
    } catch (FHIRException e) {
      throw new BadCodeMonkeyException(e);
    }
  }

  /**
   * Assert benefit balance used amount equals the expected amount.
   *
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
    assertTrue(benefitBalanceComponent.isPresent());

    Optional<BenefitComponent> benefitBalanceFinancialEntry =
        benefitBalanceComponent.get().getFinancial().stream()
            .filter(
                f ->
                    isCodeInConcept(
                        f.getType(),
                        TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
                        CCWUtils.calculateVariableReferenceUrl(expectedFinancialType)))
            .findAny();
    assertTrue(benefitBalanceFinancialEntry.isPresent());

    try {
      assertEquals(
          expectedUsedInt, benefitBalanceFinancialEntry.get().getUsedUnsignedIntType().getValue());
    } catch (FHIRException e) {
      throw new BadCodeMonkeyException(e);
    }
  }

  /**
   * Assert an EOB's care team component contains the specified values.
   *
   * @param expectedPractitioner {@link CareTeamComponent#getProvider} to find and verify
   * @param expectedCareTeamRole the {@link ClaimCareteamrole} for {@link CareTeamComponent#getRole}
   *     to find and verify
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
    assertNotNull(careTeamEntry);
    assertCodingEquals(
        expectedCareTeamRole.getSystem(),
        expectedCareTeamRole.toCode(),
        careTeamEntry.getRole().getCodingFirstRep());
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
    if (expectedCode instanceof Character)
      assertEquals(((Character) expectedCode).toString(), actual.getCode());
    else if (expectedCode instanceof String)
      assertEquals(((String) expectedCode).trim(), actual.getCode());
    else throw new BadCodeMonkeyException();
  }

  /**
   * Asserts the provided dates are equal.
   *
   * @param expected the expected {@link LocalDate}
   * @param actual the actual {@link BaseDateTimeType} to verify
   */
  static void assertDateEquals(LocalDate expected, BaseDateTimeType actual) {
    assertEquals(CommonTransformerUtils.convertToDate(expected), actual.getValue());
    assertEquals(TemporalPrecisionEnum.DAY, actual.getPrecision());
  }

  /**
   * Verifies the presense of a specified {@link Diagnosis} in an EOB's {@link ItemComponent}.
   *
   * @param diagnosis the expected diagnosis to verify the presence of in the {@link ItemComponent}
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
    assertTrue(eobDiagnosis.isPresent());
    assertTrue(
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
    assertTrue(actual.precision() >= expected.precision());
    assertTrue(actual.scale() >= expected.scale());
    assertEquals(0, expected.compareTo(actual));
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
    if (expectedCode.isPresent())
      assertCodingEquals(expectedCodingSystem, expectedCode.get(), codingForSystem.get());
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
   * Asserts an extension's quantity and unit values equal the expected values.
   *
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
    String expectedExtensionUrl = CCWUtils.calculateVariableReferenceUrl(ccwVariableForQuantity);
    Optional<? extends IBaseExtension<?, ?>> actualExtension =
        actualElement.getExtension().stream()
            .filter(e -> e.getUrl().equals(expectedExtensionUrl))
            .findFirst();
    assertTrue(actualExtension.isPresent());
    assertTrue(actualExtension.get().getValue() instanceof Quantity);
    Quantity actualQuantity = (Quantity) actualExtension.get().getValue();

    String expectedUnitCodeString;
    if (expectedUnitCode instanceof String) expectedUnitCodeString = (String) expectedUnitCode;
    else if (expectedUnitCode instanceof Character)
      expectedUnitCodeString = ((Character) expectedUnitCode).toString();
    else throw new BadCodeMonkeyException("Unsupported: " + expectedUnitCode);

    assertEquals(expectedUnitCodeString, actualQuantity.getCode());
    assertEquals(
        CCWUtils.calculateVariableReferenceUrl(ccwVariableForUnit), actualQuantity.getSystem());
  }

  /**
   * Asserts an extension's quantity and unit values equal the expected values.
   *
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
    String expectedExtensionUrl = CCWUtils.calculateVariableReferenceUrl(ccwVariableForQuantity);
    Optional<? extends IBaseExtension<?, ?>> actualExtension =
        actualElement.getExtension().stream()
            .filter(e -> e.getUrl().equals(expectedExtensionUrl))
            .findFirst();
    assertEquals(expectedUnitCode.isPresent(), actualExtension.isPresent());

    if (expectedUnitCode.isPresent())
      assertQuantityUnitInfoEquals(
          ccwVariableForQuantity, ccwVariableForUnit, expectedUnitCode.get(), actualElement);
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
   * @param expectedSystem the expected {@link Coding#getSystem()} value
   * @param expectedCode the expected {@link Coding#getCode()} value
   * @param actualCode the actual {@link Coding} to verify
   */
  static void assertHasCoding(String expectedSystem, String expectedCode, Coding actualCode) {
    assertHasCoding(expectedSystem, null, null, expectedCode, Arrays.asList(actualCode));
  }

  /**
   * Asserts a {@link CodeableConcept} has an expected {@link Coding}.
   *
   * @param expectedSystem the expected {@link Coding#getSystem()} value
   * @param expectedCode the expected {@link Coding#getCode()} value
   * @param actualCode the actual {@link List}&lt;{@link Coding}&gt; to verify
   */
  static void assertHasCoding(String expectedSystem, String expectedCode, List<Coding> actualCode) {
    assertHasCoding(expectedSystem, null, null, expectedCode, actualCode);
  }

  /**
   * Asserts a {@link CodeableConcept} has expected {@link Coding} values.
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
   * Assert extension coding does not exist.
   *
   * @param ccwVariable the {@link CcwCodebookVariable} that the expected {@link Extension} / {@link
   *     Coding} are for
   * @param expectedCode the expected {@link Coding#getCode()}
   * @param actualElement the FHIR element to find and verify the {@link Extension} of
   */
  static void assertExtensionCodingDoesNotExist(
      CcwCodebookInterface ccwVariable,
      Optional<?> expectedCode,
      IBaseHasExtensions actualElement) {
    String expectedExtensionUrl = CCWUtils.calculateVariableReferenceUrl(ccwVariable);
    Optional<? extends IBaseExtension<?, ?>> extensionForUrl =
        actualElement.getExtension().stream()
            .filter(e -> e.getUrl().equals(expectedExtensionUrl))
            .findFirst();

    assertEquals(false, extensionForUrl.isPresent());
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
   * Tests that the specified extension list contains a single Identifier with the expected string
   * value.
   *
   * @param extension a {@link List}&lt;{@link Extension}&gt; containing an Identifier
   * @param expected a {@link String} containing the expected value of the Identifier
   */
  static void assertExtensionIdentifierEqualsString(List<Extension> extension, String expected) {
    assertEquals(1, extension.size());
    assertTrue(extension.get(0).getValue() instanceof Identifier);
    Identifier identifier = (Identifier) extension.get(0).getValue();
    assertEquals(expected, identifier.getValue());
  }

  /**
   * Assert extension value quantity matches the expected values.
   *
   * @param extension the extension
   * @param expectedExtensionUrl the expected extension url
   * @param expectedSystem the expected system
   * @param expectedValue the expected value
   */
  static void assertExtensionValueQuantityEquals(
      List<Extension> extension,
      String expectedExtensionUrl,
      String expectedSystem,
      BigDecimal expectedValue) {
    assertEquals(1, extension.size());
    assertEquals(extension.get(0).getUrl(), expectedExtensionUrl);
    assertTrue(extension.get(0).getValue() instanceof Quantity);
    Quantity quantity = (Quantity) extension.get(0).getValue();
    assertEquals(expectedValue, quantity.getValue());
    assertEquals(expectedSystem, quantity.getSystem());
  }

  /**
   * Asserts the information date has the expected values.
   *
   * @param expectedSystem the expected {@link Coding#getSystem()} of the {@link
   *     SupportingInformationComponent#getCategory()} to find and verify
   * @param expectedCode the expected {@link Coding#getCode()} of the {@link
   *     SupportingInformationComponent#getCategory()} to find and verify
   * @param expectedDate the expected {@link SupportingInformationComponent#getTiming()}
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
    assertTrue(supportingInformationComponent.isPresent());
    assertEquals(
        expectedDate.toString(), supportingInformationComponent.get().getTiming().primitiveValue());
  }

  /**
   * Asserts the information date has the expected values.
   *
   * @param expectedCategorySystem the expected value for {@link
   *     SupportingInformationComponent#getCategory()}'s {@link Coding#getSystem()}
   * @param expectedCategoryCodeVariable the expected {@link CcwCodebookVariable} for {@link
   *     SupportingInformationComponent#getCategory()}'s {@link Coding#getCode()}
   * @param expectedDate the expected {@link SupportingInformationComponent#getTiming()}
   * @param actuals the actual {@link SupportingInformationComponent}s to verify
   */
  static void assertInformationDateEquals(
      String expectedCategorySystem,
      CcwCodebookVariable expectedCategoryCodeVariable,
      LocalDate expectedDate,
      List<SupportingInformationComponent> actuals) {
    String expectedCategoryCode =
        CCWUtils.calculateVariableReferenceUrl(expectedCategoryCodeVariable);
    Optional<SupportingInformationComponent> supportingInformationComponent =
        actuals.stream()
            .filter(
                a -> isCodeInConcept(a.getCategory(), expectedCategorySystem, expectedCategoryCode))
            .findAny();
    assertTrue(supportingInformationComponent.isPresent());
    assertEquals(
        expectedDate.toString(), supportingInformationComponent.get().getTiming().primitiveValue());
  }

  /**
   * Asserts the information period has the expected values.
   *
   * @param expectedSystem the expected {@link Coding#getSystem()} of the {@link
   *     SupportingInformationComponent#getCategory()} to find and verify
   * @param expectedCode the expected {@link Coding#getCode()} of the {@link
   *     SupportingInformationComponent#getCategory()} to find and verify
   * @param expectedFromDate the expected {@link SupportingInformationComponent#getTimingPeriod()}
   *     getStartElement()
   * @param expectedThruDate the expected {@link SupportingInformationComponent#getTimingPeriod()}
   *     getEndElement()
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
    assertTrue(supportingInformationComponent.isPresent());
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
   * Asserts the information period has the expected values.
   *
   * @param expectedCategorySystem the expected value for {@link
   *     SupportingInformationComponent#getCategory()}'s {@link Coding#getSystem()}
   * @param expectedCategoryCodeVariable the expected {@link CcwCodebookVariable} for {@link
   *     SupportingInformationComponent#getCategory()}'s {@link Coding#getCode()}
   * @param expectedFromDate the expected {@link SupportingInformationComponent#getTimingPeriod()}
   *     getStartElement()
   * @param expectedThruDate the expected {@link SupportingInformationComponent#getTimingPeriod()}
   *     getEndElement()
   * @param actuals the actual {@link SupportingInformationComponent}s to verify
   */
  static void assertInformationPeriodEquals(
      String expectedCategorySystem,
      CcwCodebookVariable expectedCategoryCodeVariable,
      LocalDate expectedFromDate,
      LocalDate expectedThruDate,
      List<SupportingInformationComponent> actuals) {
    String expectedCategoryCode =
        CCWUtils.calculateVariableReferenceUrl(expectedCategoryCodeVariable);
    Optional<SupportingInformationComponent> supportingInformationComponent =
        actuals.stream()
            .filter(
                a -> isCodeInConcept(a.getCategory(), expectedCategorySystem, expectedCategoryCode))
            .findAny();
    assertTrue(supportingInformationComponent.isPresent());
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
    // HAPI FHIR performs some normalization steps before serializing the object,
    // so it's important to call copy() first due to Java's pass by reference semantics.
    // Otherwise, the object will be modified in place.
    String encodedResourceXml = fhirContext.newXmlParser().encodeResourceToString(resource.copy());
    assertFalse(encodedResourceXml.contains("Optional"));
  }

  /**
   * Asserts the {@link Reference} has the expected values.
   *
   * @param expectedIdentifierSystem the expected {@link Identifier#getSystem()} to match
   * @param expectedIdentifierValue the expected {@link Identifier#getValue()} to match
   * @param actualReference the {@link Reference} to check
   */
  static void assertReferenceEquals(
      String expectedIdentifierSystem, String expectedIdentifierValue, Reference actualReference) {
    assertTrue(
        doesReferenceMatchIdentifier(
            expectedIdentifierSystem, expectedIdentifierValue, actualReference),
        "Reference doesn't match: " + actualReference);
  }

  /**
   * Asserts the reference identifier has the expected values.
   *
   * @param expectedIdentifierSystem the expected {@link Identifier#getSystem()} value
   * @param expectedIdentifierValue the expected {@link Identifier#getValue()} value
   * @param reference the actual {@link Reference} to verify
   */
  static void assertReferenceIdentifierEquals(
      String expectedIdentifierSystem, String expectedIdentifierValue, Reference reference) {
    assertTrue(reference.hasIdentifier(), "Bad reference: \" + reference");
    assertEquals(expectedIdentifierSystem, reference.getIdentifier().getSystem());
    assertEquals(expectedIdentifierValue, reference.getIdentifier().getValue());
    assertEquals(
        CommonTransformerUtils.retrieveNpiCodeDisplay(expectedIdentifierValue),
        reference.getDisplay());
  }

  /**
   * Asserts the reference identifier has the expected values.
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
   * Asserts the given {@link CodeableConcept} has the expected values.
   *
   * @param eobType the eobType {@link CodeableConcept} we are testing against for expected values
   * @param blueButtonClaimType expected blue button {@link
   *     gov.cms.bfd.server.war.commons.ClaimType} value
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
   * Validates if the reference matches the identifier.
   *
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
   * Does the role of the care team component match what is expected.
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
   * Finds the care team entry for a given npi provider.
   *
   * @param expectedProviderNpi the {@link Identifier#getValue()} of the provider to find a matching
   *     {@link CareTeamComponent} for
   * @param careTeam the {@link List} of {@link CareTeamComponent}s to search
   * @return the {@link CareTeamComponent} whose {@link CareTeamComponent#getProvider()} is an
   *     {@link Identifier} with the specified provider NPI, or else <code>null</code> if no such
   *     {@link CareTeamComponent} was found
   */
  static CareTeamComponent findCareTeamEntryForProviderNpi(
      String expectedProviderNpi, List<CareTeamComponent> careTeam) {
    return findCareTeamEntryForProviderIdentifier(
        TransformerConstants.CODING_NPI_US, expectedProviderNpi, null, careTeam);
  }

  /**
   * Finds the care team entry for a given tax number provider.
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
        IdentifierType.TAX.getSystem(), expectedProviderTaxNumber, null, careTeam);
  }

  /**
   * Finds the care team entry for a given id provider.
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
   * Verifies a {@link Coding} exists within the specified {@link CodeableConcept}.
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
   * Verifies a {@link Coding} exists within the specified {@link CodeableConcept}.
   *
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
              if (!CCWUtils.calculateVariableReferenceUrl(ccwVariable).equals(c.getSystem()))
                return false;
              return expectedCodeString.equals(c.getCode());
            });
  }

  /**
   * Verifies a {@link Coding} exists within the specified {@link CodeableConcept}.
   *
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
    assertEquals(
        coinsuranceDayCount.intValue(),
        benefit_BENE_TOT_COINSRNC_DAYS_CNT.getUsedUnsignedIntType().getValue().intValue());

    BenefitComponent benefit_CLM_NON_UTLZTN_DAYS_CNT =
        assertHasBenefitComponent(CcwCodebookVariable.CLM_NON_UTLZTN_DAYS_CNT, eob);
    assertEquals(
        nonUtilizationDayCount.intValue(),
        benefit_CLM_NON_UTLZTN_DAYS_CNT.getUsedUnsignedIntType().getValue().intValue());

    TransformerTestUtils.assertAdjudicationTotalAmountEquals(
        CcwCodebookVariable.NCH_BENE_IP_DDCTBL_AMT, deductibleAmount, eob);

    TransformerTestUtils.assertAdjudicationTotalAmountEquals(
        CcwCodebookVariable.NCH_BENE_PTA_COINSRNC_LBLTY_AMT, partACoinsuranceLiabilityAmount, eob);

    SupportingInformationComponent nchBloodPntsFrnshdQtyInfo =
        TransformerTestUtils.assertHasInfo(CcwCodebookVariable.NCH_BLOOD_PNTS_FRNSHD_QTY, eob);
    assertEquals(
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
   * Assert a benefit component exists in the provided EOB.
   *
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
    assertNotNull(benefitBalanceComponent);

    Optional<BenefitComponent> benefitOptional =
        benefitBalanceComponent.getFinancial().stream()
            .filter(
                bc ->
                    isCodeInConcept(
                        bc.getType(),
                        TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
                        CCWUtils.calculateVariableReferenceUrl(ccwVariable)))
            .findFirst();
    assertTrue(benefitOptional.isPresent());

    return benefitOptional.get();
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
        eob.getInformation().stream()
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

    assertEquals(1, eob.getDiagnosisFirstRep().getSequence());
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

    assertEquals(
        CommonTransformerUtils.buildEobId(claimType, claimId), eob.getIdElement().getIdPart());

    assertHasIdentifier(
        claimType.equals(ClaimType.PDE) ? CcwCodebookVariable.PDE_ID : CcwCodebookVariable.CLM_ID,
        String.valueOf(claimId),
        eob.getIdentifier());

    assertIdentifierExists(
        TransformerConstants.IDENTIFIER_SYSTEM_BBAPI_CLAIM_GROUP_ID,
        claimGroupId,
        eob.getIdentifier());
    assertEquals(
        TransformerUtils.referencePatient(beneficiaryId).getReference(),
        eob.getPatient().getReference());
    assertEquals(
        TransformerUtils.referenceCoverage(beneficiaryId, coverageType).getReference(),
        eob.getInsurance().getCoverage().getReference());

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

    if (dateFrom.isPresent()) {
      assertDateEquals(dateFrom.get(), eob.getBillablePeriod().getStartElement());
      assertDateEquals(dateThrough.get(), eob.getBillablePeriod().getEndElement());
    }

    if (paymentAmount.isPresent()) {
      assertEquals(paymentAmount.get(), eob.getPayment().getAmount().getValue());
    }
  }

  /**
   * Test the transformation of the common group level data elements between the {@link
   * CarrierClaim} and {@link DMEClaim} claim types to FHIR. The method parameter fields from {@link
   * CarrierClaim} and {@link DMEClaim} are listed below and their corresponding RIF CCW fields
   * (denoted in all CAPS below from {@link CarrierClaimColumn} and {@link DMEClaimColumn}).
   *
   * @param eob the {@link ExplanationOfBenefit} to test
   * @param beneficiaryId BENE_ID, *
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
      Long beneficiaryId,
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
    assertEquals(
        TransformerUtils.referencePatient(beneficiaryId).getReference(),
        referral.getSubject().getReference());
    assertReferenceIdentifierEquals(
        TransformerConstants.CODING_NPI_US,
        referringPhysicianNpi.get(),
        referral.getRequester().getAgent());
    assertEquals(1, referral.getRecipient().size());
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
   * @throws FHIRException (indicates test failure)
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
            CCWUtils.calculateVariableReferenceUrl(CcwCodebookVariable.LINE_HCT_HGB_RSLT_NUM));
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
  }

  /**
   * Test the transformation of the item level data elements between the {@link InpatientClaim}
   * {@link OutpatientClaim} {@link HospiceClaim} {@link HHAClaim}and {@link SNFClaim} claim types
   * to FHIR. The method parameter fields from {@link InpatientClaim} {@link OutpatientClaim} {@link
   * HospiceClaim}* {@link HHAClaim}and {@link SNFClaim} are listed below and their corresponding
   * RIF CCW fields (denoted in all CAPS below from {@link InpatientClaimColumn} {@link
   * OutpatientClaimColumn}* {@link HospiceClaimColumn} {@link HHAClaimColumn} and {@link
   * SNFClaimColumn}).
   *
   * @param eob the {@link ExplanationOfBenefit} to test
   * @param organizationNpi ORG_NPI_NUM,
   * @param organizationNpiDisplay the organization npi display
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
   * @param fiDocumentClaimControlNumber the fi document claim control number
   * @param fiOriginalClaimControlNumber the fi original claim control number
   */
  static void assertEobCommonGroupInpOutHHAHospiceSNFEquals(
      ExplanationOfBenefit eob,
      Optional<String> organizationNpi,
      Optional<String> organizationNpiDisplay,
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

    assertEquals(totalChargeAmount, eob.getTotalCost().getValue());
    TransformerTestUtils.assertAdjudicationTotalAmountEquals(
        CcwCodebookVariable.PRPAYAMT, primaryPayerPaidAmount, eob);

    if (fiscalIntermediaryNumber.isPresent()) {
      assertExtensionIdentifierEquals(CcwCodebookVariable.FI_NUM, fiscalIntermediaryNumber, eob);
    }
  }

  /**
   * Test the transformation of the item level data elements between the {@link InpatientClaimLine}
   * {@link OutpatientClaimLine} {@link HospiceClaimLine} {@link HHAClaimLine}and {@link
   * SNFClaimLine}* claim types to FHIR. The method parameter fields from {@link InpatientClaimLine}
   * {@link OutpatientClaimLine} {@link HospiceClaimLine} {@link HHAClaimLine}and {@link
   * SNFClaimLine}* are listed below and their corresponding RIF CCW fields (denoted in all CAPS
   * below from {@link InpatientClaimColumn} {@link OutpatientClaimColumn} {@link
   * HospiceClaimColumn}* {@link HHAClaimColumn} and {@link SNFClaimColumn}).
   *
   * @param item the {@link ItemComponent} to test
   * @param eob the {@link ExplanationOfBenefit} to test
   * @param revenueCenterCode REV_CNTR,
   * @param rateAmount REV_CNTR_RATE_AMT,
   * @param totalChargeAmount REV_CNTR_TOT_CHRG_AMT,
   * @param nonCoveredChargeAmount REV_CNTR_NCVRD_CHRG_AMT,
   * @param unitCount REV_CNTR_UNIT_CNT,
   * @param claimControlNum the claim control num
   * @param nationalDrugCodeQuantity REV_CNTR_NDC_QTY,
   * @param nationalDrugCodeQualifierCode REV_CNTR_NDC_QTY_QLFR_CD,
   * @param revenueCenterRenderingPhysicianNPI RNDRNG_PHYSN_NPI
   * @param index the index
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

    assertEquals(unitCount, item.getQuantity().getValue());

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
      assertEquals(
          java.sql.Date.valueOf(revenueCenterDate.get()).getTime(),
          item.getServicedDateType().getValue().getTime());
    }

    assertAdjudicationAmountEquals(
        CcwCodebookVariable.REV_CNTR_PMT_AMT_AMT, paymentAmount, item.getAdjudication());
  }

  /**
   * Test the transformation of the common group level data elements between the {@link
   * InpatientClaim}* {@link OutpatientClaim} and {@link SNFClaim} claim types to FHIR. The method
   * parameter fields from {@link InpatientClaim} {@link OutpatientClaim} and {@link SNFClaim} are
   * listed below and their corresponding RIF CCW fields (denoted in all CAPS below from {@link
   * InpatientClaimColumn}* {@link OutpatientClaimColumn} and {@link SNFClaimColumn}).
   *
   * @param eob the {@link ExplanationOfBenefit} to test
   * @param bloodDeductibleLiabilityAmount the blood deductible liability amount
   * @param operatingPhysicianNpi the operating physician npi
   * @param otherPhysicianNpi the other physician npi
   * @param claimQueryCode the claim query code
   * @param mcoPaidSw the mco paid sw
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
   * InpatientClaim}* {@link HHAClaim} {@link HospiceClaim} and {@link SNFClaim} claim types to
   * FHIR. The method parameter fields from {@link InpatientClaim} {@link HHAClaim} {@link
   * HospiceClaim} and {@link SNFClaim} are listed below and their corresponding RIF CCW fields
   * (denoted in all CAPS below from {@link InpatientClaimColumn} {@link HospiceClaimColumn} {@link
   * HHAClaimColumn} and {@link SNFClaimColumn}).
   *
   * @param eob the {@link ExplanationOfBenefit} to test
   * @param claimAdmissionDate the claim admission date
   * @param beneficiaryDischargeDate the beneficiary discharge date
   * @param utilizedDays the utilized days
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
   * InpatientClaim}* {@link HHAClaim} {@link HospiceClaim} and {@link SNFClaim} claim types to
   * FHIR. The method parameter fields from {@link InpatientClaim} {@link HHAClaim} {@link
   * HospiceClaim} and {@link SNFClaim} are listed below and their corresponding RIF CCW fields
   * (denoted in all CAPS below from {@link InpatientClaimColumn} {@link HHAClaimColumn} {@link
   * HospiceClaimColumn} and {@link SNFClaimColumn}).
   *
   * @param item the {@link ItemComponent} to test
   * @param deductibleCoinsuranceCd the deductible coinsurance cd
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

    assertFalse(hcpcsSecondModifierCode.isPresent());
  }

  /**
   * Assert the {@link Period} has the expected values.
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
   * Assert fda drug code display is the expected value.
   *
   * @param nationalDrugCode the national drug code
   * @param nationalDrugCodeDisplayValue the national drug code display value
   * @throws IOException the io exception (test failure)
   */
  static void assertFDADrugCodeDisplayEquals(
      String nationalDrugCode, String nationalDrugCodeDisplayValue) throws IOException {
    FDADrugCodeDisplayLookup drugCodeDisplayLookup = RDATestUtils.createFdaDrugCodeDisplayLookup();
    String normalizedDrugCode = CommonTransformerUtils.normalizeDrugCode(nationalDrugCode);
    String nationalDrugCodeDisplayValueActual =
        drugCodeDisplayLookup
            .retrieveFDADrugCodeDisplay(Set.of(normalizedDrugCode))
            .get(normalizedDrugCode);
    assertEquals(
        nationalDrugCodeDisplayValue,
        nationalDrugCodeDisplayValueActual,
        String.format("NDC code '%s' display value mismatch: ", normalizedDrugCode));
  }

  /**
   * Tests that the NPI code display is set correctly.
   *
   * @param npiCode the npi code
   * @param npiCodeDisplayValue the npi code display value
   * @throws IOException the io exception (test failure)
   */
  static void assertNPICodeDisplayEquals(String npiCode, String npiCodeDisplayValue)
      throws IOException {
    assertEquals(CommonTransformerUtils.retrieveNpiCodeDisplay(npiCode), npiCodeDisplayValue);
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
   * Transform rif record to eob explanation of benefit.
   *
   * @param rifRecord          the RIF record (e.g. a {@link CarrierClaim} instance) to transform
   * @param metricRegistry     the {@link MetricRegistry} to use
   * @param securityTagManager SamhsaSecurityTag lookup
   * @return the transformed {@link ExplanationOfBenefit} for the specified RIF record
   */
  static ExplanationOfBenefit transformRifRecordToEob(
      Object rifRecord,
      MetricRegistry metricRegistry,
      SecurityTagManager securityTagManager) {

    ClaimTransformerInterface claimTransformerInterface = null;
    if (rifRecord instanceof CarrierClaim) {
      claimTransformerInterface =
          new CarrierClaimTransformer(metricRegistry, securityTagManager, false);
    } else if (rifRecord instanceof DMEClaim) {
      claimTransformerInterface =
          new DMEClaimTransformer(metricRegistry, securityTagManager, false);
    } else if (rifRecord instanceof HHAClaim) {
      claimTransformerInterface =
          new HHAClaimTransformer(metricRegistry, securityTagManager, false);
    } else if (rifRecord instanceof HospiceClaim) {
      claimTransformerInterface =
          new HospiceClaimTransformer(metricRegistry, securityTagManager, false);
    } else if (rifRecord instanceof InpatientClaim) {
      claimTransformerInterface =
          new InpatientClaimTransformer(metricRegistry, securityTagManager, false);
    } else if (rifRecord instanceof OutpatientClaim) {
      claimTransformerInterface =
          new OutpatientClaimTransformer(metricRegistry, securityTagManager, false);
    } else if (rifRecord instanceof PartDEvent) {
      claimTransformerInterface = new PartDEventTransformer(metricRegistry);
    } else if (rifRecord instanceof SNFClaim) {
      claimTransformerInterface =
          new SNFClaimTransformer(metricRegistry, securityTagManager, false);
    } else {
      throw new BadCodeMonkeyException("Unhandled RifRecord type!");
    }
    Set<String> securityTags = new HashSet<>();
    return claimTransformerInterface.transform(
        new ClaimWithSecurityTags<>(rifRecord, securityTags));
  }
}
