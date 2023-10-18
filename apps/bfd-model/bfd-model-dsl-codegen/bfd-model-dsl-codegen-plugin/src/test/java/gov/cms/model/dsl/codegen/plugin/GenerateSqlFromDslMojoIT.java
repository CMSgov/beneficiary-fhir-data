package gov.cms.model.dsl.codegen.plugin;

import java.io.File;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Tests SQL generation using dsl mojo. */
public class GenerateSqlFromDslMojoIT extends AbstractMojoIntegrationTestCase {
  /** The output path for the test sql. */
  private static final String OUTPUT_FILE_PATH = "entities-schema.sql";

  /** The directory to find the mappings in for the test. */
  private final File mappingsDir = new File(baseFilesDir, "mappings");

  /** The directory to find the expected output sql for the test validation. */
  private final File expectedDir = new File(baseFilesDir, "expected");

  /**
   * Tests the dsl mojo plugin can generate sql.
   *
   * @param outputDirectory the output directory
   * @throws Exception unexpected test exception
   */
  @Test
  public void testPlugin(@TempDir File outputDirectory) throws Exception {
    final String mappingsDirectoryPath = mappingsDir.getAbsolutePath();
    final String outputFilePath = new File(outputDirectory, OUTPUT_FILE_PATH).getAbsolutePath();
    final var mojo = new GenerateSqlFromDslMojo(mappingsDirectoryPath, outputFilePath);
    mojo.execute();
    compareFiles(expectedDir, outputDirectory, OUTPUT_FILE_PATH);
  }
}
