package gov.cms.bfd.model.codegen;

import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableSet;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import gov.cms.bfd.model.codegen.RifLayout.RifColumnType;
import gov.cms.bfd.model.codegen.RifLayout.RifField;
import gov.cms.bfd.model.codegen.annotations.RifLayoutsGenerator;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * This <code>javac</code> annotation {@link Processor} reads in an Excel file that details a RIF
 * field layout, and then generates the Java code required to work with that layout.
 */
@AutoService(Processor.class)
public final class RifLayoutsProcessor extends AbstractProcessor {
  /**
   * Both Maven and Eclipse hide compiler messages, so setting this constant to <code>true</code>
   * will also log messages out to a new source file.
   */
  private static final boolean DEBUG = true;

  private static final String DATA_DICTIONARY_LINK =
      "https://bluebutton.cms.gov/resources/variables/";

  private final List<String> logMessages = new LinkedList<>();

  /** @see javax.annotation.processing.AbstractProcessor#getSupportedAnnotationTypes() */
  @Override
  public Set<String> getSupportedAnnotationTypes() {
    return ImmutableSet.of(RifLayoutsGenerator.class.getName());
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
          roundEnv.getElementsAnnotatedWith(RifLayoutsGenerator.class);
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
       * Don't allow exceptions of any type to propagate to the compiler.
       * Log a warning and return, instead.
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
   *     {@link RifLayoutsGenerator}
   * @throws IOException An {@link IOException} may be thrown if errors are encountered trying to
   *     generate source files.
   */
  private void process(PackageElement annotatedPackage) throws IOException {
    RifLayoutsGenerator annotation = annotatedPackage.getAnnotation(RifLayoutsGenerator.class);
    logNote(annotatedPackage, "Processing package annotated with: '%s'.", annotation);

    /*
     * Find the spreadsheet referenced by the annotation. It will define the
     * RIF layouts.
     */
    FileObject spreadsheetResource;
    try {
      spreadsheetResource =
          processingEnv
              .getFiler()
              .getResource(
                  StandardLocation.SOURCE_PATH,
                  annotatedPackage.getQualifiedName().toString(),
                  annotation.spreadsheetResource());
    } catch (IOException | IllegalArgumentException e) {
      throw new RifLayoutProcessingException(
          annotatedPackage,
          "Unable to find or open specified spreadsheet: '%s'.",
          annotation.spreadsheetResource());
    }
    logNote(annotatedPackage, "Found spreadsheet: '%s'.", annotation.spreadsheetResource());

    /*
     * Parse the spreadsheet, extracting the layouts from it. Also: define
     * the layouts that we expect to parse and generate code for.
     */
    List<MappingSpec> mappingSpecs = new LinkedList<>();
    Workbook spreadsheetWorkbook = null;
    try {
      spreadsheetWorkbook = new XSSFWorkbook(spreadsheetResource.openInputStream());

      mappingSpecs.add(
          new MappingSpec(annotatedPackage.getQualifiedName().toString())
              .setRifLayout(RifLayout.parse(spreadsheetWorkbook, annotation.beneficiarySheet()))
              .setHeaderEntity("Beneficiary")
              .setHeaderTable("Beneficiaries")
              .setHeaderEntityIdField("beneficiaryId")
              .setHeaderEntityAdditionalDatabaseFields(
                  createDetailsForAdditionalDatabaseFields(Arrays.asList("hicnUnhashed")))
              .setInnerJoinRelationship(
                  Arrays.asList(
                      new InnerJoinRelationship(
                          "beneficiaryId", null, "BeneficiaryHistory", "beneficiaryHistories"),
                      new InnerJoinRelationship(
                          "beneficiaryId",
                          null,
                          "MedicareBeneficiaryIdHistory",
                          "medicareBeneficiaryIdHistories")))
              .setHasLines(false));
      /*
       * FIXME Many BeneficiaryHistory fields are marked transient (i.e. not saved to
       * DB), as they won't ever have changed data. We should change the RIF layout to
       * exclude them, but this was implemented in a bit of a rush, and there wasn't
       * time to fix that.
       */
      mappingSpecs.add(
          new MappingSpec(annotatedPackage.getQualifiedName().toString())
              .setRifLayout(
                  RifLayout.parse(spreadsheetWorkbook, annotation.beneficiaryHistorySheet()))
              .setHeaderEntity("BeneficiaryHistory")
              .setHeaderTable("BeneficiariesHistory")
              .setHeaderEntityGeneratedIdField("beneficiaryHistoryId")
              .setHeaderEntityTransientFields(
                  "stateCode",
                  "countyCode",
                  "postalCode",
                  "race",
                  "entitlementCodeOriginal",
                  "entitlementCodeCurrent",
                  "endStageRenalDiseaseCode",
                  "medicareEnrollmentStatusCode",
                  "partATerminationCode",
                  "partBTerminationCode",
                  "nameSurname",
                  "nameGiven",
                  "nameMiddleInitial")
              .setHeaderEntityAdditionalDatabaseFields(
                  createDetailsForAdditionalDatabaseFields(
                      Arrays.asList("hicnUnhashed", "medicareBeneficiaryId")))
              .setHasLines(false));

      mappingSpecs.add(
          new MappingSpec(annotatedPackage.getQualifiedName().toString())
              .setRifLayout(
                  RifLayout.parse(spreadsheetWorkbook, annotation.medicareBeneficiaryIdSheet()))
              .setHeaderEntity("MedicareBeneficiaryIdHistory")
              .setHeaderTable("MedicareBeneficiaryIdHistory")
              .setHeaderEntityIdField("medicareBeneficiaryIdKey")
              .setHasLines(false));

      mappingSpecs.add(
          new MappingSpec(annotatedPackage.getQualifiedName().toString())
              .setRifLayout(RifLayout.parse(spreadsheetWorkbook, annotation.pdeSheet()))
              .setHeaderEntity("PartDEvent")
              .setHeaderTable("PartDEvents")
              .setHeaderEntityIdField("eventId")
              .setHasLines(false));
      mappingSpecs.add(
          new MappingSpec(annotatedPackage.getQualifiedName().toString())
              .setRifLayout(RifLayout.parse(spreadsheetWorkbook, annotation.carrierSheet()))
              .setHeaderEntity("CarrierClaim")
              .setHeaderTable("CarrierClaims")
              .setHeaderEntityIdField("claimId")
              .setHasLines(true)
              .setLineTable("CarrierClaimLines"));
      mappingSpecs.add(
          new MappingSpec(annotatedPackage.getQualifiedName().toString())
              .setRifLayout(RifLayout.parse(spreadsheetWorkbook, annotation.inpatientSheet()))
              .setHeaderEntity("InpatientClaim")
              .setHeaderTable("InpatientClaims")
              .setHeaderEntityIdField("claimId")
              .setHasLines(true)
              .setLineTable("InpatientClaimLines"));
      mappingSpecs.add(
          new MappingSpec(annotatedPackage.getQualifiedName().toString())
              .setRifLayout(RifLayout.parse(spreadsheetWorkbook, annotation.outpatientSheet()))
              .setHeaderEntity("OutpatientClaim")
              .setHeaderTable("OutpatientClaims")
              .setHeaderEntityIdField("claimId")
              .setHasLines(true)
              .setLineTable("OutpatientClaimLines"));
      mappingSpecs.add(
          new MappingSpec(annotatedPackage.getQualifiedName().toString())
              .setRifLayout(RifLayout.parse(spreadsheetWorkbook, annotation.hhaSheet()))
              .setHeaderEntity("HHAClaim")
              .setHeaderTable("HHAClaims")
              .setHeaderEntityIdField("claimId")
              .setHasLines(true)
              .setLineTable("HHAClaimLines"));
      mappingSpecs.add(
          new MappingSpec(annotatedPackage.getQualifiedName().toString())
              .setRifLayout(RifLayout.parse(spreadsheetWorkbook, annotation.dmeSheet()))
              .setHeaderEntity("DMEClaim")
              .setHeaderTable("DMEClaims")
              .setHeaderEntityIdField("claimId")
              .setHasLines(true)
              .setLineTable("DMEClaimLines"));
      mappingSpecs.add(
          new MappingSpec(annotatedPackage.getQualifiedName().toString())
              .setRifLayout(RifLayout.parse(spreadsheetWorkbook, annotation.hospiceSheet()))
              .setHeaderEntity("HospiceClaim")
              .setHeaderTable("HospiceClaims")
              .setHeaderEntityIdField("claimId")
              .setHasLines(true)
              .setLineTable("HospiceClaimLines"));
      mappingSpecs.add(
          new MappingSpec(annotatedPackage.getQualifiedName().toString())
              .setRifLayout(RifLayout.parse(spreadsheetWorkbook, annotation.snfSheet()))
              .setHeaderEntity("SNFClaim")
              .setHeaderTable("SNFClaims")
              .setHeaderEntityIdField("claimId")
              .setHasLines(true)
              .setLineTable("SNFClaimLines"));
    } finally {
      if (spreadsheetWorkbook != null) spreadsheetWorkbook.close();
    }
    logNote(annotatedPackage, "Generated mapping specification: '%s'.", mappingSpecs);

    /* Generate the code for each layout. */
    for (MappingSpec mappingSpec : mappingSpecs) generateCode(mappingSpec);
  }

  /**
   * Generates the code for the specified {@link RifLayout}.
   *
   * @param mappingSpec the {@link MappingSpec} to generate code for
   * @throws IOException An {@link IOException} may be thrown if errors are encountered trying to
   *     generate source files.
   */
  private void generateCode(MappingSpec mappingSpec) throws IOException {
    logNote("Generated code for %s", mappingSpec.getRifLayout().getName());

    /*
     * First, create the Java enum for the RIF columns.
     */
    TypeSpec columnEnum = generateColumnEnum(mappingSpec);

    /*
     * Then, create the JPA Entity for the "line" fields, containing: fields
     * and accessors.
     */
    Optional<TypeSpec> lineEntity = Optional.empty();
    if (mappingSpec.getHasLines()) {
      lineEntity = Optional.of(generateLineEntity(mappingSpec));
    }

    /*
     * Then, create the JPA Entity for the "grouped" fields, containing:
     * fields, accessors, and a RIF-to-JPA-Entity parser.
     */
    TypeSpec headerEntity = generateHeaderEntity(mappingSpec);

    /*
     * Then, create code that can be used to parse incoming RIF rows into
     * instances of those entities.
     */
    generateParser(mappingSpec, columnEnum, headerEntity, lineEntity);

    /*
     * Then, create code that can be used to write the JPA Entity out to CSV
     * files, for use with PostgreSQL's copy APIs.
     */
    generateCsvWriter(mappingSpec, headerEntity, lineEntity);
  }

  /**
   * Generates a Java {@link Enum} with entries for each {@link RifField} in the specified {@link
   * MappingSpec}.
   *
   * @param mappingSpec the {@link MappingSpec} of the layout to generate code for
   * @return the Java {@link Enum} that was generated
   * @throws IOException An {@link IOException} may be thrown if errors are encountered trying to
   *     generate source files.
   */
  private TypeSpec generateColumnEnum(MappingSpec mappingSpec) throws IOException {
    TypeSpec.Builder columnEnum =
        TypeSpec.enumBuilder(mappingSpec.getColumnEnum()).addModifiers(Modifier.PUBLIC);
    for (int fieldIndex = 0;
        fieldIndex < mappingSpec.getRifLayout().getRifFields().size();
        fieldIndex++) {
      RifField rifField = mappingSpec.getRifLayout().getRifFields().get(fieldIndex);
      columnEnum.addEnumConstant(rifField.getRifColumnName());
    }

    TypeSpec columnEnumFinal = columnEnum.build();
    JavaFile columnsEnumFile =
        JavaFile.builder(mappingSpec.getPackageName(), columnEnumFinal).build();
    columnsEnumFile.writeTo(processingEnv.getFiler());

    return columnEnumFinal;
  }

  /**
   * Generates a Java {@link Entity} for the line {@link RifField}s in the specified {@link
   * MappingSpec}.
   *
   * @param mappingSpec the {@link MappingSpec} of the layout to generate code for
   * @return the Java {@link Entity} that was generated
   * @throws IOException An {@link IOException} may be thrown if errors are encountered trying to
   *     generate source files.
   */
  private TypeSpec generateLineEntity(MappingSpec mappingSpec) throws IOException {
    RifLayout rifLayout = mappingSpec.getRifLayout();

    // Create the Entity class.
    AnnotationSpec entityAnnotation = AnnotationSpec.builder(Entity.class).build();
    AnnotationSpec tableAnnotation =
        AnnotationSpec.builder(Table.class)
            .addMember("name", "$S", "`" + mappingSpec.getLineTable() + "`")
            .build();
    TypeSpec.Builder lineEntity =
        TypeSpec.classBuilder(mappingSpec.getLineEntity())
            .addAnnotation(entityAnnotation)
            .addAnnotation(
                AnnotationSpec.builder(IdClass.class)
                    .addMember("value", "$T.class", mappingSpec.getLineEntityIdClass())
                    .build())
            .addAnnotation(tableAnnotation)
            .addModifiers(Modifier.PUBLIC);

    // Create the @IdClass needed for the composite primary key.
    TypeSpec.Builder lineIdClass =
        TypeSpec.classBuilder(mappingSpec.getLineEntityIdClass())
            .addSuperinterface(Serializable.class)
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC);
    lineIdClass.addField(
        FieldSpec.builder(
                long.class, "serialVersionUID", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
            .initializer("$L", 1L)
            .build());

    // Add a field to that @IdClass for the parent claim's ID.
    RifField parentClaimRifField =
        rifLayout.getRifFields().stream()
            .filter(f -> mappingSpec.getHeaderEntityIdField().equals(f.getJavaFieldName()))
            .findAny()
            .get();
    TypeName parentClaimIdFieldType = selectJavaFieldType(parentClaimRifField);
    FieldSpec.Builder parentIdField =
        FieldSpec.builder(
            parentClaimIdFieldType, mappingSpec.getLineEntityParentField(), Modifier.PRIVATE);
    lineIdClass.addField(parentIdField.build());
    MethodSpec.Builder parentGetter =
        MethodSpec.methodBuilder("getParentClaim")
            .addStatement("return $N", mappingSpec.getLineEntityParentField())
            .returns(parentClaimIdFieldType);
    lineIdClass.addMethod(parentGetter.build());

    // Add a field to that @IdClass class for the line number.
    RifField rifLineNumberField =
        rifLayout.getRifFields().stream()
            .filter(f -> f.getJavaFieldName().equals(mappingSpec.getLineEntityLineNumberField()))
            .findFirst()
            .get();
    TypeName lineNumberFieldType = selectJavaFieldType(rifLineNumberField);
    FieldSpec.Builder lineNumberIdField =
        FieldSpec.builder(
            lineNumberFieldType, mappingSpec.getLineEntityLineNumberField(), Modifier.PRIVATE);
    lineIdClass.addField(lineNumberIdField.build());
    MethodSpec.Builder lineNumberGetter =
        MethodSpec.methodBuilder("get" + capitalize(mappingSpec.getLineEntityLineNumberField()))
            .addStatement("return $N", mappingSpec.getLineEntityLineNumberField())
            .returns(lineNumberFieldType);
    lineIdClass.addMethod(lineNumberGetter.build());

    // Add hashCode() and equals(...) to that @IdClass.
    lineIdClass.addMethod(generateHashCodeMethod(parentIdField.build(), lineNumberIdField.build()));
    lineIdClass.addMethod(
        generateEqualsMethod(
            mappingSpec.getLineEntity(), parentIdField.build(), lineNumberIdField.build()));

    // Finalize the @IdClass and nest it inside the Entity class.
    lineEntity.addType(lineIdClass.build());

    // Add a field and accessor to the "line" Entity for the parent.
    FieldSpec parentClaimField =
        FieldSpec.builder(
                mappingSpec.getHeaderEntity(),
                mappingSpec.getLineEntityParentField(),
                Modifier.PRIVATE)
            .addAnnotation(Id.class)
            .addAnnotation(AnnotationSpec.builder(ManyToOne.class).build())
            .addAnnotation(
                AnnotationSpec.builder(JoinColumn.class)
                    .addMember("name", "$S", "`" + mappingSpec.getLineEntityParentField() + "`")
                    .addMember(
                        "foreignKey",
                        "@$T(name = $S)",
                        ForeignKey.class,
                        String.format(
                            "%s_%s_to_%s",
                            mappingSpec.getLineTable(),
                            mappingSpec.getLineEntityParentField(),
                            mappingSpec.getHeaderTable()))
                    .build())
            .build();
    lineEntity.addField(parentClaimField);
    MethodSpec parentClaimGetter =
        MethodSpec.methodBuilder(calculateGetterName(parentClaimField))
            .addModifiers(Modifier.PUBLIC)
            .addStatement("return $N", mappingSpec.getLineEntityParentField())
            .returns(mappingSpec.getHeaderEntity())
            .build();
    lineEntity.addMethod(parentClaimGetter);
    MethodSpec.Builder parentClaimSetter =
        MethodSpec.methodBuilder(calculateSetterName(parentClaimField))
            .addModifiers(Modifier.PUBLIC)
            .returns(void.class)
            .addParameter(mappingSpec.getHeaderEntity(), parentClaimField.name);
    addSetterStatement(false, parentClaimField, parentClaimSetter);
    lineEntity.addMethod(parentClaimSetter.build());

    // For each "line" RIF field, create an Entity field with accessors.
    for (int fieldIndex = mappingSpec.calculateFirstLineFieldIndex();
        fieldIndex < rifLayout.getRifFields().size();
        fieldIndex++) {
      RifField rifField = rifLayout.getRifFields().get(fieldIndex);

      FieldSpec lineField =
          FieldSpec.builder(
                  selectJavaFieldType(rifField), rifField.getJavaFieldName(), Modifier.PRIVATE)
              .addAnnotations(createAnnotations(mappingSpec, rifField))
              .build();
      lineEntity.addField(lineField);

      MethodSpec.Builder lineFieldGetter =
          MethodSpec.methodBuilder(calculateGetterName(lineField))
              .addModifiers(Modifier.PUBLIC)
              .returns(selectJavaPropertyType(rifField));
      addGetterStatement(rifField, lineField, lineFieldGetter);
      lineEntity.addMethod(lineFieldGetter.build());

      MethodSpec.Builder lineFieldSetter =
          MethodSpec.methodBuilder(calculateSetterName(lineField))
              .addModifiers(Modifier.PUBLIC)
              .returns(void.class)
              .addParameter(selectJavaPropertyType(rifField), lineField.name);
      addSetterStatement(rifField, lineField, lineFieldSetter);
      lineEntity.addMethod(lineFieldSetter.build());
    }

    TypeSpec lineEntityFinal = lineEntity.build();
    JavaFile lineEntityClassFile =
        JavaFile.builder(mappingSpec.getPackageName(), lineEntityFinal).build();
    lineEntityClassFile.writeTo(processingEnv.getFiler());

    return lineEntityFinal;
  }

  /**
   * Generates a Java {@link Entity} for the header {@link RifField}s in the specified {@link
   * MappingSpec}.
   *
   * @param mappingSpec the {@link MappingSpec} of the layout to generate code for
   * @return the Java {@link Entity} that was generated
   * @throws IOException An {@link IOException} may be thrown if errors are encountered trying to
   *     generate source files.
   */
  private TypeSpec generateHeaderEntity(MappingSpec mappingSpec) throws IOException {
    // Create the Entity class.
    AnnotationSpec entityAnnotation = AnnotationSpec.builder(Entity.class).build();
    AnnotationSpec tableAnnotation =
        AnnotationSpec.builder(Table.class)
            .addMember("name", "$S", "`" + mappingSpec.getHeaderTable() + "`")
            .build();
    TypeSpec.Builder headerEntityClass =
        TypeSpec.classBuilder(mappingSpec.getHeaderEntity())
            .addAnnotation(entityAnnotation)
            .addAnnotation(tableAnnotation)
            .addModifiers(Modifier.PUBLIC);

    // Create an Entity field with accessors for the generated-ID field (if any).
    if (mappingSpec.getHeaderEntityGeneratedIdField() != null) {
      FieldSpec.Builder idFieldBuilder =
          FieldSpec.builder(
              TypeName.LONG, mappingSpec.getHeaderEntityGeneratedIdField(), Modifier.PRIVATE);
      idFieldBuilder.addAnnotation(Id.class);
      idFieldBuilder.addAnnotation(
          AnnotationSpec.builder(Column.class)
              .addMember(
                  "name",
                  "$S",
                  String.format("`%s`", mappingSpec.getHeaderEntityGeneratedIdField()))
              .addMember("nullable", "$L", false)
              .addMember("updatable", "$L", false)
              .build());
      String sequenceName =
          String.format(
              "%s_%s_seq",
              mappingSpec.getHeaderEntity().simpleName(),
              mappingSpec.getHeaderEntityGeneratedIdField());
      /*
       * FIXME For consistency, sequence names should be mixed-case, but can't be, due
       * to https://hibernate.atlassian.net/browse/HHH-9431.
       */
      sequenceName = sequenceName.toLowerCase();
      idFieldBuilder.addAnnotation(
          AnnotationSpec.builder(GeneratedValue.class)
              .addMember("strategy", "$T.SEQUENCE", GenerationType.class)
              .addMember("generator", "$S", sequenceName)
              .build());
      idFieldBuilder.addAnnotation(
          AnnotationSpec.builder(SequenceGenerator.class)
              .addMember("name", "$S", sequenceName)
              .addMember("sequenceName", "$S", sequenceName)
              .addMember("allocationSize", "$L", 50)
              .build());
      FieldSpec idField = idFieldBuilder.build();
      headerEntityClass.addField(idField);

      MethodSpec.Builder idFieldGetter =
          MethodSpec.methodBuilder(calculateGetterName(idField))
              .addModifiers(Modifier.PUBLIC)
              .returns(idField.type);
      addGetterStatement(false, idField, idFieldGetter);
      headerEntityClass.addMethod(idFieldGetter.build());

      MethodSpec.Builder idFieldSetter =
          MethodSpec.methodBuilder(calculateSetterName(idField))
              .addModifiers(Modifier.PUBLIC)
              .returns(void.class)
              .addParameter(idField.type, idField.name);
      addSetterStatement(false, idField, idFieldSetter);
      headerEntityClass.addMethod(idFieldSetter.build());
    }

    // Create an Entity field with accessors for each RIF field.
    for (int fieldIndex = 0;
        fieldIndex <= mappingSpec.calculateLastHeaderFieldIndex();
        fieldIndex++) {
      RifField rifField = mappingSpec.getRifLayout().getRifFields().get(fieldIndex);

      FieldSpec headerField =
          FieldSpec.builder(
                  selectJavaFieldType(rifField), rifField.getJavaFieldName(), Modifier.PRIVATE)
              .addAnnotations(createAnnotations(mappingSpec, rifField))
              .build();
      headerEntityClass.addField(headerField);

      MethodSpec.Builder headerFieldGetter =
          MethodSpec.methodBuilder(calculateGetterName(headerField))
              .addModifiers(Modifier.PUBLIC)
              .returns(selectJavaPropertyType(rifField));
      addGetterStatement(rifField, headerField, headerFieldGetter);
      headerEntityClass.addMethod(headerFieldGetter.build());

      MethodSpec.Builder headerFieldSetter =
          MethodSpec.methodBuilder(calculateSetterName(headerField))
              .addModifiers(Modifier.PUBLIC)
              .returns(void.class)
              .addParameter(selectJavaPropertyType(rifField), headerField.name);
      addSetterStatement(rifField, headerField, headerFieldSetter);
      headerEntityClass.addMethod(headerFieldSetter.build());
    }

    /*
     * Create an Entity field for additional database fields that we need to store
     * data for whereas there isn't a corresponding RIF input field.
     */
    for (RifField addlDatabaseField : mappingSpec.getHeaderEntityAdditionalDatabaseFields()) {
      FieldSpec headerField =
          FieldSpec.builder(
                  selectJavaFieldType(addlDatabaseField),
                  addlDatabaseField.getJavaFieldName(),
                  Modifier.PRIVATE)
              .addAnnotations(createAnnotations(mappingSpec, addlDatabaseField))
              .build();
      headerEntityClass.addField(headerField);

      MethodSpec.Builder headerFieldGetter =
          MethodSpec.methodBuilder(calculateGetterName(headerField))
              .addModifiers(Modifier.PUBLIC)
              .returns(selectJavaPropertyType(addlDatabaseField));
      addGetterStatement(addlDatabaseField, headerField, headerFieldGetter);
      headerEntityClass.addMethod(headerFieldGetter.build());

      MethodSpec.Builder headerFieldSetter =
          MethodSpec.methodBuilder(calculateSetterName(headerField))
              .addModifiers(Modifier.PUBLIC)
              .returns(void.class)
              .addParameter(selectJavaPropertyType(addlDatabaseField), headerField.name);
      addSetterStatement(addlDatabaseField, headerField, headerFieldSetter);
      headerEntityClass.addMethod(headerFieldSetter.build());
    }

    // Add the parent-to-child join field and accessor, if appropriate.
    if (mappingSpec.getHasLines()) {
      ParameterizedTypeName childFieldType =
          ParameterizedTypeName.get(ClassName.get(List.class), mappingSpec.getLineEntity());

      FieldSpec.Builder childField =
          FieldSpec.builder(childFieldType, "lines", Modifier.PRIVATE)
              .initializer("new $T<>()", LinkedList.class);
      childField.addAnnotation(
          AnnotationSpec.builder(OneToMany.class)
              .addMember("mappedBy", "$S", mappingSpec.getLineEntityParentField())
              .addMember("orphanRemoval", "$L", true)
              .addMember("fetch", "$T.LAZY", FetchType.class)
              .addMember("cascade", "$T.ALL", CascadeType.class)
              .build());
      childField.addAnnotation(
          AnnotationSpec.builder(OrderBy.class)
              .addMember("value", "$S", mappingSpec.getLineEntityLineNumberField() + " ASC")
              .build());
      headerEntityClass.addField(childField.build());

      MethodSpec childGetter =
          MethodSpec.methodBuilder("getLines")
              .addModifiers(Modifier.PUBLIC)
              .addStatement("return $N", "lines")
              .returns(childFieldType)
              .build();
      headerEntityClass.addMethod(childGetter);
    }

    // Add the parent-to-child join field and accessor for an inner join
    // relationship
    if (mappingSpec.getHasInnerJoinRelationship()) {
      for (InnerJoinRelationship relationship : mappingSpec.getInnerJoinRelationship()) {
        String mappedBy = relationship.getMappedBy();
        String orderBy = relationship.getOrderBy();
        ClassName childEntity = mappingSpec.getClassName(relationship.getChildEntity());
        String childFieldName = relationship.getChildField();

        Class<?> fieldDeclaredType;
        Class<?> fieldActualType;
        if (orderBy != null) {
          fieldDeclaredType = List.class;
          fieldActualType = LinkedList.class;
        } else {
          fieldDeclaredType = Set.class;
          fieldActualType = HashSet.class;
        }

        ParameterizedTypeName childFieldType =
            ParameterizedTypeName.get(ClassName.get(fieldDeclaredType), childEntity);
        FieldSpec.Builder childField =
            FieldSpec.builder(childFieldType, childFieldName, Modifier.PRIVATE)
                .initializer("new $T<>()", fieldActualType);
        childField.addAnnotation(
            AnnotationSpec.builder(OneToMany.class)
                .addMember("mappedBy", "$S", mappedBy)
                .addMember("orphanRemoval", "$L", false)
                .addMember("fetch", "$T.LAZY", FetchType.class)
                .addMember("cascade", "$T.ALL", CascadeType.class)
                .build());
        if (orderBy != null)
          childField.addAnnotation(
              AnnotationSpec.builder(OrderBy.class)
                  .addMember("value", "$S", orderBy + " ASC")
                  .build());
        headerEntityClass.addField(childField.build());

        MethodSpec childGetter =
            MethodSpec.methodBuilder("get" + capitalize(childFieldName))
                .addModifiers(Modifier.PUBLIC)
                .addStatement("return $N", childFieldName)
                .returns(childFieldType)
                .build();
        headerEntityClass.addMethod(childGetter);
      }
    }

    // Add a lastUpdated field. Use the Hibernate UpdateTimestamp feature to set this field.
    if (mappingSpec.getHasLastUpdated()) {
      // lastUpdated field
      FieldSpec lastUpdatedField =
          FieldSpec.builder(OffsetDateTime.class, "lastUpdated", Modifier.PRIVATE)
              .addAnnotation(
                  AnnotationSpec.builder(Column.class)
                      .addMember("name", "$S", "lastUpdated")
                      .addMember("nullable", "$L", "true")
                      .build())
              .addAnnotation(UpdateTimestamp.class)
              .build();
      headerEntityClass.addField(lastUpdatedField);

      // Getter method
      MethodSpec lastUpdatedGetter =
          MethodSpec.methodBuilder("getLastUpdated")
              .addModifiers(Modifier.PUBLIC)
              .addStatement("return Optional.ofNullable(lastUpdated)")
              .returns(ParameterizedTypeName.get(Optional.class, OffsetDateTime.class))
              .build();
      headerEntityClass.addMethod(lastUpdatedGetter);
    }

    TypeSpec headerEntityFinal = headerEntityClass.build();
    JavaFile headerEntityFile =
        JavaFile.builder(mappingSpec.getPackageName(), headerEntityFinal).build();
    headerEntityFile.writeTo(processingEnv.getFiler());

    return headerEntityFinal;
  }

  /**
   * Generates a Java class that can handle RIF-to-Entity parsing.
   *
   * @param mappingSpec the {@link MappingSpec} of the layout to generate code for
   * @param columnEnum the RIF column {@link Enum} that was generated for the layout
   * @param headerEntity the Java {@link Entity} that was generated for the header fields
   * @param lineEntity the Java {@link Entity} that was generated for the line fields, if any
   * @return the Java parsing class that was generated
   * @throws IOException An {@link IOException} may be thrown if errors are encountered trying to
   *     generate source files.
   */
  private TypeSpec generateParser(
      MappingSpec mappingSpec,
      TypeSpec columnEnum,
      TypeSpec headerEntity,
      Optional<TypeSpec> lineEntity)
      throws IOException {
    TypeSpec.Builder parsingClass =
        TypeSpec.classBuilder(mappingSpec.getParserClass())
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL);

    // Grab some common types we'll need.
    ClassName csvRecordType = ClassName.get("org.apache.commons.csv", "CSVRecord");
    ClassName parseUtilsType = ClassName.get("gov.cms.bfd.model.rif.parse", "RifParsingUtils");

    MethodSpec.Builder parseMethod =
        MethodSpec.methodBuilder("parseRif")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .returns(mappingSpec.getHeaderEntity())
            .addParameter(
                ParameterizedTypeName.get(ClassName.get(List.class), csvRecordType), "csvRecords");

    parseMethod.addComment("Verify the inputs.");
    parseMethod.addStatement("$T.requireNonNull(csvRecords)", Objects.class);
    parseMethod
        .beginControlFlow("if (csvRecords.size() < 1)")
        .addStatement("throw new $T()", IllegalArgumentException.class)
        .endControlFlow();

    parseMethod.addCode("\n$1T header = new $1T();\n", mappingSpec.getHeaderEntity());

    // Loop over each field and generate the code needed to parse it.
    for (int fieldIndex = 0;
        fieldIndex < mappingSpec.getRifLayout().getRifFields().size();
        fieldIndex++) {
      RifField rifField = mappingSpec.getRifLayout().getRifFields().get(fieldIndex);

      // Find the Entity field for the RifField.
      Stream<FieldSpec> entitiesFieldsStream =
          mappingSpec.getHasLines()
              ? Stream.concat(
                  headerEntity.fieldSpecs.stream(), lineEntity.get().fieldSpecs.stream())
              : headerEntity.fieldSpecs.stream();
      FieldSpec entityField =
          entitiesFieldsStream
              .filter(f -> f.name.equals(rifField.getJavaFieldName()))
              .findAny()
              .get();

      // Are we starting the header parsing?
      if (fieldIndex == 0) {
        parseMethod.addCode("\n// Parse the header fields.\n");
        parseMethod.addCode("$T headerRecord = csvRecords.get(0);\n", csvRecordType);
      }

      // Are we starting the line parsing?
      if (mappingSpec.getHasLines() && fieldIndex == mappingSpec.calculateFirstLineFieldIndex()) {
        parseMethod.addCode("\n// Parse the line fields.\n");
        parseMethod.beginControlFlow(
            "for (int lineIndex = 0; lineIndex < csvRecords.size(); lineIndex++)");
        parseMethod.addStatement("$T lineRecord = csvRecords.get(lineIndex)", csvRecordType);
        parseMethod.addStatement("$1T line = new $1T()", mappingSpec.getLineEntity());

        FieldSpec lineEntityParentField =
            lineEntity.get().fieldSpecs.stream()
                .filter(f -> f.name.equals(mappingSpec.getLineEntityParentField()))
                .findAny()
                .get();
        parseMethod.addCode("line.$L(header);\n\n", calculateSetterName(lineEntityParentField));
      }

      // Determine which variables to use in assignment statement.
      String entityName;
      String recordName;
      if (mappingSpec.getHasLines() && fieldIndex >= mappingSpec.calculateFirstLineFieldIndex()) {
        entityName = "line";
        recordName = "lineRecord";
      } else {
        entityName = "header";
        recordName = "headerRecord";
      }

      // Determine which parsing utility method to use.
      String parseUtilsMethodName;
      if (rifField.getRifColumnType() == RifColumnType.CHAR
          && rifField.getRifColumnLength().orElse(Integer.MAX_VALUE) > 1) {
        // Handle a String field.
        parseUtilsMethodName =
            rifField.isRifColumnOptional() ? "parseOptionalString" : "parseString";
      } else if (rifField.getRifColumnType() == RifColumnType.CHAR
          && rifField.getRifColumnLength().orElse(Integer.MAX_VALUE) == 1) {
        // Handle a Character field.
        parseUtilsMethodName =
            rifField.isRifColumnOptional() ? "parseOptionalCharacter" : "parseCharacter";
      } else if (rifField.getRifColumnType() == RifColumnType.NUM
          && rifField.getRifColumnScale().orElse(Integer.MAX_VALUE) == 0) {
        // Handle an Integer field.
        parseUtilsMethodName =
            rifField.isRifColumnOptional() ? "parseOptionalInteger" : "parseInteger";
      } else if (rifField.getRifColumnType() == RifColumnType.NUM
          && rifField.getRifColumnScale().orElse(Integer.MAX_VALUE) > 0) {
        // Handle a Decimal field.
        parseUtilsMethodName =
            rifField.isRifColumnOptional() ? "parseOptionalDecimal" : "parseDecimal";
      } else if (rifField.getRifColumnType() == RifColumnType.DATE) {
        // Handle a LocalDate field.
        parseUtilsMethodName = rifField.isRifColumnOptional() ? "parseOptionalDate" : "parseDate";
      } else if (rifField.getRifColumnType() == RifColumnType.TIMESTAMP) {
        // Handle an Instant field.
        parseUtilsMethodName =
            rifField.isRifColumnOptional() ? "parseOptionalTimestamp" : "parseTimestamp";
      } else {
        throw new IllegalStateException();
      }

      Map<String, Object> valueAssignmentArgs = new LinkedHashMap<>();
      valueAssignmentArgs.put("entity", entityName);
      valueAssignmentArgs.put("entitySetter", calculateSetterName(entityField));
      valueAssignmentArgs.put("record", recordName);
      valueAssignmentArgs.put("parseUtilsType", parseUtilsType);
      valueAssignmentArgs.put("parseUtilsMethod", parseUtilsMethodName);
      valueAssignmentArgs.put("columnEnumType", mappingSpec.getColumnEnum());
      valueAssignmentArgs.put("columnEnumConstant", rifField.getRifColumnName());
      parseMethod.addCode(
          CodeBlock.builder()
              .addNamed(
                  "$entity:L.$entitySetter:L("
                      + "$parseUtilsType:T.$parseUtilsMethod:L("
                      + "$record:L.get("
                      + "$columnEnumType:T.$columnEnumConstant:L)));\n",
                  valueAssignmentArgs)
              .build());
    }

    // Did we just finish line parsing?
    if (mappingSpec.getHasLines()) {
      FieldSpec linesField =
          headerEntity.fieldSpecs.stream()
              .filter(f -> f.name.equals(mappingSpec.getHeaderEntityLinesField()))
              .findAny()
              .get();
      parseMethod.addStatement("header.$L().add(line)", calculateGetterName(linesField));
      parseMethod.endControlFlow();
    }

    parseMethod.addStatement("return header");
    parsingClass.addMethod(parseMethod.build());

    TypeSpec parsingClassFinal = parsingClass.build();
    JavaFile parsingClassFile =
        JavaFile.builder(mappingSpec.getPackageName(), parsingClassFinal).build();
    parsingClassFile.writeTo(processingEnv.getFiler());

    return parsingClassFinal;
  }

