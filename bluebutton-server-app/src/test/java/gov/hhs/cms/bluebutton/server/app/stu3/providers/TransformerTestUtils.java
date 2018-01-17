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
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.BenefitBalanceComponent;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.BenefitComponent;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.CareTeamComponent;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.DiagnosisComponent;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.ItemComponent;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.SupportingInformationComponent;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.dstu3.model.TemporalPrecisionEnum;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IBaseHasExtensions;
import org.junit.Assert;

import com.justdavis.karl.misc.exceptions.BadCodeMonkeyException;

import ca.uhn.fhir.context.FhirContext;

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
}
