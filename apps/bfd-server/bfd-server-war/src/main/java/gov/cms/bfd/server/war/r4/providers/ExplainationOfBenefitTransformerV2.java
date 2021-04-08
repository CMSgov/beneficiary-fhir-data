package gov.cms.bfd.server.war.r4.providers;

import com.codahale.metrics.MetricRegistry;
import gov.cms.bfd.model.codebook.data.CcwCodebookVariable;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import gov.cms.bfd.server.war.commons.adapter.ClaimAdaptorInterface;
import gov.cms.bfd.server.war.commons.adapter.ClaimLineAdaptorInterface;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import java.util.Date;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.ExplanationOfBenefitStatus;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.ItemComponent;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.RemittanceOutcome;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.Use;

public class ExplainationOfBenefitTransformerV2 {
  static ExplanationOfBenefit transform(
      MetricRegistry metricRegistry, ClaimAdaptorInterface claim) {
    ExplanationOfBenefit eob = new ExplanationOfBenefit();

    mapHeader(eob, claim);
    mapType(eob, claim);
    mapLines(eob, claim);

    return eob;
  }

  static void mapHeader(ExplanationOfBenefit eob, ClaimAdaptorInterface claim) {
    // Set profile
    claim.getProfile().ifPresent(profile -> eob.getMeta().addProfile(profile));

    // Claim Type + Claim ID => ExplanationOfBenefit.id
    if (claim.getClaimType().isPresent() && claim.getClaimId().isPresent()) {
      eob.setId(
          TransformerUtilsV2.buildEobId(claim.getClaimType().get(), claim.getClaimId().get()));
    }

    // Current timestamp => Created
    eob.setCreated(new Date());

    // "claim" => ExplanationOfBenefit.use
    eob.setUse(Use.CLAIM);

    // "complete" => ExplanationOfBenefit.outcome
    eob.setOutcome(RemittanceOutcome.COMPLETE);

    // Claim ID => ExplanationOfBenefit.identifier
    claim
        .getClaimId()
        .ifPresent(
            id -> {
              if (claim.is(ClaimTypeV2.PDE)) {
                eob.addIdentifier(
                    TransformerUtilsV2.createClaimIdentifier(CcwCodebookVariable.PDE_ID, id));
              } else {
                eob.addIdentifier(
                    TransformerUtilsV2.createClaimIdentifier(CcwCodebookVariable.CLM_ID, id));
              }
            });

    // CLM_GRP_ID => ExplanationOfBenefit.identifier
    claim
        .getClaimGroupId()
        .ifPresent(
            cgi ->
                eob.addIdentifier()
                    .setSystem(TransformerConstants.IDENTIFIER_SYSTEM_BBAPI_CLAIM_GROUP_ID)
                    .setValue(cgi.toPlainString())
                    .setType(TransformerUtilsV2.createC4BBClaimCodeableConcept()));

    // BENE_ID + Coverage Type => ExplanationOfBenefit.insurance.coverage
    if (claim.getBeneficiaryId().isPresent() && claim.getMedicareSegment().isPresent()) {
      eob.addInsurance()
          .setCoverage(
              TransformerUtilsV2.referenceCoverage(
                  claim.getBeneficiaryId().get(), claim.getMedicareSegment().get()));
    }

    // BENE_ID => ExplanationOfBenefit.patient (reference)
    claim
        .getBeneficiaryId()
        .ifPresent(beneid -> eob.setPatient(TransformerUtilsV2.referencePatient(beneid)));

    // FINAL_ACTION => ExplanationOfBenefit.status
    claim
        .getFinalAction()
        .ifPresent(
            action -> {
              switch (action) {
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
            });

    TransformerUtilsV2.validatePeriodDates(claim.getDateFrom(), claim.getDateThrough());

    // CLM_FROM_DT => ExplanationOfBenefit.billablePeriod.start
    claim
        .getDateFrom()
        .ifPresent(date -> TransformerUtilsV2.setPeriodStart(eob.getBillablePeriod(), date));

    // CLM_THRU_DT => ExplanationOfBenefit.billablePeriod.end
    claim
        .getDateThrough()
        .ifPresent(date -> TransformerUtilsV2.setPeriodEnd(eob.getBillablePeriod(), date));

    // CLM_PMT_AMT => ExplanationOfBenefit.payment.amount
    claim
        .getPaymentAmount()
        .ifPresent(amt -> eob.getPayment().setAmount(TransformerUtilsV2.createMoney(amt)));
  }

  static void mapType(ExplanationOfBenefit eob, ClaimAdaptorInterface claim) {
    // NCH_CLM_TYPE_CD => ExplanationOfBenefit.type.coding
    claim
        .getClaimTypeCode()
        .ifPresent(
            code ->
                eob.getType()
                    .addCoding(
                        TransformerUtilsV2.createCoding(
                            eob, CcwCodebookVariable.NCH_CLM_TYPE_CD, code)));

    // Claim Type => ExplanationOfBenefit.type.coding
    claim
        .getClaimType()
        .ifPresent(
            t -> {
              eob.getType()
                  .addCoding()
                  .setSystem(TransformerConstants.CODING_SYSTEM_BBAPI_EOB_TYPE)
                  .setCode(t.name());

              // Map a Coding for FHIR's ClaimType coding system, if we can.
              org.hl7.fhir.r4.model.codesystems.ClaimType fhirClaimType;
              switch (t) {
                case PDE:
                  fhirClaimType = org.hl7.fhir.r4.model.codesystems.ClaimType.PHARMACY;
                  break;

                case INPATIENT:
                case OUTPATIENT:
                case HOSPICE:
                case SNF:
                case DME:
                  fhirClaimType = org.hl7.fhir.r4.model.codesystems.ClaimType.INSTITUTIONAL;
                  break;

                case CARRIER:
                case HHA:
                  fhirClaimType = org.hl7.fhir.r4.model.codesystems.ClaimType.PROFESSIONAL;
                  break;

                default:
                  // All options on ClaimTypeV2 are covered above, but this is there to appease
                  // linter
                  throw new BadCodeMonkeyException("No match found for ClaimTypeV2");
              }

              eob.getType()
                  .addCoding(
                      new Coding(
                          fhirClaimType.getSystem(),
                          fhirClaimType.toCode(),
                          fhirClaimType.getDisplay()));
            });

    // NCH_NEAR_LINE_REC_IDENT_CD => ExplanationOfBenefit.extension
    claim
        .getNearLineRecordIdCode()
        .ifPresent(
            code ->
                eob.addExtension(
                    TransformerUtilsV2.createExtensionCoding(
                        eob, CcwCodebookVariable.NCH_NEAR_LINE_REC_IDENT_CD, code)));
  }

  static void mapLines(ExplanationOfBenefit eob, ClaimAdaptorInterface claim) {
    for (ClaimLineAdaptorInterface line : claim.getLines()) {
      ItemComponent item = TransformerUtilsV2.addItem(eob);

      // This value maps differently between PDE and other claims
      if (claim.is(ClaimTypeV2.PDE)) {
        // PROD_SRVC_ID => ExplanationOfBenefit.item.productOrService
        line.getNationalDrugCode()
            .ifPresent(
                ndc ->
                    item.setProductOrService(
                        TransformerUtilsV2.createCodeableConcept(
                            TransformerConstants.CODING_NDC,
                            null,
                            TransformerUtilsV2.retrieveFDADrugCodeDisplay(ndc),
                            ndc)));

      } else {
        // REV_CNTR_IDE_NDC_UPC_NUM => ExplanationOfBenefit.item.productOrService.extension
        TransformerUtilsV2.addNationalDrugCode(item, line.getNationalDrugCode());
      }
    }
  }
}
