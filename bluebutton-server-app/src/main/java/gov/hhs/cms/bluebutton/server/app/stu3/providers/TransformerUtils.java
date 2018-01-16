package gov.hhs.cms.bluebutton.server.app.stu3.providers;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.Optional;
import java.util.function.Consumer;

import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Coverage;
import org.hl7.fhir.dstu3.model.DateType;
import org.hl7.fhir.dstu3.model.DomainResource;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.BenefitBalanceComponent;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.BenefitComponent;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.CareTeamComponent;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.DiagnosisComponent;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.ItemComponent;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.ProcedureComponent;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.SupportingInformationComponent;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Money;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Period;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.TemporalPrecisionEnum;
import org.hl7.fhir.dstu3.model.UnsignedIntType;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IBaseHasExtensions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.justdavis.karl.misc.exceptions.BadCodeMonkeyException;

import ca.uhn.fhir.model.primitive.IdDt;
import gov.hhs.cms.bluebutton.data.model.rif.Beneficiary;
import gov.hhs.cms.bluebutton.data.model.rif.CarrierClaim;
import gov.hhs.cms.bluebutton.server.app.stu3.providers.Diagnosis.DiagnosisLabel;

/**
 * Contains shared methods used to transform CCW JPA entities (e.g.
 * {@link Beneficiary}) into FHIR resources (e.g. {@link Patient}).
 */
/**
 * @author bglover
 *
 */
public final class TransformerUtils {
	private static final Logger LOGGER = LoggerFactory.getLogger(TransformerUtils.class);

	/**
	 * Ensures that the specified {@link ExplanationOfBenefit} has the specified
	 * {@link CareTeamComponent}, and links the specified {@link ItemComponent}
	 * to that {@link CareTeamComponent} (via
	 * {@link ItemComponent#addCareTeamLinkId(int)}).
	 * 
	 * @param eob
	 *            the {@link ExplanationOfBenefit} that the
	 *            {@link CareTeamComponent} should be part of
	 * @param eobItem
	 *            the {@link ItemComponent} that should be linked to the
	 *            {@link CareTeamComponent}
	 * @param practitionerIdSystem
	 *            the {@link Identifier#getSystem()} of the practitioner to
	 *            reference in {@link CareTeamComponent#getProvider()}
	 * @param practitionerIdValue
	 *            the {@link Identifier#getValue()} of the practitioner to
	 *            reference in {@link CareTeamComponent#getProvider()}
	 * @return the {@link CareTeamComponent} that was created/linked
	 */
	static CareTeamComponent addCareTeamPractitioner(ExplanationOfBenefit eob, ItemComponent eobItem,
			String practitionerIdSystem, String practitionerIdValue, String practitionerRole) {
		// Try to find a matching pre-existing entry.
		CareTeamComponent careTeamEntry = eob.getCareTeam().stream().filter(ctc -> ctc.getProvider().hasIdentifier())
				.filter(ctc -> practitionerIdSystem.equals(ctc.getProvider().getIdentifier().getSystem())
						&& practitionerIdValue.equals(ctc.getProvider().getIdentifier().getValue()))
				.findAny().orElse(null);
	
		// If no match was found, add one to the EOB.
		if (careTeamEntry == null) {
			careTeamEntry = eob.addCareTeam();
			careTeamEntry.setSequence(eob.getCareTeam().size() + 1);
			careTeamEntry.setProvider(new Reference()
					.setIdentifier(new Identifier().setSystem(practitionerIdSystem).setValue(practitionerIdValue)));
			careTeamEntry.setRole(
					createCodeableConcept(TransformerConstants.CODING_FHIR_CARE_TEAM_ROLE, practitionerRole));
		}
	
		// care team entry is at eob level so no need to create item link id
		if (eobItem == null) {
			return careTeamEntry;
		}
	
		// Link the EOB.item to the care team entry (if it isn't already).
		if (!eobItem.getCareTeamLinkId().contains(careTeamEntry.getSequence())) {
			eobItem.addCareTeamLinkId(careTeamEntry.getSequence());
		}
	
		return careTeamEntry;
	}

