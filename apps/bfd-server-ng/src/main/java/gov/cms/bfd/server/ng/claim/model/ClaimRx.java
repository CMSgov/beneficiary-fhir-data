package gov.cms.bfd.server.ng.claim.model;

import static gov.cms.bfd.server.ng.claim.model.ClaimSubtype.PDE;

import gov.cms.bfd.server.ng.ClaimSecurityStatus;
import gov.cms.bfd.server.ng.util.SequenceGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.Collection;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Stream;
import lombok.Getter;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.Reference;

/**
 * Claim table. Suppress SonarQube Monster Class warning that dependencies to other class should be
 * reduced from 21 to the max 20. Ignore. Class itself is relatively short in lines of code.
 */
@Getter
@Entity
@Table(name = "claim_rx", schema = "idr_new")
public class ClaimRx extends ClaimBase {

  @Column(name = "clm_sbmt_frmt_cd")
  private Optional<ClaimSubmissionFormatCode> claimFormatCode;

  @Column(name = "cntrct_pbp_name")
  private Optional<String> contractName;

  @Embedded private ServiceProviderHistory serviceProviderHistory;
  @Embedded private PrescribingProviderHistory prescribingProviderHistory;
  @Embedded private AdjudicationChargeRx adjudicationCharge;
  @Embedded private ClaimPaymentDate claimPaymentDate;
  @Embedded private SubmitterContractNumber submitterContractNumber;
  @Embedded private SubmitterContractPBPNumber submitterContractPBPNumber;
  @Embedded private ClaimSubmissionDate claimSubmissionDate;
  @Embedded private ClaimProcessDate claimProcessDate;

  @Embedded private ClaimItemRx claimItems;

  /**
   * Convert the claim info to a FHIR ExplanationOfBenefit.
   *
   * @param securityStatus securityStatus
   * @return ExplanationOfBenefit
   */
  @Override
  public ExplanationOfBenefit toFhir(ClaimSecurityStatus securityStatus) {
    var eob = super.toFhir(securityStatus);

    contractName
        .flatMap(name -> getClaimTypeCode().toFhirInsurerPartD(name))
        .ifPresent(
            i -> {
              eob.addContained(i);
              eob.setInsurer(new Reference(i));
            });

    var item = claimItems.getClaimLine().toFhirItemComponent();

    item.ifPresent(eob::addItem);

    serviceProviderHistory
        .toFhirNpiType()
        .ifPresent(
            p -> {
              eob.addContained(p);
              eob.setProvider(new Reference(p));
            });

    var claimRxSupportingInfo =
        Stream.of(
                // claim rx header lvl
                claimFormatCode
                    .filter(c -> getClaimTypeCode().isClaimSubtype(PDE))
                    .map(c -> c.toFhir(supportingInfoFactory))
                    .stream(),
                submitterContractNumber.toFhir(supportingInfoFactory).stream(),
                submitterContractPBPNumber.toFhir(supportingInfoFactory).stream(),
                claimSubmissionDate.toFhir(supportingInfoFactory).stream(),
                claimProcessDate.toFhir(supportingInfoFactory).stream(),
                // claim rx line lvl
                claimItems
                    .getClaimLine()
                    .getClaimRxSupportingInfo()
                    .toFhir(supportingInfoFactory)
                    .stream(),

                // claim line rx num
                claimItems.getClaimLineRxNum().toFhir(supportingInfoFactory).stream())
            .flatMap(s -> s)
            .toList();

    Stream.of(claimRxSupportingInfo).flatMap(Collection::stream).forEach(eob::addSupportingInfo);

    var sequenceGenerator = new SequenceGenerator();
    Stream.of(prescribingProviderHistory)
        .flatMap(p -> p.toFhirCareTeamComponent(sequenceGenerator).stream())
        .forEach(
            c -> {
              eob.addCareTeam(c.careTeam());
              eob.addContained(c.practitioner());
            });

    var insurance = new ExplanationOfBenefit.InsuranceComponent();
    insurance.setFocal(true);
    getClaimTypeCode().toFhirPartDInsurance().ifPresent(eob::addInsurance);
    adjudicationCharge.toFhirTotal().forEach(eob::addTotal);
    eob.setPayment(claimPaymentDate.toFhir());

    return sortedEob(eob);
  }

  @Override
  public Optional<Integer> getDrgCode() {
    return Optional.empty();
  }

  @Override
  public ClaimSourceId getClaimSourceId() {
    return ClaimSourceId.NATIONAL_CLAIMS_HISTORY;
  }

  @Override
  public SortedSet<ClaimItemRx> getClaimItems() {
    return new TreeSet<>();
  }
}
