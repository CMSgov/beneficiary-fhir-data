package gov.cms.model.dsl.codegen.plugin;

import gov.cms.model.dsl.codegen.plugin.model.MappingBean;
import gov.cms.model.dsl.codegen.plugin.model.RootBean;
import gov.cms.model.dsl.codegen.plugin.model.validation.ValidationUtil;
import java.io.File;
import java.util.List;
import org.apache.maven.plugin.MojoExecutionException;

/** Utility methods for use in all {@link org.apache.maven.plugin.Mojo} implementations. */
public class MojoUtil {

  /** Prevents instantiation of static utility class. */
  private MojoUtil() {}

  /**
   * Ensures that the specified directory and its parent directories exist.
   *
   * @param outputDirectory path to a directory to be created/verified
   * @return {@link File} representing to the directory
   */
  public static File initializeOutputDirectory(String outputDirectory) {
    File outputDir = new File(outputDirectory);
    outputDir.mkdirs();
    return outputDir;
  }

  /**
   * Creates an exception of {@link MojoExecutionException} class with an error message built by
   * applying the specified {@link String#format} format string and arguments.
   *
   * @param formatString {@link String#format} compatible format string
   * @param args any values required to populate arguments in the format string
   * @return an exception with the specified message
   */
  public static MojoExecutionException createException(String formatString, Object... args) {
    String message = String.format(formatString, args);
    return new MojoExecutionException(message);
  }

  /**
   * Validate the data model and throw a {@link MojoExecutionException} if any of its {@link
   * MappingBean}s contain any validation errors.
   *
   * @param root {@link RootBean} containing all known mappings
   * @throws MojoExecutionException thrown if one or more mappings contain validation errors
   */
  public static void validateModel(RootBean root) throws MojoExecutionException {
    final var message = new StringBuilder();
    final List<ValidationUtil.ValidationResult> results = ValidationUtil.validateModel(root);
    for (ValidationUtil.ValidationResult result : results) {
      if (result.hasErrors()) {
        message.append(String.format("Mapping %s has errors:\n", result.getMapping().getId()));
        for (String error : result.getErrors()) {
          message.append(String.format("    %s\n", error));
        }
      }
    }
    if (message.length() > 0) {
      throw new MojoExecutionException(message.toString());
    }
  }
}
