package gov.cms.bfd.server.ng.claim.model.common;

import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.Identifier;

@Embeddable
class Identifiers {
  @Column(name = "clm_uniq_id")
  private long claimUniqueId;

  @Column(name = "clm_cntl_num")
  private Optional<String> claimControlNumber;

  @Column(name = "clm_orig_cntl_num")
  private Optional<String> claimOriginalControlNumber;

  List<Identifier> toFhir(ClaimTypeCode claimTypeCode) {
    var claimId =
        new Identifier()
            .setValue(String.valueOf(claimUniqueId))
            .setType(
                new CodeableConcept()
                    .addCoding(
                        new Coding()
                            .setSystem(SystemUrls.CARIN_CODE_SYSTEM_IDENTIFIER_TYPE)
                            .setCode("uc")
                            .setDisplay("Unique Claim ID")));
    var identifiers = new ArrayList<>(Collections.singletonList(claimId));
    var controlNumber =
        claimTypeCode.isClaimSubtype(ClaimSubtype.PDE)
            ? claimOriginalControlNumber.or(() -> claimControlNumber)
            : claimControlNumber;

    controlNumber.ifPresent(
        s ->
            identifiers.add(
                new Identifier()
                    .setSystem(SystemUrls.BLUE_BUTTON_CLAIM_CONTROL_NUMBER)
                    .setValue(s)));

    return identifiers;
  }

  /**
   * Maps original control number to RelatedClaimComponents.
   *
   * @param claimTypeCode the claim type code
   * @return a list of fhir eob.relatedclaimcomponents
   */
  public Optional<ExplanationOfBenefit.RelatedClaimComponent> toFhirRelatedClaim(
      ClaimTypeCode claimTypeCode) {
    if (claimTypeCode.isClaimSubtype(ClaimSubtype.PDE)) {
      return Optional.empty();
    }
    return claimOriginalControlNumber.map(
        origCntlNum ->
            new ExplanationOfBenefit.RelatedClaimComponent()
                .setRelationship(
                    new CodeableConcept()
                        .addCoding(
                            new Coding()
                                .setSystem(SystemUrls.HL7_EX_RELATED_CLAIM_RELATIONSHIP)
                                .setCode("prior")))
                .setReference(
                    new Identifier()
                        .setSystem(SystemUrls.BLUE_BUTTON_CLAIM_CONTROL_NUMBER)
                        .setValue(origCntlNum)));
  }
}