  /**
   * Generates a Java class that can be used to write the JPA Entity out to CSV files, for use with
   * PostgreSQL's copy APIs.
   *
   * @param mappingSpec the {@link MappingSpec} of the layout to generate code for
   * @param headerEntity the Java {@link Entity} that was generated for the header fields
   * @param lineEntity the Java {@link Entity} that was generated for the line fields, if any
   * @return the Java CSV writing class that was generated
   * @throws IOException An {@link IOException} may be thrown if errors are encountered trying to
   *     generate source files.
   */
  private TypeSpec generateCsvWriter(
      MappingSpec mappingSpec, TypeSpec headerEntity, Optional<TypeSpec> lineEntity)
      throws IOException {
    TypeSpec.Builder csvWriterClass =
        TypeSpec.classBuilder(mappingSpec.getCsvWriterClass())
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL);

    // Grab some common types we'll need.
    ArrayTypeName recordType = ArrayTypeName.of(Object.class);
    ArrayTypeName recordsListType = ArrayTypeName.of(recordType);
    ParameterizedTypeName returnType =
        ParameterizedTypeName.get(
            ClassName.get(Map.class), ClassName.get(String.class), recordsListType);

    MethodSpec.Builder csvWriterMethod =
        MethodSpec.methodBuilder("toCsvRecordsByTable")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .returns(returnType)
            .addParameter(mappingSpec.getHeaderEntity(), "entity");

