package gov.cms.bfd.server.ng;

import java.time.LocalDate;
import java.time.ZonedDateTime;

/** Constants representing specific values found in the IDR database. */
public class IdrConstants {
  /** The string value IDR uses to represent "true". */
  public static final String YES = "Y";

  /** The string value IDR uses to represent "false". */
  public static final String NO = "N";

  /** The string value IDR uses to represent "UNKNOWN". */
  public static final String UNKNOWN = "U";

  /** The value used by IDR to indicate a missing or non-applicable value in a date column. */
  public static final LocalDate DEFAULT_DATE = LocalDate.of(9999, 12, 31);

  /** The value used by IDR to indicate a missing or non-applicable value in a date column. */
  public static final ZonedDateTime DEFAULT_ZONED_DATE =
      (LocalDate.of(9999, 12, 31)).atStartOfDay(DateUtil.ZONE_ID_UTC);

  /** The code for partially adjudicated claims. */
  public static final String ADJUDICATION_STATUS_PARTIAL = "PartiallyAdjudicated";

  /** The code for fully adjudicated claims. */
  public static final String ADJUDICATION_STATUS_FINAL = "Adjudicated";
}
