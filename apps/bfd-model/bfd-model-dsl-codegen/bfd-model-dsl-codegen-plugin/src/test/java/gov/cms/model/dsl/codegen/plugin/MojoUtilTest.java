package gov.cms.model.dsl.codegen.plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Unit tests for {@link gov.cms.model.dsl.codegen.plugin.MojoUtil}. */
public class MojoUtilTest {
  /**
   * Verifies that {@link MojoUtil#initializeOutputDirectory} creates all directories.
   *
   * @param tempDir auto-created base directory to use as root for test
   */
  @Test
  public void testInitializeOutputDirectory(@TempDir File tempDir) {
    final File expectedDirectory = new File(tempDir, "a/b/c");
    final File actualDirectory =
        MojoUtil.initializeOutputDirectory(expectedDirectory.getAbsolutePath());
    assertEquals(expectedDirectory.getAbsolutePath(), actualDirectory.getAbsolutePath());
    assertTrue(new File(tempDir, "a").isDirectory());
    assertTrue(new File(tempDir, "a/b").isDirectory());
    assertTrue(new File(tempDir, "a/b/c").isDirectory());

    // second call succeeds without throwing any exceptions
    final File actualDirectory2 =
        MojoUtil.initializeOutputDirectory(expectedDirectory.getAbsolutePath());
    assertEquals(expectedDirectory.getAbsolutePath(), actualDirectory2.getAbsolutePath());
    assertTrue(new File(tempDir, "a").isDirectory());
    assertTrue(new File(tempDir, "a/b").isDirectory());
    assertTrue(new File(tempDir, "a/b/c").isDirectory());
  }

  /** Verifies that exception created by {@link MojoUtil#createException} has expected message. */
  @Test
  public void testCreateException() {
    final MojoExecutionException exception = MojoUtil.createException("test %d", 1);
    assertEquals("test 1", exception.getMessage());
  }
}