    csvWriterMethod.addComment("Verify the input.");
    csvWriterMethod.addStatement("$T.requireNonNull(entity)", Objects.class);

    csvWriterMethod.addCode("\n");
    csvWriterMethod.addStatement("$T csvRecordsByTable = new $T<>(2)", returnType, HashMap.class);

    // Generate the header conversion.
    csvWriterMethod.addCode("\n");
    csvWriterMethod.addComment("Convert the header fields.");
    csvWriterMethod.addStatement("$T headerRecords = new $T[2][]", recordsListType, Object.class);
    String headerColumnsList =
        headerEntity.fieldSpecs.stream()
            .filter(
                f -> {
                  if (mappingSpec.getHasLines()
                      && f.name.equals(mappingSpec.getHeaderEntityLinesField())) return false;
                  return true;
                })
            .map(f -> "\"" + f.name + "\"")
            .collect(Collectors.joining(", "));
    csvWriterMethod.addStatement(
        "headerRecords[0] = new $1T{ $2L }", recordType, headerColumnsList);
    String headerGettersList =
        headerEntity.fieldSpecs.stream()
            .filter(
                f -> {
                  if (mappingSpec.getHasLines()
                      && f.name.equals(mappingSpec.getHeaderEntityLinesField())) return false;
                  return true;
                })
            .map(f -> calculateFieldToCsvValueCode("entity", f, mappingSpec, null, null))
            .collect(Collectors.joining(", "));
    csvWriterMethod.addStatement(
        "$1T headerRecord = new $1T{ $2L }", recordType, headerGettersList);
    csvWriterMethod.addStatement("headerRecords[1] = headerRecord");
    csvWriterMethod.addStatement(
        "csvRecordsByTable.put($S, headerRecords)", mappingSpec.getHeaderTable());

