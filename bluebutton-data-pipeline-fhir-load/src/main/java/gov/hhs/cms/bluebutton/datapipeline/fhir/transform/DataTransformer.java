package gov.hhs.cms.bluebutton.datapipeline.fhir.transform;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hl7.fhir.dstu21.model.Address;
import org.hl7.fhir.dstu21.model.Claim;
import org.hl7.fhir.dstu21.model.CodeableConcept;
import org.hl7.fhir.dstu21.model.Coding;
import org.hl7.fhir.dstu21.model.Coverage;
import org.hl7.fhir.dstu21.model.DateType;
import org.hl7.fhir.dstu21.model.Duration;
import org.hl7.fhir.dstu21.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu21.model.ExplanationOfBenefit.DiagnosisComponent;
import org.hl7.fhir.dstu21.model.ExplanationOfBenefit.ItemsComponent;
import org.hl7.fhir.dstu21.model.IdType;
import org.hl7.fhir.dstu21.model.MedicationOrder;
import org.hl7.fhir.dstu21.model.MedicationOrder.MedicationOrderDispenseRequestComponent;
import org.hl7.fhir.dstu21.model.Money;
import org.hl7.fhir.dstu21.model.Organization;
import org.hl7.fhir.dstu21.model.Patient;
import org.hl7.fhir.dstu21.model.Period;
import org.hl7.fhir.dstu21.model.Practitioner;
import org.hl7.fhir.dstu21.model.Reference;
import org.hl7.fhir.dstu21.model.SimpleQuantity;
import org.hl7.fhir.dstu21.model.valuesets.Adjudication;
import org.hl7.fhir.instance.model.api.IBaseResource;

import gov.hhs.cms.bluebutton.datapipeline.ccw.jdo.CurrentBeneficiary;
import gov.hhs.cms.bluebutton.datapipeline.ccw.jdo.PartAClaimFact;
import gov.hhs.cms.bluebutton.datapipeline.ccw.jdo.PartBClaimFact;
import gov.hhs.cms.bluebutton.datapipeline.ccw.jdo.PartBClaimLineFact;
import gov.hhs.cms.bluebutton.datapipeline.ccw.jdo.PartDEventFact;

/**
 * Handles the translation from source/CCW {@link CurrentBeneficiary} data into
 * FHIR {@link BeneficiaryBundle}s.
 */
public final class DataTransformer {
	/**
	 * The {@link Coverage#getPlan()} value for Part A.
	 */
	static final String COVERAGE_PLAN_PART_A = "Part A";

	/**
	 * The {@link Coverage#getPlan()} value for Part B.
	 */
	static final String COVERAGE_PLAN_PART_B = "Part B";

	/**
	 * The {@link Coverage#getPlan()} value for Part D.
	 */
	static final String COVERAGE_PLAN_PART_D = "Part D";

	/**
	 * A CMS-controlled standard. More info here: <a href=
	 * "https://en.wikipedia.org/wiki/Healthcare_Common_Procedure_Coding_System">
	 * Healthcare Common Procedure Coding System</a>.
	 */
	static final String CODING_SYSTEM_HCPCS = "HCPCS";

	static final String CODING_SYSTEM_ICD9_DIAG = "http://hl7.org/fhir/sid/icd-9-cm/diagnosis";

	static final String CODING_SYSTEM_ICD9_PROC = "http://hl7.org/fhir/sid/icd-9-cm/procedure";

	/**
	 * The United States National Provider Identifier, as available at
	 * <a href="http://download.cms.gov/nppes/NPI_Files.html">NPI/NPPES File</a>
	 * .
	 */
	static final String CODING_SYSTEM_NPI_US = "http://hl7.org/fhir/sid/us-npi";

	static final String CODING_SYSTEM_ADJUDICATION_FHIR = "http://hl7.org/fhir/adjudication";

	/**
	 * The CMS-custom {@link Coding#getSystem()} value for Medicare
	 * {@link Adjudication}s.
	 */
	static final String CODING_SYSTEM_ADJUDICATION_CMS = "CMS Adjudications";

	static final String CODED_ADJUDICATION_ALLOWED_CHARGE = "Line Allowed Charge Amount";

