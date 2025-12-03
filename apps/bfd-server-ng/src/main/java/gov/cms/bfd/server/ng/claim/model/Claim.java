package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.ClaimSecurityStatus;
import gov.cms.bfd.server.ng.beneficiary.model.BeneficiarySimple;
import gov.cms.bfd.server.ng.util.DateUtil;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.stream.Stream;
import lombok.Getter;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Reference;
import org.jetbrains.annotations.Nullable;

/**
 * Claim table. Suppress SonarQube Monster Class warning that dependencies to other class should be
 * reduced from 21 to the max 20. Ignore. Class itself is relatively short in lines of code.
 */
@Entity
@Getter
@Table(name = "claim", schema = "idr")
@SuppressWarnings("java:S6539")
public class Claim {
  @Id
  @Column(name = "clm_uniq_id", insertable = false, updatable = false)
  private long claimUniqueId;

  @Column(name = "clm_type_cd")
  private ClaimTypeCode claimTypeCode;

  @Column(name = "clm_src_id")
  private ClaimSourceId claimSourceId;

  @Column(name = "clm_efctv_dt")
  private LocalDate claimEffectiveDate;

  @Column(name = "clm_srvc_prvdr_gnrc_id_num")
  private String serviceProviderNpiNumber;

  @Embedded private Meta meta;
  @Embedded private Identifiers identifiers;
  @Embedded private BillablePeriod billablePeriod;
  @Embedded private ClaimExtensions claimExtensions;
  @Embedded private BillingProvider billingProvider;
  @Embedded private BloodPints bloodPints;
  @Embedded private NchPrimaryPayorCode nchPrimaryPayorCode;
  @Embedded private TypeOfBillCode typeOfBillCode;
  @Embedded private CareTeam careTeam;
  @Embedded private BenefitBalance benefitBalance;
  @Embedded private AdjudicationCharge adjudicationCharge;
  @Embedded private ClaimPaymentAmount claimPaymentAmount;
  @Embedded private ClaimRecordType claimRecordType;

  @OneToOne
  @JoinColumn(name = "bene_sk")
  private BeneficiarySimple beneficiary;

  @OneToOne
  @JoinColumn(name = "clm_dt_sgntr_sk")
  private ClaimDateSignature claimDateSignature;

  @Nullable
  @OneToOne
  @JoinColumn(name = "clm_uniq_id")
  private ClaimFiss claimFiss;

  @Nullable
  @OneToOne
  @JoinColumn(name = "clm_uniq_id")
  private ClaimInstitutional claimInstitutional;

  @Nullable
  @OneToOne
  @JoinColumn(name = "clm_uniq_id")
  private ClaimProfessional claimProfessional;

  @OneToMany(fetch = FetchType.EAGER)
  @JoinColumn(name = "clm_uniq_id")
  private SortedSet<ClaimItem> claimItems;

  @Column(name = "clm_sbmtr_cntrct_num")
  private String contractNumber;

  @Column(name = "clm_sbmtr_cntrct_pbp_num")
  private String contractPbpNumber;

  @Nullable
  @OneToOne
  @JoinColumn(
      name = "clm_sbmtr_cntrct_num",
      insertable = false,
      updatable = false,
      referencedColumnName = "cntrct_num")
  @JoinColumn(
      name = "clm_sbmtr_cntrct_pbp_num",
      insertable = false,
      updatable = false,
      referencedColumnName = "cntrct_pbp_num")
  private Contract contract;

  @Nullable
  @ManyToOne
  @JoinColumn(
      name = "clm_srvc_prvdr_gnrc_id_num",
      insertable = false,
      updatable = false,
      referencedColumnName = "prvdr_npi_num")
  private ProviderHistory serviceProviderHistory;

  @Nullable
  @ManyToOne
  @JoinColumn(
      name = "clm_atndg_prvdr_npi_num",
      insertable = false,
      updatable = false,
      referencedColumnName = "prvdr_npi_num")
  private ProviderHistory attendingProviderHistory;

