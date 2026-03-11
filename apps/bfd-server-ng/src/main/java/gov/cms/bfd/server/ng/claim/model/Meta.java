package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.ClaimSecurityStatus;
import gov.cms.bfd.server.ng.util.DateUtil;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.ZonedDateTime;
import lombok.Getter;

@Embeddable
@Getter
class Meta {
  @Column(name = "bfd_claim_updated_ts", nullable = false)
  private ZonedDateTime updatedTimestamp;

  org.hl7.fhir.r4.model.Meta toFhir(
      ClaimTypeCode claimTypeCode,
      ClaimSecurityStatus securityStatus,
      ClaimFinalAction finalAction,
      MetaSourceSk metaSourceSk) {
    var meta = new org.hl7.fhir.r4.model.Meta().setLastUpdated(DateUtil.toDate(updatedTimestamp));
    claimTypeCode.toFhirStructureDefinition().ifPresent(meta::addProfile);
    finalAction.toFhirFinalAction().ifPresent(meta::addTag);
    meta.addSecurity(ClaimSecurityStatus.toFhir(securityStatus));
    meta.setSource(metaSourceSk.getDisplay());
    metaSourceSk.toFhirSystemType().ifPresent(meta::addTag);
    return meta;
  }
}