	static final String CODED_ADJUDICATION_DEDUCTIBLE = "Line Beneficiary Part B Deductible Amount";

	static final String CODED_ADJUDICATION_BENEFICIARY_PRIMARY_PAYER_PAID = "Line Beneficiary Primary Payer Paid Amount";

	static final String CODED_ADJUDICATION_LINE_COINSURANCE_AMOUNT = "Line Coinsurance Amount";

	static final String CODED_ADJUDICATION_PAYMENT = "Line NCH Payment Amount";

	static final String CODED_ADJUDICATION_PATIENT_PAY = "Patient Pay Amount";

	static final String CODED_ADJUDICATION_TOTAL_COST = "Total Prescription Cost";

	static final String CODING_SYSTEM_MONEY = "urn:std:iso:4217";

	static final String CODING_SYSTEM_MONEY_US = "USD";

	/**
	 * @param sourceBeneficiaries
	 *            the source/CCW {@link CurrentBeneficiary} records to be
	 *            transformed
	 * @return a {@link Stream} of FHIR {@link BeneficiaryBundle}s
	 */
	public Stream<BeneficiaryBundle> transformSourceData(Stream<CurrentBeneficiary> sourceBeneficiaries) {
		Stream<BeneficiaryBundle> transformedRecords = sourceBeneficiaries.map(b -> convertToFhir(b));
		return transformedRecords;
	}

