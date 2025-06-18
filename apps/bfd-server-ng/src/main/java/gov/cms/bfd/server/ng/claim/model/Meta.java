package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.DateUtil;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalDateTime;

@Embeddable
class Meta {
  @Column(name = "bfd_updated_ts", nullable = false)
  private LocalDateTime updatedTimestamp;

  org.hl7.fhir.r4.model.Meta toFhir(ClaimTypeCode claimTypeCode, ClaimSourceId claimSourceId) {
    var meta =
        new org.hl7.fhir.r4.model.Meta()
            .setLastUpdated(DateUtil.toDate(updatedTimestamp))
            .setSource(claimSourceId.getSource());
    claimTypeCode.toFhirStructureDefinition().ifPresent(meta::addProfile);
    claimSourceId.toFhirAdjudicationStatus().ifPresent(meta::addTag);
    return meta;
  }
}
