package gov.hhs.cms.bluebutton.server.app.stu3.providers;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.DateTimeType;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.AdjudicationComponent;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.BenefitComponent;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.CareTeamComponent;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.DiagnosisComponent;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.ItemComponent;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.SupportingInformationComponent;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.ReferralRequest;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.dstu3.model.TemporalPrecisionEnum;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IBaseHasExtensions;
import org.junit.Assert;

import com.justdavis.karl.misc.exceptions.BadCodeMonkeyException;

import ca.uhn.fhir.context.FhirContext;
import gov.hhs.cms.bluebutton.data.model.rif.CarrierClaim;
import gov.hhs.cms.bluebutton.data.model.rif.CarrierClaimLine;
import gov.hhs.cms.bluebutton.data.model.rif.DMEClaim;
import gov.hhs.cms.bluebutton.data.model.rif.DMEClaimLine;

/**
 * Contains utility methods useful for testing the transformers (e.g.
 * {@link BeneficiaryTransformer}).
 */
final class TransformerTestUtils {
	/**
	 * @param expectedCategoryCode
	 *            the expected {@link Coding#getCode()} of the
	 *            {@link AdjudicationComponent#getCategory()} to find and verify
	 * @param expectedAmount
	 *            the expected {@link AdjudicationComponent#getAmount()}
	 * @param actuals
	 *            the actual {@link AdjudicationComponent}s to verify
	 */
	static void assertAdjudicationEquals(String expectedCategoryCode, BigDecimal expectedAmount,
			List<AdjudicationComponent> actuals) {
		Optional<AdjudicationComponent> adjudication = actuals.stream().filter(a -> isCodeInConcept(a.getCategory(),
				TransformerConstants.CODING_CCW_ADJUDICATION_CATEGORY, expectedCategoryCode)).findAny();
		Assert.assertTrue(adjudication.isPresent());
		assertEquivalent(expectedAmount, adjudication.get().getAmount().getValue());
	}

	/**
	 * @param expectedCategoryCode
	 *            the expected {@link Coding#getCode()} of the
	 *            {@link AdjudicationComponent#getCategory()} to verify is not
	 *            present
	 * @param actuals
	 *            the actual {@link AdjudicationComponent}s to verify
	 */
	static void assertAdjudicationNotPresent(String expectedCategoryCode, List<AdjudicationComponent> actuals) {
		Optional<AdjudicationComponent> adjudication = actuals.stream().filter(a -> isCodeInConcept(a.getCategory(),
				TransformerConstants.CODING_CCW_ADJUDICATION_CATEGORY, expectedCategoryCode)).findAny();
		Assert.assertFalse(adjudication.isPresent());
	}

	/**
	 * @param expectedCategoryCode
	 *            the expected {@link Coding#getCode()} of the
	 *            {@link AdjudicationComponent#getCategory()} to find and verify
	 * @param expectedReasonSystem
	 *            the expected {@link Coding#getSystem()} of the
	 *            {@link AdjudicationComponent#getReason()} to find and verify
	 * @param expectedReasonCode
	 *            the expected {@link Coding#getCode()} of the
	 *            {@link AdjudicationComponent#getReason()} to find and verify
	 * @param actuals
	 *            the actual {@link AdjudicationComponent}s to verify
	 */
	static void assertAdjudicationReasonEquals(String expectedCategoryCode, String expectedReasonSystem,
			String expectedReasonCode, List<AdjudicationComponent> actuals) {
		Optional<AdjudicationComponent> adjudication = actuals.stream().filter(a -> isCodeInConcept(a.getCategory(),
				TransformerConstants.CODING_CCW_ADJUDICATION_CATEGORY, expectedCategoryCode)).findAny();
		Assert.assertTrue(adjudication.isPresent());
		assertHasCoding(expectedReasonSystem, expectedReasonCode, adjudication.get().getReason());
	}

