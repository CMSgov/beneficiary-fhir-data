package gov.cms.bfd.server.ng.input;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import gov.cms.bfd.server.ng.SystemUrls;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Coverage;

/**
 * Represents the standardized Medicare coverage parts (A, B, C, D). Provides methods for looking up
 * parts based on their exact, predefined raw URL prefix strings.
 */
@Getter
@AllArgsConstructor
public enum CoveragePart {
  /** Medicare Part A. */
  PART_A("A", "Part A", "part-a", "1", "MEDICARE"),

  /** Medicare Part B. */
  PART_B("B", "Part B", "part-b", "121", "MEDICARE FFS"),
  /** Dual enrollment. */
  DUAL(
      "DUAL",
      "Dual Medicare/Medicaid",
      "dual",
      "14",
      "Dual Eligibility Medicare/Medicaid Organization");

  private final String standardCode;
  private final String standardDisplay;
  private final String standardSystem; // "part-a", "part-b"
  private final String soptCode;
  private final String soptDisplay; // "MEDICARE", "MEDICARE FFS"

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
      if (part.getStandardSystem().equalsIgnoreCase(rawUrlPrefix)) {
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
                    "Unrecognized or unsupported coverage part identifier prefix provided."));
  }

  /**
   * Creates TypeCode.
   *
   * @return TypeCode
   */
  public CodeableConcept toFhirTypeCode() {
    var typeCode = new CodeableConcept();
    typeCode
        .addCoding()
        .setSystem(SystemUrls.SYS_SOPT)
        .setCode(this.getSoptCode())
        .setDisplay(this.getSoptDisplay());
    return typeCode;
  }

  /**
   * Creates ClassComponent.
   *
   * @return classComponent
   */
  public Coverage.ClassComponent toFhirClassComponent() {
    var classComponent = new Coverage.ClassComponent();
    classComponent
        .setType(
            new CodeableConcept()
                .addCoding(new Coding(SystemUrls.SYS_COVERAGE_CLASS, "plan", null)))
        .setValue(this.getStandardDisplay());
    return classComponent;
  }
}
