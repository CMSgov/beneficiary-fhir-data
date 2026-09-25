package gov.cms.bfd.server.ng.claim.model.institutional;

import gov.cms.bfd.server.ng.claim.model.common.ClaimLineHcpcsCode;
import jakarta.persistence.Embeddable;
import java.util.Optional;
import lombok.Getter;

/** Claim line info for basis shared systems, implements nothing new. */
@Embeddable
@Getter
public class ClaimLineInstitutionalBasisSharedSystems extends ClaimLineInstitutionalBase {

  @Override
  public Optional<ClaimLineHcpcsCode> getClaimLineHcpcsCode() {
    return Optional.of(getHcpcsCode());
  }
}