	/**
	 * @param sourceBeneficiary
	 *            a source {@link CurrentBeneficiary} record, along with its
	 *            associated claims data
	 * @return a {@link BeneficiaryBundle} that represents the specified
	 *         beneficiary and its associated claims data
	 */
	static BeneficiaryBundle convertToFhir(CurrentBeneficiary sourceBeneficiary) {
		List<IBaseResource> resources = new ArrayList<>();

		Patient patient = new Patient();
		resources.add(patient);
		patient.setId(IdType.newRandomUuid());
		patient.addIdentifier().setSystem("CCW_BENE_CRNT_VW.BENE_ID").setValue("" + sourceBeneficiary.getId());
		patient.setBirthDate(Date.valueOf(sourceBeneficiary.getBirthDate()));
		patient.addName().addFamily(sourceBeneficiary.getSurname()).addGiven(sourceBeneficiary.getGivenName());

		List<String> addressComponents = Arrays.asList(sourceBeneficiary.getContactAddress(),
				sourceBeneficiary.getContactAddressZip());
		addressComponents = addressComponents.stream().filter(c -> (c != null && c.trim().length() > 0))
				.collect(Collectors.toList());
		if (!addressComponents.isEmpty()) {
			Address address = patient.addAddress();
			for (String addressComponent : addressComponents)
				address.addLine(addressComponent);
		}

		Organization cms = new Organization();
		resources.add(cms);
		cms.setId(IdType.newRandomUuid());
		cms.setName("CMS");

		Coverage partACoverage = new Coverage();
		resources.add(partACoverage);
		partACoverage.setId(IdType.newRandomUuid());
		partACoverage.setIssuer(new Reference(cms.getId()));
		partACoverage.setPlan(COVERAGE_PLAN_PART_A);

		for (PartAClaimFact sourceClaim : sourceBeneficiary.getPartAClaimFacts()) {
			ExplanationOfBenefit eob = new ExplanationOfBenefit();
			resources.add(eob);
			eob.setId(IdType.newRandomUuid());
			eob.getCoverage().setCoverage(new Reference(partACoverage.getId()));
			eob.setPatient(new Reference().setReference(patient.getId()));
			eob.addIdentifier().setValue("" + sourceClaim.getId());

			Claim claim = new Claim();
			resources.add(claim);
			claim.setId(IdType.newRandomUuid());
			eob.setClaim(new Reference().setReference(claim.getId()));
			claim.addIdentifier().setValue("" + sourceClaim.getId());
			if (sourceClaim.getAdmittingDiagnosisCode() != null)
				claim.addDiagnosis().getDiagnosis().setSystem(CODING_SYSTEM_ICD9_DIAG)
						.setCode(sourceClaim.getAdmittingDiagnosisCode());
		}

		Coverage partBCoverage = new Coverage();
		resources.add(partBCoverage);
		partBCoverage.setId(IdType.newRandomUuid());
		partBCoverage.setIssuer(new Reference(cms.getId()));
		partBCoverage.setPlan(COVERAGE_PLAN_PART_B);

		for (PartBClaimFact sourceClaim : sourceBeneficiary.getPartBClaimFacts()) {
			ExplanationOfBenefit eob = new ExplanationOfBenefit();
			resources.add(eob);
			eob.setId(IdType.newRandomUuid());
			eob.getCoverage().setCoverage(new Reference(partBCoverage.getId()));
			eob.setPatient(new Reference().setReference(patient.getId()));
			eob.addIdentifier().setValue("" + sourceClaim.getCarrierControlNumber());

			if (!isBlank(sourceClaim.getDiagnosisCode1()))
				eob.addDiagnosis().getDiagnosis().setSystem(CODING_SYSTEM_ICD9_DIAG)
						.setCode(sourceClaim.getDiagnosisCode1());
			if (!isBlank(sourceClaim.getDiagnosisCode2()))
				eob.addDiagnosis().getDiagnosis().setSystem(CODING_SYSTEM_ICD9_DIAG)
						.setCode(sourceClaim.getDiagnosisCode2());
			if (!isBlank(sourceClaim.getDiagnosisCode3()))
				eob.addDiagnosis().getDiagnosis().setSystem(CODING_SYSTEM_ICD9_DIAG)
						.setCode(sourceClaim.getDiagnosisCode3());
			if (!isBlank(sourceClaim.getDiagnosisCode4()))
				eob.addDiagnosis().getDiagnosis().setSystem(CODING_SYSTEM_ICD9_DIAG)
						.setCode(sourceClaim.getDiagnosisCode4());
			if (!isBlank(sourceClaim.getDiagnosisCode5()))
				eob.addDiagnosis().getDiagnosis().setSystem(CODING_SYSTEM_ICD9_DIAG)
						.setCode(sourceClaim.getDiagnosisCode5());
			if (!isBlank(sourceClaim.getDiagnosisCode6()))
				eob.addDiagnosis().getDiagnosis().setSystem(CODING_SYSTEM_ICD9_DIAG)
						.setCode(sourceClaim.getDiagnosisCode6());
			if (!isBlank(sourceClaim.getDiagnosisCode7()))
				eob.addDiagnosis().getDiagnosis().setSystem(CODING_SYSTEM_ICD9_DIAG)
						.setCode(sourceClaim.getDiagnosisCode7());
			if (!isBlank(sourceClaim.getDiagnosisCode8()))
				eob.addDiagnosis().getDiagnosis().setSystem(CODING_SYSTEM_ICD9_DIAG)
						.setCode(sourceClaim.getDiagnosisCode8());

			if (sourceClaim.getProviderNpi() != null) {
				Practitioner providingPhysician = new Practitioner();
				resources.add(providingPhysician);
				providingPhysician.setId(IdType.newRandomUuid());
				providingPhysician.addIdentifier().setSystem(CODING_SYSTEM_NPI_US)
						.setValue(sourceClaim.getProviderNpi().toString());
				eob.setProvider(new Reference().setReference(providingPhysician.getId()));
			}

			for (PartBClaimLineFact sourceClaimLine : sourceClaim.getClaimLines()) {
				ItemsComponent item = eob.addItem();
				item.setSequence((int) sourceClaimLine.getLineNumber());

				if (sourceClaimLine.getProcedure() != null)
					item.setService(new Coding().setSystem(CODING_SYSTEM_HCPCS)
							.setCode(sourceClaimLine.getProcedure().getCode()));

				if (sourceClaimLine.getDateFrom() != null || sourceClaimLine.getDateThrough() != null) {
					item.setServiced(new Period().setStart(Date.valueOf(sourceClaimLine.getDateFrom()))
							.setEnd(Date.valueOf(sourceClaimLine.getDateThrough())));
				}

				if (sourceClaimLine.getAllowedAmount() != null)
					item.addAdjudication()
							.setCategory(new Coding().setSystem(CODING_SYSTEM_ADJUDICATION_CMS)
									.setCode(CODED_ADJUDICATION_ALLOWED_CHARGE))
							.getAmount().setSystem(CODING_SYSTEM_MONEY).setCode(CODING_SYSTEM_MONEY_US)
							.setValue(sourceClaimLine.getAllowedAmount());
				if (sourceClaimLine.getDeductibleAmount() != null)
					item.addAdjudication()
							.setCategory(new Coding().setSystem(CODING_SYSTEM_ADJUDICATION_CMS)
									.setCode(CODED_ADJUDICATION_DEDUCTIBLE))
							.getAmount().setSystem(CODING_SYSTEM_MONEY).setCode(CODING_SYSTEM_MONEY_US)
							.setValue(sourceClaimLine.getDeductibleAmount());
				if (sourceClaimLine.getBeneficiaryPrimaryPayerPaidAmount() != null)
					item.addAdjudication()
							.setCategory(new Coding().setSystem(CODING_SYSTEM_ADJUDICATION_CMS)
									.setCode(CODED_ADJUDICATION_BENEFICIARY_PRIMARY_PAYER_PAID))
							.getAmount().setSystem(CODING_SYSTEM_MONEY).setCode(CODING_SYSTEM_MONEY_US)
							.setValue(sourceClaimLine.getBeneficiaryPrimaryPayerPaidAmount());
				if (sourceClaimLine.getCoinsuranceAmount() != null)
					item.addAdjudication()
							.setCategory(new Coding().setSystem(CODING_SYSTEM_ADJUDICATION_CMS)
									.setCode(CODED_ADJUDICATION_LINE_COINSURANCE_AMOUNT))
							.getAmount().setSystem(CODING_SYSTEM_MONEY).setCode(CODING_SYSTEM_MONEY_US)
							.setValue(sourceClaimLine.getCoinsuranceAmount());
				if (sourceClaimLine.getNchPaymentAmount() != null)
					item.addAdjudication()
							.setCategory(new Coding().setSystem(CODING_SYSTEM_ADJUDICATION_CMS)
									.setCode(CODED_ADJUDICATION_PAYMENT))
							.getAmount().setSystem(CODING_SYSTEM_MONEY).setCode(CODING_SYSTEM_MONEY_US)
							.setValue(sourceClaimLine.getNchPaymentAmount());

				if (!isBlank(sourceClaimLine.getLineDiagnosisCode())) {
					addDiagnosisCode(eob.getDiagnosis(), sourceClaimLine.getLineDiagnosisCode());
					int diagnosisCodeIndex = getDiagnosisCodeIndex(eob.getDiagnosis(),
							sourceClaimLine.getLineDiagnosisCode());
					item.addDiagnosisLinkId(diagnosisCodeIndex);
				}

				// TODO map source MiscCd

				/*
				 * TODO: where to stick LINE_PRCSG_IND_CD?
				 * "The code on a noninstitutional claim indicating to whom payment was made or if the claim was denied."
				 */
			}
		}

		Coverage partDCoverage = new Coverage();
		resources.add(partDCoverage);
		partDCoverage.setId(IdType.newRandomUuid());
		partDCoverage.setIssuer(new Reference(cms.getId()));
		partDCoverage.setPlan(COVERAGE_PLAN_PART_D);

		for (PartDEventFact sourceEvent : sourceBeneficiary.getPartDEventFacts()) {
			ExplanationOfBenefit eob = new ExplanationOfBenefit();
			resources.add(eob);
			eob.setId(IdType.newRandomUuid());
			eob.getCoverage().setCoverage(new Reference(partDCoverage.getId()));
			eob.setPatient(new Reference().setReference(patient.getId()));
			eob.addIdentifier().setSystem("CCW_PDE_FACT.PDE_ID").setValue("" + sourceEvent.getId());

			MedicationOrder prescription = new MedicationOrder();
			resources.add(prescription);
			prescription.setId(IdType.newRandomUuid());
			eob.setPrescription(new Reference().setReference(prescription.getId()));

			if (sourceEvent.getPrescriberNpi() != null) {
				Practitioner prescriber = new Practitioner();
				resources.add(prescriber);
				prescriber.setId(IdType.newRandomUuid());
				prescriber.addIdentifier().setSystem(CODING_SYSTEM_NPI_US)
						.setValue(sourceEvent.getPrescriberNpi().toString());

				prescription.setPrescriber(new Reference().setReference(prescriber.getId()));
			}

			if (sourceEvent.getServiceProviderNpi() != null) {
				Practitioner servicingProvider = new Practitioner();
				resources.add(servicingProvider);
				servicingProvider.setId(IdType.newRandomUuid());
				servicingProvider.addIdentifier().setSystem(CODING_SYSTEM_NPI_US)
						.setValue(sourceEvent.getServiceProviderNpi().toString());
				eob.setProvider(new Reference().setReference(servicingProvider.getId()));
			}

			if (sourceEvent.getProductNdc() != null) {
				// TODO I think the system here is incorrect
				CodeableConcept medicationCoding = new CodeableConcept();
				medicationCoding.addCoding().setSystem("https://www.accessdata.fda.gov/scripts/cder/ndc/")
						.setCode("" + sourceEvent.getProductNdc());
				prescription.setMedication(medicationCoding);
			}

			ItemsComponent eventItem = eob.addItem();
			eventItem.setSequence(1);
			eventItem.setType(new Coding().setSystem("http://hl7.org/fhir/v3/ActCode").setCode("RXDINV"));
			eventItem.setServiced(new DateType().setValue(Date.valueOf(sourceEvent.getServiceDate())));

			SimpleQuantity quantity = new SimpleQuantity();
			quantity.setValue(sourceEvent.getQuantityDispensed());
			Duration numberDaysSupply = new Duration();
			numberDaysSupply.setUnit("days");
			numberDaysSupply.setValue(sourceEvent.getNumberDaysSupply());
			prescription.setDispenseRequest(new MedicationOrderDispenseRequestComponent().setQuantity(quantity)
					.setExpectedSupplyDuration(numberDaysSupply));

			Money patientPayment = (Money) new Money().setSystem(CODING_SYSTEM_MONEY_US)
					.setValue(sourceEvent.getPatientPayAmount());
			eventItem.addAdjudication().setCategory(
					new Coding().setSystem(CODING_SYSTEM_ADJUDICATION_CMS).setCode(CODED_ADJUDICATION_PATIENT_PAY))
					.setAmount(patientPayment);

			Money totalPrescriptionCost = (Money) new Money().setSystem(CODING_SYSTEM_MONEY_US)
					.setValue(sourceEvent.getTotalPrescriptionCost());
			eventItem.addAdjudication().setCategory(
					new Coding().setSystem(CODING_SYSTEM_ADJUDICATION_CMS).setCode(CODED_ADJUDICATION_TOTAL_COST))
					.setAmount(totalPrescriptionCost);
		}

		return new BeneficiaryBundle(resources);
	}