	/**
	 * @param eob
	 *            the {@link ExplanationOfBenefit} to (possibly) modify
	 * @param diagnosis
	 *            the {@link Diagnosis} to add, if it's not already present
	 * @return the {@link DiagnosisComponent#getSequence()} of the existing or
	 *         newly-added entry
	 */
	static int addDiagnosisCode(ExplanationOfBenefit eob, Diagnosis diagnosis) {
		Optional<DiagnosisComponent> existingDiagnosis = eob.getDiagnosis().stream()
				.filter(d -> d.getDiagnosis() instanceof CodeableConcept)
				.filter(d -> diagnosis.isContainedIn((CodeableConcept) d.getDiagnosis())).findAny();
		if (existingDiagnosis.isPresent())
			return existingDiagnosis.get().getSequenceElement().getValue();
	
		DiagnosisComponent diagnosisComponent = new DiagnosisComponent().setSequence(eob.getDiagnosis().size() + 1);
		diagnosisComponent.setDiagnosis(diagnosis.toCodeableConcept());
		if (diagnosis.getPresentOnAdmission().isPresent()) {
			diagnosisComponent.addType(createCodeableConcept(TransformerConstants.CODING_CCW_PRESENT_ON_ARRIVAL,
					String.valueOf(diagnosis.getPresentOnAdmission().get())));
		}
		eob.getDiagnosis().add(diagnosisComponent);
		return diagnosisComponent.getSequenceElement().getValue();
	}

	/**
	 * @param eob
	 *            the {@link ExplanationOfBenefit} that the specified
	 *            {@link ItemComponent} is a child of
	 * @param item
	 *            the {@link ItemComponent} to add an
	 *            {@link ItemComponent#getDiagnosisLinkId()} entry to
	 * @param diagnosis
	 *            the {@link Diagnosis} to add a link for
	 */
	static void addDiagnosisLink(ExplanationOfBenefit eob, ItemComponent item, Diagnosis diagnosis) {
		int diagnosisSequence = addDiagnosisCode(eob, diagnosis);
		item.addDiagnosisLinkId(diagnosisSequence);
	}

	/**
	 * <p>
	 * Adds an {@link Extension} to the specified {@link DomainResource}.
	 * {@link Extension#getValue()} will be set to a {@link CodeableConcept}
	 * containing a single {@link Coding}, with the specified system and code.
	 * </p>
	 * <p>
	 * Data Architecture Note: The {@link CodeableConcept} might seem extraneous
	 * -- why not just add the {@link Coding} directly to the {@link Extension}?
	 * The main reason for doing it this way is consistency: this is what FHIR
	 * seems to do everywhere.
	 * </p>
	 * 
	 * @param fhirElement
	 *            the FHIR element to add the {@link Extension} to
	 * @param extensionUrl
	 *            the {@link Extension#getUrl()} to use
	 * @param codingSystem
	 *            the {@link Coding#getSystem()} to use
	 * @param codingCode
	 *            the {@link Coding#getCode()} to use
	 */
	static void addExtensionCoding(IBaseHasExtensions fhirElement, String extensionUrl, String codingSystem,
			String codingCode) {
		IBaseExtension<?, ?> extension = fhirElement.addExtension();
		extension.setUrl(extensionUrl);
		extension.setValue(new Coding());
	
		CodeableConcept codeableConcept = new CodeableConcept();
		extension.setValue(codeableConcept);
	
		Coding coding = codeableConcept.addCoding();
		coding.setSystem(codingSystem).setCode(codingCode);
	}

	/**
	 * @param eob
	 *            the {@link ExplanationOfBenefit} to (possibly) modify
	 * @param infoCategory
	 *            the {@link CodeableConcept} to use as a
	 *            {@link SupportingInformationComponent#getCategory()} value, if
	 *            such an entry is not already present
	 * @return the {@link SupportingInformationComponent#getSequence()} of the
	 *         existing or newly-added entry
	 */
	static int addInformation(ExplanationOfBenefit eob, CodeableConcept infoCategory) {
		Optional<SupportingInformationComponent> existingInfo = eob.getInformation().stream()
				.filter(d -> infoCategory.equalsDeep(d.getCategory())).findAny();
		if (existingInfo.isPresent())
			return existingInfo.get().getSequenceElement().getValue();

		SupportingInformationComponent infoComponent = new SupportingInformationComponent()
				.setSequence(eob.getInformation().size() + 1);
		infoComponent.setCategory(infoCategory);
		eob.getInformation().add(infoComponent);

		return infoComponent.getSequenceElement().getValue();
	}

