package gov.cms.model.dsl.codegen.plugin.model.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import gov.cms.model.dsl.codegen.plugin.MojoUtil;
import gov.cms.model.dsl.codegen.plugin.model.ModelUtil;
import gov.cms.model.dsl.codegen.plugin.model.RootBean;
import java.io.File;
import java.util.stream.Collectors;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.jupiter.api.Test;

/** IT for model validation generated messages. */
public class ValidationUtilIT {
  /** Base resources path for files used in unit tests. */
  private static final String FILE_RESOURCES_BASE = "src/test/resources";

  /** Test specific files are stored in a resources directory named after the unit test. */
  private static final File baseFilesDir =
      new File(FILE_RESOURCES_BASE, ValidationUtilIT.class.getSimpleName());

  /** Directory containing mappings to validate. */
  private static final File mappingsDir = new File(baseFilesDir, "mappings");

  /** File containing expected error message. */
  private static final File expectedErrorsFile = new File(baseFilesDir, "expected-errors.txt");

  /**
   * Validate output of model validation matches expectations.
   *
   * @throws Exception any exception passed through indicates a failure
   */
  @Test
  public void validationOutputShouldMatchExpectations() throws Exception {
    RootBean root = ModelUtil.loadModelFromYamlFileOrDirectory(mappingsDir.getPath());
    try {
      MojoUtil.validateModel(root);
      fail("validation should have thrown an exception");
    } catch (MojoExecutionException error) {
      String expectedErrorMessage =
          Files.readLines(expectedErrorsFile, Charsets.UTF_8).stream()
              .collect(Collectors.joining("\n"));
      assertEquals(expectedErrorMessage.trim(), error.getMessage().trim());
    }
  }
}
