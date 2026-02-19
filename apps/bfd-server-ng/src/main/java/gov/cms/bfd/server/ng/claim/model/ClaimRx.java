package gov.cms.bfd.server.ng.claim.model;

import static gov.cms.bfd.server.ng.claim.model.ClaimSubtype.PDE;

import gov.cms.bfd.server.ng.ClaimSecurityStatus;
import gov.cms.bfd.server.ng.util.SequenceGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.List;
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

  @Embedded private ServiceProviderPharmacy serviceProviderHistory;
  @Embedded private PrescribingCareTeam prescribingProviderHistory;
  @Embedded private AdjudicationChargeRx adjudicationCharge;
  @Embedded private ClaimPaymentDate claimPaymentDate;
  @Embedded private SubmitterContractNumber submitterContractNumber;
  @Embedded private SubmitterContractPBPNumber submitterContractPBPNumber;
  @Embedded private ClaimSubmissionDate claimSubmissionDate;
  @Embedded private ClaimProcessDate claimProcessDate;

  /** Rx claims carry a single embedded line rather than a collection. */
  @Embedded private ClaimItemRx claimItems;

  /** {@inheritDoc} */
  @Override
  public ExplanationOfBenefit toFhir(ClaimSecurityStatus securityStatus) {
    var eob = super.toFhir(securityStatus);

    addPartDInsurer(eob);
    addClaimLineItem(eob);
    addServiceProvider(eob);
    addSupportingInfo(eob);
    addPrescribingProviderCareTeam(eob);
    addAdjudicationAndPayment(eob);
    addInsurance(eob);

    return sortedEob(eob);
  }

  private void addPartDInsurer(ExplanationOfBenefit eob) {
    contractName
        .flatMap(name -> getClaimTypeCode().toFhirInsurerPartD(name))
        .ifPresent(
            i -> {
              eob.addContained(i);
              eob.setInsurer(new Reference(i));
            });
  }

  private void addClaimLineItem(ExplanationOfBenefit eob) {
    claimItems.getClaimLine().toFhirItemComponent().ifPresent(eob::addItem);
  }

  private void addServiceProvider(ExplanationOfBenefit eob) {
    serviceProviderHistory
        .toFhirNpiType()
        .ifPresent(
            p -> {
              eob.addContained(p);
              eob.setProvider(new Reference("#" + p.getId()));
            });
  }

  private void addSupportingInfo(ExplanationOfBenefit eob) {
    buildHeaderSupportingInfo().forEach(eob::addSupportingInfo);
    buildLineSupportingInfo().forEach(eob::addSupportingInfo);
  }

  private List<ExplanationOfBenefit.SupportingInformationComponent> buildHeaderSupportingInfo() {
    return Stream.of(
            claimFormatCode
                .filter(c -> getClaimTypeCode().isClaimSubtype(PDE))
                .map(c -> c.toFhir(supportingInfoFactory)),
            submitterContractNumber.toFhir(supportingInfoFactory).stream().findFirst(),
            submitterContractPBPNumber.toFhir(supportingInfoFactory).stream().findFirst(),
            claimSubmissionDate.toFhir(supportingInfoFactory),
            claimProcessDate.toFhir(supportingInfoFactory))
        .flatMap(Optional::stream)
        .toList();
  }

  private List<ExplanationOfBenefit.SupportingInformationComponent> buildLineSupportingInfo() {
    return Stream.concat(
            claimItems
                .getClaimLine()
                .getClaimRxSupportingInfo()
                .toFhir(supportingInfoFactory)
                .stream(),
            claimItems.getClaimLineRxNum().toFhir(supportingInfoFactory).stream())
        .toList();
  }

  private void addPrescribingProviderCareTeam(ExplanationOfBenefit eob) {
    var sequenceGenerator = new SequenceGenerator();
    prescribingProviderHistory
        .toFhirCareTeamComponent(sequenceGenerator.next())
        .ifPresent(eob::addCareTeam);
  }

  private void addAdjudicationAndPayment(ExplanationOfBenefit eob) {
    adjudicationCharge.toFhirTotal().forEach(eob::addTotal);
    eob.setPayment(claimPaymentDate.toFhir());
  }

  private void addInsurance(ExplanationOfBenefit eob) {
    getClaimTypeCode().toFhirPartDInsurance().ifPresent(eob::addInsurance);
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
  public MetaSourceSk getMetaSourceSk() {
    return MetaSourceSk.DDPS;
  }

  /** Rx claims have a single embedded rather than a collection. */
  @Override
  public SortedSet<ClaimItemRx> getClaimItems() {
    var items = new TreeSet<ClaimItemRx>();
    items.add(claimItems);
    return items;
  }
}