	/**
	 * @param eob
	 *            the {@link ExplanationOfBenefit} to (possibly) modify
	 * @param diagnosis
	 *            the {@link Diagnosis} to add, if it's not already present
	 * @return the {@link ProcedureComponent#getSequence()} of the existing or
	 *         newly-added entry
	 */
	static int addProcedureCode(ExplanationOfBenefit eob, CCWProcedure
	procedure) {

		Optional<ProcedureComponent> existingProcedure = eob.getProcedure().stream()
				.filter(pc -> pc.getProcedure() instanceof CodeableConcept)
				.filter(pc -> isCodeInConcept((CodeableConcept) pc.getProcedure(), procedure.getFhirSystem(),
						procedure.getCode()))
				.findAny();
		if (existingProcedure.isPresent())
			return existingProcedure.get().getSequenceElement().getValue();

		ProcedureComponent procedureComponent = new ProcedureComponent().setSequence(eob.getProcedure().size() + 1);
		procedureComponent.setProcedure(createCodeableConcept(procedure.getFhirSystem(), procedure.getCode()));
		procedureComponent.setDate(convertToDate(procedure.getProcedureDate()));

		eob.getProcedure().add(procedureComponent);
		return procedureComponent.getSequenceElement().getValue();

	}

	/**
	 * @param claimType
	 *            the {@link ClaimType} to compute an
	 *            {@link ExplanationOfBenefit#getId()} for
	 * @param claimId
	 *            the <code>claimId</code> field value (e.g. from
	 *            {@link CarrierClaim#getClaimId()}) to compute an
	 *            {@link ExplanationOfBenefit#getId()} for
	 * @return the {@link ExplanationOfBenefit#getId()} value to use for the
	 *         specified <code>claimId</code> value
	 */
	public static String buildEobId(ClaimType claimType, String claimId) {
		return String.format("%s-%s", claimType.name().toLowerCase(), claimId);
	}

	/**
	 * @param beneficiary
	 *            the {@link Beneficiary} to calculate the
	 *            {@link Patient#getId()} value for
	 * @return the {@link Patient#getId()} value that will be used for the
	 *         specified {@link Beneficiary}
	 */
	static IdDt buildPatientId(Beneficiary beneficiary) {
		return buildPatientId(beneficiary.getBeneficiaryId());
	}

	/**
	 * @param beneficiaryId
	 *            the {@link Beneficiary#getBeneficiaryId()} to calculate the
	 *            {@link Patient#getId()} value for
	 * @return the {@link Patient#getId()} value that will be used for the
	 *         specified {@link Beneficiary}
	 */
	public static IdDt buildPatientId(String beneficiaryId) {
		return new IdDt(Patient.class.getSimpleName(), beneficiaryId);
	}

	/**
	 * @param medicareSegment
	 *            the {@link MedicareSegment} to compute a
	 *            {@link Coverage#getId()} for
	 * @param beneficiary
	 *            the {@link Beneficiary} to compute a {@link Coverage#getId()}
	 *            for
	 * @return the {@link Coverage#getId()} value to use for the specified
	 *         values
	 */
	static IdDt buildCoverageId(MedicareSegment medicareSegment, Beneficiary beneficiary) {
		return buildCoverageId(medicareSegment, beneficiary.getBeneficiaryId());
	}

	/**
	 * @param medicareSegment
	 *            the {@link MedicareSegment} to compute a
	 *            {@link Coverage#getId()} for
	 * @param beneficiaryId
	 *            the {@link Beneficiary#getBeneficiaryId()} value to compute a
	 *            {@link Coverage#getId()} for
	 * @return the {@link Coverage#getId()} value to use for the specified
	 *         values
	 */
	public static IdDt buildCoverageId(MedicareSegment medicareSegment, String beneficiaryId) {
		return new IdDt(Coverage.class.getSimpleName(),
				String.format("%s-%s", medicareSegment.getUrlPrefix(), beneficiaryId));
	}

	/**
	 * @param localDate
	 *            the {@link LocalDate} to convert
	 * @return a {@link Date} version of the specified {@link LocalDate}
	 */
	static Date convertToDate(LocalDate localDate) {
		/*
		 * We use the system TZ here to ensure that the date doesn't shift at
		 * all, as FHIR will just use this as an unzoned Date (I think, and if
		 * not, it's almost certainly using the same TZ as this system).
		 */
		return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
	}


	/**
	 * @param codingSystem
	 *            the {@link Coding#getSystem()} to use
	 * @param codingCode
	 *            the {@link Coding#getCode()} to use
	 * @return a {@link CodeableConcept} with the specified {@link Coding}
	 */
	static CodeableConcept createCodeableConcept(String codingSystem, String codingCode) {
		return createCodeableConcept(codingSystem, null, codingCode);
	}

