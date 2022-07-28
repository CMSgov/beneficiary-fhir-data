package gov.cms.model.dsl.codegen.plugin;

import static org.mockito.Mockito.verify;

import java.io.File;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class GenerateTransformersFromDslMojoIT extends AbstractMojoIntegrationTestCase {
  private static final String OUTPUT_FILE_PATH = "gov/cms/test/FissClaimTransformer.java";

  private final File mappingsDir = new File(baseFilesDir, "mappings");
  private final File expectedDir = new File(baseFilesDir, "expected");

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