    // Generate the line conversion.
    if (mappingSpec.getHasLines()) {
      FieldSpec linesField =
          headerEntity.fieldSpecs.stream()
              .filter(f -> f.name.equals(mappingSpec.getHeaderEntityLinesField()))
              .findAny()
              .get();
      String linesFieldGetter = calculateGetterName(linesField);
      csvWriterMethod.addCode("\n");
      csvWriterMethod.addComment("Convert the line fields.");
      csvWriterMethod.addStatement(
          "$T lineRecords = new $T[entity.$L().size() + 1][]",
          recordsListType,
          Object.class,
          linesFieldGetter);
      csvWriterMethod.addStatement(
          "csvRecordsByTable.put($S, lineRecords)", mappingSpec.getLineTable());
      String lineColumnsList =
          lineEntity.get().fieldSpecs.stream()
              .map(f -> "\"" + f.name + "\"")
              .collect(Collectors.joining(", "));
      csvWriterMethod.addStatement("lineRecords[0] = new $1T{ $2L }", recordType, lineColumnsList);
      csvWriterMethod.beginControlFlow(
          "for (int lineIndex = 0; lineIndex < entity.$L().size();lineIndex++)", linesFieldGetter);
      csvWriterMethod.addStatement(
          "$T lineEntity = entity.$L().get(lineIndex)",
          mappingSpec.getLineEntity(),
          linesFieldGetter);
      FieldSpec parentField =
          lineEntity.get().fieldSpecs.stream()
              .filter(f -> f.name.equals(mappingSpec.getLineEntityParentField()))
              .findAny()
              .get();
      FieldSpec headerIdField =
          headerEntity.fieldSpecs.stream()
              .filter(f -> f.name.equals(mappingSpec.getHeaderEntityIdField()))
              .findAny()
              .get();
      String lineGettersList =
          lineEntity.get().fieldSpecs.stream()
              .map(
                  f -> {
                    return calculateFieldToCsvValueCode(
                        "lineEntity", f, mappingSpec, parentField, headerIdField);
                  })
              .collect(Collectors.joining(", "));
      csvWriterMethod.addStatement("$1T lineRecord = new $1T{ $2L }", recordType, lineGettersList);
      csvWriterMethod.addStatement("lineRecords[lineIndex + 1] = lineRecord");
      csvWriterMethod.endControlFlow();
    }