	/**
	 * @param expectedFinancialTypeSystem
	 *            the expected {@link Coding#getSystem()} of the
	 *            {@link BenefitComponent#getCode)} to find and verify
	 * @param expectedFinancialTypeCode
	 *            the expected {@link Coding#getCode()} of the
	 *            {@link BenefitComponent#getCode)} to find and verify
	 * @param expectedAmount
	 *            the expected {@link BenefitComponent#getBenefitMoney}
	 * @param actuals
	 *            the actual {@link BenefitComponent}s to verify
	 */
	static void assertBenefitBalanceEquals(String expectedFinancialTypeSystem, String expectedFinancialTypeCode,
			BigDecimal expectedAmount, List<BenefitComponent> actuals) {
		Optional<BenefitComponent> benefitComponent = actuals.stream()
				.filter(a -> isCodeInConcept(a.getType(), expectedFinancialTypeSystem, expectedFinancialTypeCode))
				.findFirst();
		Assert.assertTrue(benefitComponent.isPresent());
		try {
			assertEquivalent(expectedAmount, benefitComponent.get().getAllowedMoney().getValue());
		} catch (FHIRException e) {
			throw new BadCodeMonkeyException(e);
		}
	}

	/**
	 * @param expectedFinancialTypeSystem
	 *            the expected {@link Coding#getSystem()} of the
	 *            {@link BenefitComponent#getCode)} to find and verify
	 * @param expectedFinancialTypeCode
	 *            the expected {@link Coding#getCode()} of the
	 *            {@link BenefitComponent#getCode)} to find and verify
	 * @param expectedAmount
	 *            the expected
	 *            {@link BenefitComponent#getBenefitUnsignedIntType()}
	 * @param actuals
	 *            the actual {@link BenefitComponent}s to verify
	 */
	static void assertBenefitBalanceEquals(String expectedFinancialTypeSystem, String expectedFinancialTypeCode,
			Integer expectedAmount, List<BenefitComponent> actuals) {
		Optional<BenefitComponent> benefitComponent = actuals.stream()
				.filter(a -> isCodeInConcept(a.getType(), expectedFinancialTypeSystem, expectedFinancialTypeCode))
				.findFirst();
		Assert.assertTrue(benefitComponent.isPresent());
		try {
			Assert.assertEquals(expectedAmount, benefitComponent.get().getAllowedUnsignedIntType().getValue());
		} catch (FHIRException e) {
			throw new BadCodeMonkeyException(e);
		}
	}

	/**
	 * @param expectedFinancialTypeSystem
	 *            the expected {@link Coding#getSystem()} of the
	 *            {@link BenefitComponent#getCode)} to find and verify
	 * @param expectedFinancialTypeCode
	 *            the expected {@link Coding#getCode()} of the
	 *            {@link BenefitComponent#getCode)} to find and verify
	 * @param expectedAmount
	 *            the expected
	 *            {@link BenefitComponent#getBenefitUsedUnsignedIntType}
	 * @param actuals
	 *            the actual {@link BenefitComponent}s to verify
	 */

	static void assertBenefitBalanceUsedEquals(String expectedFinancialTypeSystem,
			String expectedFinancialTypeCode, Integer expectedAmount, List<BenefitComponent> actuals) {
		Optional<BenefitComponent> benefitComponent = actuals.stream()
				.filter(a -> isCodeInConcept(a.getType(), expectedFinancialTypeSystem, expectedFinancialTypeCode))
				.findFirst();
		Assert.assertTrue(benefitComponent.isPresent());
		try {
			Assert.assertEquals(expectedAmount, benefitComponent.get().getUsedUnsignedIntType().getValue());
		} catch (FHIRException e) {
			throw new BadCodeMonkeyException(e);
		}
	}

	/**
	 * @param expectedPractitioner
	 *            {@link CareTeamComponent#getProviderIdentifier)} to find and
	 *            verify
	 * @param expectedPractitionerRole
	 *            {@link CareTeamComponent#getRole)} to find and verify
	 * @param eob
	 *            the actual {@link ExplanationOfBenefit}s to verify
	 */

	static void assertCareTeamEquals(String expectedPractitioner, String expectedPractitionerRole,
			ExplanationOfBenefit eob) {
		CareTeamComponent careTeamEntry = findCareTeamEntryForProviderIdentifier(
				TransformerConstants.CODING_NPI_US, expectedPractitioner, eob.getCareTeam());
		Assert.assertNotNull(careTeamEntry);
		assertCodingEquals(TransformerConstants.CODING_FHIR_CARE_TEAM_ROLE, expectedPractitionerRole,
				careTeamEntry.getRole().getCodingFirstRep());
	}

