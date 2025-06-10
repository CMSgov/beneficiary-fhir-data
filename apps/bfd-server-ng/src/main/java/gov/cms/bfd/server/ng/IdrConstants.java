package gov.cms.bfd.server.ng;

import static gov.cms.bfd.server.ng.coverage.MedicareStatusCode.CODE_0;
import static gov.cms.bfd.server.ng.coverage.MedicareStatusCode.CODE_00;
import static gov.cms.bfd.server.ng.coverage.MedicareStatusCode.CODE_10;
import static gov.cms.bfd.server.ng.coverage.MedicareStatusCode.CODE_11;
import static gov.cms.bfd.server.ng.coverage.MedicareStatusCode.CODE_20;
import static gov.cms.bfd.server.ng.coverage.MedicareStatusCode.CODE_21;
import static gov.cms.bfd.server.ng.coverage.MedicareStatusCode.CODE_31;
import static gov.cms.bfd.server.ng.coverage.MedicareStatusCode.CODE_40;
import static gov.cms.bfd.server.ng.coverage.MedicareStatusCode.CODE_TILDE;

import gov.cms.bfd.server.ng.coverage.MedicareStatusCode;
import gov.cms.bfd.server.ng.coverage.YesNoUnknownIndicator;
import java.time.LocalDate;
import java.util.Optional;

/** Constants representing specific values found in the IDR database. */
public class IdrConstants {
  /** The string value IDR uses to represent "true". */
  public static final String YES = "Y";

  /** The string value IDR uses to represent "false". */
  public static final String NO = "N";

  /** The value used by IDR to indicate a missing or non-applicable value in a date column. */
  public static final LocalDate DEFAULT_DATE = LocalDate.of(9999, 12, 31);

  /**
   * Translates a BFD/BlueButton Medicare Status Code to an ESRD (End-Stage Renal Disease) Indicator
   * code.
   *
   * @param medicareStatus The BFD/BlueButton Medicare Status Code (e.g., from BENE_MDCR_STUS_CD).
   * @return An {@link Optional} containing the ESRD indicator code if translation is possible,
   *     otherwise {@link Optional#empty()}.
   */
  public static Optional<String> translateMedicareStatusToEsrdCode(String medicareStatus) {
    if (medicareStatus == null) {
      return Optional.empty();
    }
    MedicareStatusCode medicareStatusCode = MedicareStatusCode.fromCode(medicareStatus).get();

    return switch (medicareStatusCode) {
      case CODE_0, CODE_00 -> Optional.of(YesNoUnknownIndicator.UNKNOWN.getCode());
      case CODE_11, CODE_21, CODE_31 -> Optional.of(YesNoUnknownIndicator.YES.getCode());
      case CODE_10, CODE_20, CODE_40 -> Optional.of(YesNoUnknownIndicator.NO.getCode());
      case CODE_TILDE -> Optional.empty();
    };
  }

  /**
   * Translates a BFD/BlueButton Medicare Status Code to a Disability Indicator code.
   *
   * @param medicareStatus The BFD/BlueButton Medicare Status Code (e.g., from BENE_MDCR_STUS_CD).
   * @return An {@link Optional} containing the Disability indicator code if translation is
   *     possible, otherwise {@link Optional#empty()}.
   */
  public static Optional<String> translateMedicareStatusToDisabilityCode(String medicareStatus) {
    if (medicareStatus == null) {
      return Optional.empty();
    }
    MedicareStatusCode medicareStatusCode = MedicareStatusCode.fromCode(medicareStatus).get();

    return switch (medicareStatusCode) {
      case CODE_0, CODE_00 -> Optional.of(YesNoUnknownIndicator.UNKNOWN.getCode());
      case CODE_20, CODE_21 -> Optional.of(YesNoUnknownIndicator.YES.getCode());
      case CODE_10, CODE_11, CODE_31, CODE_40 -> Optional.of(YesNoUnknownIndicator.NO.getCode());
      case CODE_TILDE -> Optional.empty(); // "null" mapping
    };
  }
}
