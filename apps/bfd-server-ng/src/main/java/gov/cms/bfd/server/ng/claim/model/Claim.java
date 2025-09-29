package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.beneficiary.model.BeneficiarySimple;
import gov.cms.bfd.server.ng.util.DateUtil;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import lombok.Getter;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
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

  @Column(name = "clm_nrln_ric_cd")
  private Optional<ClaimNearLineRecordTypeCode> claimNearLineRecordTypeCode;

  @Column(name = "clm_ric_cd")
  private Optional<ClaimRecordTypeCode> claimRecordTypeCode;

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

  @OneToMany(fetch = FetchType.EAGER)
  @JoinColumn(name = "clm_uniq_id")
  private Set<ClaimItem> claimItems;

  private Optional<ClaimInstitutional> getClaimInstitutional() {
    return Optional.ofNullable(claimInstitutional);
  }

  Optional<ClaimFiss> getClaimFiss() {
    return Optional.ofNullable(claimFiss);
  }

  /**
   * Convert the claim info to a FHIR ExplanationOfBenefit.
   *
   * @return ExplanationOfBenefit
   */
  public ExplanationOfBenefit toFhir() {
    var eob = new ExplanationOfBenefit();
    eob.setId(String.valueOf(claimUniqueId));
    eob.setPatient(PatientReferenceFactory.toFhir(beneficiary.getXrefSk()));
    eob.setStatus(ExplanationOfBenefit.ExplanationOfBenefitStatus.ACTIVE);
    eob.setUse(ExplanationOfBenefit.Use.CLAIM);
    eob.setType(claimTypeCode.toFhirType());
    claimTypeCode.toFhirSubtype().ifPresent(eob::setSubType);

    // Use the most recent bfd_updated_ts across the claim and its related child
    // tables when setting lastUpdated.
    var overriddenMeta =
        meta.toFhir(claimTypeCode, claimSourceId)
            .setLastUpdated(DateUtil.toDate(getMostRecentUpdated()));
    eob.setMeta(overriddenMeta);
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
          item.getClaimProcedure().toFhirProcedure().ifPresent(eob::addProcedure);
          item.getClaimProcedure()
              .toFhirDiagnosis(item.getClaimItemId().getBfdRowId())
              .ifPresent(eob::addDiagnosis);
        });
    billingProvider
        .toFhir(claimTypeCode)
        .ifPresent(
            p -> {
              eob.addContained(p);
              eob.setProvider(new Reference(p));
            });

    // Each toFhirOutcome() evaluates independently, but their logic is mutually exclusive
    // based on claim type. At most one Optional will be non-empty, so only one call
    // will actually set EOB.outcome.
    claimSourceId.toFhirOutcome().ifPresent(eob::setOutcome);
    claimTypeCode.toFhirOutcome().ifPresent(eob::setOutcome);
    getClaimFiss().flatMap(f -> f.toFhirOutcome(claimTypeCode)).ifPresent(eob::setOutcome);

    var supportingInfoFactory = new SupportingInfoFactory();
    var recordTypeCodes =
        Stream.concat(
            claimRecordTypeCode.stream()
                .map(recordTypeCode -> recordTypeCode.toFhir(supportingInfoFactory)),
            claimNearLineRecordTypeCode.stream()
                .map(
                    nearLineRecordTypeCode ->
                        nearLineRecordTypeCode.toFhir(supportingInfoFactory)));
    var initialSupportingInfo =
        Stream.concat(
                Stream.of(
                    bloodPints.toFhir(supportingInfoFactory),
                    nchPrimaryPayorCode.toFhir(supportingInfoFactory),
                    typeOfBillCode.toFhir(supportingInfoFactory)),
                recordTypeCodes)
            .toList();

    Stream.of(
            initialSupportingInfo,
            claimDateSignature.getSupportingInfo().toFhir(supportingInfoFactory),
            institutional
                .map(i -> i.getSupportingInfo().toFhir(supportingInfoFactory))
                .orElse(List.of()))
        .flatMap(Collection::stream)
        .forEach(eob::addSupportingInfo);

    careTeam
        .toFhir()
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

    claimTypeCode.toFhirInsurance().ifPresent(eob::addInsurance);
    eob.addTotal(adjudicationCharge.toFhir());
    eob.setPayment(claimPaymentAmount.toFhir());

    return sortedEob(eob);
  }

  private List<ClaimValue> getClaimValues() {
    return claimItems.stream().map(ClaimItem::getClaimValue).toList();
  }

  private ZonedDateTime getMostRecentUpdated() {
    // Collect timestamps (claim + child entities) then pick the max.
    var ciStream =
        Optional.ofNullable(claimInstitutional)
            .map(ClaimInstitutional::getBfdUpdatedTimestamp)
            .stream();
    var cfStream = getClaimFiss().map(ClaimFiss::getBfdUpdatedTimestamp).stream();
    var cdsStream =
        Optional.ofNullable(claimDateSignature)
            .map(ClaimDateSignature::getBfdUpdatedTimestamp)
            .stream();

    var itemsStream =
        claimItems == null
            ? Stream.<ZonedDateTime>empty()
            : claimItems.stream().flatMap(this::streamItemTimestamps);

    var allCandidates =
        Stream.concat(
            Stream.of(meta.getUpdatedTimestamp()),
            Stream.concat(
                ciStream, Stream.concat(cfStream, Stream.concat(cdsStream, itemsStream))));

    return allCandidates
        .filter(java.util.Objects::nonNull)
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
    return eob;
  }

  private Stream<ZonedDateTime> streamItemTimestamps(ClaimItem item) {
    var itemTs = Optional.ofNullable(item.getBfdUpdatedTimestamp()).stream();
    var lineInstitutionalStream =
        item.getClaimLineInstitutional()
            .map(
                cli -> {
                  var cliTs = Optional.ofNullable(cli.getBfdUpdatedTimestamp()).stream();
                  var ansiTs =
                      cli.getAnsiSignature()
                          .map(ansi -> Optional.ofNullable(ansi.getBfdUpdatedTimestamp()).stream())
                          .orElseGet(Stream::empty);
                  return Stream.concat(cliTs, ansiTs);
                })
            .orElseGet(Stream::empty);
    return Stream.concat(itemTs, lineInstitutionalStream);
  }
}
