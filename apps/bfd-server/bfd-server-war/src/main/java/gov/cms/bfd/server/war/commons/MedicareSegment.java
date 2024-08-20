package gov.cms.bfd.server.war.commons;

import java.util.Optional;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** Enumerates the Medicare segments/parts supported by the application. */
@Getter
@AllArgsConstructor
public enum MedicareSegment {
  /** Represents the part A medicare segment. */
  PART_A("part-a", "c4dic-part-a"),
  /** Represents the part B medicare segment. */
  PART_B("part-b", "c4dic-part-b"),
  /** Represents the part C medicare segment. */
  PART_C("part-c", "c4dic-part-c"),
  /** Represents the part D medicare segment. */
  PART_D("part-d", "c4dic-part-d");

  /** The url prefix for this segment. */
  private final String urlPrefix;

  /** The C4DIC url prefix for this segment. */
  private final String c4dicUrlPrefix;

  /**
   * Gets the {@link MedicareSegment} that matches the input, or an empty {@link Optional} if the
   * input does not match any known segment.
   *
   * @param urlPrefix the {@link #urlPrefix} to find a match for
   * @param enabledProfiles the CARIN {@link Profile} to use
   * @return the {@link MedicareSegment} (if any) whose {@link #urlPrefix} value matches the one
   *     specified
   */
  public static Optional<MedicareSegment> selectByC4dicUrlPrefix(
      String urlPrefix, Set<Profile> enabledProfiles) {
    for (MedicareSegment medicareSegment : values()) {
      if (medicareSegment.getC4dicUrlPrefix().equals(urlPrefix)) {
        if (!enabledProfiles.contains(Profile.C4DIC)) {
          return Optional.empty();
        }
        return Optional.of(medicareSegment);
      }
    }

    return Optional.empty();
  }

  /**
   * Gets the {@link MedicareSegment} that matches the input, or an empty {@link Optional} if the
   * input does not match any known segment.
   *
   * @param urlPrefix the {@link #urlPrefix} to find a match for
   * @return the {@link MedicareSegment} (if any) whose {@link #urlPrefix} value matches the one
   *     specified
   */
  public static Optional<MedicareSegment> selectByUrlPrefix(String urlPrefix) {
    for (MedicareSegment medicareSegment : values()) {
      if (medicareSegment.getUrlPrefix().equals(urlPrefix)) {
        return Optional.of(medicareSegment);
      }
    }

    return Optional.empty();
  }
}
