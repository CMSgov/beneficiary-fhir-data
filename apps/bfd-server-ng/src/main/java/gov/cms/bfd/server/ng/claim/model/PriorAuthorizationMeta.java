package gov.cms.bfd.server.ng.claim.model;

import static gov.cms.bfd.server.ng.util.SystemUrls.PROFILE_PRIOR_AUTH;

import gov.cms.bfd.server.ng.util.DateUtil;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.ZonedDateTime;
import lombok.Getter;

@Embeddable
@Getter
class PriorAuthorizationMeta {
  @Column(name = "bfd_updated_ts", nullable = false)
  private ZonedDateTime updatedTimestamp;

  org.hl7.fhir.r4.model.Meta toFhir(MetaSourceSk metaSourceSk) {
    var meta = new org.hl7.fhir.r4.model.Meta().setLastUpdated(DateUtil.toDate(updatedTimestamp));
    meta.addProfile(PROFILE_PRIOR_AUTH);
    meta.addTag(metaSourceSk.toFhirSystemType());
    meta.setSource(metaSourceSk.getDisplay());
    return meta;
  }
}
