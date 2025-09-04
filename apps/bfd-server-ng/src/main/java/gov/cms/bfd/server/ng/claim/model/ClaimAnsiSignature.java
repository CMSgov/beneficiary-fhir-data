package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.SystemUrls;
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

  @Column(name = "clm_1_rev_cntr_ansi_grp_cd")
  private Optional<String> revenueCenterAnsiGroupCode1;

  @Column(name = "clm_2_rev_cntr_ansi_grp_cd")
  private Optional<String> revenueCenterAnsiGroupCode2;

  @Column(name = "clm_3_rev_cntr_ansi_grp_cd")
  private Optional<String> revenueCenterAnsiGroupCode3;

  @Column(name = "clm_4_rev_cntr_ansi_grp_cd")
  private Optional<String> revenueCenterAnsiGroupCode4;

  List<ExplanationOfBenefit.AdjudicationComponent> toFhir() {
    return Stream.of(
            new Pair(revenueCenterAnsiGroupCode1, revenueCenterAnsiReasonCode1),
            new Pair(revenueCenterAnsiGroupCode2, revenueCenterAnsiReasonCode2),
            new Pair(revenueCenterAnsiGroupCode3, revenueCenterAnsiReasonCode3),
            new Pair(revenueCenterAnsiGroupCode4, revenueCenterAnsiReasonCode4))
        .flatMap(
            pair -> pair.group().map(code -> new Pair(Optional.of(code), pair.reason())).stream())
        .filter(pair -> pair.group().map(code -> code.length() == 2).orElse(false))
        .map(pair -> toAdjudicationComponent(pair.group().get(), String.valueOf(pair.reason())))
        .toList();
  }

  private ExplanationOfBenefit.AdjudicationComponent toAdjudicationComponent(
      String groupCode, String reasonCode) {
    return new ExplanationOfBenefit.AdjudicationComponent()
        .setCategory(
            new CodeableConcept(
                new Coding()
                    .setSystem(SystemUrls.CARIN_CODE_SYSTEM_ADJUDICATION_DISCRIMINATOR)
                    .setCode("adjustmentreason")
                    .setDisplay("Adjustment Reason")))
        .setReason(
            new CodeableConcept()
                .addCoding(
                    new Coding()
                        .setSystem(SystemUrls.X12_CLAIM_ADJUSTMENT_REASON_CODES)
                        .setCode(groupCode))
                .addCoding(
                    new Coding()
                        .setSystem(SystemUrls.X12_CLAIM_ADJUSTMENT_REASON_CODES)
                        .setCode(reasonCode)));
  }

  private record Pair(Optional<String> group, Optional<String> reason) {}
}
