package gov.cms.model.dsl.codegen.plugin;

import static org.mockito.Mockito.verify;

import java.io.File;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Tests the DslMojo transformer generator correctly generates files from mappings. */
public class GenerateTransformersFromDslMojoIT extends AbstractMojoIntegrationTestCase {
  /** The file path to output the test files to. */
  private static final String OUTPUT_FILE_PATH = "gov/cms/test/FissClaimTransformer.java";

  /** The directory to find the mappings in for the test. */
  private final File mappingsDir = new File(baseFilesDir, "mappings");

  /** The directory to find the expected output for verifying the test. */
  private final File expectedDir = new File(baseFilesDir, "expected");

  /**
   * Tests that the mappings are correctly generated and sent to the output directory.
   *
   * @param outputDirectory the output directory
   * @throws Exception unexpected test exception
   */
  @Test
  public void testPlugin(@TempDir File outputDirectory) throws Exception {
    final String mappingsDirectoryPath = mappingsDir.getAbsolutePath();
    final String outputDirectoryPath = outputDirectory.getAbsolutePath();
    final var mojo =
        new GenerateTransformersFromDslMojo(mappingsDirectoryPath, outputDirectoryPath, project);
    mojo.execute();
    verify(project).addCompileSourceRoot(outputDirectoryPath);
    compareFiles(expectedDir, outputDirectory, OUTPUT_FILE_PATH);
  }
}
