package gov.hhs.cms.bluebutton.server.app.stu3.providers;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Coverage;
import org.hl7.fhir.dstu3.model.DateType;
import org.hl7.fhir.dstu3.model.DomainResource;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.AdjudicationComponent;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.BenefitBalanceComponent;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.BenefitComponent;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.CareTeamComponent;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.DiagnosisComponent;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.ExplanationOfBenefitStatus;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.ItemComponent;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.ProcedureComponent;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.SupportingInformationComponent;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Money;
import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.dstu3.model.Observation.ObservationStatus;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Period;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.hl7.fhir.dstu3.model.Quantity;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.ReferralRequest;
import org.hl7.fhir.dstu3.model.ReferralRequest.ReferralRequestRequesterComponent;
import org.hl7.fhir.dstu3.model.ReferralRequest.ReferralRequestStatus;
import org.hl7.fhir.dstu3.model.SimpleQuantity;
import org.hl7.fhir.dstu3.model.TemporalPrecisionEnum;
import org.hl7.fhir.dstu3.model.UnsignedIntType;
import org.hl7.fhir.dstu3.model.codesystems.ClaimCareteamrole;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IBaseHasExtensions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.justdavis.karl.misc.exceptions.BadCodeMonkeyException;

import ca.uhn.fhir.model.primitive.IdDt;
import gov.hhs.cms.bluebutton.data.model.rif.Beneficiary;
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
import gov.hhs.cms.bluebutton.data.model.rif.parse.InvalidRifValueException;
import gov.hhs.cms.bluebutton.server.app.stu3.providers.Diagnosis.DiagnosisLabel;

