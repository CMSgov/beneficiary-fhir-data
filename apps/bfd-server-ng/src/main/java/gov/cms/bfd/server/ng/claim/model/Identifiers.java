package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.List;
import java.util.Optional;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Identifier;

@Embeddable
class Identifiers {
  @Column(name = "clm_uniq_id")
  private long claimUniqueId;

  @Column(name = "clm_cntl_num")
  private String claimControlNumber;

  @Column(name = "clm_orig_cntl_num")
  private Optional<String> claimOriginalControlNumber;

  public List<Identifier> toFhir() {
    var identifiers =
        new java.util.ArrayList<>(
            List.of(
                new Identifier()
                    .setValue(String.valueOf(claimUniqueId))
                    .setType(
                        new CodeableConcept()
                            .addCoding(
                                new Coding()
                                    .setSystem(SystemUrls.CARIN_CODE_SYSTEM_IDENTIFIER_TYPE)
                                    .setCode("uc")
                                    .setDisplay("Unique Claim ID"))),
                new Identifier()
                    .setSystem(SystemUrls.BLUE_BUTTON_CLAIM_CONTROL_NUMBER)
                    .setValue(claimControlNumber)));
    claimOriginalControlNumber.map(
        originalControlNumber ->
            identifiers.add(
                new Identifier()
                    .setSystem(SystemUrls.BLUE_BUTTON_CLAIM_CONTROL_NUMBER)
                    .setValue(originalControlNumber)));
    return identifiers;
  }
}
