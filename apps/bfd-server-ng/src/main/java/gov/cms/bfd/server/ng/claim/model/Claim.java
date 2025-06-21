package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.DateUtil;
import gov.cms.bfd.server.ng.beneficiary.model.Beneficiary;
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
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import lombok.Getter;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.Reference;

/** Claim table. */
@Entity
@Getter
@Table(name = "claim", schema = "idr")
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

  @OneToOne
  @JoinColumn(name = "bene_sk")
  private Beneficiary beneficiary;

  @OneToOne
  @JoinColumn(name = "clm_dt_sgntr_sk")
  private ClaimDateSignature claimDateSignature;

  @OneToOne
  @JoinColumn(name = "clm_uniq_id")
  private ClaimInstitutional claimInstitutional;

  @OneToMany(fetch = FetchType.EAGER)
  @JoinColumn(name = "clm_uniq_id")
  private List<ClaimLine> claimLines;

  @OneToMany(fetch = FetchType.EAGER)
  @JoinColumn(name = "clm_uniq_id")
  private List<ClaimValue> claimValues;

  @OneToMany(fetch = FetchType.EAGER)
  @JoinColumn(name = "clm_uniq_id")
  private List<ClaimProcedure> claimProcedures;

  /**
   * Convert the claim info to a FHIR ExplanationOfBenefit.
   *
   * @return ExplanationOfBenefit
   */
  public ExplanationOfBenefit toFhir() {
    var eob = new ExplanationOfBenefit();
    eob.setStatus(ExplanationOfBenefit.ExplanationOfBenefitStatus.ACTIVE);
    eob.setUse(ExplanationOfBenefit.Use.CLAIM);
    eob.setType(claimTypeCode.toFhirType());
    claimTypeCode.toFhirSubtype().ifPresent(eob::setSubType);

    eob.setMeta(meta.toFhir(claimTypeCode, claimSourceId));
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

    Stream.of(
            claimExtensions.toFhir(),
            claimInstitutional.getExtensions().toFhir(),
            List.of(claimDateSignature.getClaimProcessDate().toFhir()))
        .flatMap(Collection::stream)
        .forEach(eob::addExtension);

    for (var procedure : claimProcedures) {
      procedure.toFhirProcedure().ifPresent(eob::addProcedure);
      procedure.toFhirDiagnosis().ifPresent(eob::addDiagnosis);
    }

    billingProvider
        .toFhir(claimTypeCode)
        .ifPresent(
            p -> {
              eob.addContained(p);
              eob.setProvider(new Reference(p));
            });

    var supportingInfoFactory = new SupportingInfoFactory();
    var initialSupportingInfo =
        List.of(
            bloodPints.toFhir(supportingInfoFactory),
            nchPrimaryPayorCode.toFhir(supportingInfoFactory),
            typeOfBillCode.toFhir(supportingInfoFactory));
    Stream.of(
            initialSupportingInfo,
            claimDateSignature.getSupportingInfo().toFhir(supportingInfoFactory),
            claimInstitutional.getSupportingInfo().toFhir(supportingInfoFactory))
        .flatMap(Collection::stream)
        .forEach(eob::addSupportingInfo);

    claimLines.stream().map(ClaimLine::toFhir).forEach(eob::addItem);
    careTeam
        .toFhir()
        .forEach(
            c -> {
              eob.addCareTeam(c.careTeam());
              eob.addContained(c.practitioner());
            });
    eob.addAdjudication(claimInstitutional.getPpsDrgWeight().toFhir());
    eob.addBenefitBalance(
        benefitBalance.toFhir(claimInstitutional.getBenefitBalanceInstitutional(), claimValues));
    claimTypeCode.toFhirInsurance().ifPresent(eob::addInsurance);
    eob.addAdjudication(adjudicationCharge.toFhir());
    return eob;
  }
}
