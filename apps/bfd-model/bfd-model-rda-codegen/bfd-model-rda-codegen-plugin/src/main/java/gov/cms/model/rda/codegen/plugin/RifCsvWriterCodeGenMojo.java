package gov.cms.model.rda.codegen.plugin;

import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import gov.cms.model.rda.codegen.plugin.model.ColumnBean;
import gov.cms.model.rda.codegen.plugin.model.JoinBean;
import gov.cms.model.rda.codegen.plugin.model.MappingBean;
import gov.cms.model.rda.codegen.plugin.model.ModelUtil;
import gov.cms.model.rda.codegen.plugin.model.RootBean;
import gov.cms.model.rda.codegen.plugin.model.TransformationBean;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.lang.model.element.Modifier;
import lombok.SneakyThrows;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * Generates a CsvWriter class compatible with the one created by the current annotation processor.
 * There are some features in the generated class that might be legacy bugs but they are being
 * reproduced here for now in case they are not bugs.
 * <li>The column names used by line entities are taken from the parent entity's fields rather than
 *     the line entity's fields when there is a field name conflict. This would seem to be incorrect
 *     since in the RIF data the line column names are different.
 * <li>The beneficiary history id value is added to the array for BeneficiaryHistory but no matching
 *     column name is added. This creates a mis-alignment in the field names ot values.
 * <li>The join accessor methods are called and the resulting collections are added to the end of
 *     the values array even though there is no corresponding header added to the headers array for
 *     them.
 */
