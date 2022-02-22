package gov.cms.bfd.model.codegen.codebook;

import com.google.common.html.HtmlEscapers;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import gov.cms.bfd.model.codebook.model.CcwCodebookInterface;
import gov.cms.bfd.model.codebook.model.Codebook;
import gov.cms.bfd.model.codebook.model.Variable;
import gov.cms.bfd.model.codebook.unmarshall.CodebookVariableReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Map;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.tools.Diagnostic;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/** A Maven Mojo that generates the CcwCodebookVariable enum. */
@Mojo(name = "codebook-variables-enum", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class CodebookVariablesEnumMojo extends AbstractMojo {
  /** The {@link Class#getSimpleName()} of the {@link Enum} to generate */
  @Parameter(property = "enumName", defaultValue = "CcwCodebookVariable")
  private String enumName;

  /** The {@link Package#getName()} of the {@link Package} in which to generate the enum. */
  @Parameter(property = "packageName")
  private String packageName;

  @Parameter(
      property = "outputDirectory",
      defaultValue = "${project.build.directory}/generated-sources")
  private String outputDirectory;

  @Parameter(property = "project", readonly = true)
  private MavenProject project;

  /**
   * The value to stick in the enum constants' JavaDoc for {@link Variable} fields that aren't
   * defined.
   */
  private static final String MISSING_VARIABLE_FIELD = "(N/A)";

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    try {
      File outputDir = new File(outputDirectory);
      outputDir.mkdirs();

      log("Processing triggered for package '%s' and enum '%s'.", packageName, enumName);
      log("Processor sysprop java.class.path: " + System.getProperty("java.class.path"));
      log(
          "Processor classloader URLs: "
              + Arrays.toString(((URLClassLoader) getClass().getClassLoader()).getURLs()));

      processPackage(outputDir);

      project.addCompileSourceRoot(outputDirectory);
    } catch (Exception e) {
      StringWriter writer = new StringWriter();
      e.printStackTrace(new PrintWriter(writer));
      logError("FATAL ERROR: '%s'", writer.toString());
      throw new MojoExecutionException("FATAL ERROR", e);
    }
  }

  /**
   * @throws IOException An {@link IOException} may be thrown if errors are encountered trying to
   *     generate source files.
   */
  private void processPackage(File outputDir) throws IOException {
    Map<String, Variable> variablesById = CodebookVariableReader.buildVariablesMappedById();
    ClassName variableEnumName = ClassName.get(packageName, enumName);
    TypeSpec.Builder variablesEnumType =
        TypeSpec.enumBuilder(variableEnumName)
            .addModifiers(Modifier.PUBLIC)
            .addSuperinterface(CcwCodebookInterface.class);

    variablesEnumType.addJavadoc(
        "Enumerates the known CCW {@link $T} {@link $T}s, as extracted from the codebook"
            + " PDFs at <a href=\"https://www.ccwdata.org/web/guest/data-dictionaries\">CCW Data"
            + " Dictionaries</a>.\n",
        Codebook.class,
        Variable.class);

    for (Variable variable : variablesById.values()) {
      /*
       * Adds a standard enum constant, but with a lot of JavaDoc. Pulling this info
       * into IDEs should make development a lot easier for folks. (Note: technically,
       * we should HTML-escape everything, but I only bothered with the fields that
       * were actually causing problems, such as descriptions.)
       */
      TypeSpec.Builder variableEnumBuilder = TypeSpec.anonymousClassBuilder("");
      variableEnumBuilder.addJavadoc(
          "<p>The {@code $L} CCW variable has the following properties (taken from its codebook PDF at"
              + " <a href=\"https://www.ccwdata.org/web/guest/data-dictionaries\">CCW"
              + " Data Dictionaries</a>):</p>\n",
          variable.getId());
      variableEnumBuilder.addJavadoc("<ul>\n");
      variableEnumBuilder.addJavadoc(
          "<li><strong>Codebook:</strong> $L ($L)</li>\n",
          variable.getCodebook().getName(),
          variable.getCodebook().getVersion());
      variableEnumBuilder.addJavadoc("<li><strong>Label:</strong> $L</li>\n", variable.getLabel());
      variableEnumBuilder.addJavadoc("<li><strong>Description:</strong>\n");
      for (String paragraph :
          variable.getDescription().orElse(Arrays.asList(MISSING_VARIABLE_FIELD)))
        variableEnumBuilder.addJavadoc("<p>$L</p>\n", HtmlEscapers.htmlEscaper().escape(paragraph));
      variableEnumBuilder.addJavadoc("</li>\n");
      variableEnumBuilder.addJavadoc(
          "<li><strong>Short Name:</strong> $L</li>\n",
          variable.getShortName().orElse(MISSING_VARIABLE_FIELD));
      variableEnumBuilder.addJavadoc(
          "<li><strong>Long Name:</strong> $L</li>\n", variable.getLongName());
      variableEnumBuilder.addJavadoc(
          "<li><strong>Type:</strong> $L</li>\n",
          variable.getType().isPresent()
              ? variable.getType().get().toString()
              : MISSING_VARIABLE_FIELD);
      variableEnumBuilder.addJavadoc(
          "<li><strong>Length:</strong> $L</li>\n", variable.getLength());
      variableEnumBuilder.addJavadoc(
          "<li><strong>Source:</strong> $L</li>\n",
          variable.getSource().orElse(MISSING_VARIABLE_FIELD));
      variableEnumBuilder.addJavadoc(
          "<li><strong>Value Format:</strong> $L</li>\n",
          variable.getValueFormat().orElse(MISSING_VARIABLE_FIELD));
      if (variable.getValueGroups() != null)
        variableEnumBuilder.addJavadoc(
            "<li><strong>Coded Values?:</strong> $L</li>\n", variable.getValueGroups().isPresent());
      variableEnumBuilder.addJavadoc("<li><strong>Comment:</strong>\n");
      for (String paragraph : variable.getComment().orElse(Arrays.asList(MISSING_VARIABLE_FIELD)))
        variableEnumBuilder.addJavadoc("<p>$L</p>\n", HtmlEscapers.htmlEscaper().escape(paragraph));
      variableEnumBuilder.addJavadoc("</li>\n");
      variableEnumBuilder.addJavadoc("</ul>\n");
      variablesEnumType.addEnumConstant(variable.getId(), variableEnumBuilder.build());
    }

    variablesEnumType.addField(
        FieldSpec.builder(
                ParameterizedTypeName.get(Map.class, String.class, Variable.class),
                "VARIABLES_BY_ID",
                Modifier.PRIVATE,
                Modifier.STATIC,
                Modifier.FINAL)
            .initializer("$T.buildVariablesMappedById()", CodebookVariableReader.class)
            .build());
    variablesEnumType.addMethod(
        MethodSpec.methodBuilder("getVariable")
            .addModifiers(Modifier.PUBLIC)
            .addStatement("return VARIABLES_BY_ID.get(this.name())")
            .returns(Variable.class)
            .addJavadoc(
                "@return the {@link $T} data (parsed from a codebook PDF) for this {@link $T} constant\n",
                Variable.class,
                variableEnumName)
            .build());

    TypeSpec columnEnumFinal = variablesEnumType.build();
    JavaFile columnsEnumFile = JavaFile.builder(packageName, columnEnumFinal).build();
    columnsEnumFile.writeTo(outputDir);
  }

  /**
   * Reports the specified log message.
   *
   * @param logEntryKind the {@link Diagnostic.Kind} of log entry to add
   * @param associatedElement the Java AST {@link Element} that the log entry should be associated
   *     with, or <code>null</code>
   * @param messageFormat the log message format {@link String}
   * @param messageArguments the log message format arguments
   */
  private void log(String messageFormat, Object... messageArguments) {
    String logMessage = String.format(messageFormat, messageArguments);
    getLog().info(logMessage);
  }

  private void logError(String messageFormat, Object... messageArguments) {
    String logMessage = String.format(messageFormat, messageArguments);
    getLog().error(logMessage);
  }
}
