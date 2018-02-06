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
import org.hl7.fhir.dstu3.model.codesystems.ClaimCareteamrole;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IBaseHasExtensions;
import org.junit.Assert;

import com.justdavis.karl.misc.exceptions.BadCodeMonkeyException;

import ca.uhn.fhir.context.FhirContext;
import gov.hhs.cms.bluebutton.data.model.rif.CarrierClaim;
import gov.hhs.cms.bluebutton.data.model.rif.CarrierClaimColumn;
import gov.hhs.cms.bluebutton.data.model.rif.CarrierClaimLine;
import gov.hhs.cms.bluebutton.data.model.rif.DMEClaim;
import gov.hhs.cms.bluebutton.data.model.rif.DMEClaimColumn;
import gov.hhs.cms.bluebutton.data.model.rif.DMEClaimLine;
import gov.hhs.cms.bluebutton.data.model.rif.HHAClaim;
import gov.hhs.cms.bluebutton.data.model.rif.HHAClaimColumn;
import gov.hhs.cms.bluebutton.data.model.rif.HHAClaimLine;
import gov.hhs.cms.bluebutton.data.model.rif.HospiceClaim;
import gov.hhs.cms.bluebutton.data.model.rif.HospiceClaimColumn;
import gov.hhs.cms.bluebutton.data.model.rif.HospiceClaimLine;
import gov.hhs.cms.bluebutton.data.model.rif.InpatientClaim;
import gov.hhs.cms.bluebutton.data.model.rif.InpatientClaimColumn;
import gov.hhs.cms.bluebutton.data.model.rif.InpatientClaimLine;
import gov.hhs.cms.bluebutton.data.model.rif.OutpatientClaim;
import gov.hhs.cms.bluebutton.data.model.rif.OutpatientClaimColumn;
import gov.hhs.cms.bluebutton.data.model.rif.OutpatientClaimLine;
import gov.hhs.cms.bluebutton.data.model.rif.SNFClaim;
import gov.hhs.cms.bluebutton.data.model.rif.SNFClaimColumn;
import gov.hhs.cms.bluebutton.data.model.rif.SNFClaimLine;

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
	 * Tests that the specified extension list contains a single Identifier with the
	 * expected string value
	 *
	 * @param extension
	 *            a {@link List}&lt;{@link Extension}&gt; containing an Identifier
	 * @param expected
	 *            a {@link String} containing the expected value of the Identifier
	 */
	static void assertExtensionIdentifierEqualsString(List<Extension> extension, String expected) {
		Assert.assertEquals(1, extension.size());
		Assert.assertTrue(extension.get(0).getValue() instanceof Identifier);
		Identifier identifier = (Identifier) extension.get(0).getValue();
		Assert.assertEquals(expected, identifier.getValue());
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
	 * @param eobType
	 *            the eobType {@link CodeableConcept} we are testing against for
	 *            expected values
	 * @param blueButtonClaimType
	 *            expected blue button {@link ClaimType} value
	 * @param fhirClaimType
	 *            optional expected fhir
	 *            {@link org.hl7.fhir.dstu3.model.codesystems.ClaimType} value
	 * @param ccwNearLineRecordIdCode
	 *            optional expected ccw near line record id code
	 *            {@link Optional}&lt;{@link Character}&gt;
	 * @param ccwClaimTypeCode
	 *            optional expected ccw claim type code
	 *            {@link Optional}&lt;{@link String}&gt;
	 */
	static void assertMapEobType(CodeableConcept eobType, ClaimType blueButtonClaimType,
			Optional<org.hl7.fhir.dstu3.model.codesystems.ClaimType> fhirClaimType,
			Optional<Character> ccwNearLineRecordIdCode, Optional<String> ccwClaimTypeCode) {
		assertHasCoding(TransformerConstants.CODING_CCW_CLAIM_TYPE, blueButtonClaimType.name(), eobType);

		if (fhirClaimType.isPresent()) {
			assertHasCoding(TransformerConstants.CODING_FHIR_CLAIM_TYPE, fhirClaimType.get().name(), eobType);
		}

		if (ccwNearLineRecordIdCode.isPresent()) {
			assertHasCoding(TransformerConstants.CODING_CCW_RECORD_ID_CODE,
					String.valueOf(ccwNearLineRecordIdCode.get()), eobType);
		}

		if (ccwClaimTypeCode.isPresent()) {
			assertHasCoding(TransformerConstants.CODING_NCH_CLAIM_TYPE, ccwClaimTypeCode.get(), eobType);
		}
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
	 * Tests field values of an eob's benefit balance component that are common
	 * between the Inpatient and SNF claim types.
	 * 
	 * @param eob
	 *            the {@link ExplanationOfBenefit} that will be tested by this
	 *            method
	 * @param coinsuranceDayCount
	 *            BENE_TOT_COINSRNC_DAYS_CNT: a {@link BigDecimal} shared field
	 *            representing the coinsurance day count for the claim
	 * @param nonUtilizationDayCount
	 *            CLM_NON_UTLZTN_DAYS_CNT: a {@link BigDecimal} shared field
	 *            representing the non-utilization day count for the claim
	 * @param deductibleAmount
	 *            NCH_BENE_IP_DDCTBL_AMT: a {@link BigDecimal} shared field
	 *            representing the deductible amount for the claim
	 * @param partACoinsuranceLiabilityAmount
	 *            NCH_BENE_PTA_COINSRNC_LBLTY_AM: a {@link BigDecimal} shared field
	 *            representing the part A coinsurance amount for the claim
	 * @param bloodPintsFurnishedQty
	 *            NCH_BLOOD_PNTS_FRNSHD_QTY: a {@link BigDecimal} shared field
	 *            representing the blood pints furnished quantity for the claim
	 * @param noncoveredCharge
	 *            NCH_IP_NCVRD_CHRG_AMT: a {@link BigDecimal} shared field
	 *            representing the non-covered charge for the claim
	 * @param totalDeductionAmount
	 *            NCH_IP_TOT_DDCTN_AMT: a {@link BigDecimal} shared field
	 *            representing the total deduction amount for the claim
	 * @param claimPPSCapitalDisproportionateShareAmt
	 *            CLM_PPS_CPTL_DSPRPRTNT_SHR_AMT: an
	 *            {@link Optional}&lt;{@link BigDecimal}&gt; shared field
	 *            representing the claim PPS capital disproportionate share amount
	 *            for the claim
	 * @param claimPPSCapitalExceptionAmount
	 *            CLM_PPS_CPTL_EXCPTN_AMT: an
	 *            {@link Optional}&lt;{@link BigDecimal}&gt; shared field
	 *            representing the claim PPS capital exception amount for the claim
	 * @param claimPPSCapitalFSPAmount
	 *            CLM_PPS_CPTL_FSP_AMT: an
	 *            {@link Optional}&lt;{@link BigDecimal}&gt; shared field
	 *            representing the claim PPS capital FSP amount for the claim
	 * @param claimPPSCapitalIMEAmount
	 *            CLM_PPS_CPTL_IME_AMT: an
	 *            {@link Optional}&lt;{@link BigDecimal}&gt; shared field
	 *            representing the claim PPS capital IME amount for the claim
	 * @param claimPPSCapitalOutlierAmount
	 *            CLM_PPS_CPTL_OUTLIER_AMT: an
	 *            {@link Optional}&lt;{@link BigDecimal}&gt; shared field
	 *            representing the claim PPS capital outlier amount for the claim
	 * @param claimPPSOldCapitalHoldHarmlessAmount
	 *            CLM_PPS_OLD_CPTL_HLD_HRMLS_AMT: an
	 *            {@link Optional}&lt;{@link BigDecimal}&gt; shared field
	 *            representing the claim PPS old capital hold harmless amount for
	 *            the claim
	 */
	static void assertCommonBenefitComponentInpatientSNF(ExplanationOfBenefit eob, BigDecimal coinsuranceDayCount,
			BigDecimal nonUtilizationDayCount, BigDecimal deductibleAmount, BigDecimal partACoinsuranceLiabilityAmount,
			BigDecimal bloodPintsFurnishedQty, BigDecimal noncoveredCharge, BigDecimal totalDeductionAmount,
			Optional<BigDecimal> claimPPSCapitalDisproportionateShareAmt,
			Optional<BigDecimal> claimPPSCapitalExceptionAmount, Optional<BigDecimal> claimPPSCapitalFSPAmount,
			Optional<BigDecimal> claimPPSCapitalIMEAmount, Optional<BigDecimal> claimPPSCapitalOutlierAmount,
			Optional<BigDecimal> claimPPSOldCapitalHoldHarmlessAmount) {

		// coinsuranceDayCount
		assertBenefitBalanceUsedEquals(TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
				TransformerConstants.CODING_CCW_COINSURANCE_DAY_COUNT, coinsuranceDayCount.intValue(),
				eob.getBenefitBalanceFirstRep().getFinancial());

		// nonUtilizationDayCount
		assertBenefitBalanceEquals(TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
				TransformerConstants.CODED_BENEFIT_BALANCE_TYPE_NON_UTILIZATION_DAY_COUNT,
				nonUtilizationDayCount.intValue(), eob.getBenefitBalanceFirstRep().getFinancial());

		// deductibleAmount
		assertBenefitBalanceEquals(TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
				TransformerConstants.CODED_BENEFIT_BALANCE_TYPE_DEDUCTIBLE, deductibleAmount,
				eob.getBenefitBalanceFirstRep().getFinancial());

		// partACoinsuranceLiabilityAmount
		assertBenefitBalanceEquals(TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
				TransformerConstants.CODED_BENEFIT_BALANCE_TYPE_COINSURANCE_LIABILITY, partACoinsuranceLiabilityAmount,
				eob.getBenefitBalanceFirstRep().getFinancial());

		// bloodPintsFurnishedQty
		assertBenefitBalanceUsedEquals(TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
				TransformerConstants.CODED_BENEFIT_BALANCE_TYPE_BLOOD_PINTS_FURNISHED,
				bloodPintsFurnishedQty.intValue(), eob.getBenefitBalanceFirstRep().getFinancial());

		// noncoveredCharge
		assertBenefitBalanceEquals(TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
				TransformerConstants.CODED_BENEFIT_BALANCE_TYPE_NONCOVERED_CHARGE, noncoveredCharge,
				eob.getBenefitBalanceFirstRep().getFinancial());

		// totalDeductionAmount
		assertBenefitBalanceEquals(TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
				TransformerConstants.CODED_BENEFIT_BALANCE_TYPE_TOTAL_DEDUCTION, totalDeductionAmount,
				eob.getBenefitBalanceFirstRep().getFinancial());

		// claimPPSCapitalDisproportionateShareAmt
		if (claimPPSCapitalDisproportionateShareAmt.isPresent()) {
			assertBenefitBalanceEquals(TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
					TransformerConstants.CODED_BENEFIT_BALANCE_TYPE_PPS_CAPITAL_DISPROPORTIONAL_SHARE,
					claimPPSCapitalDisproportionateShareAmt.get(), eob.getBenefitBalanceFirstRep().getFinancial());
		}
		
		// claimPPSCapitalExceptionAmount
		if (claimPPSCapitalExceptionAmount.isPresent()) {
			assertBenefitBalanceEquals(TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
					TransformerConstants.CODED_BENEFIT_BALANCE_TYPE_PPS_CAPITAL_EXCEPTION,
					claimPPSCapitalExceptionAmount.get(), eob.getBenefitBalanceFirstRep().getFinancial());
		}
		
		// claimPPSCapitalFSPAmount
		if (claimPPSCapitalFSPAmount.isPresent()) {
			assertBenefitBalanceEquals(TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
					TransformerConstants.CODED_BENEFIT_BALANCE_TYPE_PPS_CAPITAL_FEDRERAL_PORTION,
					claimPPSCapitalFSPAmount.get(), eob.getBenefitBalanceFirstRep().getFinancial());
		}
		
		// claimPPSCapitalIMEAmount
		if (claimPPSCapitalIMEAmount.isPresent()) {
			assertBenefitBalanceEquals(TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
					TransformerConstants.CODED_BENEFIT_BALANCE_TYPE_PPS_CAPITAL_INDIRECT_MEDICAL_EDU,
					claimPPSCapitalIMEAmount.get(), eob.getBenefitBalanceFirstRep().getFinancial());
		}
		
		// claimPPSCapitalOutlierAmount
		if (claimPPSCapitalOutlierAmount.isPresent()) {
			assertBenefitBalanceEquals(TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
					TransformerConstants.CODED_BENEFIT_BALANCE_TYPE_PPS_CAPITAL_OUTLIER,
					claimPPSCapitalOutlierAmount.get(), eob.getBenefitBalanceFirstRep().getFinancial());
		}
		
		// claimPPSOldCapitalHoldHarmlessAmount
		if (claimPPSOldCapitalHoldHarmlessAmount.isPresent()) {
			assertBenefitBalanceEquals(TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
					TransformerConstants.CODED_BENEFIT_BALANCE_TYPE_PPS_OLD_CAPITAL_HOLD_HARMLESS,
					claimPPSOldCapitalHoldHarmlessAmount.get(), eob.getBenefitBalanceFirstRep().getFinancial());
		}
	}
	
	/**
	 * Tests EOB information fields that are common between the Inpatient and SNF
	 * claim types.
	 * 
	 * @param eob
	 *            the {@link ExplanationOfBenefit} that will be tested by this
	 *            method
	 * @param noncoveredStayFromDate
	 *            NCH_VRFD_NCVRD_STAY_FROM_DT: an
	 *            {@link Optional}&lt;{@link LocalDate}&gt; shared field
	 *            representing the non-covered stay from date for the claim
	 * @param noncoveredStayThroughDate
	 *            NCH_VRFD_NCVRD_STAY_THRU_DT: an
	 *            {@link Optional}&lt;{@link LocalDate}&gt; shared field
	 *            representing the non-covered stay through date for the claim
	 * @param coveredCareThroughDate
	 *            NCH_ACTV_OR_CVRD_LVL_CARE_THRU: an
	 *            {@link Optional}&lt;{@link LocalDate}&gt; shared field
	 *            representing the covered stay through date for the claim
	 * @param medicareBenefitsExhaustedDate
	 *            NCH_BENE_MDCR_BNFTS_EXHTD_DT_I: an
	 *            {@link Optional}&lt;{@link LocalDate}&gt; shared field
	 *            representing the medicare benefits exhausted date for the claim
	 * @param diagnosisRelatedGroupCd
	 *            CLM_DRG_CD: an {@link Optional}&lt;{@link String}&gt; shared field
	 *            representing the non-covered stay from date for the claim
	 */
	static void assertCommonEobInformationInpatientSNF(ExplanationOfBenefit eob, Optional<LocalDate> noncoveredStayFromDate, 
			Optional<LocalDate> noncoveredStayThroughDate, Optional<LocalDate> coveredCareThroughDate,
			Optional<LocalDate> medicareBenefitsExhaustedDate, Optional<String> diagnosisRelatedGroupCd) {
		
		/*
		 * TODO missing tests for: admissionTypeCd, sourceAdmissionCd,
		 * diagnosisAdmittingCode, diagnosisAdmittingCodeVersion
		 */
		
		// noncoveredStayFromDate & noncoveredStayThroughDate
		if (noncoveredStayFromDate.isPresent() && noncoveredStayThroughDate.isPresent()) {
			assertInformationPeriodEquals(TransformerConstants.CODING_BBAPI_BENEFIT_COVERAGE_DATE,
					TransformerConstants.CODED_BENEFIT_COVERAGE_DATE_NONCOVERED, noncoveredStayFromDate.get(),
					noncoveredStayThroughDate.get(), eob.getInformation());
		}
		
		// coveredCareThroughDate
		if (coveredCareThroughDate.isPresent()) {
			assertInformationDateEquals(TransformerConstants.CODING_BBAPI_BENEFIT_COVERAGE_DATE,
					TransformerConstants.CODED_BENEFIT_COVERAGE_DATE_STAY, coveredCareThroughDate.get(),
					eob.getInformation());
		}
		
		// medicareBenefitsExhaustedDate
		if(medicareBenefitsExhaustedDate.isPresent()) {
		assertInformationDateEquals(TransformerConstants.CODING_BBAPI_BENEFIT_COVERAGE_DATE,
				TransformerConstants.CODED_BENEFIT_COVERAGE_DATE_EXHAUSTED,
				medicareBenefitsExhaustedDate.get(),
				eob.getInformation());
		}
		
		// diagnosisRelatedGroupCd
		Assert.assertTrue(eob.getInformation().stream()
				.anyMatch(i -> TransformerTestUtils.isCodeInConcept(i.getCategory(),
						TransformerConstants.CODING_CCW_DIAGNOSIS_RELATED_GROUP,
						String.valueOf(diagnosisRelatedGroupCd.get()))));
	}

	/**
	 * Test the transformation of common group level header fields between all claim
	 * types
	 * 
	 * @param eob
	 *            the {@link ExplanationOfBenefit} to modify
	 * @param claimId
	 *            CLM_ID
	 * @param beneficiaryId
	 *            BENE_ID
	 * @param claimType
	 *            {@link ClaimType} to process
	 * @param claimGroupId
	 *            CLM_GRP_ID
	 * @param coverageType
	 *            {@link MedicareSegment}
	 * @param dateFrom
	 *            CLM_FROM_DT
	 * @param dateThrough
	 *            CLM_THRU_DT
	 * @param paymentAmount
	 *            CLM_PMT_AMT
	 * @param finalAction
	 *            FINAL_ACTION
	 * 
	 */
	static void assertEobCommonClaimHeaderData(ExplanationOfBenefit eob, String claimId, String beneficiaryId,
			ClaimType claimType, String claimGroupId, MedicareSegment coverageType, Optional<LocalDate> dateFrom,
			Optional<LocalDate> dateThrough, Optional<BigDecimal> paymentAmount, char finalAction) {

		assertNoEncodedOptionals(eob);

		Assert.assertEquals(TransformerUtils.buildEobId(claimType, claimId), eob.getIdElement().getIdPart());

		if (claimType.equals(ClaimType.PDE))
			assertIdentifierExists(TransformerConstants.CODING_CCW_PARTD_EVENT_ID, claimId, eob.getIdentifier());
		else
			assertIdentifierExists(TransformerConstants.CODING_CCW_CLAIM_ID, claimId, eob.getIdentifier());

		assertIdentifierExists(TransformerConstants.CODING_CCW_CLAIM_GROUP_ID, claimGroupId, eob.getIdentifier());
		Assert.assertEquals(TransformerUtils.referencePatient(beneficiaryId).getReference(),
				eob.getPatient().getReference());
		Assert.assertEquals(TransformerUtils.referenceCoverage(beneficiaryId, coverageType).getReference(),
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
		Assert.assertEquals(TransformerConstants.CODED_EOB_DISPOSITION, eob.getDisposition());

		if (paymentAmount.isPresent()) {
			Assert.assertEquals(paymentAmount.get(), eob.getPayment().getAmount().getValue());
		}
	}

	/**
	 * Test the transformation of the common group level data elements between the
	 * {@link CarrierClaim} and {@link DMEClaim} claim types to FHIR. The method
	 * parameter fields from {@link CarrierClaim} and {@link DMEClaim} are listed
	 * below and their corresponding RIF CCW fields (denoted in all CAPS below from
	 * {@link CarrierClaimColumn} and {@link DMEClaimColumn}).
	 * 
	 * @param eob
	 *            the {@link ExplanationOfBenefit} to test
	 * @param carrierNumber
	 *            CARR_NUM,
	 * @param clinicalTrialNumber
	 *            CLM_CLNCL_TRIL_NUM,
	 * @param beneficiaryPartBDeductAmount
	 *            CARR_CLM_CASH_DDCTBL_APLD_AMT,
	 * @param paymentDenialCode
	 *            CARR_CLM_PMT_DNL_CD
	 * @param providerAssignmentIndicator
	 *            CARR_CLM_PRVDR_ASGNMT_IND_SW,
	 * @param providerPaymentAmount
	 *            NCH_CLM_PRVDR_PMT_AMT,
	 * @param beneficiaryPaymentAmount
	 *            NCH_CLM_BENE_PMT_AMT,
	 * @param submittedChargeAmount
	 *            NCH_CARR_CLM_SBMTD_CHRG_AMT,
	 * @param allowedChargeAmount
	 *            NCH_CARR_CLM_ALOWD_AMT,
	 * 
	 */
	static void assertEobCommonGroupCarrierDMEEquals(ExplanationOfBenefit eob,
			String carrierNumber, Optional<String> clinicalTrialNumber, BigDecimal beneficiaryPartBDeductAmount,
			String paymentDenialCode,
			Optional<Character> providerAssignmentIndicator, BigDecimal providerPaymentAmount,
			BigDecimal beneficiaryPaymentAmount, BigDecimal submittedChargeAmount, BigDecimal allowedChargeAmount) {

		assertExtensionCodingEquals(eob, TransformerConstants.EXTENSION_CODING_CCW_CARR_PAYMENT_DENIAL,
				TransformerConstants.EXTENSION_CODING_CCW_CARR_PAYMENT_DENIAL, paymentDenialCode);

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
	 * Test the transformation of the item level data elements between the
	 * {@link CarrierClaimLine} and {@link DMEClaimLine} claim types to FHIR. The
	 * method parameter fields from {@link CarrierClaimLine} and
	 * {@link DMEClaimLine} are listed below and their corresponding RIF CCW fields
	 * (denoted in all CAPS below from {@link CarrierClaimColumn} and
	 * {@link DMEClaimColumn}).
	 * 
	 * @param item
	 *            the {@ ItemComponent} to test
	 * @param eob
	 *            the {@ ExplanationOfBenefit} to test
	 * @param beneficiaryId
	 *            BENE_ID, *
	 * @param serviceCount
	 *            LINE_SRVC_CNT,
	 * @param placeOfServiceCode
	 *            LINE_PLACE_OF_SRVC_CD,
	 * @param firstExpenseDate
	 *            LINE_1ST_EXPNS_DT,
	 * @param lastExpenseDate
	 *            LINE_LAST_EXPNS_DT,
	 * @param beneficiaryPaymentAmount
	 *            LINE_BENE_PMT_AMT,
	 * @param providerPaymentAmount
	 *            LINE_PRVDR_PMT_AMT,
	 * @param beneficiaryPartBDeductAmount
	 *            LINE_BENE_PTB_DDCTBL_AMT,
	 * @param primaryPayerCode
	 *            LINE_BENE_PRMRY_PYR_CD,
	 * @param primaryPayerPaidAmount
	 *            LINE_BENE_PRMRY_PYR_PD_AMT,
	 * @param betosCode
	 *            BETOS_CD,
	 * @param paymentAmount
	 *            LINE_NCH_PMT_AMT,
	 * @param paymentCode
	 *            LINE_PMT_80_100_CD,
	 * @param coinsuranceAmount
	 *            LINE_COINSRNC_AMT,
	 * @param submittedChargeAmount
	 *            LINE_SBMTD_CHRG_AMT,
	 * @param allowedChargeAmount
	 *            LINE_ALOWD_CHRG_AMT,
	 * @param processingIndicatorCode
	 *            LINE_PRCSG_IND_CD,
	 * @param serviceDeductibleCode
	 *            LINE_SERVICE_DEDUCTIBLE,
	 * @param diagnosisCode
	 *            LINE_ICD_DGNS_CD,
	 * @param diagnosisCodeVersion
	 *            LINE_ICD_DGNS_VRSN_CD,
	 * @param hctHgbTestTypeCode
	 *            LINE_HCT_HGB_TYPE_CD
	 * @param hctHgbTestResult
	 *            LINE_HCT_HGB_RSLT_NUM,
	 * @param cmsServiceTypeCode
	 *            LINE_CMS_TYPE_SRVC_CD,
	 * @param nationalDrugCode
	 *            LINE_NDC_CD,
	 * @param referringPhysicianNpi
	 *            RFR_PHYSN_NPI,
	 * @param referralRecipient
	 *            PFR_PHYSN_NPI (Carrier) \ PRVDR_NPI (DME)
	 * 
	 * @throws FHIRException
	 */
	static void assertEobCommonItemCarrierDMEEquals(ItemComponent item, ExplanationOfBenefit eob, String beneficiaryId,
			BigDecimal serviceCount, String placeOfServiceCode, Optional<LocalDate> firstExpenseDate,
			Optional<LocalDate> lastExpenseDate, BigDecimal beneficiaryPaymentAmount, BigDecimal providerPaymentAmount,
			BigDecimal beneficiaryPartBDeductAmount, Optional<Character> primaryPayerCode,
			BigDecimal primaryPayerPaidAmount, Optional<String> betosCode, BigDecimal paymentAmount,
			Optional<Character> paymentCode, BigDecimal coinsuranceAmount, BigDecimal submittedChargeAmount,
			BigDecimal allowedChargeAmount, Optional<String> processingIndicatorCode,
			Optional<Character> serviceDeductibleCode, Optional<String> diagnosisCode,
			Optional<Character> diagnosisCodeVersion,
			Optional<String> hctHgbTestTypeCode, BigDecimal hctHgbTestResult,
			char cmsServiceTypeCode, Optional<String> nationalDrugCode, Optional<String> referringPhysicianNpi,
			Optional<String> referralRecipient)
			throws FHIRException {

		ReferralRequest referral = (ReferralRequest) eob.getReferral().getResource();
		Assert.assertEquals(TransformerUtils.referencePatient(beneficiaryId).getReference(),
				referral.getSubject().getReference());
		assertReferenceIdentifierEquals(TransformerConstants.CODING_NPI_US, referringPhysicianNpi.get(),
				referral.getRequester().getAgent());
		Assert.assertEquals(1, referral.getRecipient().size());
		assertReferenceIdentifierEquals(TransformerConstants.CODING_NPI_US, referralRecipient.get(),
				referral.getRecipientFirstRep());

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
		assertExtensionCodingEquals(item, TransformerConstants.EXTENSION_CODING_CARRIER_PRIMARY_PAYER,
				TransformerConstants.EXTENSION_CODING_CARRIER_PRIMARY_PAYER, "" + primaryPayerCode.get());
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
	
	/**
	 * Test the transformation of the item level data elements between the
	 * {@link InpatientClaim} {@link OutpatientClaim} {@link HospiceClaim}
	 * {@link HHAClaim}and {@link SNFClaim} claim types to FHIR. The method
	 * parameter fields from {@link InpatientClaim} {@link OutpatientClaim}
	 * {@link HospiceClaim} {@link HHAClaim}and {@link SNFClaim} are listed below
	 * and their corresponding RIF CCW fields (denoted in all CAPS below from
	 * {@link InpatientClaimColumn} {@link OutpatientClaimColumn}
	 * {@link HopsiceClaimColumn} {@link HHAClaimColumn} and
	 * {@link SNFClaimColumn}).
	 * 
	 * @param eob
	 *            the {@ ExplanationOfBenefit} to test
	 * 
	 * @param organizationNpi
	 *            ORG_NPI_NUM,
	 * @param claimFacilityTypeCode
	 *            CLM_FAC_TYPE_CD,
	 * @param claimFrequencyCode
	 *            CLM_FREQ_CD,
	 * @param claimNonPaymentReasonCode
	 *            CLM_MDCR_NON_PMT_RSN_CD,
	 * @param patientDischargeStatusCode
	 *            PTNT_DSCHRG_STUS_CD,
	 * @param claimServiceClassificationTypeCode
	 *            CLM_SRVC_CLSFCTN_TYPE_CD,
	 * @param claimPrimaryPayerCode
	 *            NCH_PRMRY_PYR_CD,
	 * @param attendingPhysicianNpi
	 *            AT_PHYSN_NPI,
	 * @param totalChargeAmount
	 *            CLM_TOT_CHRG_AMT,
	 * @param primaryPayerPaidAmount
	 *            NCH_PRMRY_PYR_CLM_PD_AMT,
	 * @param fiscalIntermediaryNumber
	 * 			  FI_NUMBER
	 */
	static void assertEobCommonGroupInpOutHHAHospiceSNFEquals(ExplanationOfBenefit eob,
			Optional<String> organizationNpi, char claimFacilityTypeCode, char claimFrequencyCode,
			Optional<String> claimNonPaymentReasonCode, String patientDischargeStatusCode,
			char claimServiceClassificationTypeCode, Optional<Character> claimPrimaryPayerCode,
			Optional<String> attendingPhysicianNpi, BigDecimal totalChargeAmount, BigDecimal primaryPayerPaidAmount,
			Optional<String> fiscalIntermediaryNumber) {

		TransformerTestUtils.assertReferenceIdentifierEquals(TransformerConstants.CODING_NPI_US,
				organizationNpi.get(), eob.getOrganization());
		TransformerTestUtils.assertReferenceIdentifierEquals(TransformerConstants.CODING_NPI_US,
				organizationNpi.get(), eob.getFacility());

		TransformerTestUtils.assertExtensionCodingEquals(eob.getFacility(),
				TransformerConstants.EXTENSION_CODING_CCW_FACILITY_TYPE,
				TransformerConstants.EXTENSION_CODING_CCW_FACILITY_TYPE,
				String.valueOf(claimFacilityTypeCode));

		// TODO add tests for claimFrequencyCode, patientDischargeStatusCode and
		// claimPrimaryPayerCode

		TransformerTestUtils.assertExtensionCodingEquals(eob,
				TransformerConstants.EXTENSION_CODING_CCW_PAYMENT_DENIAL_REASON,
				TransformerConstants.EXTENSION_CODING_CCW_PAYMENT_DENIAL_REASON, claimNonPaymentReasonCode.get());

		TransformerTestUtils.assertExtensionCodingEquals(eob.getType(),
				TransformerConstants.EXTENSION_CODING_CCW_CLAIM_SERVICE_CLASSIFICATION,
				TransformerConstants.EXTENSION_CODING_CCW_CLAIM_SERVICE_CLASSIFICATION,
				String.valueOf(claimServiceClassificationTypeCode));

		TransformerTestUtils.assertCareTeamEquals(attendingPhysicianNpi.get(),
				ClaimCareteamrole.PRIMARY.toCode(), eob);

		Assert.assertEquals(totalChargeAmount, eob.getTotalCost().getValue());
		TransformerTestUtils.assertBenefitBalanceEquals(TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
				TransformerConstants.CODED_ADJUDICATION_PRIMARY_PAYER_PAID_AMOUNT, primaryPayerPaidAmount,
				eob.getBenefitBalanceFirstRep().getFinancial());
		
		if (fiscalIntermediaryNumber.isPresent()) {
			assertExtensionCodingEquals(eob,
					TransformerConstants.EXTENSION_CODING_CCW_FI_NUM,
					TransformerConstants.EXTENSION_CODING_CCW_FI_NUM, String.valueOf(fiscalIntermediaryNumber.get()));
		}
	}

	/**
	 * Test the transformation of the item level data elements between the
	 * {@link InpatientClaimLine} {@link OutpatientClaimLine}
	 * {@link HospiceClaimLine} {@link HHAClaimLine}and {@link SNFClaimLine} claim
	 * types to FHIR. The method parameter fields from {@link InpatientClaimLine}
	 * {@link OutpatientClaimLine} {@link HospiceClaimLine} {@link HHAClaimLine}and
	 * {@link SNFClaimLine} are listed below and their corresponding RIF CCW fields
	 * (denoted in all CAPS below from {@link InpatientClaimColumn}
	 * {@link OutpatientClaimColumn} {@link HopsiceClaimColumn}
	 * {@link HHAClaimColumn} and {@link SNFClaimColumn}).
	 * 
	 * @param item
	 *            the {@ ItemComponent} to test
	 * @param eob
	 *            the {@ ExplanationOfBenefit} to test
	 * 
	 * @param revenueCenterCode
	 *            REV_CNTR,
	 * 
	 * @param rateAmount
	 *            REV_CNTR_RATE_AMT,
	 * 
	 * @param totalChargeAmount
	 *            REV_CNTR_TOT_CHRG_AMT,
	 * 
	 * @param nonCoveredChargeAmount
	 *            REV_CNTR_NCVRD_CHRG_AMT,
	 * 
	 * @param unitCount
	 *            REV_CNTR_UNIT_CNT,
	 * 
	 * @param nationalDrugCodeQuantity
	 *            REV_CNTR_NDC_QTY,
	 * 
	 * @param nationalDrugCodeQualifierCode
	 *            REV_CNTR_NDC_QTY_QLFR_CD,
	 * 
	 * @param revenueCenterRenderingPhysicianNPI
	 *            RNDRNG_PHYSN_NPI
	 */
	static void assertEobCommonItemRevenueEquals(ItemComponent item, ExplanationOfBenefit eob, String revenueCenterCode,
			BigDecimal rateAmount,
			BigDecimal totalChargeAmount, BigDecimal nonCoveredChargeAmount, BigDecimal unitCount,
			Optional<BigDecimal> nationalDrugCodeQuantity, Optional<String> nationalDrugCodeQualifierCode,
			Optional<String> revenueCenterRenderingPhysicianNPI) {

		TransformerTestUtils.assertHasCoding(TransformerConstants.CODING_CMS_REVENUE_CENTER,
				revenueCenterCode, item.getRevenue());
		
		TransformerTestUtils.assertAdjudicationEquals(TransformerConstants.CODED_ADJUDICATION_RATE_AMOUNT, rateAmount,
				item.getAdjudication());

		TransformerTestUtils.assertAdjudicationEquals(TransformerConstants.CODED_ADJUDICATION_NONCOVERED_CHARGE,
				nonCoveredChargeAmount, item.getAdjudication());
		TransformerTestUtils.assertAdjudicationEquals(TransformerConstants.CODED_ADJUDICATION_TOTAL_CHARGE_AMOUNT,
				totalChargeAmount, item.getAdjudication());

		// TODO add tests for unitCount, nationalDrugCodeQuantity and
		// nationalDrugCodeQualifierCode

		TransformerTestUtils.assertCareTeamEquals(revenueCenterRenderingPhysicianNPI.get(),
				ClaimCareteamrole.PRIMARY.toCode(), eob);

	}
	
	/**
	 * Test the transformation of the common group level data elements between the
	 * {@link OutpatientClaim} {@link HospiceClaim} and {@link HHAClaim} claim
	 * types to FHIR. The method parameter fields from {@link OutpatientClaim}
	 * {@link HospiceClaim} and {@link HHAClaim} are listed below and their
	 * corresponding RIF CCW fields (denoted in all CAPS below from
	 * {@link OutpatientClaimColumn} {@link HospiceClaimColumn} and
	 * {@link HHAClaimColumn}).
	 * 
	 * @param item
	 *            the {@link ItemComponent} to test
	 *            
	 * @param revenueCenterDate
	 * 			  REV_CNTR_DT,
	 * 
	 * @param paymentAmount
	 * 			  REV_CNTR_PMT_AMT_AMT
	 */
	static void assertEobCommonItemRevenueOutHHAHospice(ItemComponent item,
			Optional<LocalDate> revenueCenterDate, BigDecimal paymentAmount) throws FHIRException {
		
		if (revenueCenterDate.isPresent()) {
			Assert.assertEquals(Date.valueOf(revenueCenterDate.get()), item.getServicedDateType().getValue());
		}
		
		assertAdjudicationEquals(TransformerConstants.CODED_ADJUDICATION_PAYMENT,
				paymentAmount,
				item.getAdjudication());
	}


	/**
	 * Test the transformation of the common group level data elements between the
	 * {@link InpatientClaim} {@link OutpatientClaim} and {@link SNFClaim} claim
	 * types to FHIR. The method parameter fields from {@link InpatientClaim}
	 * {@link OutpatientClaim} and {@link SNFClaim} are listed below and their
	 * corresponding RIF CCW fields (denoted in all CAPS below from
	 * {@link InpatientClaimColumn} {@link OutpatientClaimColumn} and
	 * {@link SNFClaimColumn}).
	 * 
	 * @param eob
	 *            the {@link ExplanationOfBenefit} to test
	 * 
	 */
	static void assertEobCommonGroupInpOutSNFEquals(ExplanationOfBenefit eob,
			BigDecimal bloodDeductibleLiabilityAmount, Optional<String> operatingPhysicianNpi,
			Optional<String> otherPhysicianNpi, char claimQueryCode, Optional<Character> mcoPaidSw) {

		TransformerTestUtils.assertBenefitBalanceEquals(TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
				TransformerConstants.CODED_BENEFIT_BALANCE_TYPE_BLOOD_DEDUCTIBLE_LIABILITY,
				bloodDeductibleLiabilityAmount, eob.getBenefitBalanceFirstRep().getFinancial());

		TransformerTestUtils.assertCareTeamEquals(operatingPhysicianNpi.get(),
				ClaimCareteamrole.ASSIST.toCode(), eob);
		TransformerTestUtils.assertCareTeamEquals(otherPhysicianNpi.get(), ClaimCareteamrole.OTHER.toCode(),
				eob);

		TransformerTestUtils.assertExtensionCodingEquals(eob.getBillablePeriod(),
				TransformerConstants.EXTENSION_CODING_CLAIM_QUERY, TransformerConstants.EXTENSION_CODING_CLAIM_QUERY,
				String.valueOf(claimQueryCode));

	}
	
	/**
	 * Test the transformation of the common group level data elements between the
	 * {@link InpatientClaim} {@link HHAClaim} {@link HospiceClaim} and {@link SNFClaim} claim
	 * types to FHIR. The method parameter fields from {@link InpatientClaim}
	 * {@link HHAClaim} {@link HospiceClaim} and {@link SNFClaim} are listed below and their
	 * corresponding RIF CCW fields (denoted in all CAPS below from
	 * {@link InpatientClaimColumn} {@link HospiceClaimColumn} {@link HHAClaimColumn} and
	 * {@link SNFClaimColumn}).
	 * 
	 * @param eob
	 *            the {@link ExplanationOfBenefit} to test
	 * 
	 */
	
	static void assertEobCommonGroupInpHHAHospiceSNFEquals(ExplanationOfBenefit eob,
			Optional<LocalDate> claimAdmissionDate, Optional<LocalDate> beneficiaryDischargeDate,
			Optional<BigDecimal> utilizedDays) {
		
		TransformerTestUtils.assertDateEquals(claimAdmissionDate.get(), eob.getHospitalization().getStartElement());
		
		if (beneficiaryDischargeDate.isPresent()) {
			TransformerTestUtils.assertDateEquals(beneficiaryDischargeDate.get(), eob.getHospitalization().getEndElement());
		}
		
		if (utilizedDays.isPresent()) {
			TransformerTestUtils.assertBenefitBalanceUsedEquals(TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
					TransformerConstants.CODED_BENEFIT_BALANCE_TYPE_SYSTEM_UTILIZATION_DAY_COUNT, utilizedDays.get().intValue(),
					eob.getBenefitBalanceFirstRep().getFinancial());
		}
		
	}
	
	/**
	 * Test the transformation of the common group level data elements between the
	 * {@link InpatientClaim} {@link HHAClaim} {@link HospiceClaim} and {@link SNFClaim} claim
	 * types to FHIR. The method parameter fields from {@link InpatientClaim}
	 * {@link HHAClaim} {@link HospiceClaim} and {@link SNFClaim} are listed below and their
	 * corresponding RIF CCW fields (denoted in all CAPS below from
	 * {@link InpatientClaimColumn} {@link HHAClaimColumn} {@link HospiceColumn} and
	 * {@link SNFClaimColumn}).
	 * 
	 * @param item
	 *            the {@link ItemComponent} to test
	 * 
	 */
	
	static void assertEobCommonGroupInpHHAHospiceSNFCoinsuranceEquals(ItemComponent item,
			Optional<Character> deductibleCoinsuranceCd) {
		
		TransformerTestUtils.assertExtensionCodingEquals(item.getRevenue(), TransformerConstants.EXTENSION_CODING_CCW_DEDUCTIBLE_COINSURANCE_CODE,
				TransformerConstants.EXTENSION_CODING_CCW_DEDUCTIBLE_COINSURANCE_CODE,
				String.valueOf(deductibleCoinsuranceCd.get()));
		
	}

	/**
	 * Tests the provider number field is set as expected in the EOB. This field is
	 * common among these claim types: Inpatient, Outpatient, Hospice, HHA and SNF.
	 * 
	 * @param eob
	 *            the {@link ExplanationOfBenefit} this method will test against
	 * @param providerNumber
	 *            a {@link String} PRVDR_NUM: representing the expected provider
	 *            number for the claim
	 */
	static void assertProviderNumber(ExplanationOfBenefit eob, String providerNumber) {
		assertReferenceIdentifierEquals(TransformerConstants.IDENTIFIER_CMS_PROVIDER_NUMBER,
				providerNumber, eob.getProvider());
	}
	
	/**
	 * Tests that the hcpcs code and hcpcs modifier codes are set as expected in the
	 * item component. The hcpcsCode field is common among these claim types:
	 * Carrier, Inpatient, Outpatient, DME, Hospice, HHA and SNF. The modifier
	 * fields are common among these claim types: Carrier, Outpatient, DME, Hospice
	 * and HHA.
	 *
	 * @param item
	 *            the {@link ItemComponent} this method will test against
	 * @param hcpcCode
	 *            the {@link Optional}&lt;{@link String}&gt; HCPCS_CD: representing
	 *            the hcpcs code for the claim
	 * @param hcpcsInitialModifierCode
	 *            the {@link Optional}&lt;{@link String}&gt; HCPCS_1ST_MDFR_CD:
	 *            representing the expected hcpcs initial modifier code for the
	 *            claim
	 * @param hcpcsSecondModifierCode
	 *            the {@link Optional}&lt;{@link String}&gt; HCPCS_2ND_MDFR_CD:
	 *            representing the expected hcpcs second modifier code for the claim
	 * @param hcpcsYearCode
	 *            the {@link Optional}&lt;{@link Character}&gt;
	 *            CARR_CLM_HCPCS_YR_CD: representing the hcpcs year code for the
	 *            claim
	 * @param index
	 *            the {@link int} modifier index in the item containing the expected
	 *            code
	 */
	static void assertHcpcsCodes(ItemComponent item, Optional<String> hcpcsCode,
			Optional<String> hcpcsInitialModifierCode, Optional<String> hcpcsSecondModifierCode, Optional<Character> hcpcsYearCode,
			int index) {
		if (hcpcsYearCode.isPresent()) { // some claim types have a year code...
			assertHasCoding(TransformerConstants.CODING_HCPCS, "" + hcpcsYearCode.get(), hcpcsInitialModifierCode.get(),
					item.getModifier().get(index));
			TransformerTestUtils.assertHasCoding(TransformerConstants.CODING_HCPCS, "" + hcpcsYearCode.get(),
					hcpcsCode.get(), item.getService());
		} else { // while others do not...
			if (hcpcsInitialModifierCode.isPresent()) {
				assertHasCoding(TransformerConstants.CODING_HCPCS, hcpcsInitialModifierCode.get(),
						item.getModifier().get(index));
			}
			if (hcpcsCode.isPresent()) {
				TransformerTestUtils.assertHasCoding(TransformerConstants.CODING_HCPCS,
						hcpcsCode.get(), item.getService());
			}

		}
		Assert.assertFalse(hcpcsSecondModifierCode.isPresent());

	}
}
