package gov.cms.bfd.server.ng.input;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import gov.cms.bfd.server.ng.IdrConstants;
import java.util.Optional;
import lombok.Getter;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Coverage;

/**
 * Represents the standardized Medicare coverage parts (A, B, C, D). Provides methods for looking up
 * parts based on their exact, predefined raw URL prefix strings.
 */
@Getter
public enum CoveragePart {
  /** Represents the part A medicare segment. */
  PART_A("A", "Part A", "part-a"),
  /** Represents the part B medicare segment. */
  PART_B("B", "Part B", "part-b"),
  /** Represents the part C medicare segment. */
  PART_C("C", "Part C", "part-c"),
  /** Represents the part D medicare segment. */
  PART_D("D", "Part D", "part-d");

  private final String code; // The single character code (A, B, C, D)
  private final String displayName;
  private final String standardUrlPrefix; // e.g., "part-a"

  CoveragePart(String code, String displayName, String standardUrlPrefix) {
    this.code = code;
    this.displayName = displayName;
    this.standardUrlPrefix = standardUrlPrefix;
  }

  /**
   * Finds a {@link CoveragePart} by matching the provided {@code rawUrlPrefix} against either the
   * standard or C4DIC predefined prefixes, based on the {@code isC4dicContext}. The match is
   * case-insensitive.
   *
   * @param rawUrlPrefix The exact raw URL prefix string captured by a regex (e.g., "part-a",
   *     "c4dic-part-b"). {@code false} for standard prefixes.
   * @return An {@link Optional} containing the matching {@link CoveragePart}, or {@link
   *     Optional#empty()} if no exact match.
   */
  public static Optional<CoveragePart> fromExactRawPrefix(String rawUrlPrefix) {
    if (rawUrlPrefix == null || rawUrlPrefix.isBlank()) {
      return Optional.empty();
    }
    for (CoveragePart part : values()) {
      if (part.getStandardUrlPrefix().equalsIgnoreCase(rawUrlPrefix)) {
        return Optional.of(part);
      }
    }
    return Optional.empty();
  }

  /**
   * Finds a {@link CoveragePart} by matching the provided {@code rawUrlPrefix} or throws an
   * exception.
   *
   * @param rawUrlPrefix The exact raw URL prefix string.
   * @return The matching {@link CoveragePart}.
   * @throws InvalidRequestException if no match is found.
   */
  public static CoveragePart fromExactRawPrefixOrThrow(String rawUrlPrefix) {
    return fromExactRawPrefix(rawUrlPrefix)
        .orElseThrow(
            () ->
                new InvalidRequestException(
                    "Unrecognized coverage part identifier prefix: '"
                        + rawUrlPrefix
                        + "' for the given context."));
  }

  /**
   * Finds a {@link CoveragePart} enum constant by its single character code (e.g., "A", "B").
   *
   * @param code The single character code. Must not be null.
   * @return An {@link Optional} containing the matching {@link CoveragePart}, or {@link
   *     Optional#empty()} if not found.
   */
  public static Optional<CoveragePart> forCode(String code) {
    if (code == null || code.length() != 1) {
      throw new IllegalArgumentException(
          "Input code must be a single non-null character for CoveragePart.forCode(). Received: "
              + (code == null ? "null" : "'" + code + "'"));
    }
    for (CoveragePart part : values()) {
      if (part.getCode().equals(code)) {
        return Optional.of(part);
      }
    }
    return Optional.empty();
  }

  /**
   * Adds PartA Coverage Elements To Coverage.
   *
   * @param coverage The Coverage.
   */
  public static void addPartACoverageElementsToCoverage(Coverage coverage) {
    CodeableConcept typeCode = new CodeableConcept();
    typeCode.addCoding().setSystem(IdrConstants.SYS_SOPT).setCode("1").setDisplay("MEDICARE");
    coverage.setType(typeCode);

    Coverage.ClassComponent classComponent = new Coverage.ClassComponent();
    classComponent
        .setType(
            new CodeableConcept()
                .addCoding(new Coding(IdrConstants.SYS_COVERAGE_CLASS, "plan", null)))
        .setValue("Part A");
    coverage.addClass_(classComponent);
  }

  /**
   * Adds PartB Coverage Elements To Coverage.
   *
   * @param coverage The Coverage.
   */
  public static void addPartBCoverageElementsToCoverage(Coverage coverage) {
    CodeableConcept typeCode = new CodeableConcept();
    typeCode.addCoding().setSystem(IdrConstants.SYS_SOPT).setCode("121").setDisplay("MEDICARE FFS");
    coverage.setType(typeCode);

    Coverage.ClassComponent classComponent = new Coverage.ClassComponent();
    classComponent
        .setType(
            new CodeableConcept()
                .addCoding(new Coding(IdrConstants.SYS_COVERAGE_CLASS, "plan", null)))
        .setValue("Part B");
    coverage.addClass_(classComponent);
  }
}
