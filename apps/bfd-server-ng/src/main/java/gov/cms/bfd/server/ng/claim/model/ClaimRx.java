package gov.cms.bfd.server.ng.claim.model;

import static gov.cms.bfd.server.ng.claim.model.ClaimSubtype.PDE;

import gov.cms.bfd.server.ng.ClaimFilterOptions;
import gov.cms.bfd.server.ng.beneficiary.model.BeneficiarySimple;
import gov.cms.bfd.server.ng.util.SequenceGenerator;
import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Stream;
import lombok.Getter;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.Reference;

/**
 * Claim table. Suppress SonarQube Monster Class warning that dependencies to other class should be
 * reduced from 21 to the max 20. Ignore. Class itself is relatively short in lines of code.
 */
@Getter
@Entity
@Table(name = "claim_rx", schema = "idr")
public class ClaimRx extends ClaimBase {

  @Column(name = "clm_sbmt_frmt_cd")
  private Optional<ClaimSubmissionFormatCode> claimFormatCode;

  @Column(name = "cntrct_pbp_name")
  private Optional<String> contractName;

  @Column(name = "clm_prcng_excptn_cd")
  private Optional<ClaimPricingReasonCode> pricingCode;

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

  /** Default constructor for JPA. */
  public ClaimRx() {
    super();
  }

  /**
   * Parameterized constructor.
   *
   * @param claimUniqueId claim unique id
   * @param claimTypeCode claim type code
   * @param claimEffectiveDate claim effective date
   * @param finalAction final action
   * @param latestClaimIndicator latest claim indicator
   * @param claimAdjustmentTypeCode claim adjustment type code
   * @param meta meta
   * @param identifiers identifiers
   * @param billablePeriod billable period
   * @param claimIDRLoadDate claim IDR load date
   * @param beneficiary beneficiary
   * @param claimFormatCode claim format code
   * @param contractName contract name
   * @param pricingCode pricing code
   * @param serviceProviderHistory service provider history
   * @param prescribingProviderHistory prescribing provider history
   * @param adjudicationCharge adjudication charge
   * @param claimPaymentDate claim payment date
   * @param submitterContractNumber submitter contract number
   * @param submitterContractPBPNumber submitter contract PBP number
   * @param claimSubmissionDate claim submission date
   * @param claimProcessDate claim process date
   * @param claimItems claim items
   */
  public ClaimRx(
      long claimUniqueId,
      ClaimTypeCode claimTypeCode,
      LocalDate claimEffectiveDate,
      ClaimFinalAction finalAction,
      Boolean latestClaimIndicator,
      Optional<ClaimAdjustmentTypeCode> claimAdjustmentTypeCode,
      Meta meta,
      Identifiers identifiers,
      BillablePeriod billablePeriod,
      ClaimIDRLoadDate claimIDRLoadDate,
      BeneficiarySimple beneficiary,
      Optional<ClaimSubmissionFormatCode> claimFormatCode,
      Optional<String> contractName,
      Optional<ClaimPricingReasonCode> pricingCode,
      ServiceProviderPharmacy serviceProviderHistory,
      PrescribingCareTeam prescribingProviderHistory,
      AdjudicationChargeRx adjudicationCharge,
      ClaimPaymentDate claimPaymentDate,
      SubmitterContractNumber submitterContractNumber,
      SubmitterContractPBPNumber submitterContractPBPNumber,
      ClaimSubmissionDate claimSubmissionDate,
      ClaimProcessDate claimProcessDate,
      ClaimItemRx claimItems) {
    super(
        claimUniqueId,
        claimTypeCode,
        claimEffectiveDate,
        finalAction,
        latestClaimIndicator,
        claimAdjustmentTypeCode,
        meta,
        identifiers,
        billablePeriod,
        claimIDRLoadDate,
        beneficiary);
    this.claimFormatCode = claimFormatCode != null ? claimFormatCode : Optional.empty();
    this.contractName = contractName != null ? contractName : Optional.empty();
    this.pricingCode = pricingCode != null ? pricingCode : Optional.empty();
    this.serviceProviderHistory = serviceProviderHistory;
    this.prescribingProviderHistory = prescribingProviderHistory;
    this.adjudicationCharge = adjudicationCharge;
    this.claimPaymentDate = claimPaymentDate;
    this.submitterContractNumber = submitterContractNumber;
    this.submitterContractPBPNumber = submitterContractPBPNumber;
    this.claimSubmissionDate = claimSubmissionDate;
    this.claimProcessDate = claimProcessDate;
    this.claimItems = claimItems;
  }

