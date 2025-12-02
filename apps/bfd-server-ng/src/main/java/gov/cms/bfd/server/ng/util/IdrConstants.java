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

  /** The code for partially adjudicated claims. */
  public static final String ADJUDICATION_STATUS_PARTIAL = "PartiallyAdjudicated";

  /** The code for fully adjudicated claims. */
  public static final String ADJUDICATION_STATUS_FINAL = "Adjudicated";

  /** The Samhsa Security code. */
  public static final String SAMHSA_SECURITY_CODE = "42CFRPart2";

  /** The display value for the 42 CFR Part 2 security tag. */
  public static final String SAMHSA_SECURITY_DISPLAY = "42 CFR Part 2";

  /** The description for claim related PAYER_CODE_EFFECTIVE_8_11_2020. */
  public static final String PAYER_CODE_EFFECTIVE_8_11_2020 = "PAYER CODE (EFFECTIVE 8/11/2020)";
}
