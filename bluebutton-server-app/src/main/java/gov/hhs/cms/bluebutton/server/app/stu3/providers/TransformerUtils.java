package gov.hhs.cms.bluebutton.server.app.stu3.providers;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.Optional;

import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Coverage;
import org.hl7.fhir.dstu3.model.DomainResource;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.CareTeamComponent;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.DiagnosisComponent;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.ItemComponent;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Period;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.TemporalPrecisionEnum;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IBaseHasExtensions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.model.primitive.IdDt;
import gov.hhs.cms.bluebutton.data.model.rif.Beneficiary;
import gov.hhs.cms.bluebutton.data.model.rif.CarrierClaim;

/**
 * Contains shared methods used to transform CCW JPA entities (e.g.
 * {@link Beneficiary}) into FHIR resources (e.g. {@link Patient}).
 */
final class TransformerUtils {
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
					createCodeableConcept(TransformerConstants.CODING_SYSTEM_CARE_TEAM_ROLE, practitionerRole));
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
			diagnosisComponent.addType(createCodeableConcept(TransformerConstants.CODING_SYSTEM_CCW_INP_POA_CD,
					diagnosis.getPresentOnAdmission().get()));
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
	static String buildEobId(ClaimType claimType, String claimId) {
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
		return new IdDt("Patient", beneficiary.getBeneficiaryId());
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
	private static Reference createIdentifierReference(String identifierSystem, String identifierValue) {
		return new Reference().setIdentifier(new Identifier().setSystem(identifierSystem).setValue(identifierValue));
	}

	/**
	 * @param subPlan
	 *            the {@link Coverage#getSubPlan()} value to match
	 * @param beneficiaryPatientId
	 *            the {@link #TransformerConstants.CODING_SYSTEM_CCW_BENE_ID} ID
	 *            value for the {@link Coverage#getBeneficiary()} value to match
	 * @return a {@link Reference} to the {@link Coverage} resource where
	 *         {@link Coverage#getPlan()} matches {@link #COVERAGE_PLAN} and the
	 *         other parameters specified also match
	 */
	static Reference referenceCoverage(String beneficiaryPatientId, String subPlan) {
		return new Reference(String.format("Coverage?beneficiary.identifier=%s|%s&subplan=%s",
				TransformerConstants.CODING_SYSTEM_CCW_BENE_ID, beneficiaryPatientId, subPlan));
	}

	/**
	 * @param patientId
	 *            the {@link #TransformerConstants.CODING_SYSTEM_CCW_BENE_ID} ID
	 *            value for the beneficiary to match
	 * @return a {@link Reference} to the {@link Patient} resource that matches
	 *         the specified parameters
	 */
	static Reference referencePatient(String patientId) {
		return new Reference(
				String.format("Patient?identifier=%s|%s", TransformerConstants.CODING_SYSTEM_CCW_BENE_ID, patientId));
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
		return createIdentifierReference(TransformerConstants.CODING_SYSTEM_NPI_US, practitionerNpi);
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
}