	/**
	 * @param diagnoses
	 *            the {@link DiagnosisComponent}s to add this code to, if it
	 *            isn't already present
	 * @param diagnosisCode
	 *            the diagnosis code to add, if it isn't already present
	 */
	private static void addDiagnosisCode(List<DiagnosisComponent> diagnoses, String diagnosisCode) {
		for (DiagnosisComponent diagnosis : diagnoses) {
			if (diagnosisCode.equals(diagnosis.getDiagnosis().getCode()))
				return;
		}

		DiagnosisComponent diagnosisComponent = new DiagnosisComponent().setSequence(diagnoses.size());
		diagnosisComponent.getDiagnosis().setSystem(CODING_SYSTEM_ICD9_DIAG).setCode(diagnosisCode);
		diagnoses.add(diagnosisComponent);
	}

	/**
	 * @param diagnoses
	 *            the {@link DiagnosisComponent}s to search
	 * @param diagnosisCode
	 *            the diagnosis code to find a match for
	 * @return the index of the specified diagnosis code in the specified list
	 */
	private static int getDiagnosisCodeIndex(List<DiagnosisComponent> diagnoses, String diagnosisCode) {
		for (DiagnosisComponent diagnosis : diagnoses) {
			if (diagnosisCode.equals(diagnosis.getDiagnosis().getCode()))
				return diagnosis.getSequence();
		}

		throw new IllegalArgumentException();
	}

	/**
	 * @param value
	 *            the value to check
	 * @return <code>true</code> if the specified value is <code>null</code> or
	 *         only contains whitespace
	 */
	private static boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}
}