	/**
	 * @param expectedSystem
	 *            the expected {@link Coding#getSystem()} value
	 * @param expectedCode
	 *            the expected {@link Coding#getCode()} value
	 * @param actual
	 *            the actual {@link Coding} to verify
	 */
	static void assertCodingEquals(String expectedSystem, String expectedCode, Coding actual) {
		assertCodingEquals(expectedSystem, null, expectedCode, actual);
	}

	/**
	 * @param expectedSystem
	 *            the expected {@link Coding#getSystem()} value
	 * @param expectedVersion
	 *            the expected {@link Coding#getVersion()} value
	 * @param expectedCode
	 *            the expected {@link Coding#getCode()} value
	 * @param actual
	 *            the actual {@link Coding} to verify
	 */
	private static void assertCodingEquals(String expectedSystem, String expectedVersion, String expectedCode,
			Coding actual) {
		Assert.assertEquals(expectedSystem, actual.getSystem());
		Assert.assertEquals(expectedVersion, actual.getVersion());
		Assert.assertEquals(expectedCode, actual.getCode());
	}

	/**
	 * @param expected
	 *            the expected {@link LocalDate}
	 * @param actual
	 *            the actual {@link DateTimeType} to verify
	 */
	static void assertDateEquals(LocalDate expected, DateTimeType actual) {
		Assert.assertEquals(Date.from(expected.atStartOfDay(ZoneId.systemDefault()).toInstant()), actual.getValue());
		Assert.assertEquals(TemporalPrecisionEnum.DAY, actual.getPrecision());
	}

	/**
	 * @param expectedDiagnosis
	 *            the expected {@link IcdCode} to verify the presence of in the
	 *            {@link ItemComponent}
	 * @param eob
	 *            the {@link ExplanationOfBenefit} to verify
	 * @param eobItem
	 *            the {@link ItemComponent} to verify
	 */
	static void assertDiagnosisLinkPresent(Optional<Diagnosis> diagnosis, ExplanationOfBenefit eob,
			ItemComponent eobItem) {
		if (!diagnosis.isPresent())
			return;

		Optional<DiagnosisComponent> eobDiagnosis = eob.getDiagnosis().stream()
				.filter(d -> d.getDiagnosis() instanceof CodeableConcept)
				.filter(d -> diagnosis.get().isContainedIn((CodeableConcept) d.getDiagnosis())).findAny();
		Assert.assertTrue(eobDiagnosis.isPresent());
		Assert.assertTrue(eobItem.getDiagnosisLinkId().stream()
				.filter(l -> eobDiagnosis.get().getSequence() == l.getValue()).findAny().isPresent());
	}

	/**
	 * Verifies that the specific "actual" {@link BigDecimal} value is
	 * equivalent to the "expected" value, with no loss of precision or scale.
	 * 
	 * @param expected
	 *            the "expected" {@link BigDecimal} value
	 * @param actual
	 *            the "actual" {@link BigDecimal} value
	 */
	static void assertEquivalent(BigDecimal expected, BigDecimal actual) {
		Assert.assertTrue(actual.precision() >= expected.precision());
		Assert.assertTrue(actual.scale() >= expected.scale());
		Assert.assertEquals(0, expected.compareTo(actual));
	}

	/**
	 * @param fhirElement
	 *            the FHIR element to check the extension of
	 * @param expectedExtensionUrl
	 *            the expected {@link Extension#getUrl()} of the
	 *            {@link Extension} to look for
	 * @param expectedCodingSystem
	 *            the expected {@link Coding#getSystem()}
	 * @param expectedCode
	 *            the expected {@link Coding#getCode()}
	 */
	static void assertExtensionCodingEquals(IBaseHasExtensions fhirElement, String expectedExtensionUrl,
			String expectedCodingSystem, String expectedCode) {
		IBaseExtension<?, ?> extensionForUrl = fhirElement.getExtension().stream()
				.filter(e -> e.getUrl().equals(expectedExtensionUrl)).findFirst().get();
		assertHasCoding(expectedCodingSystem, expectedCode, (CodeableConcept) extensionForUrl.getValue());
	}