  private Optional<ProviderHistory> getAttendingProviderHistory() {
    return Optional.ofNullable(attendingProviderHistory);
  }

  @Nullable
  @ManyToOne
  @JoinColumn(
      name = "clm_oprtg_prvdr_npi_num",
      insertable = false,
      updatable = false,
      referencedColumnName = "prvdr_npi_num")
  private ProviderHistory operatingProviderHistory;

  private Optional<ProviderHistory> getOperatingProviderHistory() {
    return Optional.ofNullable(operatingProviderHistory);
  }

  @Nullable
  @ManyToOne
  @JoinColumn(
      name = "prvdr_blg_prvdr_npi_num",
      insertable = false,
      updatable = false,
      referencedColumnName = "prvdr_npi_num")
  private ProviderHistory billingProviderHistory;

  private Optional<ProviderHistory> getBillingProviderHistory() {
    return Optional.ofNullable(billingProviderHistory);
  }

  @Nullable
  @ManyToOne
  @JoinColumn(
      name = "clm_othr_prvdr_npi_num",
      insertable = false,
      updatable = false,
      referencedColumnName = "prvdr_npi_num")
  private ProviderHistory otherProviderHistory;

  private Optional<ProviderHistory> getOtherProviderHistory() {
    return Optional.ofNullable(otherProviderHistory);
  }

  @Nullable
  @ManyToOne
  @JoinColumn(
      name = "clm_rndrg_prvdr_npi_num",
      insertable = false,
      updatable = false,
      referencedColumnName = "prvdr_npi_num")
  private ProviderHistory renderingProviderHistory;

  private Optional<ProviderHistory> getRenderingProviderHistory() {
    return Optional.ofNullable(renderingProviderHistory);
  }

  @Nullable
  @ManyToOne
  @JoinColumn(
      name = "prvdr_prscrbng_prvdr_npi_num",
      insertable = false,
      updatable = false,
      referencedColumnName = "prvdr_npi_num")
  private ProviderHistory prescribingProviderHistory;

  private Optional<ProviderHistory> getPrescribingProviderHistory() {
    return Optional.ofNullable(prescribingProviderHistory);
  }

  @Nullable
  @ManyToOne
  @JoinColumn(
      name = "prvdr_rfrg_prvdr_npi_num",
      insertable = false,
      updatable = false,
      referencedColumnName = "prvdr_npi_num")
  private ProviderHistory referringProviderHistory;

  private Optional<ProviderHistory> getReferringProviderHistory() {
    return Optional.ofNullable(referringProviderHistory);
  }

  Optional<ClaimInstitutional> getClaimInstitutional() {
    return Optional.ofNullable(claimInstitutional);
  }

  Optional<ClaimProfessional> getClaimProfessional() {
    return Optional.ofNullable(claimProfessional);
  }

  Optional<ClaimFiss> getClaimFiss() {
    return Optional.ofNullable(claimFiss);
  }

  private Optional<Contract> getContract() {
    return Optional.ofNullable(contract);
  }

  private Optional<ProviderHistory> getServiceProviderHistory() {
    return Optional.ofNullable(serviceProviderHistory);
  }

  /**
   * Accessor for institutional DRG code, if this is an institutional claim.
   *
   * @return optional DRG code
   */
  public Optional<Integer> getDrgCode() {
    return getClaimInstitutional()
        .flatMap(i -> i.getSupportingInfo().getDiagnosisDrgCode().getDiagnosisDrgCode());
  }

