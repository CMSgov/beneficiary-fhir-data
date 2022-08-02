package gov.cms.model.dsl.codegen.plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/** Abstract base class for integration tests that execute a mojo and verify its output. */
class AbstractMojoIntegrationTestCase {
  /** Base resources path for files used in unit tests. */
  public static final String FILE_RESOURCES_BASE = "src/test/resources";

  /** Test specific files are stored in a resources directory named after the unit test. */
  protected final File baseFilesDir = new File(FILE_RESOURCES_BASE, getClass().getSimpleName());

  /** Mock for the {@link MavenProject} that would normally be injected into the mojo at runtime. */
  @Mock protected MavenProject project;

  /**
   * Load files as strings from the given expected and output directories and compare them. The
   * output file path can include sub-directories but these are stripped off when searching for
   * matching files in the expected directory.
   *
   * @param expectedDirectory directory containing expected versions of the files
   * @param outputDirectory root of directory tree containing output files
   * @param outputFilePath path within the output directory to output file to be compared
   * @throws IOException IO error during file read
   */
  void compareFiles(File expectedDirectory, File outputDirectory, String outputFilePath)
      throws IOException {
    File actualFile = new File(outputDirectory, outputFilePath);
    File expectedFile = new File(expectedDirectory, actualFile.getName());
    String actualText = readAndNormalizeFile(actualFile);
    String expectedText = readAndNormalizeFile(expectedFile);
    assertEquals(expectedText, actualText, "file mismatch in " + outputFilePath);
  }

  /** Auto creates mocks based on {@link Mock} annotations. */
  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  /**
   * Reads the file line by line to build up a consolidated string representation with a common line
   * ending. Ensures that line ending differences per platform do not lead to test failures. Using
   * an entire file for string comparison so that viewing the difference in an IDE can highlight all
   * of the differences visually and the whole file is checked in each test.
   *
   * @param file the file to read
   * @return normalized string representation of the file
   * @throws IOException IO error during file read
   */
  private String readAndNormalizeFile(File file) throws IOException {
    StringBuilder result = new StringBuilder();
    for (String line : Files.readLines(file, Charsets.UTF_8)) {
      String normalized = line.stripTrailing();
      if (normalized.length() > 0) {
        result.append(normalized).append("\n");
      }
    }
    return result.toString();
  }
}
