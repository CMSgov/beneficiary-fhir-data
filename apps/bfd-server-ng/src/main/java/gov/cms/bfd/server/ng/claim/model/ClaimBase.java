package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.ClaimSecurityStatus;
import gov.cms.bfd.server.ng.beneficiary.model.BeneficiarySimple;
import gov.cms.bfd.server.ng.util.DateUtil;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Transient;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.Optional;
import java.util.SortedSet;
import java.util.stream.Stream;
import lombok.Getter;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Reference;

/**
 * Claim table. Suppress generic wildcard type warning. Wildcard is intentional since each ClaimBase
 * subtype has its own ClaimItemBase subtype.
 */
@Getter
@MappedSuperclass
@SuppressWarnings({"JpaAttributeTypeInspection"})
public abstract class ClaimBase<T extends ClaimItemBase> {
  @Id
  @Column(name = "clm_uniq_id", insertable = false, updatable = false)
  private long claimUniqueId;

  @Column(name = "clm_src_id")
  private ClaimSourceId claimSourceId;

  @Column(name = "meta_src_sk")
  private MetaSourceSk metaSourceSk;

  @Column(name = "clm_type_cd")
  private ClaimTypeCode claimTypeCode;

  @Column(name = "clm_efctv_dt")
  private LocalDate claimEffectiveDate;

  @Column(name = "clm_finl_actn_ind")
  private ClaimFinalAction finalAction;

  @Column(name = "clm_adjstmt_type_cd")
  private Optional<ClaimAdjustmentTypeCode> claimAdjustmentTypeCode;

  @Embedded private Meta meta;
  @Embedded private Identifiers identifiers;
  @Embedded private BillablePeriod billablePeriod;
  @Embedded private ClaimIDRLoadDate claimIDRLoadDate;

  @OneToOne
  @JoinColumn(name = "bene_sk")
  private BeneficiarySimple beneficiary;

  @Transient protected SupportingInfoFactory supportingInfoFactory = new SupportingInfoFactory();

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
    claimTypeCode.toFhirAdjudication().ifPresent(eob::addAdjudication);

    eob.setMeta(
        meta.toFhir(claimTypeCode, claimSourceId, securityStatus, finalAction, metaSourceSk));
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

    claimSourceId.toFhirOutcome().ifPresent(eob::setOutcome);

    var initialSupportingInfo =
        Stream.of(
                claimAdjustmentTypeCode.map(c -> c.toFhir(supportingInfoFactory)),
                Optional.of(claimIDRLoadDate.toFhir(supportingInfoFactory)))
            .flatMap(Optional::stream)
            .toList();

    initialSupportingInfo.forEach(eob::addSupportingInfo);

    return sortedEob(eob);
  }

  protected ExplanationOfBenefit sortedEob(ExplanationOfBenefit eob) {
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

  /**
   * Returns the set of claim items associated with this claim.
   *
   * @return a sorted set of claim items.
   */
  public abstract SortedSet<T> getClaimItems();

  /**
   * Returns the Diagnosis-Related Group (DRG) code for this claim, if available.
   *
   * @return the DRG code
   */
  public abstract Optional<Integer> getDrgCode();
}
