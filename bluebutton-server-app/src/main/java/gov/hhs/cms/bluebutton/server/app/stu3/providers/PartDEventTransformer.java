package gov.hhs.cms.bluebutton.server.app.stu3.providers;

import java.math.BigDecimal;
import java.util.Optional;

import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.DateType;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.ItemComponent;
import org.hl7.fhir.dstu3.model.SimpleQuantity;
import org.hl7.fhir.dstu3.model.codesystems.ClaimCareteamrole;
import org.hl7.fhir.dstu3.model.codesystems.V3ActCode;

import com.justdavis.karl.misc.exceptions.BadCodeMonkeyException;

import gov.hhs.cms.bluebutton.data.codebook.data.CcwCodebookVariable;
import gov.hhs.cms.bluebutton.data.model.rif.PartDEvent;
import gov.hhs.cms.bluebutton.data.model.rif.parse.InvalidRifValueException;

/**
 * Transforms CCW {@link PartDEvent} instances into FHIR
 * {@link ExplanationOfBenefit} resources.
 */
final class PartDEventTransformer {
	/**
	 * @param claim
	 *            the CCW {@link PartDEvent} to transform
	 * @return a FHIR {@link ExplanationOfBenefit} resource that represents the
	 *         specified {@link PartDEvent}
	 */
	static ExplanationOfBenefit transform(Object claim) {
		if (!(claim instanceof PartDEvent))
			throw new BadCodeMonkeyException();
		return transformClaim((PartDEvent) claim);
	}