    csvWriterMethod.addStatement("return csvRecordsByTable");
    csvWriterClass.addMethod(csvWriterMethod.build());

    TypeSpec parsingClassFinal = csvWriterClass.build();
    JavaFile parsingClassFile =
        JavaFile.builder(mappingSpec.getPackageName(), parsingClassFinal).build();
    parsingClassFile.writeTo(processingEnv.getFiler());

    return parsingClassFinal;
  }

  /**
   * Used in {@link #generateCsvWriter(MappingSpec, TypeSpec, Optional)} and generates the
   * field-to-CSV-value conversion code for the specified field.
   *
   * @param instanceName the name of the object that the value will be pulled from
   * @param field the field to generate conversion code for
   * @param mappingSpec the {@link MappingSpec} of the field to generate conversion code for
   * @param parentField the {@link MappingSpec#getLineEntityParentField()} field, or <code>null
   *     </code> if this is a header field
   * @param headerIdField the {@link MappingSpec#getHeaderEntityIdField()} field, or <code>null
   *     </code> if this is a header field
   * @return the field-to-CSV-value conversion code for the specified field
   */
  private String calculateFieldToCsvValueCode(
      String instanceName,
      FieldSpec field,
      MappingSpec mappingSpec,
      FieldSpec parentField,
      FieldSpec headerIdField) {
    StringBuilder code = new StringBuilder();
    code.append(instanceName);
    code.append(".");

    Optional<RifField> rifField =
        mappingSpec.getRifLayout().getRifFields().stream()
            .filter(f -> field.name.equals(f.getJavaFieldName()))
            .findAny();
    if (field == parentField) {
      // This is the line-level "parent" field.
      code.append(calculateGetterName(parentField));
      code.append("().");
      code.append(calculateGetterName(headerIdField));
      code.append("()");
    } else if (rifField.isPresent() && rifField.get().isRifColumnOptional()) {
      code.append(calculateGetterName(field));
      code.append("().orElse(null)");
    } else {
      code.append(calculateGetterName(field));
      code.append("()");
    }

    return code.toString();
  }

  /**
   * @param fields the fields that should be hashed
   * @return a new <code>hashCode()</code> implementation that uses the specified fields
   */
  private static MethodSpec generateHashCodeMethod(FieldSpec... fields) {
    MethodSpec.Builder hashCodeMethod =
        MethodSpec.methodBuilder("hashCode")
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PUBLIC)
            .returns(int.class)
            .addStatement(
                "return $T.hash($L)",
                Objects.class,
                Arrays.stream(fields).map(f -> f.name).collect(Collectors.joining(", ")));
    return hashCodeMethod.build();
  }

  /**
   * @param typeName the {@link TypeName} of the class to add this method for
   * @param fields the fields that should be compared
   * @return a new <code>equals(...)</code> implementation that uses the specified fields
   */
  private static MethodSpec generateEqualsMethod(TypeName typeName, FieldSpec... fields) {
    MethodSpec.Builder hashCodeMethod =
        MethodSpec.methodBuilder("equals")
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PUBLIC)
            .addParameter(Object.class, "obj")
            .returns(boolean.class);

    hashCodeMethod
        .beginControlFlow("if (this == obj)")
        .addStatement("return true")
        .endControlFlow();
    hashCodeMethod
        .beginControlFlow("if (obj == null)")
        .addStatement("return false")
        .endControlFlow();
    hashCodeMethod
        .beginControlFlow("if (getClass() != obj.getClass())")
        .addStatement("return false")
        .endControlFlow();
    hashCodeMethod.addStatement("$T other = ($T) obj", typeName, typeName);
    for (FieldSpec field : fields) {
      hashCodeMethod
          .beginControlFlow("if ($T.deepEquals($N, other.$N))", Objects.class, field, field)
          .addStatement("return false")
          .endControlFlow();
    }
    hashCodeMethod.addStatement("return true");

    return hashCodeMethod.build();
  }

  /**
   * @param rifField the {@link RifField} to select the corresponding Java type for
   * @return the {@link TypeName} of the Java type that should be used to represent the specified
   *     {@link RifField} in a JPA entity
   */
  private static TypeName selectJavaFieldType(RifField rifField) {
    if (rifField.getRifColumnType() == RifColumnType.CHAR
        && rifField.getRifColumnLength().orElse(Integer.MAX_VALUE) == 1
        && !rifField.isRifColumnOptional()) return TypeName.CHAR;
    else if (rifField.getRifColumnType() == RifColumnType.CHAR
        && rifField.getRifColumnLength().orElse(Integer.MAX_VALUE) == 1
        && rifField.isRifColumnOptional()) return ClassName.get(Character.class);
    else if (rifField.getRifColumnType() == RifColumnType.CHAR) return ClassName.get(String.class);
    else if (rifField.getRifColumnType() == RifColumnType.DATE
        && rifField.getRifColumnLength().orElse(0) == 8) return ClassName.get(LocalDate.class);
    else if (rifField.getRifColumnType() == RifColumnType.TIMESTAMP
        && rifField.getRifColumnLength().orElse(0) == 20) return ClassName.get(Instant.class);
    else if (rifField.getRifColumnType() == RifColumnType.NUM
        && rifField.getRifColumnScale().orElse(Integer.MAX_VALUE) > 0)
      return ClassName.get(BigDecimal.class);
    else if (rifField.getRifColumnType() == RifColumnType.NUM
        && rifField.getRifColumnScale().orElse(Integer.MAX_VALUE) == 0
        && !rifField.isRifColumnOptional()) return TypeName.INT;
    else if (rifField.getRifColumnType() == RifColumnType.NUM
        && rifField.getRifColumnScale().orElse(Integer.MAX_VALUE) == 0
        && rifField.isRifColumnOptional()) return ClassName.get(Integer.class);
    else throw new IllegalArgumentException("Unhandled field type: " + rifField);
  }

  /**
   * @param rifField the {@link RifField} to select the corresponding Java getter/setter type for
   * @return the {@link TypeName} of the Java type that should be used to represent the specified
   *     {@link RifField}'s getter/setter in a JPA entity
   */
  private static TypeName selectJavaPropertyType(RifField rifField) {
    if (!rifField.isRifColumnOptional()) return selectJavaFieldType(rifField);
    else
      return ParameterizedTypeName.get(
          ClassName.get(Optional.class), selectJavaFieldType(rifField));
  }

  /**
   * @param mappingSpec the {@link MappingSpec} for the specified {@link RifField}
   * @param rifField the {@link RifField} to create the corresponding {@link AnnotationSpec}s for
   * @return an ordered {@link List} of {@link AnnotationSpec}s representing the JPA, etc.
   *     annotations that should be applied to the specified {@link RifField}
   */
  private static List<AnnotationSpec> createAnnotations(
      MappingSpec mappingSpec, RifField rifField) {
    LinkedList<AnnotationSpec> annotations = new LinkedList<>();

    // Add an @Id annotation, if appropriate.
    if (rifField.getJavaFieldName().equals(mappingSpec.getHeaderEntityIdField())
        || (mappingSpec.getHasLines()
            && rifField.getJavaFieldName().equals(mappingSpec.getLineEntityLineNumberField()))) {
      AnnotationSpec.Builder idAnnotation = AnnotationSpec.builder(Id.class);
      annotations.add(idAnnotation.build());
    }

    // Add an @Column annotation to every non-transient column.
    boolean isTransient =
        mappingSpec.getHeaderEntityTransientFields().contains(rifField.getJavaFieldName());
    if (!isTransient) {
      AnnotationSpec.Builder columnAnnotation =
          AnnotationSpec.builder(Column.class)
              .addMember("name", "$S", "`" + rifField.getJavaFieldName() + "`")
              .addMember("nullable", "$L", rifField.isRifColumnOptional());
      if (rifField.getRifColumnType() == RifColumnType.CHAR
          && rifField.getRifColumnLength().isPresent()) {
        columnAnnotation.addMember("length", "$L", rifField.getRifColumnLength().get());
      } else if (rifField.getRifColumnType() == RifColumnType.NUM) {
        /*
         * In SQL, the precision is the number of digits in the unscaled value, e.g.
         * "123.45" has a precision of 5. The scale is the number of digits to the right
         * of the decimal point, e.g. "123.45" has a scale of 2.
         */

        if (rifField.getRifColumnLength().isPresent() && rifField.getRifColumnScale().isPresent()) {
          columnAnnotation.addMember("precision", "$L", rifField.getRifColumnLength().get());
          columnAnnotation.addMember("scale", "$L", rifField.getRifColumnScale().get());
        } else {
          /*
           * Unfortunately, Hibernate's SQL schema generation (HBM2DDL) doesn't correctly
           * handle SQL numeric datatypes that don't have a defined precision and scale.
           * What it _should_ do is represent those types in PostgreSQL as a "NUMERIC",
           * but what it does instead is insert a default precision and scale as
           * "NUMBER(19, 2)". The only way to force the correct behavior is to specify a
           * columnDefinition, so we do that. This leads to incorrect behavior with HSQL
           * (for different reasons), but fortunately that doesn't happen to cause
           * problems with our tests.
           */
          StringBuilder columnDefinition = new StringBuilder();
          columnDefinition.append("numeric");
          if (rifField.getRifColumnLength().isPresent()
              || rifField.getRifColumnScale().isPresent()) {
            columnDefinition.append('(');
            if (rifField.getRifColumnLength().isPresent()) {
              columnDefinition.append(rifField.getRifColumnLength().get());
            }
            if (rifField.getRifColumnScale().isPresent()) {
              columnDefinition.append(", ");
              columnDefinition.append(rifField.getRifColumnScale().get());
            }
            columnDefinition.append(')');
          }
          columnAnnotation.addMember("columnDefinition", "$S", columnDefinition.toString());
        }
      }
      annotations.add(columnAnnotation.build());
    } else {
      annotations.add(AnnotationSpec.builder(Transient.class).build());
    }

    return annotations;
  }

  /**
   * @param List<String> the {@link RifField} to create an additional Annotated database field for
   * @return an ordered {@link List} of {@link RifField}s representing the additional fields that
   *     need to be stored to the database via JPA
   * @throws MalformedURLException
   */
  private static List<RifField> createDetailsForAdditionalDatabaseFields(
      List<String> additionalDatabaseFields) throws MalformedURLException {
    List<RifField> addlDatabaseFields = new ArrayList<RifField>();

    for (String additionalDatabaseField : additionalDatabaseFields) {
      if (additionalDatabaseField.contentEquals("hicnUnhashed")) {
        RifField hicnUnhashed =
            new RifField(
                "BENE_CRNT_HIC_NUM",
                RifColumnType.CHAR,
                Optional.of(64),
                Optional.of(0),
                Boolean.TRUE,
                new URL(DATA_DICTIONARY_LINK + "benecrnthicnum"),
                "BENE_CRNT_HIC_NUM",
                "hicnUnhashed");
        addlDatabaseFields.add(hicnUnhashed);
        continue;
      }
      if (additionalDatabaseField.contentEquals("medicareBeneficiaryId")) {
        RifField medicareBeneficiaryId =
            new RifField(
                "MBI_NUM",
                RifColumnType.CHAR,
                Optional.of(11),
                Optional.of(0),
                Boolean.TRUE,
                new URL(DATA_DICTIONARY_LINK + "mbinum"),
                "MBI_NUM",
                "medicareBeneficiaryId");
        addlDatabaseFields.add(medicareBeneficiaryId);
        continue;
      }
    }
    return addlDatabaseFields;
  }

  /**
   * @param entityField the JPA entity {@link FieldSpec} for the field that the desired getter will
   *     wrap
   * @return the name of the Java "getter" for the specified {@link FieldSpec}
   */
  private static String calculateGetterName(FieldSpec entityField) {
    if (entityField.type.equals(TypeName.BOOLEAN)
        || entityField.type.equals(ClassName.get(Boolean.class)))
      return "is" + capitalize(entityField.name);
    else return "get" + capitalize(entityField.name);
  }

  /**
   * @param rifField the {@link RifField} to generate the "getter" statement for
   * @param entityField the {@link FieldSpec} for the field being wrapped by the "getter"
   * @param entityGetter the "getter" method to generate the statement in
   */
  private static void addGetterStatement(
      RifField rifField, FieldSpec entityField, MethodSpec.Builder entityGetter) {
    addGetterStatement(rifField.isRifColumnOptional(), entityField, entityGetter);
  }

  /**
   * @param optional <code>true</code> if the property is an {@link Optional} one, <code>false
   *     </code> otherwise
   * @param entityField the {@link FieldSpec} for the field being wrapped by the "getter"
   * @param entityGetter the "getter" method to generate the statement in
   */
  private static void addGetterStatement(
      boolean optional, FieldSpec entityField, MethodSpec.Builder entityGetter) {
    if (!optional) entityGetter.addStatement("return $N", entityField);
    else entityGetter.addStatement("return $T.ofNullable($N)", Optional.class, entityField);
  }

  /**
   * @param entityField the JPA entity {@link FieldSpec} for the field that the desired setter will
   *     wrap
   * @return the name of the Java "setter" for the specified {@link FieldSpec}
   */
  private static String calculateSetterName(FieldSpec entityField) {
    return "set" + capitalize(entityField.name);
  }

  /**
   * @param rifField the {@link RifField} to generate the "setter" statement for
   * @param entityField the {@link FieldSpec} for the field being wrapped by the "setter"
   * @param entitySetter the "setter" method to generate the statement in
   */
  private static void addSetterStatement(
      RifField rifField, FieldSpec entityField, MethodSpec.Builder entitySetter) {
    addSetterStatement(rifField.isRifColumnOptional(), entityField, entitySetter);
  }

  /**
   * @param rifField <code>true</code> if the property is an {@link Optional} one, <code>false
   *     </code> otherwise
   * @param entityField the {@link FieldSpec} for the field being wrapped by the "setter"
   * @param entitySetter the "setter" method to generate the statement in
   */
  private static void addSetterStatement(
      boolean optional, FieldSpec entityField, MethodSpec.Builder entitySetter) {
    if (!optional) entitySetter.addStatement("this.$N = $N", entityField, entityField);
    else entitySetter.addStatement("this.$N = $N.orElse(null)", entityField, entityField);
  }

  /**
   * @param name the {@link String} to capitalize the first letter of
   * @return a capitalized {@link String}
   */
  private static String capitalize(String name) {
    char first = name.charAt(0);
    return String.format("%s%s", Character.toUpperCase(first), name.substring(1));
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
              .createResource(StandardLocation.SOURCE_OUTPUT, "", "rif-layout-processor-log.txt");
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