  /**
   * Convert the claim info to a FHIR ExplanationOfBenefit.
   *
   * @param securityStatus securityStatus
   * @return ExplanationOfBenefit
   */
  public ExplanationOfBenefit toFhir(ClaimSecurityStatus securityStatus) {
    var eob = new ExplanationOfBenefit();
    eob.setId(String.valueOf(claimUniqueId));
    eob.setPatient(PatientReferenceFactory.toFhir(beneficiary.getXrefSk()));
    eob.setStatus(ExplanationOfBenefit.ExplanationOfBenefitStatus.ACTIVE);
    eob.setUse(ExplanationOfBenefit.Use.CLAIM);
    eob.setType(claimTypeCode.toFhirType());
    claimTypeCode.toFhirSubtype().ifPresent(eob::setSubType);

    var latestTs = getMostRecentUpdated();
    eob.setMeta(meta.toFhir(claimTypeCode, claimSourceId, securityStatus, latestTs));
    eob.setIdentifier(identifiers.toFhir());
    eob.setBillablePeriod(billablePeriod.toFhir());
    eob.setCreated(DateUtil.toDate(claimEffectiveDate));
    claimTypeCode
        .toFhirInsurerPartAB()
        .ifPresent(
            i -> {
              eob.addContained(i);
              eob.setInsurer(new Reference(i));
            });
    getContract()
        .flatMap(Contract::getContractName)
        .flatMap(name -> claimTypeCode.toFhirInsurerPartD(name))
        .ifPresent(
            i -> {
              eob.addContained(i);
              eob.setInsurer(new Reference(i));
            });
    var institutional = getClaimInstitutional();
    Stream.of(
            claimExtensions.toFhir(),
            institutional.map(i -> i.getExtensions().toFhir()).orElse(List.of()),
            List.of(claimDateSignature.getClaimProcessDate().toFhir()))
        .flatMap(Collection::stream)
        .forEach(eob::addExtension);

    claimItems.forEach(
        item -> {
          item.getClaimLine().toFhir(item).ifPresent(eob::addItem);
          item.getClaimLine()
              .getClaimRenderingProvider()
              .toFhirCareTeam(
                  item.getClaimLine().getClaimLineNumber(), getRenderingProviderHistory())
              .ifPresent(
                  c -> {
                    eob.addCareTeam(c.careTeam());
                    eob.addContained(c.practitioner());
                  });
          item.getClaimProcedure().toFhirProcedure().ifPresent(eob::addProcedure);
          item.getClaimProcedure()
              .toFhirDiagnosis(item.getClaimItemId().getBfdRowId(), claimTypeCode)
              .ifPresent(eob::addDiagnosis);
          item.getClaimLineProfessional()
              .flatMap(i -> i.toFhirObservation(item.getClaimItemId().getBfdRowId()))
              .ifPresent(eob::addContained);
        });

    getBillingProviderHistory()
        .flatMap(ph -> billingProvider.toFhir(ph))
        .ifPresent(
            p -> {
              eob.addContained(p);
              eob.setProvider(new Reference(p));
            });

    getServiceProviderHistory()
        .flatMap(ProviderHistory::toFhirNpiTypePartD)
        .ifPresent(
            p -> {
              eob.addContained(p);
              eob.setProvider(new Reference(p));
            });

    claimSourceId.toFhirOutcome().ifPresent(eob::setOutcome);
    claimTypeCode.toFhirOutcome().ifPresent(eob::setOutcome);
    getClaimFiss().flatMap(f -> f.toFhirOutcome(claimTypeCode)).ifPresent(eob::setOutcome);

    var supportingInfoFactory = new SupportingInfoFactory();
    var recordTypeCodes = claimRecordType.toFhir(supportingInfoFactory);

    var initialSupportingInfo =
        Stream.concat(
                Stream.of(
                    bloodPints.toFhir(supportingInfoFactory),
                    nchPrimaryPayorCode.toFhir(supportingInfoFactory),
                    typeOfBillCode.toFhir(supportingInfoFactory)),
                recordTypeCodes)
            .toList();

    var claimRxSupportingInfo =
        getAllClaimRxSupportingInfo().stream()
            .flatMap(rxSupportingInfo -> rxSupportingInfo.toFhir(supportingInfoFactory).stream())
            .toList();

    var claimLineRxNumbers =
        claimItems.stream()
            .map(ClaimItem::getClaimLineRxNum)
            .map(claimLineRxNumber -> claimLineRxNumber.toFhir(supportingInfoFactory))
            .flatMap(Optional::stream)
            .toList();

    Stream.of(
            initialSupportingInfo,
            claimDateSignature.getSupportingInfo().toFhir(supportingInfoFactory),
            institutional
                .map(i -> i.getSupportingInfo().toFhir(supportingInfoFactory))
                .orElse(List.of()),
            claimRxSupportingInfo,
            claimLineRxNumbers)
        .flatMap(Collection::stream)
        .forEach(eob::addSupportingInfo);

    careTeam
        .toFhir(
            getAttendingProviderHistory(),
            getOperatingProviderHistory(),
            getOtherProviderHistory(),
            getRenderingProviderHistory(),
            getPrescribingProviderHistory(),
            getReferringProviderHistory())
        .forEach(
            c -> {
              eob.addCareTeam(c.careTeam());
              eob.addContained(c.practitioner());
            });

    institutional.ifPresent(
        i -> {
          eob.addAdjudication(i.getPpsDrgWeight().toFhir());
          eob.addBenefitBalance(
              benefitBalance.toFhir(i.getBenefitBalanceInstitutional(), getClaimValues()));
        });

    var insurance = new ExplanationOfBenefit.InsuranceComponent();
    insurance.setFocal(true);
    claimRecordType.toFhirReference(claimTypeCode).ifPresent(insurance::setCoverage);

    claimTypeCode.toFhirInsurance().ifPresent(eob::addInsurance);
    claimTypeCode
        .toFhirPartDInsurance(contractNumber, contractPbpNumber)
        .ifPresent(eob::addInsurance);
    adjudicationCharge.toFhir().forEach(eob::addTotal);
    eob.setPayment(claimPaymentAmount.toFhir());

    getClaimProfessional()
        .ifPresent(
            professional -> {
              eob.getExtension().addAll(professional.toFhirExtension());
              eob.addTotal(professional.toFhirTotal());
              eob.addBenefitBalance(benefitBalance.toFhir());
            });

    return sortedEob(eob);
  }

