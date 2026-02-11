package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Optional;

/** Claim audit trail information. */
@Embeddable
public class ClaimAuditTrailContext {

  @Column(name = "meta_src_sk", insertable = false, updatable = false)
  private MetaSourceSk metaSourceSk;

  @Column(name = "clm_audt_trl_stus_cd")
  private Optional<String> claimAuditTrailStatusCode;

  @Column(name = "clm_audt_trl_lctn_cd")
  private ClaimAuditTrailLocationCode claimAuditTrailLocationCode;

  Optional<ClaimAuditTrailStatusCode> getAuditTrailStatusCode() {
    // composite code is derived using source, audit trail status code, and for VMS's case, audit
    // trail location code since there exists some overlap in which descriptions and outcomes
    // differ.
    return claimAuditTrailStatusCode.flatMap(
        status ->
            ClaimAuditTrailStatusCode.tryFromCode(
                metaSourceSk, status, claimAuditTrailLocationCode));
  }
}