	/**
	 * @param claimGroup
	 *            the CCW {@link PartDEvent} to transform
	 * @return a FHIR {@link ExplanationOfBenefit} resource that represents the
	 *         specified {@link PartDEvent}
	 */
	private static ExplanationOfBenefit transformClaim(PartDEvent claimGroup) {
		ExplanationOfBenefit eob = new ExplanationOfBenefit();

		// Common group level fields between all claim types
		TransformerUtils.mapEobCommonClaimHeaderData(eob, claimGroup.getEventId(), claimGroup.getBeneficiaryId(),
				ClaimType.PDE, claimGroup.getClaimGroupId().toPlainString(), MedicareSegment.PART_D, Optional.empty(),
				Optional.empty(), Optional.empty(), claimGroup.getFinalAction());

		eob.addIdentifier().setSystem(TransformerConstants.IDENTIFIER_RX_SERVICE_REFERENCE_NUMBER)
				.setValue(String.valueOf(claimGroup.getPrescriptionReferenceNumber()));

		// map eob type codes into FHIR
		TransformerUtils.mapEobType(eob, ClaimType.PDE, Optional.empty(), Optional.empty());
		
		eob.getInsurance().getCoverage().addExtension(TransformerUtils
				.createExtensionIdentifier(CcwCodebookVariable.PLAN_CNTRCT_REC_ID, claimGroup.getPlanContractId()));

		eob.getInsurance().getCoverage().addExtension(TransformerUtils
				.createExtensionIdentifier(CcwCodebookVariable.PLAN_PBP_REC_NUM, claimGroup.getPlanBenefitPackageId()));

		if (claimGroup.getPaymentDate().isPresent()) {
			eob.getPayment().setDate(TransformerUtils.convertToDate(claimGroup.getPaymentDate().get()));
		}

		ItemComponent rxItem = eob.addItem();
		rxItem.setSequence(1);

		ExplanationOfBenefit.DetailComponent detail = new ExplanationOfBenefit.DetailComponent();
		switch (claimGroup.getCompoundCode()) {
		// COMPOUNDED
		case 2:
			/* Pharmacy dispense invoice for a compound */
			detail.getType().addCoding(new Coding().setSystem(V3ActCode.RXCINV.getSystem())
					.setCode(V3ActCode.RXCINV.toCode()).setDisplay(V3ActCode.RXCINV.getDisplay()));
			break;
		// NOT_COMPOUNDED
		case 1:
			/*
			 * Pharmacy dispense invoice not involving a compound
			 */
			detail.getType().addCoding(new Coding().setSystem(V3ActCode.RXCINV.getSystem())
					.setCode(V3ActCode.RXDINV.toCode()).setDisplay(V3ActCode.RXDINV.getDisplay()));
			break;
		// NOT_SPECIFIED
		case 0:
			/*
			 * Pharmacy dispense invoice not specified - do not set a value
			 */
			break;
		default:
			/*
			 * Unexpected value encountered - compound code should be either
			 * compounded or not compounded.
			 */
			throw new InvalidRifValueException(
					"Unexpected value encountered - compound code should be either compounded or not compounded: "
							+ claimGroup.getCompoundCode());
		}

		rxItem.addDetail(detail);

		rxItem.setServiced(
				new DateType().setValue(TransformerUtils.convertToDate(claimGroup.getPrescriptionFillDate())));

		Coding rxCategoryCoding;
		BigDecimal rxPaidAmountValue;
		switch (claimGroup.getDrugCoverageStatusCode()) {
		/*
		 * If covered by Part D, use value from partDPlanCoveredPaidAmount
		 */
		// COVERED
		case 'C':
			rxCategoryCoding = new Coding().setSystem(TransformerConstants.CODING_CCW_ADJUDICATION_CATEGORY)
					.setCode(TransformerConstants.CODED_ADJUDICATION_PART_D_COVERED);
			rxPaidAmountValue = claimGroup.getPartDPlanCoveredPaidAmount();
			break;
		/*
		 * If not covered by Part D, use value from
		 * partDPlanNonCoveredPaidAmount. There are 2 categories of non-covered
		 * payment amounts: supplemental drugs covered by enhanced plans, and
		 * over the counter drugs that are covered only under specific
		 * circumstances.
		 */
		// SUPPLEMENTAL
		case 'E':
			rxCategoryCoding = new Coding().setSystem(TransformerConstants.CODING_CCW_ADJUDICATION_CATEGORY)
					.setCode(TransformerConstants.CODED_ADJUDICATION_PART_D_NONCOVERED_SUPPLEMENT);
			rxPaidAmountValue = claimGroup.getPartDPlanNonCoveredPaidAmount();
			break;
		// OVER THE COUNTER
		case '0':
			rxCategoryCoding = new Coding().setSystem(TransformerConstants.CODING_CCW_ADJUDICATION_CATEGORY)
					.setCode(TransformerConstants.CODED_ADJUDICATION_PART_D_NONCOVERED_OTC);
			rxPaidAmountValue = claimGroup.getPartDPlanNonCoveredPaidAmount();
			break;
		default:
			/*
			 * Unexpected value encountered - drug coverage status code should
			 * be one of the three above.
			 */
			rxCategoryCoding = new Coding().setSystem(TransformerConstants.CODING_CCW_ADJUDICATION_CATEGORY)
					.setCode("Unknown:" + claimGroup.getDrugCoverageStatusCode());
			rxPaidAmountValue = claimGroup.getPartDPlanCoveredPaidAmount();
		}
		CodeableConcept rxCategory = new CodeableConcept();
		rxCategory.addCoding(rxCategoryCoding);
		rxItem.addAdjudication().setCategory(rxCategory).getAmount().setSystem(TransformerConstants.CODING_MONEY)
				.setCode(TransformerConstants.CODED_MONEY_USD).setValue(rxPaidAmountValue);

		rxItem.addAdjudication()
				.setCategory(TransformerUtils.createCodeableConcept(TransformerConstants.CODING_CCW_ADJUDICATION_CATEGORY,
						TransformerConstants.CODED_ADJUDICATION_GDCB))
				.getAmount().setSystem(TransformerConstants.CODING_MONEY)
				.setCode(TransformerConstants.CODED_MONEY_USD)
				.setValue(claimGroup.getGrossCostBelowOutOfPocketThreshold());

		rxItem.addAdjudication()
				.setCategory(TransformerUtils.createCodeableConcept(TransformerConstants.CODING_CCW_ADJUDICATION_CATEGORY,
						TransformerConstants.CODED_ADJUDICATION_GDCA))
				.getAmount().setSystem(TransformerConstants.CODING_MONEY)
				.setCode(TransformerConstants.CODED_MONEY_USD)
				.setValue(claimGroup.getGrossCostAboveOutOfPocketThreshold());

		rxItem.addAdjudication()
				.setCategory(TransformerUtils.createCodeableConcept(TransformerConstants.CODING_CCW_ADJUDICATION_CATEGORY,
						TransformerConstants.CODED_ADJUDICATION_PATIENT_PAY))
				.getAmount().setSystem(TransformerConstants.CODING_MONEY)
				.setCode(TransformerConstants.CODED_MONEY_USD).setValue(claimGroup.getPatientPaidAmount());

		rxItem.addAdjudication()
				.setCategory(TransformerUtils.createCodeableConcept(TransformerConstants.CODING_CCW_ADJUDICATION_CATEGORY,
						TransformerConstants.CODED_ADJUDICATION_OTHER_TROOP_AMOUNT))
				.getAmount().setSystem(TransformerConstants.CODING_MONEY)
				.setCode(TransformerConstants.CODED_MONEY_USD)
				.setValue(claimGroup.getOtherTrueOutOfPocketPaidAmount());

		rxItem.addAdjudication()
				.setCategory(TransformerUtils.createCodeableConcept(TransformerConstants.CODING_CCW_ADJUDICATION_CATEGORY,
						TransformerConstants.CODED_ADJUDICATION_LOW_INCOME_SUBSIDY_AMOUNT))
				.getAmount().setSystem(TransformerConstants.CODING_MONEY)
				.setCode(TransformerConstants.CODED_MONEY_USD)
				.setValue(claimGroup.getLowIncomeSubsidyPaidAmount());

		rxItem.addAdjudication()
				.setCategory(TransformerUtils.createCodeableConcept(TransformerConstants.CODING_CCW_ADJUDICATION_CATEGORY,
						TransformerConstants.CODED_ADJUDICATION_PATIENT_LIABILITY_REDUCED_AMOUNT))
				.getAmount().setSystem(TransformerConstants.CODING_MONEY)
				.setCode(TransformerConstants.CODED_MONEY_USD)
				.setValue(claimGroup.getPatientLiabilityReductionOtherPaidAmount());

		rxItem.addAdjudication()
				.setCategory(TransformerUtils.createCodeableConcept(TransformerConstants.CODING_CCW_ADJUDICATION_CATEGORY,
						TransformerConstants.CODED_ADJUDICATION_TOTAL_COST))
				.getAmount().setSystem(TransformerConstants.CODING_MONEY)
				.setCode(TransformerConstants.CODED_MONEY_USD).setValue(claimGroup.getTotalPrescriptionCost());

		rxItem.addAdjudication()
				.setCategory(TransformerUtils.createCodeableConcept(TransformerConstants.CODING_CCW_ADJUDICATION_CATEGORY,
						TransformerConstants.CODED_ADJUDICATION_GAP_DISCOUNT_AMOUNT))
				.getAmount().setSystem(TransformerConstants.CODING_MONEY)
				.setCode(TransformerConstants.CODED_MONEY_USD).setValue(claimGroup.getGapDiscountAmount());

		if (claimGroup.getPrescriberIdQualifierCode() == null
				|| !claimGroup.getPrescriberIdQualifierCode().equalsIgnoreCase("01"))
			throw new InvalidRifValueException(
					"Prescriber ID Qualifier Code is invalid: " + claimGroup.getPrescriberIdQualifierCode());

		if (claimGroup.getPrescriberId() != null) {
			TransformerUtils.addCareTeamPractitioner(eob, rxItem, TransformerConstants.CODING_NPI_US,
					claimGroup.getPrescriberId(),
					ClaimCareteamrole.PRIMARY.toCode());
		}

		rxItem.setService(TransformerUtils.createCodeableConcept(TransformerConstants.CODING_NDC,
				claimGroup.getNationalDrugCode()));

		SimpleQuantity quantityDispensed = new SimpleQuantity();
		quantityDispensed.setValue(claimGroup.getQuantityDispensed());
		rxItem.setQuantity(quantityDispensed);
		
		rxItem.getQuantity().addExtension(
				TransformerUtils.createExtensionQuantity(CcwCodebookVariable.FILL_NUM, claimGroup.getFillNumber()));
		
		rxItem.getQuantity().addExtension(TransformerUtils.createExtensionQuantity(CcwCodebookVariable.DAYS_SUPLY_NUM,
				claimGroup.getDaysSupply()));

		// TODO CBBD-241 - This code was commented out because values other than
		// "01"
		// were coming thru
		// such as "07". Need to discuss with Karl if this check needs to be
		// here -

		/*
		 * if (claimGroup.serviceProviderIdQualiferCode == null ||
		 * !claimGroup.serviceProviderIdQualiferCode.equalsIgnoreCase("01"))
		 * throw new InvalidRifValueException(
		 * "Service Provider ID Qualifier Code is invalid: " +
		 * claimGroup.serviceProviderIdQualiferCode);
		 */

		if (!claimGroup.getServiceProviderId().isEmpty()) {
			eob.setOrganization(
					TransformerUtils.createIdentifierReference(TransformerConstants.CODING_NPI_US,
							claimGroup.getServiceProviderId()));
			eob.setFacility(
					TransformerUtils.createIdentifierReference(TransformerConstants.CODING_NPI_US,
							claimGroup.getServiceProviderId()));
			eob.getFacility().addExtension(TransformerUtils.createExtensionCoding(eob,
					CcwCodebookVariable.PHRMCY_SRVC_TYPE_CD, claimGroup.getPharmacyTypeCode()));
		}

		/*
		 * Storing code values in EOB.information below
		 */

		TransformerUtils.addInformation(eob, TransformerUtils.createCodeableConcept(eob,
				CcwCodebookVariable.DAW_PROD_SLCTN_CD, claimGroup.getDispenseAsWrittenProductSelectionCode()));

		if (claimGroup.getDispensingStatusCode().isPresent())
			TransformerUtils.addInformation(eob, TransformerUtils.createCodeableConcept(eob,
					CcwCodebookVariable.DSPNSNG_STUS_CD, claimGroup.getDispensingStatusCode()));

		TransformerUtils.addInformation(eob, TransformerUtils.createCodeableConcept(eob,
				CcwCodebookVariable.DRUG_CVRG_STUS_CD, claimGroup.getDrugCoverageStatusCode()));

		if (claimGroup.getAdjustmentDeletionCode().isPresent())
			TransformerUtils.addInformation(eob, TransformerUtils.createCodeableConcept(eob,
					CcwCodebookVariable.ADJSTMT_DLTN_CD, claimGroup.getAdjustmentDeletionCode()));

		if (claimGroup.getNonstandardFormatCode().isPresent())
			TransformerUtils.addInformation(eob, TransformerUtils.createCodeableConcept(eob,
					CcwCodebookVariable.NSTD_FRMT_CD, claimGroup.getNonstandardFormatCode()));

		if (claimGroup.getPricingExceptionCode().isPresent())
			TransformerUtils.addInformation(eob, TransformerUtils.createCodeableConcept(eob,
					CcwCodebookVariable.PRCNG_EXCPTN_CD, claimGroup.getPricingExceptionCode()));

		if (claimGroup.getCatastrophicCoverageCode().isPresent())
			TransformerUtils.addInformation(eob, TransformerUtils.createCodeableConcept(eob,
					CcwCodebookVariable.CTSTRPHC_CVRG_CD, claimGroup.getCatastrophicCoverageCode()));

		if (claimGroup.getPrescriptionOriginationCode().isPresent())
			TransformerUtils.addInformation(eob, TransformerUtils.createCodeableConcept(eob,
					CcwCodebookVariable.RX_ORGN_CD, claimGroup.getPrescriptionOriginationCode()));

		if (claimGroup.getBrandGenericCode().isPresent())
			TransformerUtils.addInformation(eob, TransformerUtils.createCodeableConcept(eob,
					CcwCodebookVariable.BRND_GNRC_CD, claimGroup.getBrandGenericCode()));

		TransformerUtils.addInformation(eob, TransformerUtils.createCodeableConcept(eob,
				CcwCodebookVariable.PHRMCY_SRVC_TYPE_CD, claimGroup.getPharmacyTypeCode()));

		TransformerUtils.addInformation(eob, TransformerUtils.createCodeableConcept(eob,
				CcwCodebookVariable.PTNT_RSDNC_CD, claimGroup.getPatientResidenceCode()));

		if (claimGroup.getSubmissionClarificationCode().isPresent())
			TransformerUtils.addInformation(eob, TransformerUtils.createCodeableConcept(eob,
					CcwCodebookVariable.SUBMSN_CLR_CD, claimGroup.getSubmissionClarificationCode()));

		return eob;
	}

}

