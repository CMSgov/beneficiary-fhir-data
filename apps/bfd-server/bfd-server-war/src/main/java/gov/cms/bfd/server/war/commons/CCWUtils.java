package gov.cms.bfd.server.war.commons;

import gov.cms.bfd.model.codebook.model.CcwCodebookInterface;

/** A set of methods to work with {@link CcwCodebookInterface} instances. */
public class CCWUtils {
  /**
   * Calculates the variable reference url.
   *
   * @param ccwVariable the {@link CcwCodebookInterface} being mapped
   * @return the public URL at which documentation for the specified {@link CcwCodebookInterface} is
   *     published
   */
  public static String calculateVariableReferenceUrl(CcwCodebookInterface ccwVariable) {
    return calculateVariableReferenceUrl(ccwVariable, false);
  }

  /**
   * Calculates the variable reference url.
   *
   * @param ccwVariable the {@link CcwCodebookInterface} being mapped
   * @param skipReplaceCcwSystem if set, {@link TransformerConstants#CCW_SYSTEM_MAP} will not be
   *     checked.
   * @return the public URL at which documentation for the specified {@link CcwCodebookInterface} is
   *     published
   */
  public static String calculateVariableReferenceUrl(
      CcwCodebookInterface ccwVariable, boolean skipReplaceCcwSystem) {
    String ccwVarId = ccwVariable.getVariable().getId().toLowerCase();
    // If the ccw variable exists in the CCW_SYSTEM_MAP map, then we can return the value of this
    // mapping as the system. Otherwise, it will be constructed.
    if (!skipReplaceCcwSystem && TransformerConstants.CCW_SYSTEM_MAP.containsKey(ccwVarId)) {
      return TransformerConstants.CCW_SYSTEM_MAP.get(ccwVarId);
    }
    return String.format("%s/%s", TransformerConstants.BASE_URL_CCW_VARIABLES, ccwVarId);
  }
}