	/**
	 * @param expectedSystem
	 *            the expected {@link Coding#getSystem()} value
	 * @param expectedCode
	 *            the expected {@link Coding#getCode()} value
	 * @param actualConcept
	 *            the actual {@link CodeableConcept} to verify
	 */
	static void assertHasCoding(String expectedSystem, String expectedCode, CodeableConcept actualConcept) {
		assertHasCoding(expectedSystem, null, expectedCode, actualConcept);
	}

	/**
	 * @param expectedSystem
	 *            the expected {@link Coding#getSystem()} value
	 * @param expectedVersion
	 *            the expected {@link Coding#getVersion()} value
	 * @param expectedCode
	 *            the expected {@link Coding#getCode()} value
	 * @param actualConcept
	 *            the actual {@link CodeableConcept} to verify
	 */
	static void assertHasCoding(String expectedSystem, String expectedVersion, String expectedCode,
			CodeableConcept actualConcept) {
		Assert.assertTrue("No matching Coding found: " + actualConcept.toString(),
				isCodeInConcept(actualConcept, expectedSystem, expectedVersion, expectedCode));
	}

	/**
	 * @param expectedSystem
	 *            the expected {@link Identifier#getSystem()} value
	 * @param expectedId
	 *            the expected {@link Identifier#getValue()} value
	 * @param actuals
	 *            the actual {@link Identifier} to verify
	 */
	static void assertIdentifierExists(String expectedSystem, String expectedId, List<Identifier> actuals) {
		Assert.assertTrue(actuals.stream().filter(i -> expectedSystem.equals(i.getSystem()))
				.anyMatch(i -> expectedId.equals(i.getValue())));
	}

	/**
	 * @param expectedSystem
	 *            the expected {@link Coding#getSystem()} of the
	 *            {@link SupportingInformationComponent#getCategory()} to find
	 *            and verify
	 * @param expectedCode
	 *            the expected {@link Coding#getCoding()} of the
	 *            {@link SupportingInformationComponent#getCategory()} to find
	 *            and verify
	 * @param expectedDate
	 *            the expected
	 *            {@link SupportingInformationComponent#getTiming().primitiveValue()}
	 * @param actuals
	 *            the actual {@link SupportingInformationComponent}s to verify
	 */
	static void assertInformationDateEquals(String expectedSystem, String expectedCode, LocalDate expectedDate,
			List<SupportingInformationComponent> actuals) {
		Optional<SupportingInformationComponent> supportingInformationComponent = actuals.stream()
				.filter(a -> isCodeInConcept(a.getCategory(), expectedSystem, expectedCode)).findAny();
		Assert.assertTrue(supportingInformationComponent.isPresent());
		Assert.assertEquals(expectedDate.toString(), supportingInformationComponent.get().getTiming().primitiveValue());
	}

	/**
	 * @param expectedSystem
	 *            the expected {@link Coding#getSystem()} of the
	 *            {@link SupportingInformationComponent#getCategory()} to find
	 *            and verify
	 * @param expectedCode
	 *            the expected {@link Coding#getCoding()} of the
	 *            {@link SupportingInformationComponent#getCategory()} to find
	 *            and verify
	 * @param expectedFromDate
	 *            the expected
	 *            {@link SupportingInformationComponent#getTimingPeriod().getStartElement()}
	 * @param expectedThruDate
	 *            the expected
	 *            {@link SupportingInformationComponent#getTimingPeriod().getEndElement()}
	 * @param actuals
	 *            the actual {@link SupportingInformationComponent}s to verify
	 */
	static void assertInformationPeriodEquals(String expectedSystem, String expectedCode,
			LocalDate expectedFromDate, LocalDate expectedThruDate, List<SupportingInformationComponent> actuals) {
		Optional<SupportingInformationComponent> supportingInformationComponent = actuals.stream()
				.filter(a -> isCodeInConcept(a.getCategory(), expectedSystem, expectedCode)).findAny();
		Assert.assertTrue(supportingInformationComponent.isPresent());
		try {
			assertDateEquals(expectedFromDate,
					supportingInformationComponent.get().getTimingPeriod().getStartElement());
			assertDateEquals(expectedThruDate, supportingInformationComponent.get().getTimingPeriod().getEndElement());
		} catch (FHIRException e) {
			throw new BadCodeMonkeyException(e);
		}
	}

