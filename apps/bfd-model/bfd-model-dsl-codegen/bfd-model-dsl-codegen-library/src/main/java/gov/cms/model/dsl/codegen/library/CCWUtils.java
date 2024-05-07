package gov.cms.model.dsl.codegen.library;

import gov.cms.bfd.model.codebook.model.CcwCodebookInterface;

/** POC: A set of methods to work with {@link CcwCodebookInterface} instances. */
public class CCWUtils {
  /**
   * Calculates the variable reference url.
   *
   * @param ccwVariable the {@link CcwCodebookInterface} being mapped
   * @return the public URL at which documentation for the specified {@link CcwCodebookInterface} is
   *     published
   */
  public static String calculateVariableReferenceUrl(CcwCodebookInterface ccwVariable) {
    return String.format(
        "%s/%s",
        TransformerConstants.BASE_URL_CCW_VARIABLES,
        ccwVariable.getVariable().getId().toLowerCase());
  }
}