@Mojo(name = "csv-writers", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class RifCsvWriterCodeGenMojo extends AbstractMojo {
  private static final String CLASS_NAME_SUFFIX = "CsvWriter";
  public static final String ENTITY_VAR_NAME = "entity";
  public static final String LINE_ENTITY_VAR_NAME = "lineEntity";

  @Parameter(property = "mappingFile")
  private String mappingFile;

  @Parameter(
      property = "outputDirectory",
      defaultValue = "${project.build.directory}/generated-sources/rda-entities")
  private String outputDirectory;

  @Parameter(property = "project", readonly = true)
  private MavenProject project;

  @SneakyThrows(IOException.class)
  public void execute() throws MojoExecutionException {
    final File outputDir = MojoUtil.initializeOutputDirectory(outputDirectory);
    final RootBean root = ModelUtil.loadModelFromYamlFileOrDirectory(mappingFile);
    for (MappingBean mapping : root.getMappings()) {
      if (mapping.getSourceType() == MappingBean.SourceType.RifCsv && mapping.hasTransformer()) {
        TypeSpec rootEntity = createCsvWriterClassForMapping(mapping, root::findMappingWithId);
        JavaFile javaFile = JavaFile.builder(mapping.transformerPackage(), rootEntity).build();
        javaFile.writeTo(outputDir);
      }
    }
    project.addCompileSourceRoot(outputDirectory);
  }

  private TypeSpec createCsvWriterClassForMapping(
      MappingBean mapping, Function<String, Optional<MappingBean>> findMappingWithId) {
    final var writerClassName =
        ClassName.get(mapping.entityPackageName(), mapping.getId() + CLASS_NAME_SUFFIX);
    TypeSpec.Builder csvWriterClass =
        TypeSpec.classBuilder(writerClassName).addModifiers(Modifier.PUBLIC, Modifier.FINAL);

    // Grab some common types we'll need.
    ArrayTypeName recordType = ArrayTypeName.of(Object.class);
    ArrayTypeName recordsListType = ArrayTypeName.of(recordType);
    ParameterizedTypeName returnType =
        ParameterizedTypeName.get(
            ClassName.get(Map.class), ClassName.get(String.class), recordsListType);

    final var columnsByFieldName = new HashMap<String, ColumnBean>();
    addFirstNamedFieldToColumnMap(columnsByFieldName, mapping);

    final var entityClassName = PoetUtil.toClassName(mapping.getEntityClassName());
    MethodSpec.Builder csvWriterMethod =
        MethodSpec.methodBuilder("toCsvRecordsByTable")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .returns(returnType)
            .addParameter(entityClassName, ENTITY_VAR_NAME);

    csvWriterMethod.addComment("Verify the input.");
    csvWriterMethod.addStatement("$T.requireNonNull($L)", Objects.class, ENTITY_VAR_NAME);

    csvWriterMethod.addCode("\n");
    csvWriterMethod.addStatement("$T csvRecordsByTable = new $T<>(2)", returnType, HashMap.class);

    // Generate the header conversion.
    csvWriterMethod.addCode("\n");
    csvWriterMethod.addComment("Convert the header fields.");
    csvWriterMethod.addStatement("$T headerRecords = new $T[2][]", recordsListType, Object.class);

    final var headerColumnsList = calculateCsvColumns(columnsByFieldName, mapping);
    final var headerGettersList = calculateCsvValues(ENTITY_VAR_NAME, mapping);
    csvWriterMethod.addStatement(
        "headerRecords[0] = new $1T{ $2L }", recordType, headerColumnsList);
    csvWriterMethod.addStatement(
        "$1T headerRecord = new $1T{ $2L }", recordType, headerGettersList);
    csvWriterMethod.addStatement("headerRecords[1] = headerRecord");
    csvWriterMethod.addStatement(
        "csvRecordsByTable.put($S, headerRecords)", mapping.getTable().getName());

    if (mapping.getArrays().size() == 1) {
      final var arrayElement = mapping.getArrays().get(0);
      final var lineMapping = findMappingWithId.apply(arrayElement.getMapping()).get();
      addFirstNamedFieldToColumnMap(columnsByFieldName, lineMapping);

      final var linesFieldName = arrayElement.getTo();
      final var linesFieldGetter =
          ENTITY_VAR_NAME + "." + PoetUtil.fieldToMethodName("get", linesFieldName) + "()";
      final var parentIdGetter =
          LINE_ENTITY_VAR_NAME
              + ".getParentClaim()."
              + PoetUtil.fieldToMethodName("get", mapping.getTable().getPrimaryKeyColumns().get(0))
              + "()";

      csvWriterMethod.addCode("\n");
      csvWriterMethod.addComment("Convert the line fields.");
      csvWriterMethod.addStatement(
          "$T lineRecords = new $T[$L.size() + 1][]",
          recordsListType,
          Object.class,
          linesFieldGetter);
      csvWriterMethod.addStatement(
          "csvRecordsByTable.put($S, lineRecords)", lineMapping.getTable().getName());

      final var lineColumnsList = calculateCsvColumns(columnsByFieldName, lineMapping);
      final var lineGettersList =
          parentIdGetter + ", " + calculateCsvValues(LINE_ENTITY_VAR_NAME, lineMapping);

      csvWriterMethod.addStatement("lineRecords[0] = new $1T{ $2L }", recordType, lineColumnsList);
      csvWriterMethod.beginControlFlow(
          "for (int lineIndex = 0; lineIndex < $L.size();lineIndex++)", linesFieldGetter);
      csvWriterMethod.addStatement(
          "$T $L = $L.get(lineIndex)",
          PoetUtil.toClassName(lineMapping.getEntityClassName()),
          LINE_ENTITY_VAR_NAME,
          linesFieldGetter);
      csvWriterMethod.addStatement("$1T lineRecord = new $1T{ $2L }", recordType, lineGettersList);
      csvWriterMethod.addStatement("lineRecords[lineIndex + 1] = lineRecord");
      csvWriterMethod.endControlFlow();
    }

    csvWriterMethod.addStatement("return csvRecordsByTable");
    csvWriterClass.addMethod(csvWriterMethod.build());

    return csvWriterClass.build();
  }

  private String calculateCsvColumns(
      Map<String, ColumnBean> columnsByFieldName, MappingBean mapping) {
    final var columnNames = new ArrayList<String>();
    for (ColumnBean column : mapping.getTable().getColumns()) {
      if (column.hasSequence()) {
        // WORK AROUND: For some reason the annotation processor code does not print a header for
        // the sequence id but does include its value so we simulate that here.
        continue;
      }
      columnNames.add(columnsByFieldName.get(column.getName()).getDbName().toUpperCase());
    }
    return columnNames.stream().map(name -> "\"" + name + "\"").collect(Collectors.joining(", "));
  }

  private String calculateCsvValues(String entityVarName, MappingBean mapping) {
    final var getters = new ArrayList<String>();
    for (ColumnBean column : mapping.getTable().getColumns()) {
      final var transformation = mapping.findTransformationByToName(column.getName());
      final var isOptional = transformation.map(TransformationBean::isOptional).orElse(false);
      getters.add(createPropertyGetter(entityVarName, column.getName(), isOptional));
    }
    for (JoinBean join : mapping.getTable().getJoins()) {
      if (mapping.getTable().isPrimaryKey(join)) {
        // don't include joins where the field is also the primary key of the table
        continue;
      }
      if (mapping.findArrayByFromName(join.getFieldName()).isPresent()) {
        // don't include joins that are also arrays because those are RIF lines
        continue;
      }
      getters.add(createPropertyGetter(entityVarName, join.getFieldName(), false));
    }
    return String.join(", ", getters);
  }

  private String createPropertyGetter(
      String entityVarName, String propertyName, boolean isOptional) {
    var code = entityVarName + "." + PoetUtil.fieldToMethodName("get", propertyName) + "()";
    if (isOptional) {
      code = code + ".orElse(null)";
    }
    return code;
  }

  /**
   * WORK AROUND: The annotation processor does a strange thing when generating CsvWriters. It uses
   * the name of the first available column with a given java field name instead of the appropriate
   * one for the particular entity it is generating code for. This causes the column names for line
   * entities to use the column names from the parent entity field with the same name if both
   * entities have fields with the same name.
   *
   * <p>This method simulates that behavior by building a lookup table of java field name to column
   * name using the first column for any given field name and ignoring any subsequent one.
   *
   * <p>The original behavior might be a bug but fore this POC we want to reproduce the existing
   * behavior.
   */
  private void addFirstNamedFieldToColumnMap(
      Map<String, ColumnBean> columnsByFieldName, MappingBean mapping) {
    for (ColumnBean column : mapping.getTable().getColumns()) {
      if (!columnsByFieldName.containsKey(column.getName())) {
        columnsByFieldName.put(column.getName(), column);
      }
    }
  }
}