	/**
	 * Verifies that the specified FHIR {@link Resource} has no unwrapped
	 * {@link Optional} values. This is important, as such values don't
	 * serialize to FHIR correctly.
	 * 
	 * @param resource
	 *            the FHIR {@link Resource} to check
	 */
	static void assertNoEncodedOptionals(Resource resource) {
		FhirContext fhirContext = FhirContext.forDstu3();
		String encodedResource = fhirContext.newXmlParser().encodeResourceToString(resource);
		System.out.println(encodedResource);

		Assert.assertFalse(encodedResource.contains("Optional"));
	}

	/**
	 * @param expectedIdentifierSystem
	 *            the expected {@link Identifier#getSystem()} to match
	 * @param expectedIdentifierValue
	 *            the expected {@link Identifier#getValue()} to match
	 * @param actualReference
	 *            the {@link Reference} to check
	 */
	static void assertReferenceEquals(String expectedIdentifierSystem, String expectedIdentifierValue,
			Reference actualReference) {
		Assert.assertTrue("Reference doesn't match: " + actualReference,
				doesReferenceMatchIdentifier(expectedIdentifierSystem, expectedIdentifierValue, actualReference));
	}

	/**
	 * @param expectedIdentifierSystem
	 *            the expected {@link Identifier#getSystem()} value
	 * @param expectedIdentifierValue
	 *            the expected {@link Identifier#getValue()} value
	 * @param reference
	 *            the actual {@link Reference} to verify
	 */
	static void assertReferenceIdentifierEquals(String expectedIdentifierSystem, String expectedIdentifierValue,
			Reference reference) {
		Assert.assertTrue("Bad reference: " + reference, reference.hasIdentifier());
		Assert.assertEquals(expectedIdentifierSystem, reference.getIdentifier().getSystem());
		Assert.assertEquals(expectedIdentifierValue, reference.getIdentifier().getValue());
	}

	/**
	 * @param expectedIdentifierSystem
	 *            the expected {@link Identifier#getSystem()} to match
	 * @param expectedIdentifierValue
	 *            the expected {@link Identifier#getValue()} to match
	 * @param actualReference
	 *            the {@link Reference} to check
	 * @return <code>true</code> if the specified {@link Reference} matches the
	 *         expected {@link Identifier}
	 */
	private static boolean doesReferenceMatchIdentifier(String expectedIdentifierSystem, String expectedIdentifierValue,
			Reference actualReference) {
		if (!actualReference.hasIdentifier())
			return false;
		return expectedIdentifierSystem.equals(actualReference.getIdentifier().getSystem())
				&& expectedIdentifierValue.equals(actualReference.getIdentifier().getValue());
	}

	/**
	 * @param expectedProviderNpi
	 *            the {@link Identifier#getValue()} of the provider to find a
	 *            matching {@link CareTeamComponent} for
	 * @param careTeam
	 *            the {@link List} of {@link CareTeamComponent}s to search
	 * @return the {@link CareTeamComponent} whose
	 *         {@link CareTeamComponent#getProvider()} is an {@link Identifier}
	 *         with the specified provider NPI, or else <code>null</code> if no
	 *         such {@link CareTeamComponent} was found
	 */
	static CareTeamComponent findCareTeamEntryForProviderIdentifier(String expectedProviderNpi,
			List<CareTeamComponent> careTeam) {
		return findCareTeamEntryForProviderIdentifier(TransformerConstants.CODING_NPI_US, expectedProviderNpi,
				careTeam);
	}

	/**
	 * @param expectedIdentifierSystem
	 *            the {@link Identifier#getSystem()} of the provider to find a
	 *            matching {@link CareTeamComponent} for
	 * @param expectedIdentifierValue
	 *            the {@link Identifier#getValue()} of the provider to find a
	 *            matching {@link CareTeamComponent} for
	 * @param careTeam
	 *            the {@link List} of {@link CareTeamComponent}s to search
	 * @return the {@link CareTeamComponent} whose
	 *         {@link CareTeamComponent#getProvider()} is an {@link Identifier}
	 *         with the specified provider NPI, or else <code>null</code> if no
	 *         such {@link CareTeamComponent} was found
	 */
	private static CareTeamComponent findCareTeamEntryForProviderIdentifier(String expectedIdentifierSystem,
			String expectedIdentifierValue, List<CareTeamComponent> careTeam) {
		Optional<CareTeamComponent> careTeamEntry = careTeam.stream()
				.filter(ctc -> doesReferenceMatchIdentifier(expectedIdentifierSystem, expectedIdentifierValue,
						ctc.getProvider()))
				.findFirst();
		return careTeamEntry.orElse(null);
	}

