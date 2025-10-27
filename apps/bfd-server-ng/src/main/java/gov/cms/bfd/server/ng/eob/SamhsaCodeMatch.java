package gov.cms.bfd.server.ng.eob;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

/**
 * Represents a SAMHSA code that matched during claim filtering. Used for logging and auditing
 * purposes.
 */
@Getter
@AllArgsConstructor
@ToString
public class SamhsaCodeMatch {
  /** The code value that matched. */
  private final String code;

  /** The code system (ICD-10-CM, CPT, DRG, etc.). */
  private final String system;

  /** The type of code (diagnosis, procedure, DRG, HCPCS). */
  private final String codeType;
}
