package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.DateUtil;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

import java.time.LocalDate;

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

  @ManyToOne private ClaimDateSignature claimDateSignature;
  @OneToOne private ClaimInstitutional claimInstitutional;

  @Embedded private Meta meta;
  @Embedded private Identifiers identifiers;
  @Embedded private BillablePeriod billablePeriod;
  @Embedded private ClaimContractorNumber claimContractorNumber;
  @Embedded private ClaimRecordTypeCode claimRecordTypeCode;
  @Embedded private ClaimDispositionCode claimDispositionCode;
  @Embedded private ClaimPaymentAmount claimPaymentAmount;
  @Embedded private ClaimProcessDate claimProcessDate;
  @Embedded private BillingProvider billingProvider;
  @Embedded private BloodPints bloodPints;
  @Embedded private NchPrimaryPayorCode nchPrimaryPayorCode;
  @Embedded private TypeOfBillCode typeOfBillCode;

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
    eob.addExtension(claimContractorNumber.toFhir());
    eob.addExtension(claimRecordTypeCode.toFhir());
    eob.addExtension(claimDispositionCode.toFhir());
    eob.setPayment(claimPaymentAmount.toFhir());
    eob.addExtension(claimProcessDate.toFhir());
    billingProvider
        .toFhir(claimTypeCode)
        .ifPresent(
            p -> {
              eob.addContained(p);
              eob.setProvider(p.castToReference(eob));
            });

    var supportingInfoFactory = new SupportingInfoFactory();
    eob.addSupportingInfo(bloodPints.toFhir(supportingInfoFactory));
    eob.addSupportingInfo(nchPrimaryPayorCode.toFhir(supportingInfoFactory));
    eob.addSupportingInfo(typeOfBillCode.toFhir(supportingInfoFactory));
    for (var supportingInfo : claimDateSignature.toFhir(supportingInfoFactory)) {
      eob.addSupportingInfo(supportingInfo);
    }
    for (var supportingInfo : claimInstitutional.toFhir(supportingInfoFactory)) {
      eob.addSupportingInfo(supportingInfo);
    }
    eob.addItem();
    return eob;
  }
}