/**
 * Contains shared methods used to transform CCW JPA entities (e.g.
 * {@link Beneficiary}) into FHIR resources (e.g. {@link Patient}).
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

		if (!diagnosis.getLabels().isEmpty()) {
			diagnosisComponent.addType(createCodeableConcept(TransformerConstants.CODING_FHIR_DIAGNOSIS_TYPE,
					String.valueOf(diagnosis.getLabels())));
		}
	    if (diagnosis.getPresentOnAdmission().isPresent()) {
	    	addExtensionCoding(diagnosisComponent, TransformerConstants.CODING_CCW_PRESENT_ON_ARRIVAL,
					TransformerConstants.CODING_CCW_PRESENT_ON_ARRIVAL,
					String.valueOf(diagnosis.getPresentOnAdmission().get()));
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
		return new Reference(String.format("Patient/%s", patientId));
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
		// FIXME: check if this field is non-nullable and if not remove the "if" check 
		if (deductibleAmount != null) {
			bc = new BenefitComponent(
					TransformerUtils.createCodeableConcept(TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
							TransformerConstants.CODED_BENEFIT_BALANCE_TYPE_DEDUCTIBLE));
			bc.setAllowed(new Money().setSystem(TransformerConstants.CODED_MONEY_USD).setValue(deductibleAmount));
			benefitBalances.getFinancial().add(bc);
		}

		// partACoinsuranceLiabilityAmount
		// FIXME: check if this field is non-nullable and if not remove the "if" check 
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
		// FIXME: check if this field is non-nullable and if not remove the "if" check 
		if (noncoveredCharge != null) {
			bc = new BenefitComponent(
					TransformerUtils.createCodeableConcept(TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
							TransformerConstants.CODED_BENEFIT_BALANCE_TYPE_NONCOVERED_CHARGE));
			bc.setAllowed(new Money().setSystem(TransformerConstants.CODED_MONEY_USD).setValue(noncoveredCharge));
			benefitBalances.getFinancial().add(bc);
		}

		// totalDeductionAmount
		// FIXME: check if this field is non-nullable and if not remove the "if" check 
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
	 * Adds EOB information to fields that are common between the Inpatient and SNF
	 * claim types.
	 * 
	 * @param eob
	 *            the {@link ExplanationOfBenefit} that fields will be added to by
	 *            this method
	 * @param admissionTypeCd
	 *            CLM_IP_ADMSN_TYPE_CD: a {@link Character} shared field
	 *            representing the admission type cd for the claim
	 * @param sourceAdmissionCd
	 *            CLM_SRC_IP_ADMSN_CD: an {@link Optional}&lt;{@link Character}&gt;
	 *            shared field representing the source admission cd for the claim
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
			eob.addDiagnosis().setPackageCode(TransformerUtils.createCodeableConcept(
					TransformerConstants.CODING_CCW_DIAGNOSIS_RELATED_GROUP,
					diagnosisRelatedGroupCd.get()));
		}
	}

	/**
	 * maps a blue button claim type to a FHIR claim type
	 * 
	 * @param eobType
	 * 		the {@link CodeableConcept} that will get remapped 
	 * @param blueButtonClaimType
	 * 		the blue button {@link ClaimType} we are mapping from 
	 * @param ccwNearLineRecordIdCode
	 * 		if present, the blue button near line id code {@link Optional}&lt;{@link Character}&gt; gets remapped to a ccw record id code
	 * @param ccwClaimTypeCode
	 * 		if present, the blue button claim type code {@link Optional}&lt;{@link String}&gt; gets remapped to a nch claim type code
	 */
	static void mapEobType(ExplanationOfBenefit eob, ClaimType blueButtonClaimType,
			Optional<Character> ccwNearLineRecordIdCode, Optional<String> ccwClaimTypeCode) {
		
		// map blue button claim type code into a nch claim type
		if(ccwClaimTypeCode.isPresent() ) {
			eob.getType().addCoding().setSystem(TransformerConstants.CODING_NCH_CLAIM_TYPE).setCode(
					ccwClaimTypeCode.get());
		}
		
		// This Coding MUST always be present as it's the only one we can definitely map for all 8 of our claim types.
		eob.getType().addCoding().setSystem(TransformerConstants.CODING_CCW_CLAIM_TYPE).setCode(blueButtonClaimType.name());
		
		// Map a Coding for FHIR's ClaimType coding system, if we can.
		switch(blueButtonClaimType) {
		case CARRIER:
		case OUTPATIENT:
			// map these blue button claim types to PROFESSIONAL
			eob.getType().addCoding().setSystem(TransformerConstants.CODING_FHIR_CLAIM_TYPE).setCode(
					org.hl7.fhir.dstu3.model.codesystems.ClaimType.PROFESSIONAL.name());
			break;
			
		case INPATIENT:
		case HOSPICE:
		case SNF:
			// map these blue button claim types to INSTITUTIONAL
			eob.getType().addCoding().setSystem(TransformerConstants.CODING_FHIR_CLAIM_TYPE).setCode(
					org.hl7.fhir.dstu3.model.codesystems.ClaimType.INSTITUTIONAL.name());
			break;
		case PDE:
			// map these blue button claim types to PHARMACY
			eob.getType().addCoding().setSystem(TransformerConstants.CODING_FHIR_CLAIM_TYPE).setCode(
					org.hl7.fhir.dstu3.model.codesystems.ClaimType.PHARMACY.name());
			break;
			
		case HHA:
		case DME:
			// FUTURE these blue button claim types currently have no equivalent CODING_FHIR_CLAIM_TYPE mapping
			break;
			
		default:
			// unknown claim type
			throw new BadCodeMonkeyException();
		}
		
		// map blue button near line record id to a ccw record id code
		if(ccwNearLineRecordIdCode.isPresent()) {
			eob.getType().addCoding().setSystem(TransformerConstants.CODING_CCW_RECORD_ID_CODE).setCode(
					String.valueOf(ccwNearLineRecordIdCode.get()));
		}
  }

	/**
	 * Transforms the common group level header fields between all claim types
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
	static void mapEobCommonClaimHeaderData(ExplanationOfBenefit eob, String claimId,
			String beneficiaryId, ClaimType claimType, String claimGroupId, MedicareSegment coverageType,
			Optional<LocalDate> dateFrom, Optional<LocalDate> dateThrough, Optional<BigDecimal> paymentAmount,
			char finalAction) {

		eob.setId(buildEobId(claimType, claimId));

		if (claimType.equals(ClaimType.PDE))
			eob.addIdentifier().setSystem(TransformerConstants.CODING_CCW_PARTD_EVENT_ID).setValue(claimId);
		else
			eob.addIdentifier().setSystem(TransformerConstants.CODING_CCW_CLAIM_ID).setValue(claimId);

		eob.addIdentifier().setSystem(TransformerConstants.CODING_CCW_CLAIM_GROUP_ID)
				.setValue(claimGroupId);

		eob.getInsurance()
				.setCoverage(referenceCoverage(beneficiaryId, coverageType));
		eob.setPatient(referencePatient(beneficiaryId));
		switch (finalAction) {
		case 'F':
			eob.setStatus(ExplanationOfBenefitStatus.ACTIVE);
			break;
		case 'N':
			eob.setStatus(ExplanationOfBenefitStatus.CANCELLED);
			break;
		default:
			// unknown final action value
			throw new BadCodeMonkeyException();
		}
		
		if (dateFrom.isPresent()) {
			validatePeriodDates(dateFrom, dateThrough);
			setPeriodStart(eob.getBillablePeriod(), dateFrom.get());
			setPeriodEnd(eob.getBillablePeriod(), dateThrough.get());
		}

		eob.setDisposition(TransformerConstants.CODED_EOB_DISPOSITION);

		if (paymentAmount.isPresent()) {
			eob.getPayment().setAmount(
					(Money) new Money().setSystem(TransformerConstants.CODED_MONEY_USD).setValue(paymentAmount.get()));
		}
	}

	/**
	 * Transforms the common group level data elements between the
	 * {@link CarrierClaim} and {@link DMEClaim} claim types to FHIR. The method
	 * parameter fields from {@link CarrierClaim} and {@link DMEClaim} are listed
	 * below and their corresponding RIF CCW fields (denoted in all CAPS below from
	 * {@link CarrierClaimColumn} and {@link DMEClaimColumn}).
	 * 
	 * @param eob
	 *            the {@link ExplanationOfBenefit} to modify
	 * @param beneficiaryId
	 *            BENE_ID, *
	 * @param carrierNumber
	 *            CARR_NUM,
	 * @param clinicalTrialNumber
	 *            CLM_CLNCL_TRIL_NUM,
	 * @param beneficiaryPartBDeductAmount
	 *            CARR_CLM_CASH_DDCTBL_APLD_AMT,
	 * @param paymentDenialCode
	 *            CARR_CLM_PMT_DNL_CD,
	 * @param referringPhysicianNpi
	 *            RFR_PHYSN_NPI
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
	static void mapEobCommonGroupCarrierDME(ExplanationOfBenefit eob, String beneficiaryId,
			String carrierNumber, Optional<String> clinicalTrialNumber, BigDecimal beneficiaryPartBDeductAmount,
			String paymentDenialCode, Optional<String> referringPhysicianNpi,
			Optional<Character> providerAssignmentIndicator, BigDecimal providerPaymentAmount,
			BigDecimal beneficiaryPaymentAmount, BigDecimal submittedChargeAmount,
			BigDecimal allowedChargeAmount) {
		/*
		 * FIXME this should be mapped as an extension valueIdentifier instead of as a
		 * valueCodeableConcept
		 */
		addExtensionCoding(eob, TransformerConstants.EXTENSION_IDENTIFIER_CARRIER_NUMBER,
				TransformerConstants.EXTENSION_IDENTIFIER_CARRIER_NUMBER, carrierNumber);
		addExtensionCoding(eob, TransformerConstants.EXTENSION_CODING_CCW_CARR_PAYMENT_DENIAL,
				TransformerConstants.EXTENSION_CODING_CCW_CARR_PAYMENT_DENIAL, paymentDenialCode);

		/*
		 * Referrals are represented as contained resources, since they share the
		 * lifecycle and identity of their containing EOB.
		 */
		if (referringPhysicianNpi.isPresent()) {
			ReferralRequest referral = new ReferralRequest();
			referral.setStatus(ReferralRequestStatus.COMPLETED);
			referral.setSubject(referencePatient(beneficiaryId));
			referral.setRequester(new ReferralRequestRequesterComponent(
					referencePractitioner(referringPhysicianNpi.get())));
			referral.addRecipient(referencePractitioner(referringPhysicianNpi.get()));
			// Set the ReferralRequest as a contained resource in the EOB:
			eob.setReferral(new Reference(referral));
		}

		if (providerAssignmentIndicator.isPresent()) {
			addExtensionCoding(eob, TransformerConstants.CODING_CCW_PROVIDER_ASSIGNMENT,
					TransformerConstants.CODING_CCW_PROVIDER_ASSIGNMENT,
					String.valueOf(providerAssignmentIndicator.get()));
		}

		if (clinicalTrialNumber.isPresent()) {
			/*
			 * FIXME this should be mapped as an extension valueIdentifier instead of as a
			 * valueCodeableConcept
			 */
			addExtensionCoding(eob, TransformerConstants.EXTENSION_IDENTIFIER_CLINICAL_TRIAL_NUMBER,
					TransformerConstants.EXTENSION_IDENTIFIER_CLINICAL_TRIAL_NUMBER, clinicalTrialNumber.get());
		}

		BenefitComponent beneficiaryPartBDeductAmt = new BenefitComponent(
				createCodeableConcept(TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
						TransformerConstants.CODED_ADJUDICATION_NCH_BENEFICIARY_PART_B_DEDUCTIBLE));
		beneficiaryPartBDeductAmt.setAllowed(
				new Money().setSystem(TransformerConstants.CODED_MONEY_USD).setValue(beneficiaryPartBDeductAmount));
		eob.getBenefitBalanceFirstRep().getFinancial().add((beneficiaryPartBDeductAmt));

		BenefitComponent providerPaymentAmt = new BenefitComponent(
				createCodeableConcept(TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
						TransformerConstants.CODED_ADJUDICATION_PROVIDER_PAYMENT_AMOUNT));
		providerPaymentAmt.setAllowed(
					new Money().setSystem(TransformerConstants.CODED_MONEY_USD).setValue(providerPaymentAmount));
		eob.getBenefitBalanceFirstRep().getFinancial().add(providerPaymentAmt);

		BenefitComponent beneficiaryPaymentAmt = new BenefitComponent(
				createCodeableConcept(TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
							TransformerConstants.CODED_ADJUDICATION_BENEFICIARY_PAYMENT_AMOUNT));
		beneficiaryPaymentAmt.setAllowed(
					new Money().setSystem(TransformerConstants.CODED_MONEY_USD).setValue(beneficiaryPaymentAmount));
		eob.getBenefitBalanceFirstRep().getFinancial().add(beneficiaryPaymentAmt);

		BenefitComponent submittedChargeAmt = new BenefitComponent(
					createCodeableConcept(TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
							TransformerConstants.CODED_ADJUDICATION_SUBMITTED_CHARGE_AMOUNT));
		submittedChargeAmt.setAllowed(
					new Money().setSystem(TransformerConstants.CODED_MONEY_USD).setValue(submittedChargeAmount));
		eob.getBenefitBalanceFirstRep().getFinancial().add(submittedChargeAmt);

		BenefitComponent allowedChargeAmt = new BenefitComponent(
				createCodeableConcept(TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
						TransformerConstants.CODED_ADJUDICATION_ALLOWED_CHARGE));
		allowedChargeAmt.setAllowed(
					new Money().setSystem(TransformerConstants.CODED_MONEY_USD).setValue(allowedChargeAmount));
		eob.getBenefitBalanceFirstRep().getFinancial().add(allowedChargeAmt);

	}


	/**
	 * Transforms the common item level data elements between the
	 * {@link CarrierClaimLine} and {@link DMEClaimLine} claim types to FHIR. The
	 * method parameter fields from {@link CarrierClaimLine} and
	 * {@link DMEClaimLine} are listed below and their corresponding RIF CCW fields
	 * (denoted in all CAPS below from {@link CarrierClaimColumn} and
	 * {@link DMEClaimColumn}).
	 * 
	 * @param item
	 *            the {@ ItemComponent} to modify
	 * @param eob
	 *            the {@ ExplanationOfBenefit} to modify
	 * @param claimId
	 *            CLM_ID,
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
	 *            LINE_NDC_CD
	 * 
	 * @return the {@link ItemComponent}
	 */
	static ItemComponent mapEobCommonItemCarrierDME(ItemComponent item, ExplanationOfBenefit eob, String claimId,
			BigDecimal serviceCount, String placeOfServiceCode, Optional<LocalDate> firstExpenseDate,
			Optional<LocalDate> lastExpenseDate, BigDecimal beneficiaryPaymentAmount, BigDecimal providerPaymentAmount,
			BigDecimal beneficiaryPartBDeductAmount, Optional<Character> primaryPayerCode,
			BigDecimal primaryPayerPaidAmount, Optional<String> betosCode, BigDecimal paymentAmount,
			Optional<Character> paymentCode, BigDecimal coinsuranceAmount, BigDecimal submittedChargeAmount,
			BigDecimal allowedChargeAmount, Optional<String> processingIndicatorCode,
			Optional<Character> serviceDeductibleCode, Optional<String> diagnosisCode,
			Optional<Character> diagnosisCodeVersion,
			Optional<String> hctHgbTestTypeCode, BigDecimal hctHgbTestResult,
			char cmsServiceTypeCode, Optional<String> nationalDrugCode) {

		SimpleQuantity serviceCnt = new SimpleQuantity();
		serviceCnt.setValue(serviceCount);
		item.setQuantity(serviceCnt);

		item.setCategory(createCodeableConcept(TransformerConstants.CODING_CCW_TYPE_SERVICE,
				"" + cmsServiceTypeCode));

		item.setLocation(createCodeableConcept(TransformerConstants.CODING_CCW_PLACE_OF_SERVICE,
				placeOfServiceCode));

		if (betosCode.isPresent()) {
			addExtensionCoding(item, TransformerConstants.CODING_BETOS,
					TransformerConstants.CODING_BETOS, betosCode.get());
		}

		if (firstExpenseDate.isPresent() && lastExpenseDate.isPresent()) {
			validatePeriodDates(firstExpenseDate, lastExpenseDate);
			item.setServiced(new Period()
					.setStart((convertToDate(firstExpenseDate.get())),
							TemporalPrecisionEnum.DAY)
					.setEnd((convertToDate(lastExpenseDate.get())),
							TemporalPrecisionEnum.DAY));
		}

		AdjudicationComponent adjudicationForPayment = item.addAdjudication();
		adjudicationForPayment
				.setCategory(
						createCodeableConcept(TransformerConstants.CODING_CCW_ADJUDICATION_CATEGORY,
								TransformerConstants.CODED_ADJUDICATION_PAYMENT))
				.getAmount().setSystem(TransformerConstants.CODING_MONEY).setCode(TransformerConstants.CODED_MONEY_USD)
				.setValue(paymentAmount);
		addExtensionCoding(adjudicationForPayment,
				TransformerConstants.EXTENSION_CODING_CCW_PAYMENT_80_100_INDICATOR,
				TransformerConstants.EXTENSION_CODING_CCW_PAYMENT_80_100_INDICATOR,
				"" + paymentCode.get());

		item.addAdjudication()
				.setCategory(
						createCodeableConcept(TransformerConstants.CODING_CCW_ADJUDICATION_CATEGORY,
								TransformerConstants.CODED_ADJUDICATION_BENEFICIARY_PAYMENT_AMOUNT))
				.getAmount().setSystem(TransformerConstants.CODING_MONEY).setCode(TransformerConstants.CODED_MONEY_USD)
				.setValue(beneficiaryPaymentAmount);

		item.addAdjudication()
				.setCategory(
						createCodeableConcept(TransformerConstants.CODING_CCW_ADJUDICATION_CATEGORY,
								TransformerConstants.CODED_ADJUDICATION_PROVIDER_PAYMENT_AMOUNT))
				.getAmount().setSystem(TransformerConstants.CODING_MONEY).setCode(TransformerConstants.CODED_MONEY_USD)
				.setValue(providerPaymentAmount);

		item.addAdjudication()
				.setCategory(
						createCodeableConcept(TransformerConstants.CODING_CCW_ADJUDICATION_CATEGORY,
								TransformerConstants.CODED_ADJUDICATION_DEDUCTIBLE))
				.getAmount().setSystem(TransformerConstants.CODING_MONEY).setCode(TransformerConstants.CODED_MONEY_USD)
				.setValue(beneficiaryPartBDeductAmount);

		if (primaryPayerCode.isPresent()) {
			addExtensionCoding(item, TransformerConstants.EXTENSION_CODING_PRIMARY_PAYER,
					TransformerConstants.EXTENSION_CODING_PRIMARY_PAYER,
					String.valueOf(primaryPayerCode.get()));
		}

		item.addAdjudication()
				.setCategory(
						createCodeableConcept(TransformerConstants.CODING_CCW_ADJUDICATION_CATEGORY,
								TransformerConstants.CODED_ADJUDICATION_PRIMARY_PAYER_PAID_AMOUNT))
				.getAmount().setSystem(TransformerConstants.CODING_MONEY).setCode(TransformerConstants.CODED_MONEY_USD)
				.setValue(primaryPayerPaidAmount);
		item.addAdjudication()
				.setCategory(
						createCodeableConcept(TransformerConstants.CODING_CCW_ADJUDICATION_CATEGORY,
								TransformerConstants.CODED_ADJUDICATION_LINE_COINSURANCE_AMOUNT))
				.getAmount().setSystem(TransformerConstants.CODING_MONEY).setCode(TransformerConstants.CODED_MONEY_USD)
				.setValue(coinsuranceAmount);

		item.addAdjudication()
				.setCategory(
						createCodeableConcept(TransformerConstants.CODING_CCW_ADJUDICATION_CATEGORY,
								TransformerConstants.CODED_ADJUDICATION_SUBMITTED_CHARGE_AMOUNT))
				.getAmount().setSystem(TransformerConstants.CODING_MONEY).setCode(TransformerConstants.CODED_MONEY_USD)
				.setValue(submittedChargeAmount);

		item.addAdjudication()
				.setCategory(
						createCodeableConcept(TransformerConstants.CODING_CCW_ADJUDICATION_CATEGORY,
								TransformerConstants.CODED_ADJUDICATION_ALLOWED_CHARGE))
				.getAmount().setSystem(TransformerConstants.CODING_MONEY).setCode(TransformerConstants.CODED_MONEY_USD)
				.setValue(allowedChargeAmount);

		item.addAdjudication()
				.setCategory(createCodeableConcept(TransformerConstants.CODING_CCW_ADJUDICATION_CATEGORY,
						TransformerConstants.CODED_ADJUDICATION_LINE_PROCESSING_INDICATOR))
				.setReason(createCodeableConcept(TransformerConstants.CODING_CCW_PROCESSING_INDICATOR,
						processingIndicatorCode.get()));

		addExtensionCoding(item, TransformerConstants.EXTENSION_CODING_CCW_LINE_DEDUCTIBLE_SWITCH,
				TransformerConstants.EXTENSION_CODING_CCW_LINE_DEDUCTIBLE_SWITCH,
				"" + serviceDeductibleCode.get());

		Optional<Diagnosis> lineDiagnosis = Diagnosis.from(diagnosisCode, diagnosisCodeVersion);
		if (lineDiagnosis.isPresent())
			addDiagnosisLink(eob, item, lineDiagnosis.get());

		if (hctHgbTestTypeCode.isPresent() && hctHgbTestResult.compareTo(BigDecimal.ZERO) != 0) {
			Observation hctHgbObservation = new Observation();
			hctHgbObservation.setStatus(ObservationStatus.UNKNOWN);
			CodeableConcept hctHgbTestType = new CodeableConcept();
			hctHgbTestType.addCoding().setSystem(TransformerConstants.CODING_CCW_HCT_OR_HGB_TEST_TYPE)
					.setCode(hctHgbTestTypeCode.get());
			hctHgbObservation.setCode(hctHgbTestType);
			hctHgbObservation.setValue(new Quantity().setValue(hctHgbTestResult));
			item.addExtension().setUrl(TransformerConstants.EXTENSION_CMS_HCT_OR_HGB_RESULTS)
					.setValue(new Reference(hctHgbObservation));
		} else if (!hctHgbTestTypeCode.isPresent() && hctHgbTestResult.compareTo(BigDecimal.ZERO) == 0) {
			// Nothing to do here; don't map a non-existent Observation.
		} else {
			throw new InvalidRifValueException(
					String.format("Inconsistent hctHgbTestTypeCode and hctHgbTestResult" + " values for claim '%s'.",
							claimId));
		}

		if (nationalDrugCode.isPresent()) {
			addExtensionCoding(item, TransformerConstants.CODING_NDC, TransformerConstants.CODING_NDC,
					nationalDrugCode.get());
		}

		return item;
	}

	/**
	 * Transforms the common item level data elements between the
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
	 *            the {@ ItemComponent} to modify
	 * @param eob
	 *            the {@ ExplanationOfBenefit} to modify
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
	 * 
	 * @return the {@link ItemComponent}
	 */
	static ItemComponent mapEobCommonItemRevenue(ItemComponent item, ExplanationOfBenefit eob, String revenueCenterCode,
			BigDecimal rateAmount,
			BigDecimal totalChargeAmount, BigDecimal nonCoveredChargeAmount, BigDecimal unitCount,
			Optional<BigDecimal> nationalDrugCodeQuantity, Optional<String> nationalDrugCodeQualifierCode,
			Optional<String> revenueCenterRenderingPhysicianNPI) {

		item.setRevenue(TransformerUtils.createCodeableConcept(TransformerConstants.CODING_CMS_REVENUE_CENTER,
				revenueCenterCode));

		item.addAdjudication()
				.setCategory(
						TransformerUtils.createCodeableConcept(TransformerConstants.CODING_CCW_ADJUDICATION_CATEGORY,
								TransformerConstants.CODED_ADJUDICATION_RATE_AMOUNT))
				.getAmount().setSystem(TransformerConstants.CODING_MONEY).setCode(TransformerConstants.CODED_MONEY_USD)
				.setValue(rateAmount);

		item.addAdjudication()
				.setCategory(
						TransformerUtils.createCodeableConcept(TransformerConstants.CODING_CCW_ADJUDICATION_CATEGORY,
								TransformerConstants.CODED_ADJUDICATION_TOTAL_CHARGE_AMOUNT))
				.getAmount().setSystem(TransformerConstants.CODING_MONEY).setCode(TransformerConstants.CODED_MONEY_USD)
				.setValue(totalChargeAmount);

		item.addAdjudication()
				.setCategory(
						TransformerUtils.createCodeableConcept(TransformerConstants.CODING_CCW_ADJUDICATION_CATEGORY,
								TransformerConstants.CODED_ADJUDICATION_NONCOVERED_CHARGE))
				.getAmount().setSystem(TransformerConstants.CODING_MONEY).setCode(TransformerConstants.CODED_MONEY_USD)
				.setValue(nonCoveredChargeAmount);

		/*
		 * Set item quantity to Unit Count first if > 0; NDC quantity next if present;
		 * otherwise set to 0
		 */
		SimpleQuantity qty = new SimpleQuantity();
		if (unitCount.compareTo(BigDecimal.ZERO) > 0) {
			qty.setValue(unitCount);
		} else if (nationalDrugCodeQuantity.isPresent()) {
			qty.setValue(nationalDrugCodeQuantity.get());
		} else {
			qty.setValue(0);
		}
		item.setQuantity(qty);

		if (nationalDrugCodeQualifierCode.isPresent()) {
			item.addModifier(TransformerUtils.createCodeableConcept(TransformerConstants.CODING_CCW_NDC_UNIT,
					nationalDrugCodeQualifierCode.get()));
		}

		if (revenueCenterRenderingPhysicianNPI.isPresent()) {
			TransformerUtils.addCareTeamPractitioner(eob, item, TransformerConstants.CODING_NPI_US,
					revenueCenterRenderingPhysicianNPI.get(), ClaimCareteamrole.PRIMARY.toCode());
		}

		return item;
	}

	/**
	 * Transforms the common group level data elements between the
	 * {@link InpatientClaim}, {@link OutpatientClaim} and {@link SNFClaim} claim
	 * types to FHIR. The method parameter fields from {@link InpatientClaim},
	 * {@link OutpatientClaim} and {@link SNFClaim} are listed below and their
	 * corresponding RIF CCW fields (denoted in all CAPS below from
	 * {@link InpatientClaimColumn} {@link OutpatientClaimColumn}and
	 * {@link SNFClaimColumn}).
	 * 
	 * @param eob
	 *            the {@link ExplanationOfBenefit} to modify
	 * @param beneficiaryId
	 *            BENE_ID, *
	 * @param carrierNumber
	 *            CARR_NUM,
	 * @param clinicalTrialNumber
	 *            CLM_CLNCL_TRIL_NUM,
	 * @param beneficiaryPartBDeductAmount
	 *            CARR_CLM_CASH_DDCTBL_APLD_AMT,
	 * @param paymentDenialCode
	 *            CARR_CLM_PMT_DNL_CD,
	 * @param referringPhysicianNpi
	 *            RFR_PHYSN_NPI
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
	static void mapEobCommonGroupInpOutSNF(ExplanationOfBenefit eob,
			BigDecimal bloodDeductibleLiabilityAmount, Optional<String> operatingPhysicianNpi,
			Optional<String> otherPhysicianNpi, char claimQueryCode, Optional<Character> mcoPaidSw) {

		BenefitComponent benefitInpatientNchPrimaryPayerAmt = new BenefitComponent(
				TransformerUtils.createCodeableConcept(TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
						TransformerConstants.CODED_BENEFIT_BALANCE_TYPE_BLOOD_DEDUCTIBLE_LIABILITY));
		benefitInpatientNchPrimaryPayerAmt.setAllowed(
				new Money().setSystem(TransformerConstants.CODED_MONEY_USD).setValue(bloodDeductibleLiabilityAmount));
		eob.getBenefitBalanceFirstRep().getFinancial().add((benefitInpatientNchPrimaryPayerAmt));
		
		if (operatingPhysicianNpi.isPresent()) {
			TransformerUtils.addCareTeamPractitioner(eob, null, TransformerConstants.CODING_NPI_US,
					operatingPhysicianNpi.get(), ClaimCareteamrole.ASSIST.toCode());
		}

		if (otherPhysicianNpi.isPresent()) {
			TransformerUtils.addCareTeamPractitioner(eob, null, TransformerConstants.CODING_NPI_US,
					otherPhysicianNpi.get(), ClaimCareteamrole.OTHER.toCode());
		}

		TransformerUtils.addExtensionCoding(eob.getBillablePeriod(), TransformerConstants.EXTENSION_CODING_CLAIM_QUERY,
				TransformerConstants.EXTENSION_CODING_CLAIM_QUERY, String.valueOf(claimQueryCode));

		if (mcoPaidSw.isPresent()) {
			TransformerUtils.addInformation(eob, TransformerUtils
					.createCodeableConcept(TransformerConstants.CODING_CCW_MCO_PAID, String.valueOf(mcoPaidSw.get())));
		}

	}

	/**
	 * Transforms the common group level data elements between the
	 * {@link InpatientClaimLine} {@link OutpatientClaimLine}
	 * {@link HospiceClaimLine} {@link HHAClaimLine}and {@link SNFClaimLine} claim
	 * types to FHIR. The method parameter fields from {@link InpatientClaimLine}
	 * {@link OutpatientClaimLine} {@link HospiceClaimLine} {@link HHAClaimLine}and
	 * {@link SNFClaimLine} are listed below and their corresponding RIF CCW fields
	 * (denoted in all CAPS below from {@link InpatientClaimColumn}
	 * {@link OutpatientClaimColumn} {@link HopsiceClaimColumn}
	 * {@link HHAClaimColumn} and {@link SNFClaimColumn}).
	 * 
	 * @param eob
	 *            the {@link ExplanationOfBenefit} to modify
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
	 *            NCH_PRMRY_PYR_CLM_PD_AMT
	 * 
	 */
	static void mapEobCommonGroupInpOutHHAHospiceSNF(ExplanationOfBenefit eob,
			Optional<String> organizationNpi, char claimFacilityTypeCode, char claimFrequencyCode,
			Optional<String> claimNonPaymentReasonCode, String patientDischargeStatusCode,
			char claimServiceClassificationTypeCode, Optional<Character> claimPrimaryPayerCode,
			Optional<String> attendingPhysicianNpi, BigDecimal totalChargeAmount, BigDecimal primaryPayerPaidAmount) {

		if (organizationNpi.isPresent()) {
			eob.setOrganization(TransformerUtils.createIdentifierReference(TransformerConstants.CODING_NPI_US,
					organizationNpi.get()));
			eob.setFacility(TransformerUtils.createIdentifierReference(TransformerConstants.CODING_NPI_US,
					organizationNpi.get()));
		}

		TransformerUtils.addExtensionCoding(eob.getFacility(),
				TransformerConstants.EXTENSION_CODING_CCW_FACILITY_TYPE,
				TransformerConstants.EXTENSION_CODING_CCW_FACILITY_TYPE, String.valueOf(claimFacilityTypeCode));
		
		TransformerUtils.addInformation(eob, TransformerUtils.createCodeableConcept(
					TransformerConstants.CODING_CCW_CLAIM_FREQUENCY, String.valueOf(claimFrequencyCode)));

		if (claimNonPaymentReasonCode.isPresent()) {
			TransformerUtils.addExtensionCoding(eob,
						TransformerConstants.EXTENSION_CODING_CCW_PAYMENT_DENIAL_REASON,
						TransformerConstants.EXTENSION_CODING_CCW_PAYMENT_DENIAL_REASON,
						claimNonPaymentReasonCode.get());
			}

		if (!patientDischargeStatusCode.isEmpty()) {
			TransformerUtils.addInformation(eob, TransformerUtils.createCodeableConcept(
						TransformerConstants.CODING_CCW_PATIENT_DISCHARGE_STATUS, patientDischargeStatusCode));
			}

		TransformerUtils.addExtensionCoding(eob.getType(),
				TransformerConstants.EXTENSION_CODING_CCW_CLAIM_SERVICE_CLASSIFICATION,
				TransformerConstants.EXTENSION_CODING_CCW_CLAIM_SERVICE_CLASSIFICATION,
				String.valueOf(claimServiceClassificationTypeCode));
		if (claimPrimaryPayerCode.isPresent()) {
			TransformerUtils.addInformation(eob, TransformerUtils.createCodeableConcept(
					TransformerConstants.EXTENSION_CODING_PRIMARY_PAYER, String.valueOf(claimPrimaryPayerCode.get())));
		}

		if (attendingPhysicianNpi.isPresent()) {
			TransformerUtils.addCareTeamPractitioner(eob, null, TransformerConstants.CODING_NPI_US,
					attendingPhysicianNpi.get(), ClaimCareteamrole.PRIMARY.toCode());
		}
		eob.setTotalCost(
				(Money) new Money().setSystem(TransformerConstants.CODED_MONEY_USD).setValue(totalChargeAmount));

		BenefitComponent benefitInpatientNchPrimaryPayerAmt = new BenefitComponent(
				TransformerUtils.createCodeableConcept(TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
						TransformerConstants.CODED_ADJUDICATION_PRIMARY_PAYER_PAID_AMOUNT));
		benefitInpatientNchPrimaryPayerAmt.setAllowed(new Money().setSystem(TransformerConstants.CODED_MONEY_USD)
				.setValue(primaryPayerPaidAmount));
		eob.getBenefitBalanceFirstRep().getFinancial().add((benefitInpatientNchPrimaryPayerAmt));

	}
	
	/**
	 * Transforms the common group level data elements between the
	 * {@link InpatientClaim} {@link HHAClaim} {@link HospiceClaim} and {@link SNFClaim} claim
	 * types to FHIR. The method parameter fields from {@link InpatientClaim}
	 * {@link HHAClaim} {@link HospiceClaim} and {@link SNFClaim} are listed below and their  
	 * corresponding RIF CCW fields (denoted in all CAPS below from {@link InpatientClaimColumn}
	 * {@link HHAClaimColumn} {@link HospiceColumn} and {@link SNFClaimColumn}).
	 * 
	 * @param eob
	 *            the {@link ExplanationOfBenefit} to modify
	 * 
	 * @param claimAdmissionDate
	 * 			CLM_ADMSN_DT,
	 * 
	 * @param benficiaryDischargeDate,
	 * 
	 * @param utilizedDays
	 * 			CLM_UTLZTN_CNT,
	 * 
	 * @param benefitBalances
	 * 
	 * @return the {@link ExplanationOfBenefit}
	 */
	
	static ExplanationOfBenefit mapEobCommonGroupInpHHAHospiceSNF(ExplanationOfBenefit eob,
			Optional<LocalDate> claimAdmissionDate, Optional<LocalDate> beneficiaryDischargeDate,
			Optional<BigDecimal> utilizedDays, BenefitBalanceComponent benefitBalances) {
		
		if (claimAdmissionDate.isPresent() || beneficiaryDischargeDate.isPresent()) {
			TransformerUtils.validatePeriodDates(claimAdmissionDate,
					beneficiaryDischargeDate);
			Period period = new Period();
			if (claimAdmissionDate.isPresent()) {
				period.setStart(TransformerUtils.convertToDate(claimAdmissionDate.get()),
						TemporalPrecisionEnum.DAY);
			}
			if (beneficiaryDischargeDate.isPresent()) {
				period.setEnd(TransformerUtils.convertToDate(beneficiaryDischargeDate.get()),
						TemporalPrecisionEnum.DAY);
			}
			eob.setHospitalization(period);
		}
		
		if (utilizedDays.isPresent()) {
			BenefitComponent utilizationDayCount = new BenefitComponent(
					TransformerUtils.createCodeableConcept(TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
							TransformerConstants.CODED_BENEFIT_BALANCE_TYPE_SYSTEM_UTILIZATION_DAY_COUNT));
			utilizationDayCount.setUsed(new UnsignedIntType(utilizedDays.get().intValue()));
			benefitBalances.getFinancial().add(utilizationDayCount);
		}
	
		return eob;
	}
	
	/**
	 * Transforms the common group level data elements between the
	 * {@link InpatientClaim} {@link HHAClaim} {@link HospiceClaim} and {@link SNFClaim} claim
	 * types to FHIR. The method parameter fields from {@link InpatientClaim}
	 * {@link HHAClaim} {@link HospiceClaim} and {@link SNFClaim} are listed below and their  
	 * corresponding RIF CCW fields (denoted in all CAPS below from {@link InpatientClaimColumn}
	 * {@link HHAClaimColumn} {@link HospiceColumn} and {@link SNFClaimColumn}).
	 * 
	 * @param item
	 *            the {@link ItemComponent} to modify
	 *            
	 * @param deductibleCoinsruanceCd
	 * 			REV_CNTR_DDCTBL_COINSRNC_CD
	 */
	
	static void mapEobCommonGroupInpHHAHospiceSNFCoinsurance(ItemComponent item,
			Optional<Character> deductibleCoinsuranceCd) {
		
		if (deductibleCoinsuranceCd.isPresent()) {
			TransformerUtils.addExtensionCoding(item.getRevenue(),
					TransformerConstants.EXTENSION_CODING_CCW_DEDUCTIBLE_COINSURANCE_CODE,
					TransformerConstants.EXTENSION_CODING_CCW_DEDUCTIBLE_COINSURANCE_CODE,
					String.valueOf(deductibleCoinsuranceCd.get()));
		}
		
	}
	

	/**
	 * Extract the Diagnosis values for codes 1-12
	 * 
	 * @param diagnosisPrincipalCode
	 * @param diagnosisPrincipalCodeVersion
	 * @param diagnosis1Code
	 *            through diagnosis12Code
	 * @param diagnosis1CodeVersion
	 *            through diagnosis12CodeVersion
	 * 
	 * @return the {@link Diagnosis}es that can be extracted from the specified
	 * 
	 */
	public static List<Diagnosis> extractDiagnoses1Thru12(Optional<String> diagnosisPrincipalCode,
			Optional<Character> diagnosisPrincipalCodeVersion, Optional<String> diagnosis1Code,
			Optional<Character> diagnosis1CodeVersion, Optional<String> diagnosis2Code,
			Optional<Character> diagnosis2CodeVersion, Optional<String> diagnosis3Code,
			Optional<Character> diagnosis3CodeVersion, Optional<String> diagnosis4Code,
			Optional<Character> diagnosis4CodeVersion, Optional<String> diagnosis5Code,
			Optional<Character> diagnosis5CodeVersion, Optional<String> diagnosis6Code,
			Optional<Character> diagnosis6CodeVersion, Optional<String> diagnosis7Code,
			Optional<Character> diagnosis7CodeVersion, Optional<String> diagnosis8Code,
			Optional<Character> diagnosis8CodeVersion, Optional<String> diagnosis9Code,
			Optional<Character> diagnosis9CodeVersion, Optional<String> diagnosis10Code,
			Optional<Character> diagnosis10CodeVersion, Optional<String> diagnosis11Code,
			Optional<Character> diagnosis11CodeVersion, Optional<String> diagnosis12Code,
			Optional<Character> diagnosis12CodeVersion) {
		List<Diagnosis> diagnoses = new LinkedList<>();

		/*
		 * Seems silly, but allows the block below to be simple one-liners,
		 * rather than requiring if-blocks.
		 */
		Consumer<Optional<Diagnosis>> diagnosisAdder = d -> {
			if (d.isPresent())
				diagnoses.add(d.get());
		};

		diagnosisAdder.accept(
				Diagnosis.from(diagnosisPrincipalCode, diagnosisPrincipalCodeVersion, DiagnosisLabel.PRINCIPAL));
		diagnosisAdder.accept(Diagnosis.from(diagnosis1Code, diagnosis1CodeVersion, DiagnosisLabel.PRINCIPAL));
		diagnosisAdder.accept(Diagnosis.from(diagnosis2Code, diagnosis2CodeVersion));
		diagnosisAdder.accept(Diagnosis.from(diagnosis3Code, diagnosis3CodeVersion));
		diagnosisAdder.accept(Diagnosis.from(diagnosis4Code, diagnosis4CodeVersion));
		diagnosisAdder.accept(Diagnosis.from(diagnosis5Code, diagnosis5CodeVersion));
		diagnosisAdder.accept(Diagnosis.from(diagnosis6Code, diagnosis6CodeVersion));
		diagnosisAdder.accept(Diagnosis.from(diagnosis7Code, diagnosis7CodeVersion));
		diagnosisAdder.accept(Diagnosis.from(diagnosis8Code, diagnosis8CodeVersion));
		diagnosisAdder.accept(Diagnosis.from(diagnosis9Code, diagnosis9CodeVersion));
		diagnosisAdder.accept(Diagnosis.from(diagnosis10Code, diagnosis10CodeVersion));
		diagnosisAdder.accept(Diagnosis.from(diagnosis11Code, diagnosis11CodeVersion));
		diagnosisAdder.accept(Diagnosis.from(diagnosis12Code, diagnosis12CodeVersion));

		return diagnoses;
	}

	/**
	 * Extract the Diagnosis values for codes 13-25
	 * 
	 * @param diagnosis13Code
	 *            through diagnosis25Code
	 * @param diagnosis13CodeVersion
	 *            through diagnosis25CodeVersion
	 * 
	 * @return the {@link Diagnosis}es that can be extracted from the specified
	 * 
	 */
	public static List<Diagnosis> extractDiagnoses13Thru25(Optional<String> diagnosis13Code,
			Optional<Character> diagnosis13CodeVersion, Optional<String> diagnosis14Code,
			Optional<Character> diagnosis14CodeVersion, Optional<String> diagnosis15Code,
			Optional<Character> diagnosis15CodeVersion, Optional<String> diagnosis16Code,
			Optional<Character> diagnosis16CodeVersion, Optional<String> diagnosis17Code,
			Optional<Character> diagnosis17CodeVersion, Optional<String> diagnosis18Code,
			Optional<Character> diagnosis18CodeVersion, Optional<String> diagnosis19Code,
			Optional<Character> diagnosis19CodeVersion, Optional<String> diagnosis20Code,
			Optional<Character> diagnosis20CodeVersion, Optional<String> diagnosis21Code,
			Optional<Character> diagnosis21CodeVersion, Optional<String> diagnosis22Code,
			Optional<Character> diagnosis22CodeVersion, Optional<String> diagnosis23Code,
			Optional<Character> diagnosis23CodeVersion, Optional<String> diagnosis24Code,
			Optional<Character> diagnosis24CodeVersion, Optional<String> diagnosis25Code,
			Optional<Character> diagnosis25CodeVersion) {
		List<Diagnosis> diagnoses = new LinkedList<>();

		/*
		 * Seems silly, but allows the block below to be simple one-liners, rather than
		 * requiring if-blocks.
		 */
		Consumer<Optional<Diagnosis>> diagnosisAdder = d -> {
			if (d.isPresent())
				diagnoses.add(d.get());
		};

		diagnosisAdder.accept(Diagnosis.from(diagnosis13Code, diagnosis13CodeVersion));
		diagnosisAdder.accept(Diagnosis.from(diagnosis14Code, diagnosis14CodeVersion));
		diagnosisAdder.accept(Diagnosis.from(diagnosis15Code, diagnosis15CodeVersion));
		diagnosisAdder.accept(Diagnosis.from(diagnosis16Code, diagnosis16CodeVersion));
		diagnosisAdder.accept(Diagnosis.from(diagnosis17Code, diagnosis17CodeVersion));
		diagnosisAdder.accept(Diagnosis.from(diagnosis18Code, diagnosis18CodeVersion));
		diagnosisAdder.accept(Diagnosis.from(diagnosis19Code, diagnosis19CodeVersion));
		diagnosisAdder.accept(Diagnosis.from(diagnosis20Code, diagnosis20CodeVersion));
		diagnosisAdder.accept(Diagnosis.from(diagnosis21Code, diagnosis21CodeVersion));
		diagnosisAdder.accept(Diagnosis.from(diagnosis22Code, diagnosis22CodeVersion));
		diagnosisAdder.accept(Diagnosis.from(diagnosis23Code, diagnosis23CodeVersion));
		diagnosisAdder.accept(Diagnosis.from(diagnosis24Code, diagnosis24CodeVersion));
		diagnosisAdder.accept(Diagnosis.from(diagnosis25Code, diagnosis25CodeVersion));

		return diagnoses;
	}

	/**
	 * Extract the External Diagnosis values for codes 1-12
	 * 
	 * @param diagnosisExternalFirstCode
	 * @param diagnosisExternalFirstCodeVersion
	 * @param diagnosisExternal1Code
	 *            through diagnosisExternal12Code
	 * @param diagnosisExternal1CodeVersion
	 *            through diagnosisExternal12CodeVersion
	 * 
	 * @return the {@link Diagnosis}es that can be extracted from the specified
	 * 
	 */
	public static List<Diagnosis> extractExternalDiagnoses1Thru12(Optional<String> diagnosisExternalFirstCode,
			Optional<Character> diagnosisExternalFirstCodeVersion, Optional<String> diagnosisExternal1Code,
			Optional<Character> diagnosisExternal1CodeVersion, Optional<String> diagnosisExternal2Code,
			Optional<Character> diagnosisExternal2CodeVersion, Optional<String> diagnosisExternal3Code,
			Optional<Character> diagnosisExternal3CodeVersion, Optional<String> diagnosisExternal4Code,
			Optional<Character> diagnosisExternal4CodeVersion, Optional<String> diagnosisExternal5Code,
			Optional<Character> diagnosisExternal5CodeVersion, Optional<String> diagnosisExternal6Code,
			Optional<Character> diagnosisExternal6CodeVersion, Optional<String> diagnosisExternal7Code,
			Optional<Character> diagnosisExternal7CodeVersion, Optional<String> diagnosisExternal8Code,
			Optional<Character> diagnosisExternal8CodeVersion, Optional<String> diagnosisExternal9Code,
			Optional<Character> diagnosisExternal9CodeVersion, Optional<String> diagnosisExternal10Code,
			Optional<Character> diagnosisExternal10CodeVersion, Optional<String> diagnosisExternal11Code,
			Optional<Character> diagnosisExternal11CodeVersion, Optional<String> diagnosisExternal12Code,
			Optional<Character> diagnosisExternal12CodeVersion) {
		List<Diagnosis> diagnoses = new LinkedList<>();

		/*
		 * Seems silly, but allows the block below to be simple one-liners, rather than
		 * requiring if-blocks.
		 */
		Consumer<Optional<Diagnosis>> diagnosisAdder = d -> {
			if (d.isPresent())
				diagnoses.add(d.get());
		};

		diagnosisAdder.accept(Diagnosis.from(diagnosisExternalFirstCode, diagnosisExternalFirstCodeVersion,
				DiagnosisLabel.FIRSTEXTERNAL));
		diagnosisAdder
				.accept(Diagnosis.from(diagnosisExternal1Code, diagnosisExternal1CodeVersion,
						DiagnosisLabel.FIRSTEXTERNAL));
		diagnosisAdder
				.accept(Diagnosis.from(diagnosisExternal2Code, diagnosisExternal2CodeVersion, DiagnosisLabel.EXTERNAL));
		diagnosisAdder
				.accept(Diagnosis.from(diagnosisExternal3Code, diagnosisExternal3CodeVersion, DiagnosisLabel.EXTERNAL));
		diagnosisAdder
				.accept(Diagnosis.from(diagnosisExternal4Code, diagnosisExternal4CodeVersion, DiagnosisLabel.EXTERNAL));
		diagnosisAdder
				.accept(Diagnosis.from(diagnosisExternal5Code, diagnosisExternal5CodeVersion, DiagnosisLabel.EXTERNAL));
		diagnosisAdder
				.accept(Diagnosis.from(diagnosisExternal6Code, diagnosisExternal6CodeVersion, DiagnosisLabel.EXTERNAL));
		diagnosisAdder
				.accept(Diagnosis.from(diagnosisExternal7Code, diagnosisExternal7CodeVersion, DiagnosisLabel.EXTERNAL));
		diagnosisAdder
				.accept(Diagnosis.from(diagnosisExternal8Code, diagnosisExternal8CodeVersion, DiagnosisLabel.EXTERNAL));
		diagnosisAdder
				.accept(Diagnosis.from(diagnosisExternal9Code, diagnosisExternal9CodeVersion, DiagnosisLabel.EXTERNAL));
		diagnosisAdder
				.accept(Diagnosis.from(diagnosisExternal10Code, diagnosisExternal10CodeVersion,
						DiagnosisLabel.EXTERNAL));
		diagnosisAdder
				.accept(Diagnosis.from(diagnosisExternal11Code, diagnosisExternal11CodeVersion,
						DiagnosisLabel.EXTERNAL));
		diagnosisAdder
				.accept(Diagnosis.from(diagnosisExternal12Code, diagnosisExternal12CodeVersion,
						DiagnosisLabel.EXTERNAL));

		return diagnoses;
	}

	/**
	 * Extract the Procedure values for codes 1-25
	 * 
	 * @param procedure1Code
	 *            through procedure25Code,
	 * @param procedure1CodeVersion
	 *            through procedure25CodeVersion
	 * @param procedure1Date
	 *            through procedure25Date
	 * 
	 * @return the {@link CCWProcedure}es that can be extracted from the specified
	 *         claim types
	 */
	public static List<CCWProcedure> extractCCWProcedures(Optional<String> procedure1Code,
			Optional<Character> procedure1CodeVersion, Optional<LocalDate> procedure1Date,
			Optional<String> procedure2Code, Optional<Character> procedure2CodeVersion,
			Optional<LocalDate> procedure2Date, Optional<String> procedure3Code,
			Optional<Character> procedure3CodeVersion, Optional<LocalDate> procedure3Date,
			Optional<String> procedure4Code, Optional<Character> procedure4CodeVersion,
			Optional<LocalDate> procedure4Date, Optional<String> procedure5Code,
			Optional<Character> procedure5CodeVersion, Optional<LocalDate> procedure5Date,
			Optional<String> procedure6Code, Optional<Character> procedure6CodeVersion,
			Optional<LocalDate> procedure6Date, Optional<String> procedure7Code,
			Optional<Character> procedure7CodeVersion, Optional<LocalDate> procedure7Date,
			Optional<String> procedure8Code, Optional<Character> procedure8CodeVersion,
			Optional<LocalDate> procedure8Date, Optional<String> procedure9Code,
			Optional<Character> procedure9CodeVersion, Optional<LocalDate> procedure9Date,
			Optional<String> procedure10Code, Optional<Character> procedure10CodeVersion,
			Optional<LocalDate> procedure10Date, Optional<String> procedure11Code,
			Optional<Character> procedure11CodeVersion, Optional<LocalDate> procedure11Date,
			Optional<String> procedure12Code, Optional<Character> procedure12CodeVersion,
			Optional<LocalDate> procedure12Date, Optional<String> procedure13Code,
			Optional<Character> procedure13CodeVersion, Optional<LocalDate> procedure13Date,
			Optional<String> procedure14Code, Optional<Character> procedure14CodeVersion,
			Optional<LocalDate> procedure14Date, Optional<String> procedure15Code,
			Optional<Character> procedure15CodeVersion, Optional<LocalDate> procedure15Date,
			Optional<String> procedure16Code, Optional<Character> procedure16CodeVersion,
			Optional<LocalDate> procedure16Date, Optional<String> procedure17Code,
			Optional<Character> procedure17CodeVersion, Optional<LocalDate> procedure17Date,
			Optional<String> procedure18Code, Optional<Character> procedure18CodeVersion,
			Optional<LocalDate> procedure18Date, Optional<String> procedure19Code,
			Optional<Character> procedure19CodeVersion, Optional<LocalDate> procedure19Date,
			Optional<String> procedure20Code, Optional<Character> procedure20CodeVersion,
			Optional<LocalDate> procedure20Date, Optional<String> procedure21Code,
			Optional<Character> procedure21CodeVersion, Optional<LocalDate> procedure21Date,
			Optional<String> procedure22Code, Optional<Character> procedure22CodeVersion,
			Optional<LocalDate> procedure22Date, Optional<String> procedure23Code,
			Optional<Character> procedure23CodeVersion, Optional<LocalDate> procedure23Date,
			Optional<String> procedure24Code, Optional<Character> procedure24CodeVersion,
			Optional<LocalDate> procedure24Date, Optional<String> procedure25Code,
			Optional<Character> procedure25CodeVersion, Optional<LocalDate> procedure25Date) {

		List<CCWProcedure> ccwProcedures = new LinkedList<>();

		/*
		 * Seems silly, but allows the block below to be simple one-liners, rather than
		 * requiring if-blocks.
		 */
		Consumer<Optional<CCWProcedure>> ccwProcedureAdder = p -> {
			if (p.isPresent())
				ccwProcedures.add(p.get());
		};

		ccwProcedureAdder.accept(CCWProcedure.from(procedure1Code, procedure1CodeVersion, procedure1Date));
		ccwProcedureAdder.accept(CCWProcedure.from(procedure2Code, procedure2CodeVersion, procedure2Date));
		ccwProcedureAdder.accept(CCWProcedure.from(procedure3Code, procedure3CodeVersion, procedure3Date));
		ccwProcedureAdder.accept(CCWProcedure.from(procedure4Code, procedure4CodeVersion, procedure4Date));
		ccwProcedureAdder.accept(CCWProcedure.from(procedure5Code, procedure5CodeVersion, procedure5Date));
		ccwProcedureAdder.accept(CCWProcedure.from(procedure6Code, procedure6CodeVersion, procedure6Date));
		ccwProcedureAdder.accept(CCWProcedure.from(procedure7Code, procedure7CodeVersion, procedure7Date));
		ccwProcedureAdder.accept(CCWProcedure.from(procedure8Code, procedure8CodeVersion, procedure8Date));
		ccwProcedureAdder.accept(CCWProcedure.from(procedure9Code, procedure9CodeVersion, procedure9Date));
		ccwProcedureAdder.accept(CCWProcedure.from(procedure10Code, procedure10CodeVersion, procedure10Date));
		ccwProcedureAdder.accept(CCWProcedure.from(procedure11Code, procedure11CodeVersion, procedure11Date));
		ccwProcedureAdder.accept(CCWProcedure.from(procedure12Code, procedure12CodeVersion, procedure12Date));
		ccwProcedureAdder.accept(CCWProcedure.from(procedure13Code, procedure13CodeVersion, procedure13Date));
		ccwProcedureAdder.accept(CCWProcedure.from(procedure14Code, procedure14CodeVersion, procedure14Date));
		ccwProcedureAdder.accept(CCWProcedure.from(procedure15Code, procedure15CodeVersion, procedure15Date));
		ccwProcedureAdder.accept(CCWProcedure.from(procedure16Code, procedure16CodeVersion, procedure16Date));
		ccwProcedureAdder.accept(CCWProcedure.from(procedure17Code, procedure17CodeVersion, procedure17Date));
		ccwProcedureAdder.accept(CCWProcedure.from(procedure18Code, procedure18CodeVersion, procedure18Date));
		ccwProcedureAdder.accept(CCWProcedure.from(procedure19Code, procedure19CodeVersion, procedure19Date));
		ccwProcedureAdder.accept(CCWProcedure.from(procedure20Code, procedure20CodeVersion, procedure20Date));
		ccwProcedureAdder.accept(CCWProcedure.from(procedure21Code, procedure21CodeVersion, procedure21Date));
		ccwProcedureAdder.accept(CCWProcedure.from(procedure22Code, procedure22CodeVersion, procedure22Date));
		ccwProcedureAdder.accept(CCWProcedure.from(procedure23Code, procedure23CodeVersion, procedure23Date));
		ccwProcedureAdder.accept(CCWProcedure.from(procedure24Code, procedure24CodeVersion, procedure24Date));
		ccwProcedureAdder.accept(CCWProcedure.from(procedure25Code, procedure25CodeVersion, procedure25Date));

		return ccwProcedures;
	}

	/**
	 * Sets the provider number field which is common among these claim types:
	 * Inpatient, Outpatient, Hospice, HHA and SNF.
	 * 
	 * @param eob
	 *            the {@link ExplanationOfBenefit} this method will modify
	 * @param providerNumber
	 *            a {@link String} PRVDR_NUM: representing the provider number for
	 *            the claim
	 */
	static void setProviderNumber(ExplanationOfBenefit eob, String providerNumber) {
		eob.setProvider(TransformerUtils.createIdentifierReference(TransformerConstants.IDENTIFIER_CMS_PROVIDER_NUMBER,
				providerNumber));
	}
	
	/**
	 * Sets the hcpcsCode field which is common among these claim types: Carrier,
	 * Inpatient, Outpatient, DME, Hospice, HHA and SNF. Sets the hcpcs related
	 * fields which are common among these claim types: Carrier, Outpatient, DME,
	 * Hospice and HHA
	 *
	 * @param item
	 *            the {@link ItemComponent} this method will modify
	 * @param hcpcsCode
	 *            the {@link Optional}&lt;{@link String}&gt; HCPCS_CD: representing
	 *            the hcpcs code for the claim
	 * @param hcpcsInitialModifierCode
	 *            the {@link Optional}&lt;{@link String}&gt; HCPCS_1ST_MDFR_CD:
	 *            representing the hcpcs initial modifier code for the claim
	 * @param hcpcsSecondModifierCode
	 *            the {@link Optional}&lt;{@link String}&gt; HCPCS_2ND_MDFR_CD:
	 *            representing the hcpcs second modifier code for the claim
	 * @param hcpcsYearCode
	 *            the {@link Optional}&lt;{@link Character}&gt;
	 *            CARR_CLM_HCPCS_YR_CD: representing the hcpcs year code for the
	 *            claim
	 */
	static void setHcpcsModifierCodes(ItemComponent item, Optional<String> hcpcsCode,
			Optional<String> hcpcsInitialModifierCode, Optional<String> hcpcsSecondModifierCode, Optional<Character> hcpcsYearCode) {
		if (hcpcsYearCode.isPresent()) { // some claim types have a year code...
			if (hcpcsInitialModifierCode.isPresent()) {
				item.addModifier(TransformerUtils.createCodeableConcept(TransformerConstants.CODING_HCPCS,
						"" + hcpcsYearCode.get(), hcpcsInitialModifierCode.get()));
			}
			if (hcpcsSecondModifierCode.isPresent()) {
				item.addModifier(TransformerUtils.createCodeableConcept(TransformerConstants.CODING_HCPCS,
						"" + hcpcsYearCode.get(), hcpcsSecondModifierCode.get()));
			}
			if (hcpcsCode.isPresent()) {
				item.setService(createCodeableConcept(TransformerConstants.CODING_HCPCS, "" + hcpcsYearCode.get(),
						hcpcsCode.get()));
			}
		}
		else { // while others do not...
			if (hcpcsInitialModifierCode.isPresent()) {
				item.addModifier(TransformerUtils.createCodeableConcept(TransformerConstants.CODING_HCPCS,
						hcpcsInitialModifierCode.get()));
			}
			if (hcpcsSecondModifierCode.isPresent()) {
				item.addModifier(TransformerUtils.createCodeableConcept(TransformerConstants.CODING_HCPCS,
						hcpcsSecondModifierCode.get()));
			}
			if (hcpcsCode.isPresent()) {
				item.setService(createCodeableConcept(TransformerConstants.CODING_HCPCS, hcpcsCode.get()));
			}
		}
	}
}