  /** {@inheritDoc} */
  @Override
  public ExplanationOfBenefit toFhir(ClaimFilterOptions options, ClaimState claimState) {
    var eob = super.toFhir(options, claimState);

    if (eob.getMeta() != null) {
      eob.getMeta().getProfile().clear();
      switch (options.getQueryProfile()) {
        case BASIS -> {
          eob.getMeta().addProfile(SystemUrls.CARIN_STRUCTURE_DEFINITION_PHARMACY_BASIS);
        }
        case REGULAR -> {
          eob.getMeta().addProfile(SystemUrls.CARIN_STRUCTURE_DEFINITION_PHARMACY);
        }
        case CMS -> {
          eob.getMeta().addProfile(SystemUrls.CARIN_STRUCTURE_DEFINITION_PHARMACY);
          eob.getMeta().addProfile(SystemUrls.CMS_STRUCTURE_DEFINITION_PHARMACY);
        }
      }
    }

    addPartDInsurer(eob);
    addClaimLineItem(eob, options);
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

  private void addClaimLineItem(ExplanationOfBenefit eob, ClaimFilterOptions options) {
    getClaimItems().getClaimLine().toFhirItemComponent(options).ifPresent(eob::addItem);
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
    Optional<ExplanationOfBenefit.SupportingInformationComponent> formatCode =
        claimFormatCode
            .filter(c -> getClaimTypeCode().isClaimSubtype(PDE))
            .map(c -> c.toFhir(supportingInfoFactory));
    Optional<ExplanationOfBenefit.SupportingInformationComponent> contractNum =
        submitterContractNumber != null
            ? submitterContractNumber.toFhir(supportingInfoFactory).stream().findFirst()
            : Optional.empty();
    Optional<ExplanationOfBenefit.SupportingInformationComponent> pbpNum =
        submitterContractPBPNumber != null
            ? submitterContractPBPNumber.toFhir(supportingInfoFactory).stream().findFirst()
            : Optional.empty();
    Optional<ExplanationOfBenefit.SupportingInformationComponent> subDate =
        claimSubmissionDate != null
            ? claimSubmissionDate.toFhir(supportingInfoFactory)
            : Optional.empty();
    Optional<ExplanationOfBenefit.SupportingInformationComponent> procDate =
        claimProcessDate != null
            ? claimProcessDate.toFhir(supportingInfoFactory)
            : Optional.empty();

    return Stream.of(formatCode, contractNum, pbpNum, subDate, procDate)
        .flatMap(Optional::stream)
        .toList();
  }

  private List<ExplanationOfBenefit.SupportingInformationComponent> buildLineSupportingInfo() {
    var line = getClaimItems().getClaimLine();
    var lineSupportingInfo = line.getClaimRxSupportingInfo();
    var lineRxNum = getClaimItems().getClaimLineRxNum();

    Stream<ExplanationOfBenefit.SupportingInformationComponent> supportingStream = Stream.empty();
    if (lineSupportingInfo != null) {
      supportingStream = lineSupportingInfo.toFhir(supportingInfoFactory).stream();
    }

    Stream<ExplanationOfBenefit.SupportingInformationComponent> rxNumStream = Stream.empty();
    if (lineRxNum != null) {
      rxNumStream = lineRxNum.toFhir(supportingInfoFactory).stream();
    }

    return Stream.concat(supportingStream, rxNumStream).toList();
  }

  private void addPrescribingProviderCareTeam(ExplanationOfBenefit eob) {
    var sequenceGenerator = new SequenceGenerator();
    prescribingProviderHistory
        .toFhirCareTeamComponent(sequenceGenerator.next(), getClaimTypeCode().toContext())
        .ifPresent(eob::addCareTeam);
  }

  private void addAdjudicationAndPayment(ExplanationOfBenefit eob) {
    if (adjudicationCharge != null) {
      adjudicationCharge.toFhirTotal().forEach(eob::addTotal);
      adjudicationCharge.toFhirAdjudication().forEach(eob::addAdjudication);
    }

    getTotalDrugCostAmount()
        .map(AdjudicationChargeType.TOTAL_DRUG_COST_AMOUNT::toFhirTotal)
        .ifPresent(eob::addTotal);

    headerAdjudicationComponent().ifPresent(eob::addAdjudication);

    if (claimPaymentDate != null) {
      claimPaymentDate.toFhir().ifPresent(eob::setPayment);
    }
  }

  private Optional<ExplanationOfBenefit.AdjudicationComponent> headerAdjudicationComponent() {
    if (pricingCode.isEmpty()) {
      return Optional.empty();
    }

    var reasonCode = pricingCode.get().toFhir();
    var reasonCodeableConcept = new CodeableConcept();
    reasonCodeableConcept.addCoding(reasonCode);

    return Optional.of(
        new ExplanationOfBenefit.AdjudicationComponent()
            .setCategory(
                new CodeableConcept(
                    new Coding()
                        .setSystem(SystemUrls.CARIN_CODE_SYSTEM_ADJUDICATION_DISCRIMINATOR)
                        .setCode("benefitpaymentstatus")
                        .setDisplay("Benefit Payment Status")))
            .setReason(reasonCodeableConcept));
  }

  /**
   * Accessor for claim line rx total drug cost amount, if this is an PDE claim.
   *
   * @return optional total drug cost amount
   */
  public Optional<BigDecimal> getTotalDrugCostAmount() {
    var line = getClaimItems().getClaimLine();
    if (line == null || line.getAdjudicationCharge() == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(line.getAdjudicationCharge().getTotalDrugCost());
  }

  private void addInsurance(ExplanationOfBenefit eob) {
    eob.addInsurance(getClaimTypeCode().toFhirPartDInsurance());
  }

  @Override
  ClaimSourceId getClaimSourceId() {
    return ClaimSourceId.NATIONAL_CLAIMS_HISTORY;
  }

  @Override
  MetaSourceSk getMetaSourceSk() {
    return MetaSourceSk.DDPS;
  }

  /**
   * Returns the system type.
   *
   * @return system type
   */
  public static SystemType getSystemType() {
    return SystemType.DDPS;
  }

  @Override
  public Optional<Integer> getDrgCode() {
    return Optional.empty();
  }

  /** Rx claims have a single embedded rather than a collection. */
  @Override
  public SortedSet<ClaimItemBase> getItems() {
    var items = new TreeSet<ClaimItemBase>();
    items.add(getClaimItems());
    return items;
  }

  @Override
  public Optional<ClaimRelatedCondition> getClaimRelatedCondition() {
    return Optional.empty();
  }
}
