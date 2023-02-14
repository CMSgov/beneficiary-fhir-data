package gov.cms.model.dsl.codegen.plugin;

import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import gov.cms.model.dsl.codegen.plugin.model.ColumnBean;
import gov.cms.model.dsl.codegen.plugin.model.JoinBean;
import gov.cms.model.dsl.codegen.plugin.model.MappingBean;
import gov.cms.model.dsl.codegen.plugin.model.ModelUtil;
import gov.cms.model.dsl.codegen.plugin.model.RootBean;
import gov.cms.model.dsl.codegen.plugin.model.TransformationBean;
import gov.cms.model.dsl.codegen.plugin.transformer.TransformerUtil;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.lang.model.element.Modifier;
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
 *
 * <ul>
 *   <li>The column names used by line entities are taken from the parent entity's fields rather
 *       than the line entity's fields when there is a field name conflict. This would seem to be
 *       incorrect since in the RIF data the line column names are different.
 *   <li>The beneficiary history id value is added to the array for BeneficiaryHistory but no
 *       matching column name is added. This creates a mis-alignment in the field names ot values.
 *   <li>The join accessor methods are called and the resulting collections are added to the end of
 *       the values array even though there is no corresponding header added to the headers array
 *       for them.
 * </ul>
 */
@Mojo(name = "csv-writers", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class GenerateCsvWritersFromDslMojo extends AbstractMojo {
  /** Suffix added to entity name to create name of generated class. */
  public static final String CLASS_NAME_SUFFIX = "CsvWriter";
  /** Name of variable used to reference the entity object. */
  public static final String ENTITY_VAR_NAME = "entity";
  /** Name of variable used to hold reference to each line entity. */
  public static final String LINE_ENTITY_VAR_NAME = "lineEntity";
  /** Name of the generated public method. */
  public static final String METHOD_NAME = "toCsvRecordsByTable";
  /** Name used to reference the map containing the header and line arrays. */
  private static final String RETURN_VALUE_VAR_NAME = "csvRecordsByTable";

  /** Path to a single mapping file or a directory containing one or more mapping files. */
  @Parameter(property = "mappingPath")
  private String mappingPath;

  /** Path to directory to contain generated code. */
  @Parameter(
      property = "csvWritersDirectory",
      defaultValue = "${project.build.directory}/generated-sources/csv_writers")
  private String csvWritersDirectory;

  /**
   * Instance of {@link MavenProject} used to call {@link MavenProject#addCompileSourceRoot(String)}
   * to ensure our generated classes are compiled.
   */
  @Parameter(property = "project", readonly = true)
  private MavenProject project;

  /** Parameterless constructor used by Maven to instantiate the plugin. */
  public GenerateCsvWritersFromDslMojo() {}

  /**
   * All fields constructor for use in unit tests.
   *
   * @param mappingPath path to file or directory containing mappings
   * @param csvWritersDirectory path to directory to contain generated code
   * @param project instance of {@link MavenProject}
   */
  public GenerateCsvWritersFromDslMojo(
      String mappingPath, String csvWritersDirectory, MavenProject project) {
    this.mappingPath = mappingPath;
    this.csvWritersDirectory = csvWritersDirectory;
    this.project = project;
  }

  /**
   * Executed by maven to execute the mojo. Reads all mapping files and generates a csv writer class
   * for every {@link MappingBean} that uses RIF data as its source and has a non-empty {@link
   * MappingBean#transformerClassName} value.
   *
   * @throws MojoExecutionException if the process fails due to some error
   */
  public void execute() throws MojoExecutionException {
    try {
      final File outputDir = MojoUtil.initializeOutputDirectory(csvWritersDirectory);
      final RootBean root = ModelUtil.loadModelFromYamlFileOrDirectory(mappingPath);
      MojoUtil.validateModel(root);
      for (MappingBean mapping : root.getMappings()) {
        if (mapping.getSourceType() == MappingBean.SourceType.RifCsv && mapping.hasTransformer()) {
          TypeSpec rootEntity = createCsvWriterClassForMapping(root, mapping);
          JavaFile javaFile = JavaFile.builder(mapping.transformerPackage(), rootEntity).build();
          javaFile.writeTo(outputDir);
        }
      }
      project.addCompileSourceRoot(csvWritersDirectory);
    } catch (IOException ex) {
      throw new MojoExecutionException("I/O error during code generation", ex);
    }
  }

  /**
   * Creates a {@link TypeSpec} defining a csv writer class for the given {@link MappingBean}. The
   * public {@link #METHOD_NAME} method of the class processes one list of RIF CSV records and
   * produces a {@link Map} containing the header and (if required) line record arrays.
   *
   * @param root {@link RootBean} containing all known mappings
   * @param mapping {@link MappingBean} to create csv writer class for
   * @return the {@link TypeSpec}
   * @throws MojoExecutionException if any problems arise
   */
  private TypeSpec createCsvWriterClassForMapping(RootBean root, MappingBean mapping)
      throws MojoExecutionException {
    final var writerClassName =
        ClassName.get(mapping.getEntityClassPackage(), mapping.getId() + CLASS_NAME_SUFFIX);
    TypeSpec.Builder csvWriterClass =
        TypeSpec.classBuilder(writerClassName).addModifiers(Modifier.PUBLIC, Modifier.FINAL);

    // Grab some common types we'll need.
    ArrayTypeName recordType = ArrayTypeName.of(Object.class);
    ArrayTypeName recordsListType = ArrayTypeName.of(recordType);
    ParameterizedTypeName returnType =
        ParameterizedTypeName.get(
            ClassName.get(Map.class), ClassName.get(String.class), recordsListType);

    final var columnsByFieldName = new HashMap<String, String>();
    addFirstNamedFieldToColumnNameMap(mapping, columnsByFieldName);

    final var entityClassName = TransformerUtil.toClassName(mapping.getEntityClassName());
    MethodSpec.Builder csvWriterMethod =
        MethodSpec.methodBuilder(METHOD_NAME)
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .returns(returnType)
            .addParameter(entityClassName, ENTITY_VAR_NAME);

    csvWriterMethod.addComment("Verify the input.");
    csvWriterMethod.addStatement("$T.requireNonNull($L)", Objects.class, ENTITY_VAR_NAME);

    csvWriterMethod.addCode("\n");
    csvWriterMethod.addStatement(
        "$T $L = new $T<>(2)", returnType, RETURN_VALUE_VAR_NAME, HashMap.class);

    // Generate the header conversion.
    csvWriterMethod.addCode("\n");
    addHeaderRecords(mapping, recordType, recordsListType, columnsByFieldName, csvWriterMethod);

    if (mapping.getArrayJoins().size() == 1) {
      csvWriterMethod.addCode("\n");
      addLineRecords(
          root,
          mapping,
          recordType,
          recordsListType,
          columnsByFieldName,
          csvWriterMethod,
          mapping.getArrayJoins().get(0));
    }

    csvWriterMethod.addStatement("return $L", RETURN_VALUE_VAR_NAME);
    csvWriterClass.addMethod(csvWriterMethod.build());

    return csvWriterClass.build();
  }

  /**
   * Adds the statements that define the header records.
   *
   * @param mapping {@link MappingBean} to create csv writer class for
   * @param recordType {@link TypeSpec} for array of values
   * @param recordsListType {@link TypeSpec} for array of records
   * @param columnsByFieldName lookup table of column name to CSV field name
   * @param csvWriterMethod {@link MethodSpec.Builder} to add statements to
   */
  private void addHeaderRecords(
      MappingBean mapping,
      ArrayTypeName recordType,
      ArrayTypeName recordsListType,
      HashMap<String, String> columnsByFieldName,
      MethodSpec.Builder csvWriterMethod) {
    csvWriterMethod.addComment("Convert the header fields.");
    csvWriterMethod.addStatement("$T headerRecords = new $T[2][]", recordsListType, Object.class);

    final var headerColumnsList = calculateCsvColumns(mapping, columnsByFieldName);
    final var headerGettersList = calculateCsvValues(mapping, ENTITY_VAR_NAME);
    csvWriterMethod.addStatement(
        "headerRecords[0] = new $1T{ $2L }", recordType, headerColumnsList);
    csvWriterMethod.addStatement(
        "$1T headerRecord = new $1T{ $2L }", recordType, headerGettersList);
    csvWriterMethod.addStatement("headerRecords[1] = headerRecord");
    csvWriterMethod.addStatement(
        "$L.put($S, headerRecords)", RETURN_VALUE_VAR_NAME, mapping.getTable().getName());
  }

  /**
   * Add the statements that define the line records.
   *
   * @param root {@link RootBean} containing all known mappings
   * @param mapping {@link MappingBean} to create csv writer class for
   * @param recordType {@link TypeSpec} for array of values
   * @param recordsListType {@link TypeSpec} for array of records
   * @param columnsByFieldName lookup table of column name to CSV field name
   * @param csvWriterMethod {@link MethodSpec.Builder} to add statements to
   * @param arrayJoin {@link JoinBean} specifying the join field name and line item entity
   * @throws MojoExecutionException if any problems arise
   */
  private void addLineRecords(
      RootBean root,
      MappingBean mapping,
      ArrayTypeName recordType,
      ArrayTypeName recordsListType,
      HashMap<String, String> columnsByFieldName,
      MethodSpec.Builder csvWriterMethod,
      JoinBean arrayJoin)
      throws MojoExecutionException {
    final var lineMapping =
        root.findMappingForJoinBean(arrayJoin)
            .orElseThrow(
                () ->
                    MojoUtil.createException(
                        "No known mapping found for join: mapping=%s join=%s to=%s",
                        mapping.getId(), arrayJoin.getFieldName(), arrayJoin.getEntityMapping()));
    addFirstNamedFieldToColumnNameMap(lineMapping, columnsByFieldName);

    final var linesFieldName = arrayJoin.getFieldName();
    final var linesFieldGetter =
        ENTITY_VAR_NAME + "." + PoetUtil.fieldToMethodName("get", linesFieldName) + "()";
    final var parentIdGetter =
        LINE_ENTITY_VAR_NAME
            + ".getParentClaim()."
            + PoetUtil.fieldToMethodName("get", mapping.getTable().getPrimaryKeyColumns().get(0))
            + "()";

    csvWriterMethod.addComment("Convert the line fields.");
    csvWriterMethod.addStatement(
        "$T lineRecords = new $T[$L.size() + 1][]",
        recordsListType,
        Object.class,
        linesFieldGetter);
    csvWriterMethod.addStatement(
        "$L.put($S, lineRecords)", RETURN_VALUE_VAR_NAME, lineMapping.getTable().getName());

    final var lineColumnsList = calculateCsvColumns(lineMapping, columnsByFieldName);
    final var lineGettersList =
        parentIdGetter + ", " + calculateCsvValues(lineMapping, LINE_ENTITY_VAR_NAME);

    csvWriterMethod.addStatement("lineRecords[0] = new $1T{ $2L }", recordType, lineColumnsList);
    csvWriterMethod.beginControlFlow(
        "for (int lineIndex = 0; lineIndex < $L.size();lineIndex++)", linesFieldGetter);
    csvWriterMethod.addStatement(
        "$T $L = $L.get(lineIndex)",
        TransformerUtil.toClassName(lineMapping.getEntityClassName()),
        LINE_ENTITY_VAR_NAME,
        linesFieldGetter);
    csvWriterMethod.addStatement("$1T lineRecord = new $1T{ $2L }", recordType, lineGettersList);
    csvWriterMethod.addStatement("lineRecords[lineIndex + 1] = lineRecord");
    csvWriterMethod.endControlFlow();
  }

  /**
   * Creates a string containing all of the column names for the given entity separated by commas.
   *
   * @param mapping {@link MappingBean} containing the columns
   * @param columnsByFieldName lookup table of column name to CSV field name
   * @return the string
   */
  private String calculateCsvColumns(MappingBean mapping, Map<String, String> columnsByFieldName) {
    final var columnNames = new ArrayList<String>();
    for (ColumnBean column : mapping.getTable().getColumns()) {
      if (column.hasSequence()) {
        // WORK AROUND: For some reason the annotation processor code does not print a header for
        // the sequence id but does include its value so we simulate that here.
        continue;
      }
      if (column.isDbOnly()) {
        // skip columns that aren't actually properties of the entity
        continue;
      }
      columnNames.add(columnsByFieldName.get(column.getName()));
    }
    return columnNames.stream().map(name -> "\"" + name + "\"").collect(Collectors.joining(", "));
  }

  /**
   * Creates a string containing all of the column values for the given entity separated by commas.
   *
   * @param mapping {@link MappingBean} containing the columns
   * @param entityVarName name of variable holding the entity object
   * @return the string
   */
  private String calculateCsvValues(MappingBean mapping, String entityVarName) {
    final var getters = new ArrayList<String>();
    for (ColumnBean column : mapping.getTable().getColumns()) {
      if (column.isDbOnly()) {
        // skip columns that aren't actually fields in the entity
        continue;
      }
      final var transformation = mapping.findTransformationByToName(column.getName());
      final var isOptional = transformation.map(TransformationBean::isOptional).orElse(false);
      getters.add(createPropertyGetter(entityVarName, column.getName(), isOptional));
    }
    for (JoinBean join : mapping.getNonArrayJoins()) {
      if (mapping.getTable().isPrimaryKey(join)) {
        // don't include joins where the field is also the primary key of the table
        continue;
      }
      getters.add(createPropertyGetter(entityVarName, join.getFieldName(), false));
    }
    return String.join(", ", getters);
  }

  /**
   * Creates a string containing an appropriate getter call to access the specified property.
   * Optional fields are mapped to null if empty.
   *
   * @param entityVarName name of variable holding the entity object
   * @param propertyName name of the property being accessed
   * @param isOptional true if property os an {@link java.util.Optional}
   * @return the string
   */
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
   *
   * @param mapping {@link MappingBean} containing the columns
   * @param columnsByFieldName lookup table of column name to CSV field name
   */
  private void addFirstNamedFieldToColumnNameMap(
      MappingBean mapping, Map<String, String> columnsByFieldName) {
    for (ColumnBean column : mapping.getTable().getColumns()) {
      if (!columnsByFieldName.containsKey(column.getName())) {
        columnsByFieldName.put(column.getName(), column.getDbName().toUpperCase());
      }
    }
  }
}
