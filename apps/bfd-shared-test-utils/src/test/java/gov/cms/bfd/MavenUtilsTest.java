package gov.cms.bfd;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.com.google.common.base.Strings;

/** Unit tests for {@link MavenUtils}. */
public class MavenUtilsTest {
  /** Verifies that pom file can be found and parsed successfully. */
  @Test
  void findsProjectVersionSuccessfully() {
    String version = MavenUtils.findProjectVersion();
    assertFalse(Strings.isNullOrEmpty(version), "version string should be non-empty");
  }

  /** Verifies that method throws an exception if it cannot find a pom file. */
  @Test
  void failsToFindPomFile() {
    assertThrows(
        RuntimeException.class, () -> MavenUtils.findProjectVersion("not-a-real-pom-file"));
  }
}
