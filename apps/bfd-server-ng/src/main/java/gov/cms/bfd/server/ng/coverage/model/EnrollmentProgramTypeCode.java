package gov.cms.bfd.server.ng.coverage.model;

import gov.cms.bfd.server.ng.input.CoveragePart;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Optional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Enrollment Program Type Code. */
@RequiredArgsConstructor
@Getter
public enum EnrollmentProgramTypeCode {
  /** Beneficiary enrollment program type code 1 denotes Part C. */
  PART_C("1", EnumSet.of(CoveragePart.PART_C)),
  /** Beneficiary enrollment program type code 2 denotes Part D. */
  PART_D("2", EnumSet.of(CoveragePart.PART_D)),
  /** Beneficiary enrollment program type code 3 denotes Parts C and D. */
  PART_C_AND_D("3", EnumSet.of(CoveragePart.PART_C, CoveragePart.PART_D));

  private final String code;
  private final EnumSet<CoveragePart> supportedCoverageParts;

  /**
   * Finds a {@link EnrollmentProgramTypeCode} enum constant by its BENE_ENRLMT_PGM_TYPE_CD code.
   *
   * @param enrollmentProgramTypeCode The BENE_ENRLMT_PGM_TYPE_CD code from the database.
   * @return An {@link Optional} containing the matching {@link EnrollmentProgramTypeCode}, or
   *     {@link Optional#empty()} if no match is found.
   */
  public static Optional<EnrollmentProgramTypeCode> tryFromCode(String enrollmentProgramTypeCode) {
    if (enrollmentProgramTypeCode == null || enrollmentProgramTypeCode.isBlank()) {
      return Optional.empty();
    }
    return Arrays.stream(EnrollmentProgramTypeCode.values())
        .filter(type -> type.getCode().equals(enrollmentProgramTypeCode))
        .findFirst();
  }

  /**
   * Returns whether the current enrollment program type code supports the specified coverage part.
   *
   * @param coveragePart The coverage part.
   * @return boolean
   */
  public boolean supports(CoveragePart coveragePart) {
    return supportedCoverageParts.contains(coveragePart);
  }
}
