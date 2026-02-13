package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.Getter;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** Claim ANSI signature table. */
@MappedSuperclass
@Getter
public class ClaimAnsiSignatureBase {

  @Column(name = "clm_1_rev_cntr_ansi_grp_cd")
  private Optional<RevenueCenterAnsiGroupCode> revenueCenterAnsiGroupCode1;

  @Column(name = "clm_2_rev_cntr_ansi_grp_cd")
  private Optional<RevenueCenterAnsiGroupCode> revenueCenterAnsiGroupCode2;

  @Column(name = "clm_3_rev_cntr_ansi_grp_cd")
  private Optional<RevenueCenterAnsiGroupCode> revenueCenterAnsiGroupCode3;

  @Column(name = "clm_4_rev_cntr_ansi_grp_cd")
  private Optional<RevenueCenterAnsiGroupCode> revenueCenterAnsiGroupCode4;

  @Column(name = "clm_1_rev_cntr_ansi_rsn_cd")
  private Optional<RevenueCenterAnsiReasonCode> revenueCenterAnsiReasonCode1;

  @Column(name = "clm_2_rev_cntr_ansi_rsn_cd")
  private Optional<RevenueCenterAnsiReasonCode> revenueCenterAnsiReasonCode2;

  @Column(name = "clm_3_rev_cntr_ansi_rsn_cd")
  private Optional<RevenueCenterAnsiReasonCode> revenueCenterAnsiReasonCode3;

  @Column(name = "clm_4_rev_cntr_ansi_rsn_cd")
  private Optional<RevenueCenterAnsiReasonCode> revenueCenterAnsiReasonCode4;

  List<ExplanationOfBenefit.AdjudicationComponent> toFhir() {
    return Stream.of(
            toAdjudicationComponent(revenueCenterAnsiGroupCode1, revenueCenterAnsiReasonCode1),
            toAdjudicationComponent(revenueCenterAnsiGroupCode2, revenueCenterAnsiReasonCode2),
            toAdjudicationComponent(revenueCenterAnsiGroupCode3, revenueCenterAnsiReasonCode3),
            toAdjudicationComponent(revenueCenterAnsiGroupCode4, revenueCenterAnsiReasonCode4))
        .flatMap(Optional::stream)
        .toList();
  }

  private Optional<ExplanationOfBenefit.AdjudicationComponent> toAdjudicationComponent(
      Optional<RevenueCenterAnsiGroupCode> groupOpt,
      Optional<RevenueCenterAnsiReasonCode> reasonOpt) {
    var groupCodings =
        groupOpt.flatMap(RevenueCenterAnsiGroupCode::toFhirCodings).orElse(List.of());

    if (groupCodings.isEmpty()) {
      return Optional.empty();
    }

    var codings =
        Stream.concat(
                groupCodings.stream(),
                reasonOpt
                    .flatMap(RevenueCenterAnsiReasonCode::toFhirCodings)
                    .orElse(List.of())
                    .stream())
            .toList();

    return Optional.of(
        new ExplanationOfBenefit.AdjudicationComponent()
            .setCategory(
                new CodeableConcept(
                    new Coding()
                        .setSystem(SystemUrls.CARIN_CODE_SYSTEM_ADJUDICATION_DISCRIMINATOR)
                        .setCode("adjustmentreason")
                        .setDisplay("Adjustment Reason")))
            .setReason(new CodeableConcept().setCoding(codings)));
  }
}
