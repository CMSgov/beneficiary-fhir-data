package gov.cms.bfd.server.war.commons;

import gov.cms.bfd.model.codebook.model.CcwCodebookInterface;

/** As set of methods to work with CCW Codebook variables. */
public class CCWUtils {
  /**
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
