package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.DateUtil;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

@Entity
@Getter
@Table(name = "claim", schema = "idr")
public class Claim {
  @Column(name = "clm_uniq_id")
  private long claimUniqueId;

  @Column(name = "bene_sk")
  private long beneSk;

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

  @ManyToOne private ClaimDateSignature claimDateSignature;
  @OneToOne private ClaimInstitutional claimInstitutional;
  @ManyToMany private List<ClaimLine> claimLines;

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
              eob.setInsurer(i.castToReference(eob));
            });
    eob.setExtension(
        Stream.of(claimExtensions.toFhir(), claimInstitutional.getExtensions().toFhir())
            .flatMap(Collection::stream)
            .toList());
    billingProvider
        .toFhir(claimTypeCode)
        .ifPresent(
            p -> {
              eob.addContained(p);
              eob.setProvider(p.castToReference(eob));
            });

    var supportingInfoFactory = new SupportingInfoFactory();
    var initialSupportingInfo =
        List.of(
            bloodPints.toFhir(supportingInfoFactory),
            nchPrimaryPayorCode.toFhir(supportingInfoFactory),
            typeOfBillCode.toFhir(supportingInfoFactory));
    Stream.of(
            initialSupportingInfo,
            claimDateSignature.toFhir(supportingInfoFactory),
            claimInstitutional.getSupportingInfo().toFhir(supportingInfoFactory))
        .flatMap(Collection::stream)
        .forEach(eob::addSupportingInfo);

    eob.setItem(claimLines.stream().map(ClaimLine::toFhir).toList());
    careTeam
        .toFhir(eob)
        .forEach(
            c -> {
              eob.addCareTeam(c.careTeam());
              eob.addContained(c.practitioner());
            });
    eob.addAdjudication(claimInstitutional.getPpsDrgWeight().toFhir());
    return eob;
  }
}