	/**
	 * @param concept
	 *            the {@link CodeableConcept} to check
	 * @param codingSystem
	 *            the {@link Coding#getSystem()} to match
	 * @param codingCode
	 *            the {@link Coding#getCode()} to match
	 * @return <code>true</code> if the specified {@link CodeableConcept}
	 *         contains the specified {@link Coding}, <code>false</code> if it
	 *         does not
	 */
	static boolean isCodeInConcept(CodeableConcept concept, String codingSystem, String codingCode) {
		return isCodeInConcept(concept, codingSystem, null, codingCode);
	}

	/**
	 * @param concept
	 *            the {@link CodeableConcept} to check
	 * @param codingSystem
	 *            the {@link Coding#getSystem()} to match
	 * @param codingSystem
	 *            the {@link Coding#getVersion()} to match
	 * @param codingCode
	 *            the {@link Coding#getCode()} to match
	 * @return <code>true</code> if the specified {@link CodeableConcept}
	 *         contains the specified {@link Coding}, <code>false</code> if it
	 *         does not
	 */
	static boolean isCodeInConcept(CodeableConcept concept, String codingSystem, String codingVersion,
			String codingCode) {
		return concept.getCoding().stream().anyMatch(c -> {
			if (!codingSystem.equals(c.getSystem()))
				return false;
			if (codingVersion != null && !codingVersion.equals(c.getVersion()))
				return false;
			if (!codingCode.equals(c.getCode()))
				return false;

			return true;
		});
	}

