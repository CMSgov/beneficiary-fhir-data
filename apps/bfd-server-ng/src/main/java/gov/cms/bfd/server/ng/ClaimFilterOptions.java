package gov.cms.bfd.server.ng;

import java.util.Set;
import lombok.Builder;
import lombok.Value;

/**
 * Claim Filter options. Controls SAMHSA filtering, tax number inclusion, and FHIR field projection
 * via the _elements parameter.
 */
@Value
@Builder
public class ClaimFilterOptions {

  @Builder.Default SamhsaFilterMode samhsaFilterMode = SamhsaFilterMode.EXCLUDE;

  @Builder.Default boolean includeTaxNumber = false;

  /**
   * Optional set of FHIR element names for field projection (_elements parameter). When non-null
   * and non-empty, only the specified top-level elements (plus mandatory fields like id, meta,
   * status, patient) should be included in the serialized response. A null value means no
   * projection is applied.
   */
  @Builder.Default Set<String> elements = null;
}