	/**
	 * @param codingSystem
	 *            the {@link Coding#getSystem()} to use
	 * @param codingVersion
	 *            the {@link Coding#getVersion()} to use
	 * @param codingCode
	 *            the {@link Coding#getCode()} to use
	 * @return a {@link CodeableConcept} with the specified {@link Coding}
	 */
	static CodeableConcept createCodeableConcept(String codingSystem, String codingVersion, String codingCode) {
		CodeableConcept codeableConcept = new CodeableConcept();
		Coding coding = codeableConcept.addCoding().setSystem(codingSystem).setCode(codingCode);
		if (codingVersion != null)
			coding.setVersion(codingVersion);
		return codeableConcept;
	}

	/**
	 * @param identifierSystem
	 *            the {@link Identifier#getSystem()} to use in
	 *            {@link Reference#getIdentifier()}
	 * @param identifierValue
	 *            the {@link Identifier#getValue()} to use in
	 *            {@link Reference#getIdentifier()}
	 * @return a {@link Reference} with the specified {@link Identifier}
	 */
	static Reference createIdentifierReference(String identifierSystem, String identifierValue) {
		return new Reference().setIdentifier(new Identifier().setSystem(identifierSystem).setValue(identifierValue));
	}

	/**
	 * @return a Reference to the {@link Organization} for CMS, which will only
	 *         be valid if {@link #upsertSharedData()} has been run
	 */
	static Reference createReferenceToCms() {
		return new Reference("Organization?name=" + urlEncode(TransformerConstants.COVERAGE_ISSUER));
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
	 * @param beneficiaryPatientId
	 *            the {@link #TransformerConstants.CODING_SYSTEM_CCW_BENE_ID} ID
	 *            value for the {@link Coverage#getBeneficiary()} value to match
	 * @param coverageType
	 *            the {@link MedicareSegment} value to match
	 * @return a {@link Reference} to the {@link Coverage} resource where
	 *         {@link Coverage#getPlan()} matches {@link #COVERAGE_PLAN} and the
	 *         other parameters specified also match
	 */
	static Reference referenceCoverage(String beneficiaryPatientId, MedicareSegment coverageType) {
		return new Reference(buildCoverageId(coverageType, beneficiaryPatientId));
	}

	/**
	 * @param patientId
	 *            the {@link #TransformerConstants.CODING_SYSTEM_CCW_BENE_ID} ID
	 *            value for the beneficiary to match
	 * @return a {@link Reference} to the {@link Patient} resource that matches
	 *         the specified parameters
	 */
	static Reference referencePatient(String patientId) {
		// FIXME Should reference the Patient ID now (not an identifier)
		return new Reference(
				String.format("Patient?identifier=%s|%s", "CCW.BENE_ID", patientId));
	}

	/**
	 * @param beneficiary
	 *            the {@link Beneficiary} to generate a {@link Patient}
	 *            {@link Reference} for
	 * @return a {@link Reference} to the {@link Patient} resource for the
	 *         specified {@link Beneficiary}
	 */
	static Reference referencePatient(Beneficiary beneficiary) {
		return referencePatient(beneficiary.getBeneficiaryId());
	}

	/**
	 * @param practitionerNpi
	 *            the {@link Practitioner#getIdentifier()} value to match (where
	 *            {@link Identifier#getSystem()} is
	 *            {@value #TransformerConstants.CODING_SYSTEM_NPI_US})
	 * @return a {@link Reference} to the {@link Practitioner} resource that
	 *         matches the specified parameters
	 */
	static Reference referencePractitioner(String practitionerNpi) {
		return createIdentifierReference(TransformerConstants.CODING_NPI_US, practitionerNpi);
	}

	/**
	 * @param period
	 *            the {@link Period} to adjust
	 * @param date
	 *            the {@link LocalDate} to set the {@link Period#getEnd()} value
	 *            with/to
	 */
	static void setPeriodEnd(Period period, LocalDate date) {
		period.setEnd(Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant()), TemporalPrecisionEnum.DAY);
	}

	/**
	 * @param period
	 *            the {@link Period} to adjust
	 * @param date
	 *            the {@link LocalDate} to set the {@link Period#getStart()}
	 *            value with/to
	 */
	static void setPeriodStart(Period period, LocalDate date) {
		period.setStart(Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant()), TemporalPrecisionEnum.DAY);
	}

	/**
	 * @param urlText
	 *            the URL or URL portion to be encoded
	 * @return a URL-encoded version of the specified text
	 */
	static String urlEncode(String urlText) {
		try {
			return URLEncoder.encode(urlText, StandardCharsets.UTF_8.name());
		} catch (UnsupportedEncodingException e) {
			throw new BadCodeMonkeyException(e);
		}
	}

	/**
	 * validate the from/thru dates to ensure the from date is before or the
	 * same as the thru date
	 * 
	 * @param dateFrom
	 *            start date {@link LocalDate}
	 * @param dateThrough
	 *            through date {@link LocalDate} to verify
	 */
	static void validatePeriodDates(LocalDate dateFrom, LocalDate dateThrough) {
		if (dateFrom == null)
			return;
		if (dateThrough == null)
			return;
		// FIXME see CBBD-236 (ETL service fails on some Hospice claims "From
		// date is after the Through Date")
		// We are seeing this scenario in production where the from date is
		// after the through date so we are just logging the error for now.
		if (dateFrom.isAfter(dateThrough))
			LOGGER.debug(String.format("Error - From Date '%s' is after the Through Date '%s'", dateFrom, dateThrough));
	}

	/**
	 * validate the <Optional>from/<Optional>thru dates to ensure the from date
	 * is before or the same as the thru date
	 * 
	 * @param <Optional>dateFrom
	 *            start date {@link <Optional>LocalDate}
	 * @param <Optional>dateThrough
	 *            through date {@link <Optional>LocalDate} to verify
	 */
	static void validatePeriodDates(Optional<LocalDate> dateFrom, Optional<LocalDate> dateThrough) {
		if (!dateFrom.isPresent())
			return;
		if (!dateThrough.isPresent())
			return;
		validatePeriodDates(dateFrom.get(), dateThrough.get());
	}

	/**
	 * Adds field values to the benefit balance component that are common between
	 * the Inpatient and SNF claim types.
	 * 
	 * @param benefitBalances
	 *            the {@link BenefitBalanceComponent} that will be updated by this
	 *            method
	 * @param coinsuranceDayCount
	 *            a {@link BigDecimal} shared field representing the coinsurance day
	 *            count for the claim
	 * @param nonUtilizationDayCount
	 *            a {@link BigDecimal} shared field representing the non-utilization
	 *            day count for the claim
	 * @param deductibleAmount
	 *            a {@link BigDecimal} shared field representing the deductible
	 *            amount for the claim
	 * @param partACoinsuranceLiabilityAmount
	 *            a {@link BigDecimal} shared field representing the part A
	 *            coinsurance amount for the claim
	 * @param bloodPintsFurnishedQty
	 *            a {@link BigDecimal} shared field representing the blood pints
	 *            furnished quantity for the claim
	 * @param noncoveredCharge
	 *            a {@link BigDecimal} shared field representing the non-covered
	 *            charge for the claim
	 * @param totalDeductionAmount
	 *            a {@link BigDecimal} shared field representing the total deduction
	 *            amount for the claim
	 * @param claimPPSCapitalDisproportionateShareAmt
	 *            an {@link Optional}&lt;{@link BigDecimal}&gt; shared field
	 *            representing the claim PPS capital disproportionate share amount
	 *            for the claim
	 * @param claimPPSCapitalExceptionAmount
	 *            an {@link Optional}&lt;{@link BigDecimal}&gt; shared field
	 *            representing the claim PPS capital exception amount for the claim
	 * @param claimPPSCapitalFSPAmount
	 *            an {@link Optional}&lt;{@link BigDecimal}&gt; shared field
	 *            representing the claim PPS capital FSP amount for the claim
	 * @param claimPPSCapitalIMEAmount
	 *            an {@link Optional}&lt;{@link BigDecimal}&gt; shared field
	 *            representing the claim PPS capital IME amount for the claim
	 * @param claimPPSCapitalOutlierAmount
	 *            an {@link Optional}&lt;{@link BigDecimal}&gt; shared field
	 *            representing the claim PPS capital outlier amount for the claim
	 * @param claimPPSOldCapitalHoldHarmlessAmount
	 *            an {@link Optional}&lt;{@link BigDecimal}&gt; shared field
	 *            representing the claim PPS old capital hold harmless amount for
	 *            the claim
	 */
	static void addCommonBenefitComponentInpatientSNF(BenefitBalanceComponent benefitBalances,
			BigDecimal coinsuranceDayCount, BigDecimal nonUtilizationDayCount, BigDecimal deductibleAmount,
			BigDecimal partACoinsuranceLiabilityAmount, BigDecimal bloodPintsFurnishedQty, BigDecimal noncoveredCharge,
			BigDecimal totalDeductionAmount, Optional<BigDecimal> claimPPSCapitalDisproportionateShareAmt,
			Optional<BigDecimal> claimPPSCapitalExceptionAmount, Optional<BigDecimal> claimPPSCapitalFSPAmount,
			Optional<BigDecimal> claimPPSCapitalIMEAmount, Optional<BigDecimal> claimPPSCapitalOutlierAmount,
			Optional<BigDecimal> claimPPSOldCapitalHoldHarmlessAmount) {
		BenefitComponent bc;

		// coinsuranceDayCount
		bc = new BenefitComponent(
				TransformerUtils.createCodeableConcept(TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
						TransformerConstants.CODING_CCW_COINSURANCE_DAY_COUNT));
		bc.setUsed(new UnsignedIntType(coinsuranceDayCount.intValue()));
		benefitBalances.getFinancial().add(bc);

		// nonUtilizationDayCount
		bc = new BenefitComponent(
				TransformerUtils.createCodeableConcept(TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
						TransformerConstants.CODED_BENEFIT_BALANCE_TYPE_NON_UTILIZATION_DAY_COUNT));
		bc.setAllowed(new UnsignedIntType(nonUtilizationDayCount.intValue()));
		benefitBalances.getFinancial().add(bc);

		// deductibleAmount
		if (deductibleAmount != null) {
			bc = new BenefitComponent(
					TransformerUtils.createCodeableConcept(TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
							TransformerConstants.CODED_BENEFIT_BALANCE_TYPE_DEDUCTIBLE));
			bc.setAllowed(new Money().setSystem(TransformerConstants.CODED_MONEY_USD).setValue(deductibleAmount));
			benefitBalances.getFinancial().add(bc);
		}

		// partACoinsuranceLiabilityAmount
		if (partACoinsuranceLiabilityAmount != null) {
			bc = new BenefitComponent(
					TransformerUtils.createCodeableConcept(TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
							TransformerConstants.CODED_BENEFIT_BALANCE_TYPE_COINSURANCE_LIABILITY));
			bc.setAllowed(new Money().setSystem(TransformerConstants.CODED_MONEY_USD)
					.setValue(partACoinsuranceLiabilityAmount));
			benefitBalances.getFinancial().add(bc);
		}

		// bloodPintsFurnishedQty
		bc = new BenefitComponent(
				TransformerUtils.createCodeableConcept(TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
						TransformerConstants.CODED_BENEFIT_BALANCE_TYPE_BLOOD_PINTS_FURNISHED));
		bc.setUsed(new UnsignedIntType(bloodPintsFurnishedQty.intValue()));
		benefitBalances.getFinancial().add(bc);

		// noncoveredCharge
		if (noncoveredCharge != null) {
			bc = new BenefitComponent(
					TransformerUtils.createCodeableConcept(TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
							TransformerConstants.CODED_BENEFIT_BALANCE_TYPE_NONCOVERED_CHARGE));
			bc.setAllowed(new Money().setSystem(TransformerConstants.CODED_MONEY_USD).setValue(noncoveredCharge));
			benefitBalances.getFinancial().add(bc);
		}

		// totalDeductionAmount
		if (totalDeductionAmount != null) {
			bc = new BenefitComponent(
					TransformerUtils.createCodeableConcept(TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
							TransformerConstants.CODED_BENEFIT_BALANCE_TYPE_TOTAL_DEDUCTION));
			bc.setAllowed(new Money().setSystem(TransformerConstants.CODED_MONEY_USD).setValue(totalDeductionAmount));
			benefitBalances.getFinancial().add(bc);
		}

		// claimPPSCapitalDisproportionateShareAmt
		if (claimPPSCapitalDisproportionateShareAmt.isPresent()) {
			bc = new BenefitComponent(
					TransformerUtils.createCodeableConcept(TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
							TransformerConstants.CODED_BENEFIT_BALANCE_TYPE_PPS_CAPITAL_DISPROPORTIONAL_SHARE));
			bc.setAllowed(new Money().setSystem(TransformerConstants.CODED_MONEY_USD)
					.setValue(claimPPSCapitalDisproportionateShareAmt.get()));
			benefitBalances.getFinancial().add(bc);
		}

		// claimPPSCapitalExceptionAmount
		if (claimPPSCapitalExceptionAmount.isPresent()) {
			bc = new BenefitComponent(
					TransformerUtils.createCodeableConcept(TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
							TransformerConstants.CODED_BENEFIT_BALANCE_TYPE_PPS_CAPITAL_EXCEPTION));
			bc.setAllowed(new Money().setSystem(TransformerConstants.CODED_MONEY_USD)
					.setValue(claimPPSCapitalExceptionAmount.get()));
			benefitBalances.getFinancial().add(bc);
		}

		// claimPPSCapitalFSPAmount
		if (claimPPSCapitalFSPAmount.isPresent()) {
			bc = new BenefitComponent(
					TransformerUtils.createCodeableConcept(TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
							TransformerConstants.CODED_BENEFIT_BALANCE_TYPE_PPS_CAPITAL_FEDRERAL_PORTION));
			bc.setAllowed(new Money().setSystem(TransformerConstants.CODED_MONEY_USD)
					.setValue(claimPPSCapitalFSPAmount.get()));
			benefitBalances.getFinancial().add(bc);
		}

		// claimPPSCapitalIMEAmount
		if (claimPPSCapitalIMEAmount.isPresent()) {
			bc = new BenefitComponent(
					TransformerUtils.createCodeableConcept(TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
							TransformerConstants.CODED_BENEFIT_BALANCE_TYPE_PPS_CAPITAL_INDIRECT_MEDICAL_EDU));
			bc.setAllowed(new Money().setSystem(TransformerConstants.CODED_MONEY_USD)
					.setValue(claimPPSCapitalIMEAmount.get()));
			benefitBalances.getFinancial().add(bc);
		}

		// claimPPSCapitalOutlierAmount
		if (claimPPSCapitalOutlierAmount.isPresent()) {
			bc = new BenefitComponent(
					TransformerUtils.createCodeableConcept(TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
							TransformerConstants.CODED_BENEFIT_BALANCE_TYPE_PPS_CAPITAL_OUTLIER));
			bc.setAllowed(new Money().setSystem(TransformerConstants.CODED_MONEY_USD)
					.setValue(claimPPSCapitalOutlierAmount.get()));
			benefitBalances.getFinancial().add(bc);
		}

		// claimPPSOldCapitalHoldHarmlessAmount
		if (claimPPSOldCapitalHoldHarmlessAmount.isPresent()) {
			bc = new BenefitComponent(
					TransformerUtils.createCodeableConcept(TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
							TransformerConstants.CODED_BENEFIT_BALANCE_TYPE_PPS_OLD_CAPITAL_HOLD_HARMLESS));
			bc.setAllowed(new Money().setSystem(TransformerConstants.CODED_MONEY_USD)
					.setValue(claimPPSOldCapitalHoldHarmlessAmount.get()));
			benefitBalances.getFinancial().add(bc);
		}

	}

	/**
	 * Extracts diagnoses that are common between Inpatient and SNF claim types.
	 * 
	 * @param diagnosisAdder
	 *            a {@link Consumer}&lt;{link Optional}&lt;{@link Diagnosis}&gt;&gt;
	 *            object used to add the common diagnosis fields
	 * @param diagnosisAdmittingCode
	 *            an {@link Optional}&lt;{link String}&gt; shared field representing
	 * @param diagnosisAdmittingCodeVersion
	 *            an {@link Optional}&lt;{link Character}&gt; shared field
	 *            representing the diagnosis admitting code version for the claim
	 * @param admittingLabel
	 *            a {@link DiagnosisLabel} shared field representing the admitting
	 *            label for the claim
	 */
	static void extractCommonDiagnosesInpatientSNF(Consumer<Optional<Diagnosis>> diagnosisAdder,
			Optional<String> diagnosisAdmittingCode, Optional<Character> diagnosisAdmittingCodeVersion,
			DiagnosisLabel admittingLabel) {
		diagnosisAdder.accept(Diagnosis.from(diagnosisAdmittingCode, diagnosisAdmittingCodeVersion, admittingLabel));
	}

	/**
	 * Adds EOB information to fields that are common between the Inpatient and SNF
	 * claim types.
	 * 
	 * @param eob
	 *            the {@link ExplanationOfBenefit} that fields will be added to by
	 *            this method
	 * @param admissionTypeCd
	 *            a {@link Character} shared field representing the admission type
	 *            cd for the claim
	 * @param sourceAdmissionCd
	 *            an {@link Optional}&lt;{@link Character}&gt; shared field
	 *            representing the source admission cd for the claim
	 * @param noncoveredStayFromDate
	 *            an {@link Optional}&lt;{@link LocalDate}&gt; shared field
	 *            representing the non-covered stay from date for the claim
	 * @param noncoveredStayThroughDate
	 *            an {@link Optional}&lt;{@link LocalDate}&gt; shared field
	 *            representing the non-covered stay through date for the claim
	 * @param coveredCareThroughDate
	 *            an {@link Optional}&lt;{@link LocalDate}&gt; shared field
	 *            representing the covered stay through date for the claim
	 * @param medicareBenefitsExhaustedDate
	 *            an {@link Optional}&lt;{@link LocalDate}&gt; shared field
	 *            representing the medicare benefits exhausted date for the claim
	 * @param diagnosisRelatedGroupCd
	 *            an {@link Optional}&lt;{@link String}&gt; shared field
	 *            representing the non-covered stay from date for the claim
	 */
	static void addCommonEobInformationInpatientSNF(ExplanationOfBenefit eob, Character admissionTypeCd,
			Optional<Character> sourceAdmissionCd, Optional<LocalDate> noncoveredStayFromDate,
			Optional<LocalDate> noncoveredStayThroughDate, Optional<LocalDate> coveredCareThroughDate,
			Optional<LocalDate> medicareBenefitsExhaustedDate, Optional<String> diagnosisRelatedGroupCd) {

		// admissionTypeCd
		eob.addInformation().setCategory(TransformerUtils.createCodeableConcept(
				TransformerConstants.CODING_CCW_ADMISSION_TYPE, String.valueOf(admissionTypeCd)));

		// sourceAdmissionCd
		if (sourceAdmissionCd.isPresent()) {
			eob.addInformation().setCategory(TransformerUtils.createCodeableConcept(
					TransformerConstants.CODING_CMS_SOURCE_ADMISSION, String.valueOf(sourceAdmissionCd.get())));
		}

		// noncoveredStayFromDate & noncoveredStayThroughDate
		if (noncoveredStayFromDate.isPresent() && noncoveredStayThroughDate.isPresent()) {
			TransformerUtils.validatePeriodDates(noncoveredStayFromDate, noncoveredStayThroughDate);
			eob.addInformation()
					.setCategory(TransformerUtils.createCodeableConcept(
							TransformerConstants.CODING_BBAPI_BENEFIT_COVERAGE_DATE,
							TransformerConstants.CODED_BENEFIT_COVERAGE_DATE_NONCOVERED))
					.setTiming(new Period()
							.setStart(TransformerUtils.convertToDate((noncoveredStayFromDate.get())),
									TemporalPrecisionEnum.DAY)
							.setEnd(TransformerUtils.convertToDate((noncoveredStayThroughDate.get())),
									TemporalPrecisionEnum.DAY));
		}

		// coveredCareThroughDate
		if (coveredCareThroughDate.isPresent()) {
			eob.addInformation()
					.setCategory(TransformerUtils.createCodeableConcept(
							TransformerConstants.CODING_BBAPI_BENEFIT_COVERAGE_DATE,
							TransformerConstants.CODED_BENEFIT_COVERAGE_DATE_STAY))
					.setTiming(new DateType(TransformerUtils.convertToDate(coveredCareThroughDate.get())));
		}
		
		// medicareBenefitsExhaustedDate
		if (medicareBenefitsExhaustedDate.isPresent()) {
			eob.addInformation()
					.setCategory(TransformerUtils.createCodeableConcept(
							TransformerConstants.CODING_BBAPI_BENEFIT_COVERAGE_DATE,
							TransformerConstants.CODED_BENEFIT_COVERAGE_DATE_EXHAUSTED))
					.setTiming(new DateType(TransformerUtils.convertToDate(medicareBenefitsExhaustedDate.get())));
		}
		
		// diagnosisRelatedGroupCd
		if (diagnosisRelatedGroupCd.isPresent()) {
			eob.addInformation().setCategory(TransformerUtils.createCodeableConcept(
					TransformerConstants.CODING_CCW_DIAGNOSIS_RELATED_GROUP,
					diagnosisRelatedGroupCd.get()));
		}
	}
}
