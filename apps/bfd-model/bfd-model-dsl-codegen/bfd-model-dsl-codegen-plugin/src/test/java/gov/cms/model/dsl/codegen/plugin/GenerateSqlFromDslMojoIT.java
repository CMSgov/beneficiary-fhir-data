package gov.cms.model.dsl.codegen.plugin;

import java.io.File;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class GenerateSqlFromDslMojoIT extends AbstractMojoIntegrationTestCase {
  private static final String OUTPUT_FILE_PATH = "entities-schema.sql";

  private final File mappingsDir = new File(baseFilesDir, "mappings");
  private final File expectedDir = new File(baseFilesDir, "expected");

  @Test
  public void testPlugin(@TempDir File outputDirectory) throws Exception {
    final String mappingsDirectoryPath = mappingsDir.getAbsolutePath();
    final String outputFilePath = new File(outputDirectory, OUTPUT_FILE_PATH).getAbsolutePath();
    final var mojo = new GenerateSqlFromDslMojo(mappingsDirectoryPath, outputFilePath);
    mojo.execute();
    compareFiles(expectedDir, outputDirectory, OUTPUT_FILE_PATH);
  }
}
