package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import lombok.Getter;

/** Institutional claim line table. */
@Getter
@Embeddable
public class ClaimLineInstitutionalInfo extends ClaimLineInstitutionalBase {

  @Embedded private ClaimAnsiSignatureInfo ansiSignature;
}
