package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.ZonedDateTime;
import lombok.Getter;

@Embeddable
@Getter
class Meta {
  @Column(name = "bfd_updated_ts", nullable = false)
  private ZonedDateTime updatedTimestamp;

  org.hl7.fhir.r4.model.Meta toFhir(ClaimTypeCode claimTypeCode, ClaimSourceId claimSourceId) {
    var meta = new org.hl7.fhir.r4.model.Meta().setSource(claimSourceId.getSource());
    claimTypeCode.toFhirStructureDefinition().ifPresent(meta::addProfile);
    claimSourceId.toFhirAdjudicationStatus().ifPresent(meta::addTag);
    return meta;
  }
}
