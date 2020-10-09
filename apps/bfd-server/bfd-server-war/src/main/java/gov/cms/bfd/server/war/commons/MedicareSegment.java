package gov.cms.bfd.server.war.commons;

import java.util.Optional;
import org.hl7.fhir.dstu3.model.Coverage;

/** Enumerates the Medicare segments/parts supported by the application. */
public enum MedicareSegment {
  PART_A("part-a"),

  PART_B("part-b"),

  PART_C("part-c"),

  PART_D("part-d");

  private final String urlPrefix;

  /**
   * Enum constant constructor.
   *
   * @param urlPrefix the value to use for {@link #getUrlPrefix()}
   */
  private MedicareSegment(String urlPrefix) {
    this.urlPrefix = urlPrefix;
  }

  /**
   * @return a {@link String} that can be used in URLs to uniquely identify this {@link
   *     MedicareSegment} (e.g. as part of FHIR {@link Coverage#getId()} values)
   */
  public String getUrlPrefix() {
    return urlPrefix;
  }

  /**
   * @param urlPrefix the {@link #getUrlPrefix()} to find a match for
   * @return the {@link MedicareSegment} (if any) whose {@link MedicareSegment#getUrlPrefix()} value
   *     matches the one specified
   */
  public static Optional<MedicareSegment> selectByUrlPrefix(String urlPrefix) {
    for (MedicareSegment medicareSegment : values()) {
      if (medicareSegment.getUrlPrefix().equals(urlPrefix)) return Optional.of(medicareSegment);
    }

    return Optional.empty();
  }
}
