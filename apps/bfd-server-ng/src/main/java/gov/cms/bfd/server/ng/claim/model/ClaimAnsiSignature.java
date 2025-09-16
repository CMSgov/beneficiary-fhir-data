package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** Claim ANSI signature table. */
@Entity
@Table(name = "claim_ansi_signature", schema = "idr")
public class ClaimAnsiSignature {
  @Id
  @Column(name = "clm_ansi_sgntr_sk", insertable = false, updatable = false)
  private long ansiSignatureSk;

  @Column(name = "clm_1_rev_cntr_ansi_rsn_cd")
  private Optional<String> revenueCenterAnsiReasonCode1;

  @Column(name = "clm_2_rev_cntr_ansi_rsn_cd")
  private Optional<String> revenueCenterAnsiReasonCode2;

  @Column(name = "clm_3_rev_cntr_ansi_rsn_cd")
  private Optional<String> revenueCenterAnsiReasonCode3;

  @Column(name = "clm_4_rev_cntr_ansi_rsn_cd")
  private Optional<String> revenueCenterAnsiReasonCode4;

  List<ExplanationOfBenefit.AdjudicationComponent> toFhir() {
    var codes =
        Stream.of(
                revenueCenterAnsiReasonCode1,
                revenueCenterAnsiReasonCode2,
                revenueCenterAnsiReasonCode3,
                revenueCenterAnsiReasonCode4)
            .flatMap(Optional::stream);
    return codes.map(this::toAdjudicationComponent).toList();
  }

  private ExplanationOfBenefit.AdjudicationComponent toAdjudicationComponent(String code) {
    return new ExplanationOfBenefit.AdjudicationComponent()
        .setCategory(
            new CodeableConcept(
                new Coding()
                    .setSystem(SystemUrls.CARIN_CODE_SYSTEM_ADJUDICATION_DISCRIMINATOR)
                    .setCode("adjustmentreason")
                    .setDisplay("Adjustment Reason")))
        .setReason(
            new CodeableConcept(
                new Coding()
                    .setSystem(SystemUrls.X12_CLAIM_ADJUSTMENT_REASON_CODES)
                    .setCode(code)));
  }
}
