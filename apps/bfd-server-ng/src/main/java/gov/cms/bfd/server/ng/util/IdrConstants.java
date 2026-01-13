package gov.cms.bfd.server.ng.util;

import java.time.LocalDate;

/** Constants representing specific values found in the IDR database. */
public class IdrConstants {

  // Private constructor to prevent instantiation
  private IdrConstants() {
    // Intentionally empty
  }

  /** The string value IDR uses to represent "true". */
  public static final String YES = "Y";

  /** The string value IDR uses to represent "false". */
  public static final String NO = "N";

  /** The string value IDR uses to represent "UNKNOWN". */
  public static final String UNKNOWN = "U";

  /** The value used by IDR to indicate a missing or non-applicable value in a date column. */
  public static final LocalDate DEFAULT_DATE = LocalDate.of(9999, 12, 31);

  /** The code for non-final action claims. */
  public static final String NOT_FINAL_ACTION = "NotFinalAction";

  /** The code for final action claims. */
  public static final String FINAL_ACTION = "FinalAction";

  /** The code for partially adjudicated claims. */
  public static final String SYSTEM_TYPE_NCH = "NationalClaimsHistory";

  /** The code for fully adjudicated claims. */
  public static final String SYSTEM_TYPE_SHARED = "SharedSystem";

  /** The Samhsa Security code. */
  public static final String SAMHSA_SECURITY_CODE = "42CFRPart2";

  /** The display value for the 42 CFR Part 2 security tag. */
  public static final String SAMHSA_SECURITY_DISPLAY = "42 CFR Part 2";
}
