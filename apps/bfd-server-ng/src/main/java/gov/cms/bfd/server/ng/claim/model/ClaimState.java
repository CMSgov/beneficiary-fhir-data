package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.ClaimSecurityStatus;
import lombok.Builder;
import lombok.Value;
import lombok.With;

/** Internal state computed during claim processing. */
@Value
@Builder
public class ClaimState {

  @With @Builder.Default ClaimSecurityStatus securityStatus = ClaimSecurityStatus.NONE;
}
