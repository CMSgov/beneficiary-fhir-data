package gov.cms.bfd.server.ng;

import lombok.Builder;
import lombok.Value;

/** Claim Filter options. */
@Value
@Builder
public class ClaimFilterOptions {

  @Builder.Default SamhsaFilterMode samhsaFilterMode = SamhsaFilterMode.EXCLUDE;

  @Builder.Default boolean includeTaxNumber = false;
}
