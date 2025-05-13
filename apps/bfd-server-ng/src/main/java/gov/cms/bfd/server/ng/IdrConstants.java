package gov.cms.bfd.server.ng;

import java.time.LocalDate;

/** Constants representing specific values found in the IDR database. */
public class IdrConstants {
  /** The string value IDR uses to represent "true". */
  public static final String YES = "Y";

  /** The string value IDR uses to represent "false". */
  public static final String NO = "N";

  /** The value used by IDR to indicate a missing or non-applicable value in a date column. */
  public static final LocalDate DEFAULT_DATE = LocalDate.of(9999, 12, 31);
}
