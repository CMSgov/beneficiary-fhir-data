package gov.cms.bfd.server.ng.coverage;

import gov.cms.bfd.server.ng.IdrConstants;
import java.util.Arrays;
import java.util.Optional;
import lombok.Getter;

/** Medicare Status Code Type. */
@Getter
public enum MedicareStatusCodeType {
  /** Medicare Status Code 10. */
  CODE_10("10", IdrConstants.NO, IdrConstants.NO),
  /** Medicare Status Code 11. */
  CODE_11("11", IdrConstants.YES, IdrConstants.NO),
  /** Medicare Status Code. 20 */
  CODE_20("20", IdrConstants.NO, IdrConstants.YES),
  /** Medicare Status Code 21. */
  CODE_21("21", IdrConstants.YES, IdrConstants.YES),
  /** Medicare Status Code 31. */
  CODE_31("31", IdrConstants.YES, IdrConstants.NO),
  /** Medicare Status Code 0. */
  CODE_0("0", IdrConstants.UNKNOWN, IdrConstants.UNKNOWN),
  /** Medicare Status Code 00. */
  CODE_00("00", IdrConstants.UNKNOWN, IdrConstants.UNKNOWN),
  /** Medicare Status Code null. */
  CODE_EMPTY("", "", ""); // Or specific codes for unknown

  private final String code;
  private final String esrdIndicator;
  private final String disabilityIndicator;

  MedicareStatusCodeType(String code, String esrdIndicator, String disabilityIndicator) {
    this.code = code;
    this.esrdIndicator = esrdIndicator;
    this.disabilityIndicator = disabilityIndicator;
  }

  /**
   * Finds a {@link MedicareStatusCodeType} enum constant by its BENE_MDCR_STUS_CD code.
   *
   * @param beneMdcrStusCd The BENE_MDCR_STUS_CD code from the database.
   * @return An {@link Optional} containing the matching {@link MedicareStatusCodeType}, or {@link
   *     Optional#empty()} if no match is found.
   */
  public static Optional<MedicareStatusCodeType> fromCode(String beneMdcrStusCd) {
    if (beneMdcrStusCd == null || beneMdcrStusCd.isBlank()) {
      return Optional.empty();
    }
    return Arrays.stream(MedicareStatusCodeType.values())
        .filter(type -> type.getCode().equals(beneMdcrStusCd))
        .findFirst();
  }
}
