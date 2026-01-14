package gov.cms.bfd.server.ng.claim.model;

import static gov.cms.bfd.server.ng.claim.model.ClaimSubtype.PDE;

import gov.cms.bfd.server.ng.ClaimSecurityStatus;
import gov.cms.bfd.server.ng.beneficiary.model.BeneficiarySimple;
import gov.cms.bfd.server.ng.util.DateUtil;
import gov.cms.bfd.server.ng.util.SequenceGenerator;
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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.SortedSet;
import java.util.stream.Stream;
import lombok.Getter;
import org.hl7.fhir.r4.model.*;
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

  @Column(name = "clm_sbmt_frmt_cd")
  private Optional<ClaimSubmissionFormatCode> claimFormatCode;

  @Column(name = "clm_finl_actn_ind")
  private ClaimFinalAction finalAction;

  @Column(name = "clm_cntrctr_num")
  private Optional<ClaimContractorNumber> claimContractorNumber;

  @Column(name = "clm_disp_cd")
  private Optional<ClaimDispositionCode> claimDispositionCode;

  @Column(name = "clm_query_cd")
  private Optional<ClaimQueryCode> claimQueryCode;

  @Column(name = "clm_adjstmt_type_cd")
  private Optional<ClaimAdjustmentTypeCode> claimAdjustmentTypeCode;

  @Embedded private Meta meta;
  @Embedded private Identifiers identifiers;
  @Embedded private BillablePeriod billablePeriod;
  @Embedded private BillingProvider billingProvider;
  @Embedded private BloodPints bloodPints;
  @Embedded private NchPrimaryPayorCode nchPrimaryPayorCode;
  @Embedded private TypeOfBillCode typeOfBillCode;
  @Embedded private CareTeam careTeam;
  @Embedded private AdjudicationCharge adjudicationCharge;
  @Embedded private ClaimPaymentAmount claimPaymentAmount;
  @Embedded private ClaimRecordType claimRecordType;
  @Embedded private ClaimIDRLoadDate claimIDRLoadDate;
  @Embedded private SubmitterContractNumber submitterContractNumber;
  @Embedded private SubmitterContractPBPNumber submitterContractPBPNumber;

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

  @Nullable
  @ManyToOne(fetch = FetchType.EAGER)
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
   * Accessor for institutional bene paid amount, if this is an institutional claim.
   *
   * @return optional institutional bene paid amount
   */
  public Optional<BigDecimal> getBenePaidAmount() {
    return getClaimInstitutional()
        .map(i -> i.getAdjudicationChargeInstitutional().getBenePaidAmount());
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

    eob.setMeta(meta.toFhir(claimTypeCode, claimSourceId, securityStatus, finalAction));
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
    var consolidatedDiagnoses = computeConsolidatedDiagnoses();

    claimItems.forEach(
        item -> {
          item.getClaimLine().toFhir(item, consolidatedDiagnoses).ifPresent(eob::addItem);

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
          item.getClaimLineProfessional()
              .flatMap(i -> i.toFhirObservation(item.getClaimItemId().getBfdRowId()))
              .ifPresent(eob::addContained);
        });
    var diagnosisSequenceGenerator = new SequenceGenerator();
    claimTypeCode
        .toContext()
        .ifPresent(
            ctx ->
                consolidatedDiagnoses.forEach(
                    d ->
                        d.toFhirDiagnosis(diagnosisSequenceGenerator, ctx)
                            .ifPresent(eob::addDiagnosis)));

    getBillingProviderHistory()
        .ifPresent(
            ph -> {
              var p = billingProvider.toFhir(ph);
              eob.addContained(p);
              eob.setProvider(new Reference(p));
            });

    getServiceProviderHistory()
        .ifPresent(
            ph -> {
              var p = ph.toFhirNpiType();
              eob.addContained(p);
              eob.setProvider(new Reference(p));
            });

    claimSourceId.toFhirOutcome().ifPresent(eob::setOutcome);
    claimTypeCode.toFhirOutcome().ifPresent(eob::setOutcome);
    getClaimFiss().flatMap(f -> f.toFhirOutcome(claimTypeCode)).ifPresent(eob::setOutcome);

    var supportingInfoFactory = new SupportingInfoFactory();
    var recordTypeCodes = claimRecordType.toFhir(supportingInfoFactory);

    var initialSupportingInfo =
        Stream.of(
                bloodPints.toFhir(supportingInfoFactory),
                nchPrimaryPayorCode.toFhir(supportingInfoFactory),
                typeOfBillCode.toFhir(supportingInfoFactory),
                claimContractorNumber.map(c -> c.toFhir(supportingInfoFactory)),
                claimDispositionCode.map(c -> c.toFhir(supportingInfoFactory)),
                claimQueryCode.map(c -> c.toFhir(supportingInfoFactory)),
                claimAdjustmentTypeCode.map(c -> c.toFhir(supportingInfoFactory)),
                Optional.of(claimIDRLoadDate.toFhir(supportingInfoFactory)))
            .flatMap(Optional::stream)
            .toList();

    var claimRxSupportingInfo =
        Stream.of(
                // claim rx header lvl
                claimFormatCode
                    .filter(c -> claimTypeCode.isClaimSubtype(PDE))
                    .map(c -> c.toFhir(supportingInfoFactory))
                    .stream(),
                submitterContractNumber.toFhir(supportingInfoFactory).stream(),
                submitterContractPBPNumber.toFhir(supportingInfoFactory).stream(),
                // claim rx line lvl
                getClaimLineRxSupportingInfo().stream()
                    .flatMap(rx -> rx.toFhir(supportingInfoFactory).stream()),
                // claim line rx num
                claimItems.stream()
                    .map(item -> item.getClaimLineRxNum().toFhir(supportingInfoFactory))
                    .flatMap(Optional::stream))
            .flatMap(s -> s)
            .toList();

    var claimRelatedConditionCds =
        claimItems.stream()
            .map(ClaimItem::getClaimRelatedCondition)
            .map(crc -> crc.toFhir(supportingInfoFactory))
            .flatMap(Optional::stream)
            .toList();

    var professional = getClaimProfessional();

    Stream.of(
            initialSupportingInfo,
            // In real data, this should only ever have one value, but we're explicitly
            // limiting it to be defensive.
            recordTypeCodes.limit(1).toList(),
            claimDateSignature.getSupportingInfo().toFhir(supportingInfoFactory),
            institutional
                .map(i -> i.getSupportingInfo().toFhir(supportingInfoFactory))
                .orElse(List.of()),
            professional
                .map(p -> p.getSupportingInfo().toFhir(supportingInfoFactory))
                .orElse(List.of()),
            claimRxSupportingInfo,
            claimRelatedConditionCds)
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
          var adjudicationChargeInstitutional = i.getAdjudicationChargeInstitutional();
          adjudicationChargeInstitutional.toFhir(getClaimValues()).forEach(eob::addAdjudication);
        });

    var insurance = new ExplanationOfBenefit.InsuranceComponent();
    insurance.setFocal(true);
    claimRecordType.toFhirReference(claimTypeCode).ifPresent(insurance::setCoverage);

    claimTypeCode.toFhirInsurance(claimRecordType).ifPresent(eob::addInsurance);
    claimTypeCode.toFhirPartDInsurance().ifPresent(eob::addInsurance);
    adjudicationCharge.toFhirTotal().forEach(eob::addTotal);
    getBenePaidAmount()
        .map(AdjudicationChargeType.BENE_PAID_AMOUNT::toFhirTotal)
        .ifPresent(eob::addTotal);
    adjudicationCharge.toFhirAdjudication().forEach(eob::addAdjudication);
    eob.setPayment(claimPaymentAmount.toFhir());

    professional.ifPresent(
        p -> {
          p.toFhirAdjudication().forEach(eob::addAdjudication);
          p.toFhirOutcome(claimTypeCode).ifPresent(eob::setOutcome);
        });

    return sortedEob(eob);
  }

  private List<ClaimValue> getClaimValues() {
    return claimItems.stream().map(ClaimItem::getClaimValue).toList();
  }

  private List<ClaimLineRxSupportingInfo> getClaimLineRxSupportingInfo() {
    return claimItems.stream()
        .map(ClaimItem::getClaimLineRx)
        .flatMap(Optional::stream)
        .map(ClaimLineRx::getClaimRxSupportingInfo)
        .toList();
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

  private List<ClaimProcedure> computeConsolidatedDiagnoses() {
    var claimContextOpt = claimTypeCode.toContext();
    if (claimContextOpt.isEmpty()) {
      return Collections.emptyList();
    }
    var claimContext = claimContextOpt.get();

    // Group the diagnoses by code + ICD indicator and sort them by rank.
    // We'll pick the first diagnosis from each group and discard the rest.
    var diagnosisMap = new LinkedHashMap<String, PriorityQueue<ClaimProcedure>>();
    var poaDiagnoses = new HashMap<String, String>();
    for (var item : claimItems) {
      var procedure = item.getClaimProcedure();
      var keyOpt = procedure.getDiagnosisKey();
      if (keyOpt.isEmpty()) {
        continue;
      }
      var key = keyOpt.get();
      procedure
          .getClaimPoaIndicator()
          .ifPresent(p -> poaDiagnoses.merge(key, p, (oldVal, newVal) -> oldVal + newVal));

      var queue =
          diagnosisMap.computeIfAbsent(
              key,
              _ ->
                  new PriorityQueue<ClaimProcedure>(
                      Comparator.comparing(a -> a.getDiagnosisPriority(claimContext).orElse(0))));

      queue.add(item.getClaimProcedure());
    }

    return diagnosisMap.values().stream()
        .map(
            d -> {
              var procedure = d.peek();
              // POA may not be set on the diagnosis we pick, but it may be present on one of the
              // duplicates.
              // Check these and set the POA indicator where applicable.
              var poaIndicator = poaDiagnoses.getOrDefault(procedure.getDiagnosisKey().get(), "");
              if (procedure.getClaimPoaIndicator().isEmpty() && !poaIndicator.isEmpty()) {
                procedure.setClaimPoaIndicator(poaIndicator);
              }
              return procedure;
            })
        .toList();
  }
}