  private List<ClaimValue> getClaimValues() {
    return claimItems.stream().map(ClaimItem::getClaimValue).toList();
  }

  private List<ClaimRxSupportingInfo> getAllClaimRxSupportingInfo() {
    return claimItems.stream()
        .map(ClaimItem::getClaimLineRx)
        .flatMap(Optional::stream)
        .map(ClaimLineRx::getClaimRxSupportingInfo)
        .toList();
  }

  private ZonedDateTime getMostRecentUpdated() {
    // Collect timestamps (claim + child entities) then pick the max.
    var ciStream = getClaimInstitutional().map(ClaimInstitutional::getBfdUpdatedTimestamp).stream();
    var cfStream = getClaimFiss().map(ClaimFiss::getBfdUpdatedTimestamp).stream();
    var cdsStream = Stream.of(claimDateSignature.getBfdUpdatedTimestamp());
    var itemsStream = claimItems.stream().flatMap(ClaimItem::streamTimestamps);

    return Stream.of(
            Stream.of(meta.getUpdatedTimestamp()), ciStream, cfStream, cdsStream, itemsStream)
        .flatMap(s -> s)
        .max(Comparator.naturalOrder())
        .orElse(meta.getUpdatedTimestamp());
  }

  private ExplanationOfBenefit sortedEob(ExplanationOfBenefit eob) {
    eob.getCareTeam()
        .sort(Comparator.comparing(ExplanationOfBenefit.CareTeamComponent::getSequence));
    eob.getProcedure()
        .sort(Comparator.comparing(ExplanationOfBenefit.ProcedureComponent::getSequence));
    eob.getDiagnosis()
        .sort(Comparator.comparing(ExplanationOfBenefit.DiagnosisComponent::getSequence));
    eob.getSupportingInfo()
        .sort(
            Comparator.comparing(ExplanationOfBenefit.SupportingInformationComponent::getSequence));
    eob.getItem().sort(Comparator.comparing(ExplanationOfBenefit.ItemComponent::getSequence));
    // Sorting the extensions isn't strictly necessary, but it can interfere with the snapshot tests
    // if the order changes.
    eob.getExtension().sort(Comparator.comparing(Extension::getUrl));
    return eob;
  }
}
