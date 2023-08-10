package gov.cms.bfd.model.codebook.extractor;

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
import java.util.List;
import java.util.Map;
import javax.lang.model.element.Modifier;
import lombok.Builder;

@Builder
public class CodebookEnumGenerator {
  /**
   * The value to stick in the enum constants' JavaDoc for {@link Variable} fields that aren't
   * defined.
   */
  private static final String MISSING_VARIABLE_FIELD = "(N/A)";

  /** Directory containing generated XML files. */
  private final File xmlDirectory;

  /** Base directory for generated source code. */
  private final File javaDirectory;

  /** Java package containing the enum. */
  private final String packageName;

  /** Java class name for the enum. */
  private final String enumName;

  /**
   * Generates source files from the specified package.
   *
   * @throws IOException An {@link IOException} may be thrown if errors are encountered trying to
   *     generate source files.
   */
  public void generateEnum() throws IOException {
    Map<String, Variable> variablesById =
        new CodebookVariableReader(xmlDirectory).buildVariablesMappedById();
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
      for (String paragraph : variable.getDescription().orElse(List.of(MISSING_VARIABLE_FIELD))) {
        variableEnumBuilder.addJavadoc("<p>$L</p>\n", HtmlEscapers.htmlEscaper().escape(paragraph));
      }
      variableEnumBuilder.addJavadoc("</li>\n");
      variableEnumBuilder.addJavadoc(
          "<li><strong>Short Name:</strong> $L</li>\n",
          variable.getShortName().orElse(MISSING_VARIABLE_FIELD));
      variableEnumBuilder.addJavadoc(
          "<li><strong>Long Name:</strong> $L</li>\n", variable.getLongName());
      variableEnumBuilder.addJavadoc(
          "<li><strong>Type:</strong> $L</li>\n",
          variable.getType().map(Object::toString).orElse(MISSING_VARIABLE_FIELD));
      variableEnumBuilder.addJavadoc(
          "<li><strong>Length:</strong> $L</li>\n", variable.getLength());
      variableEnumBuilder.addJavadoc(
          "<li><strong>Source:</strong> $L</li>\n",
          variable.getSource().orElse(MISSING_VARIABLE_FIELD));
      variableEnumBuilder.addJavadoc(
          "<li><strong>Value Format:</strong> $L</li>\n",
          variable.getValueFormat().orElse(MISSING_VARIABLE_FIELD));
      variableEnumBuilder.addJavadoc(
          "<li><strong>Coded Values?:</strong> $L</li>\n", variable.getValueGroups().isPresent());
      variableEnumBuilder.addJavadoc("<li><strong>Comment:</strong>\n");
      for (String paragraph : variable.getComment().orElse(List.of(MISSING_VARIABLE_FIELD))) {
        variableEnumBuilder.addJavadoc("<p>$L</p>\n", HtmlEscapers.htmlEscaper().escape(paragraph));
      }
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
            .initializer("new $T().buildVariablesMappedById()", CodebookVariableReader.class)
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
    columnsEnumFile.writeTo(javaDirectory);
  }
}
