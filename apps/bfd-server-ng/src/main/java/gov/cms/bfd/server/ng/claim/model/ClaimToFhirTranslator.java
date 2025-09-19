package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.DateUtil;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.Reference;

/**
 * Converts a {@link Claim} into a FHIR {@link ExplanationOfBenefit}.
 *
 * <p>Refactored {@link Claim} to keep the model class small and focused.
 */
public final class ClaimToFhirTranslator {
  private ClaimToFhirTranslator() {}

  /**
   * Converts the provided {@link Claim} into {@link ExplanationOfBenefit} resource.
   *
   * @param claim the claim to convert (not null)
   * @return an ExplanationOfBenefit representing the claim
   */
  public static ExplanationOfBenefit toFhir(Claim claim) {
    var eob = new ExplanationOfBenefit();
    eob.setId(String.valueOf(claim.getClaimUniqueId()));
    eob.setPatient(PatientReferenceFactory.toFhir(claim.getBeneficiary().getXrefSk()));
    eob.setStatus(ExplanationOfBenefit.ExplanationOfBenefitStatus.ACTIVE);
    eob.setUse(ExplanationOfBenefit.Use.CLAIM);
    var claimTypeCode = claim.getClaimTypeCode();
    var claimSourceId = claim.getClaimSourceId();
    eob.setType(claimTypeCode.toFhirType());
    claimTypeCode.toFhirSubtype().ifPresent(eob::setSubType);

    // meta / lastUpdated
    var fhirMeta = claim.getMeta().toFhir(claimTypeCode, claimSourceId);
    var latest = claim.getLatestUpdatedTimestamp();
    if (latest != null) {
      fhirMeta.setLastUpdated(DateUtil.toDate(latest));
    }
    eob.setMeta(fhirMeta);

    eob.setIdentifier(claim.getIdentifiers().toFhir());
    eob.setBillablePeriod(claim.getBillablePeriod().toFhir());
    eob.setCreated(DateUtil.toDate(claim.getClaimEffectiveDate()));
    claimTypeCode
        .toFhirInsurerPartAB()
        .ifPresent(
            i -> {
              eob.addContained(i);
              eob.setInsurer(new Reference(i));
            });

    var institutional = claim.getClaimInstitutional();
    Stream.of(
            claim.getClaimExtensions().toFhir(),
            institutional.map(i -> i.getExtensions().toFhir()).orElse(List.of()),
            List.of(claim.getClaimDateSignature().getClaimProcessDate().toFhir()))
        .flatMap(Collection::stream)
        .forEach(eob::addExtension);

    claim
        .getClaimItems()
        .forEach(
            item -> {
              item.getClaimLine().toFhir(item).ifPresent(eob::addItem);
              item.getClaimProcedure().toFhirProcedure().ifPresent(eob::addProcedure);
              item.getClaimProcedure()
                  .toFhirDiagnosis(item.getClaimItemId().getBfdRowId())
                  .ifPresent(eob::addDiagnosis);
            });
    claim
        .getBillingProvider()
        .toFhir(claimTypeCode)
        .ifPresent(
            p -> {
              eob.addContained(p);
              eob.setProvider(new Reference(p));
            });

    claimSourceId.toFhirOutcome().ifPresent(eob::setOutcome);
    claimTypeCode.toFhirOutcome().ifPresent(eob::setOutcome);

    claim.getClaimFiss().flatMap(cf -> cf.toFhirOutcome(claimTypeCode)).ifPresent(eob::setOutcome);

    var supportingInfoFactory = new SupportingInfoFactory();
    var initialSupportingInfo =
        List.of(
            claim.getBloodPints().toFhir(supportingInfoFactory),
            claim.getNchPrimaryPayorCode().toFhir(supportingInfoFactory),
            claim.getTypeOfBillCode().toFhir(supportingInfoFactory));
    Stream.of(
            initialSupportingInfo,
            claim.getClaimDateSignature().getSupportingInfo().toFhir(supportingInfoFactory),
            institutional
                .map(i -> i.getSupportingInfo().toFhir(supportingInfoFactory))
                .orElse(List.of()))
        .flatMap(Collection::stream)
        .forEach(eob::addSupportingInfo);

    claim
        .getCareTeam()
        .toFhir()
        .forEach(
            c -> {
              eob.addCareTeam(c.careTeam());
              eob.addContained(c.practitioner());
            });

    institutional.ifPresent(
        i -> {
          eob.addAdjudication(i.getPpsDrgWeight().toFhir());
          var claimValues = claim.getClaimItems().stream().map(ClaimItem::getClaimValue).toList();
          eob.addBenefitBalance(
              claim.getBenefitBalance().toFhir(i.getBenefitBalanceInstitutional(), claimValues));
          claim.getAnsiSignatures().stream()
              .flatMap(sig -> sig.toFhir().stream())
              .forEach(eob::addAdjudication);
        });

    claimTypeCode.toFhirInsurance().ifPresent(eob::addInsurance);
    eob.addTotal(claim.getAdjudicationCharge().toFhir());
    eob.setPayment(claim.getClaimPaymentAmount().toFhir());

    return ClaimToFhirTranslatorHelper.sortedEob(eob);
  }
}
