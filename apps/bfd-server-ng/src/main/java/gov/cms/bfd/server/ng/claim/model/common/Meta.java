package gov.cms.bfd.server.ng.claim.model.common;

import gov.cms.bfd.server.ng.claim.EobProfile;
import gov.cms.bfd.server.ng.claim.EobProfileType;
import gov.cms.bfd.server.ng.input.ClaimSecurityStatus;
import gov.cms.bfd.server.ng.util.DateUtil;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.ZonedDateTime;
import lombok.Getter;

@SuppressWarnings({"checkstyle:MissingJavadocMethod", "checkstyle:MissingJavadocType"})
@Embeddable
@Getter
public class Meta {
  @Column(name = "bfd_claim_updated_ts", nullable = false)
  private ZonedDateTime updatedTimestamp;

  public org.hl7.fhir.r4.model.Meta toFhir(
      ClaimTypeCode claimTypeCode,
      EobProfileType profileType,
      ClaimSecurityStatus securityStatus,
      ClaimFinalAction finalAction,
      MetaSourceSk metaSourceSk) {
    var meta = new org.hl7.fhir.r4.model.Meta().setLastUpdated(DateUtil.toDate(updatedTimestamp));
    claimTypeCode
        .getProfileCategory()
        .map(category -> EobProfile.of(category, profileType))
        .ifPresent(profile -> profile.getMetaProfiles().forEach(meta::addProfile));
    meta.addTag(metaSourceSk.toFhirSystemType());
    finalAction.toFhirFinalAction().ifPresent(meta::addTag);
    meta.addSecurity(ClaimSecurityStatus.toFhir(securityStatus));
    meta.setSource(metaSourceSk.getDisplay());
    return meta;
  }
}