	/**
	 * Uses the setters of the specified record to set all {@link Optional}
	 * fields to {@link Optional#empty()}.
	 * 
	 * @param record
	 *            the record to modify
	 */
	static void setAllOptionalsToEmpty(Object record) {
		try {
			for (Method method : record.getClass().getMethods()) {
				if (!method.getName().startsWith("set"))
					continue;
				if (method.getParameterTypes().length != 1)
					continue;
				if (!method.getParameterTypes()[0].equals(Optional.class))
					continue;

				method.invoke(record, Optional.empty());
			}
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * Test the transformation of the common group level data elements between the
	 * Carrier and DME claim types to FHIR
	 * 
	 * @param eob
	 *            the {@link ExplanationOfBenefit} to test against
	 * @param common
	 *            fields between {@link CarrierClaim} and {@link DMEClaim}
	 */
	static void assertEobCommonGroupCarrierDMEEquals(ExplanationOfBenefit eob, String beneficiaryId,
			String carrierNumber, Optional<String> clinicalTrialNumber, BigDecimal beneficiaryPartBDeductAmount,
			String paymentDenialCode, Optional<String> referringPhysicianNpi,
			Optional<Character> providerAssignmentIndicator, BigDecimal providerPaymentAmount,
			BigDecimal beneficiaryPaymentAmount, BigDecimal submittedChargeAmount, BigDecimal allowedChargeAmount) {

		assertExtensionCodingEquals(eob, TransformerConstants.EXTENSION_CODING_CCW_CARR_PAYMENT_DENIAL,
				TransformerConstants.EXTENSION_CODING_CCW_CARR_PAYMENT_DENIAL, paymentDenialCode);

		ReferralRequest referral = (ReferralRequest) eob.getReferral().getResource();
		Assert.assertEquals(TransformerUtils.referencePatient(beneficiaryId).getReference(),
				referral.getSubject().getReference());
		assertReferenceIdentifierEquals(TransformerConstants.CODING_NPI_US, referringPhysicianNpi.get(),
				referral.getRequester().getAgent());
		Assert.assertEquals(1, referral.getRecipient().size());
		assertReferenceIdentifierEquals(TransformerConstants.CODING_NPI_US, referringPhysicianNpi.get(),
				referral.getRecipientFirstRep());

		assertExtensionCodingEquals(eob, TransformerConstants.CODING_CCW_PROVIDER_ASSIGNMENT,
				TransformerConstants.CODING_CCW_PROVIDER_ASSIGNMENT, String.valueOf(providerAssignmentIndicator.get()));

		assertExtensionCodingEquals(eob, TransformerConstants.EXTENSION_IDENTIFIER_CARRIER_NUMBER,
				TransformerConstants.EXTENSION_IDENTIFIER_CARRIER_NUMBER, carrierNumber);
		assertExtensionCodingEquals(eob, TransformerConstants.EXTENSION_IDENTIFIER_CLINICAL_TRIAL_NUMBER,
				TransformerConstants.EXTENSION_IDENTIFIER_CLINICAL_TRIAL_NUMBER, clinicalTrialNumber.get());
		assertBenefitBalanceEquals(TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
				TransformerConstants.CODED_ADJUDICATION_NCH_BENEFICIARY_PART_B_DEDUCTIBLE, beneficiaryPartBDeductAmount,
				eob.getBenefitBalanceFirstRep().getFinancial());
		assertBenefitBalanceEquals(TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
				TransformerConstants.CODED_ADJUDICATION_PROVIDER_PAYMENT_AMOUNT, providerPaymentAmount,
				eob.getBenefitBalanceFirstRep().getFinancial());
		assertBenefitBalanceEquals(TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
				TransformerConstants.CODED_ADJUDICATION_BENEFICIARY_PAYMENT_AMOUNT, beneficiaryPaymentAmount,
				eob.getBenefitBalanceFirstRep().getFinancial());
		assertBenefitBalanceEquals(TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
				TransformerConstants.CODED_ADJUDICATION_SUBMITTED_CHARGE_AMOUNT, submittedChargeAmount,
				eob.getBenefitBalanceFirstRep().getFinancial());
		assertBenefitBalanceEquals(TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
				TransformerConstants.CODED_ADJUDICATION_ALLOWED_CHARGE, allowedChargeAmount,
				eob.getBenefitBalanceFirstRep().getFinancial());

	}

	/**
	 * Test the transformation of the item level data elements between the Carrier
	 * and DME claim types to FHIR
	 * 
	 * @param item
	 *            the {@link ItemComponent} to test against
	 * @param common
	 *            fields between {@link CarrierClaimLine} and {@link DMEClaimLine}
	 * 
	 * @throws FHIRException
	 */
	static void assertEobCommonItemCarrierDMEEquals(ItemComponent item, ExplanationOfBenefit eob,
			BigDecimal serviceCount, String placeOfServiceCode, Optional<LocalDate> firstExpenseDate,
			Optional<LocalDate> lastExpenseDate, BigDecimal beneficiaryPaymentAmount, BigDecimal providerPaymentAmount,
			BigDecimal beneficiaryPartBDeductAmount, Optional<Character> primaryPayerCode,
			BigDecimal primaryPayerPaidAmount, Optional<String> betosCode, BigDecimal paymentAmount,
			Optional<Character> paymentCode, BigDecimal coinsuranceAmount, BigDecimal submittedChargeAmount,
			BigDecimal allowedChargeAmount, Optional<String> processingIndicatorCode,
			Optional<Character> serviceDeductibleCode, Optional<String> diagnosisCode,
			Optional<Character> diagnosisCodeVersion,
			Optional<String> hctHgbTestTypeCode, BigDecimal hctHgbTestResult,
			char cmsServiceTypeCode, Optional<String> nationalDrugCode)
			throws FHIRException {

		Assert.assertEquals(serviceCount, item.getQuantity().getValue());
		assertHasCoding(TransformerConstants.CODING_CCW_TYPE_SERVICE,
				"" + cmsServiceTypeCode, item.getCategory());
		assertHasCoding(TransformerConstants.CODING_CCW_PLACE_OF_SERVICE, placeOfServiceCode,
				item.getLocationCodeableConcept());
		assertExtensionCodingEquals(item, TransformerConstants.CODING_BETOS, TransformerConstants.CODING_BETOS,
				betosCode.get());
		assertDateEquals(firstExpenseDate.get(), item.getServicedPeriod().getStartElement());
		assertDateEquals(lastExpenseDate.get(), item.getServicedPeriod().getEndElement());

		assertAdjudicationEquals(TransformerConstants.CODED_ADJUDICATION_PAYMENT, paymentAmount,
				item.getAdjudication());
		AdjudicationComponent adjudicationForPayment = item.getAdjudication().stream()
				.filter(a -> isCodeInConcept(a.getCategory(),
						TransformerConstants.CODING_CCW_ADJUDICATION_CATEGORY,
						TransformerConstants.CODED_ADJUDICATION_PAYMENT))
				.findAny().get();
		assertExtensionCodingEquals(adjudicationForPayment,
				TransformerConstants.EXTENSION_CODING_CCW_PAYMENT_80_100_INDICATOR,
				TransformerConstants.EXTENSION_CODING_CCW_PAYMENT_80_100_INDICATOR, "" + paymentCode.get());
		assertAdjudicationEquals(TransformerConstants.CODED_ADJUDICATION_BENEFICIARY_PAYMENT_AMOUNT,
				beneficiaryPaymentAmount, item.getAdjudication());
		assertAdjudicationEquals(TransformerConstants.CODED_ADJUDICATION_PROVIDER_PAYMENT_AMOUNT,
				providerPaymentAmount, item.getAdjudication());
		assertAdjudicationEquals(TransformerConstants.CODED_ADJUDICATION_DEDUCTIBLE,
				beneficiaryPartBDeductAmount, item.getAdjudication());
		assertExtensionCodingEquals(item, TransformerConstants.EXTENSION_CODING_PRIMARY_PAYER,
				TransformerConstants.EXTENSION_CODING_PRIMARY_PAYER, "" + primaryPayerCode.get());
		assertAdjudicationEquals(TransformerConstants.CODED_ADJUDICATION_PRIMARY_PAYER_PAID_AMOUNT,
				primaryPayerPaidAmount, item.getAdjudication());
		assertAdjudicationEquals(TransformerConstants.CODED_ADJUDICATION_LINE_COINSURANCE_AMOUNT, coinsuranceAmount,
				item.getAdjudication());
		assertAdjudicationEquals(TransformerConstants.CODED_ADJUDICATION_SUBMITTED_CHARGE_AMOUNT, submittedChargeAmount,
				item.getAdjudication());
		assertAdjudicationEquals(TransformerConstants.CODED_ADJUDICATION_ALLOWED_CHARGE, allowedChargeAmount,
				item.getAdjudication());
		assertAdjudicationReasonEquals(TransformerConstants.CODED_ADJUDICATION_LINE_PROCESSING_INDICATOR,
				TransformerConstants.CODING_CCW_PROCESSING_INDICATOR, processingIndicatorCode.get(),
				item.getAdjudication());
		assertExtensionCodingEquals(item, TransformerConstants.EXTENSION_CODING_CCW_LINE_DEDUCTIBLE_SWITCH,
				TransformerConstants.EXTENSION_CODING_CCW_LINE_DEDUCTIBLE_SWITCH, "" + serviceDeductibleCode.get());

		assertDiagnosisLinkPresent(Diagnosis.from(diagnosisCode, diagnosisCodeVersion), eob, item);

		List<Extension> hctHgbObservationExtension = item
				.getExtensionsByUrl(TransformerConstants.EXTENSION_CMS_HCT_OR_HGB_RESULTS);
		Assert.assertEquals(1, hctHgbObservationExtension.size());
		Assert.assertTrue(hctHgbObservationExtension.get(0).getValue() instanceof Reference);
		Reference hctHgbReference = (Reference) hctHgbObservationExtension.get(0).getValue();
		Assert.assertTrue(hctHgbReference.getResource() instanceof Observation);
		Observation hctHgbObservation = (Observation) hctHgbReference.getResource();
		assertCodingEquals(TransformerConstants.CODING_CCW_HCT_OR_HGB_TEST_TYPE,
				hctHgbTestTypeCode.get(), hctHgbObservation.getCode().getCodingFirstRep());
		Assert.assertEquals(hctHgbTestResult, hctHgbObservation.getValueQuantity().getValue());

		assertExtensionCodingEquals(item, TransformerConstants.CODING_NDC, TransformerConstants.CODING_NDC,
				nationalDrugCode.get());
	}
}
