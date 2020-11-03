package gov.cms.bfd.model.codegen.codebook;

import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableSet;
import com.google.common.html.HtmlEscapers;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeSpec.Builder;
import gov.cms.bfd.model.codebook.model.CcwCodebookInterface;
import gov.cms.bfd.model.codebook.model.Codebook;
import gov.cms.bfd.model.codebook.model.Variable;
import gov.cms.bfd.model.codebook.unmarshall.CodebookVariableReader;
import gov.cms.bfd.model.codegen.RifLayoutProcessingException;
import gov.cms.bfd.model.codegen.annotations.CodebookVariableEnumGeneration;
import gov.cms.bfd.model.codegen.annotations.RifLayoutsGenerator;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

/**
 * This <code>javac</code> annotation {@link Processor} reads in the available {@link Codebook}s,
 * and then generates XXX.
 */
@AutoService(Processor.class)
public class CodebookVariablesEnumProcessor extends AbstractProcessor {
  /**
   * The value to stick in the enum constants' JavaDoc for {@link Variable} fields that aren't
   * defined.
   */
  private static final String MISSING_VARIABLE_FIELD = "(N/A)";

  /**
   * Both Maven and Eclipse hide compiler messages, so setting this constant to <code>true</code>
   * will also log messages out to a new source file.
   */
  private static final boolean DEBUG = true;

  private final List<String> logMessages = new LinkedList<>();

  /** @see javax.annotation.processing.AbstractProcessor#getSupportedAnnotationTypes() */
  @Override
  public Set<String> getSupportedAnnotationTypes() {
    return ImmutableSet.of(CodebookVariableEnumGeneration.class.getName());
  }

  /** @see javax.annotation.processing.AbstractProcessor#getSupportedSourceVersion() */
  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latestSupported();
  }

  /**
   * @see javax.annotation.processing.AbstractProcessor#process(java.util.Set,
   *     javax.annotation.processing.RoundEnvironment)
   */
  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    try {
      logNote(
          "Processing triggered for '%s' on root elements '%s'.",
          annotations, roundEnv.getRootElements());

      Set<? extends Element> annotatedElements =
          roundEnv.getElementsAnnotatedWith(CodebookVariableEnumGeneration.class);
      for (Element annotatedElement : annotatedElements) {
        if (annotatedElement.getKind() != ElementKind.PACKAGE)
          throw new RifLayoutProcessingException(
              annotatedElement,
              "The %s annotation is only valid on packages (i.e. in package-info.java).",
              RifLayoutsGenerator.class.getName());
        process((PackageElement) annotatedElement);
      }
    } catch (RifLayoutProcessingException e) {
      log(Diagnostic.Kind.ERROR, e.getMessage(), e.getElement());
    } catch (Exception e) {
      /*
       * Don't allow exceptions of any type to propagate to the compiler. Log a
       * warning and return, instead.
       */
      StringWriter writer = new StringWriter();
      e.printStackTrace(new PrintWriter(writer));
      log(Diagnostic.Kind.ERROR, "FATAL ERROR: " + writer.toString());
    }

    if (roundEnv.processingOver()) writeDebugLogMessages();

    return true;
  }

  /**
   * @param annotatedPackage the {@link PackageElement} to process that has been annotated with
   *     {@link CodebookVariableEnumGeneration}
   * @throws IOException An {@link IOException} may be thrown if errors are encountered trying to
   *     generate source files.
   */
  private void process(PackageElement annotatedPackage) throws IOException {
    CodebookVariableEnumGeneration annotation =
        annotatedPackage.getAnnotation(CodebookVariableEnumGeneration.class);
    logNote(annotatedPackage, "Processing package annotated with: '%s'.", annotation);

    Map<String, Variable> variablesById = CodebookVariableReader.buildVariablesMappedById();
    ClassName variableEnumName =
        ClassName.get(annotatedPackage.getQualifiedName().toString(), annotation.enumName());
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
      Builder variableEnumBuilder = TypeSpec.anonymousClassBuilder("");
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
    JavaFile columnsEnumFile =
        JavaFile.builder(annotatedPackage.getQualifiedName().toString(), columnEnumFinal).build();
    columnsEnumFile.writeTo(processingEnv.getFiler());
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
  private void log(
      Diagnostic.Kind logEntryKind,
      Element associatedElement,
      String messageFormat,
      Object... messageArguments) {
    String logMessage = String.format(messageFormat, messageArguments);
    processingEnv.getMessager().printMessage(logEntryKind, logMessage, associatedElement);

    String logMessageFull;
    if (associatedElement != null)
      logMessageFull =
          String.format("[%s] at '%s': %s", logEntryKind, associatedElement, logMessage);
    else logMessageFull = String.format("[%s]: %s", logEntryKind, logMessage);
    logMessages.add(logMessageFull);
  }

  /**
   * Reports the specified log message.
   *
   * @param logEntryKind the {@link Diagnostic.Kind} of log entry to add
   * @param messageFormat the log message format {@link String}
   * @param messageArguments the log message format arguments
   */
  private void log(Diagnostic.Kind logEntryKind, String messageFormat, Object... messageArguments) {
    log(logEntryKind, null, messageFormat, messageArguments);
  }

  /**
   * Reports the specified log message.
   *
   * @param associatedElement the Java AST {@link Element} that the log entry should be associated
   *     with, or <code>null</code>
   * @param messageFormat the log message format {@link String}
   * @param messageArguments the log message format arguments
   */
  private void logNote(
      Element associatedElement, String messageFormat, Object... messageArguments) {
    log(Diagnostic.Kind.NOTE, associatedElement, messageFormat, messageArguments);
  }

  /**
   * Reports the specified log message.
   *
   * @param associatedElement the Java AST {@link Element} that the log entry should be associated
   *     with, or <code>null</code>
   * @param messageFormat the log message format {@link String}
   * @param messageArguments the log message format arguments
   */
  private void logNote(String messageFormat, Object... messageArguments) {
    log(Diagnostic.Kind.NOTE, null, messageFormat, messageArguments);
  }

  /**
   * Writes out all of the messages in {@link #logMessages} to a log file in the
   * annotation-generated source directory.
   */
  private void writeDebugLogMessages() {
    if (!DEBUG) return;

    try {
      FileObject logResource =
          processingEnv
              .getFiler()
              .createResource(
                  StandardLocation.SOURCE_OUTPUT, "", this.getClass().getSimpleName() + "-log.txt");
      Writer logWriter = logResource.openWriter();
      for (String logMessage : logMessages) {
        logWriter.write(logMessage);
        logWriter.write('\n');
      }
      logWriter.flush();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
