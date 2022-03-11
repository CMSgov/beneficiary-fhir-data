package gov.cms.bfd.model.codegen;

import com.google.common.io.ByteSource;
import com.google.common.io.Files;
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
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDate;
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
import javax.lang.model.element.Modifier;
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
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/** A Maven Mojo that generates code for RIF JPA entities. */
@Mojo(name = "rif-layouts", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class RifLayoutsMojo extends AbstractMojo {
  private static final String DATA_DICTIONARY_LINK =
      "https://bluebutton.cms.gov/resources/variables/";

  private static final String PARENT_CLAIM = "parentClaim";
  private static final String PARENT_BENEFICIARY = "parentBeneficiary";

  /** The {@link Package#getName()} of the {@link Package} in which to generate the enum. */
  @Parameter(property = "packageName")
  private String packageName;

  /**
   * the file name of the spreadsheet (in this package) that details the RIF layouts to generate
   * code for
   */
  @Parameter(property = "spreadsheetResource")
  private String spreadsheetResource;

  /** the name of the sheet in the Excel file that contains the RIF layout for beneficiary data */
  @Parameter(property = "beneficiarySheet")
  private String beneficiarySheet;

  /**
   * the name of the sheet in the Excel file that contains the RIF layout for beneficiary history
   * data
   */
  @Parameter(property = "beneficiaryHistorySheet")
  private String beneficiaryHistorySheet;

  /**
   * the name of the sheet in the Excel file that contains the RIF layout for medicare beneficiary
   * id data
   */
  @Parameter(property = "medicareBeneficiaryIdSheet")
  private String medicareBeneficiaryIdSheet;

  /** the name of the sheet in the Excel file that contains the RIF layout for PDE claims data */
  @Parameter(property = "pdeSheet")
  private String pdeSheet;

  /**
   * the name of the sheet in the Excel file that contains the RIF layout for carrier claims data
   */
  @Parameter(property = "carrierSheet")
  private String carrierSheet;

  /**
   * the name of the sheet in the Excel file that contains the RIF layout for inpatient claims data
   */
  @Parameter(property = "inpatientSheet")
  private String inpatientSheet;

  /**
   * the name of the sheet in the Excel file that contains the RIF layout for outpatient claims data
   */
  @Parameter(property = "outpatientSheet")
  private String outpatientSheet;

  /** the name of the sheet in the Excel file that contains the RIF layout for HHA claims data */
  @Parameter(property = "hhaSheet")
  private String hhaSheet;

  /** the name of the sheet in the Excel file that contains the RIF layout for DME claims data */
  @Parameter(property = "dmeSheet")
  private String dmeSheet;

  /**
   * the name of the sheet in the Excel file that contains the RIF layout for hospice claims data
   */
  @Parameter(property = "hospiceSheet")
  private String hospiceSheet;

  /** the name of the sheet in the Excel file that contains the RIF layout for SNF claims data */
  @Parameter(property = "snfSheet")
  private String snfSheet;

  @Parameter(property = "inputDirectory", defaultValue = "${project.build.sourceDirectory}")
  private String inputDirectory;

  @Parameter(
      property = "outputDirectory",
      defaultValue = "${project.build.directory}/generated-sources")
  private String outputDirectory;

  @Parameter(
      property = "annotationsDirectory",
      defaultValue = "${project.build.directory}/generated-sources/annotations")
  private String annotationsDirectory;

  @Parameter(property = "project", readonly = true)
  private MavenProject project;

  private File outputDir;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    try {
      outputDir = new File(outputDirectory);
      outputDir.mkdirs();
      process();
      project.addCompileSourceRoot(outputDirectory);
      // Work around bug in hibernate that causes compile to fail if meta data annotation classes
      // already exist.
      deleteAnnotationsDirectory();
    } catch (Exception e) {
      StringWriter writer = new StringWriter();
      e.printStackTrace(new PrintWriter(writer));
      logError("FATAL ERROR: '%s'", writer.toString());
      throw new MojoExecutionException("FATAL ERROR", e);
    }
  }

  private void process() throws IOException {
    log("Processing package '%s'.", packageName);

    /*
     * Find the spreadsheet referenced by the annotation. It will define the
     * RIF layouts.
     */
    ByteSource spreadsheetSource;
    try {
      var file =
          new File(
              inputDirectory
                  + "/"
                  + packageName.replaceAll("\\.", "/")
                  + "/"
                  + spreadsheetResource);
      spreadsheetSource = Files.asByteSource(file);
    } catch (Exception e) {
      throw new IOException(
          String.format(
              "Unable to find or open specified spreadsheet: '%s' in package '%s'.",
              spreadsheetResource, packageName));
    }
    log("Found spreadsheet: '%s' in package '%s'.", spreadsheetResource, packageName);

    /*
     * Parse the spreadsheet, extracting the layouts from it. Also: define
     * the layouts that we expect to parse and generate code for.
     */
    List<MappingSpec> mappingSpecs = new LinkedList<>();
    Workbook spreadsheetWorkbook = null;
    try {
      spreadsheetWorkbook = new XSSFWorkbook(spreadsheetSource.openStream());

      mappingSpecs.add(
          new MappingSpec(packageName)
              .setRifLayout(RifLayout.parse(spreadsheetWorkbook, beneficiarySheet))
              .setHeaderEntity("Beneficiary")
              .setHeaderTable("beneficiaries")
              .setHeaderEntityIdField("BENE_ID")
              .setHeaderEntityAdditionalDatabaseFields(
                  createDetailsForAdditionalDatabaseFields(
                      Arrays.asList("HICN_UNHASHED", "MBI_HASH", "LAST_UPDATED")))
              .setInnerJoinRelationship(
                  Arrays.asList(
                      new InnerJoinRelationship(
                          "beneficiaryId", null, "BeneficiaryHistory", "beneficiaryHistories"),
                      new InnerJoinRelationship(
                          "beneficiaryId",
                          null,
                          "MedicareBeneficiaryIdHistory",
                          "medicareBeneficiaryIdHistories")))
              .setIsBeneficiaryEntity(true));
      /*
       * FIXME Many BeneficiaryHistory fields are marked transient (i.e. not saved to
       * DB), as they won't ever have changed data. We should change the RIF layout to
       * exclude them, but this was implemented in a bit of a rush, and there wasn't
       * time to fix that.
       */
      mappingSpecs.add(
          new MappingSpec(packageName)
              .setRifLayout(RifLayout.parse(spreadsheetWorkbook, beneficiaryHistorySheet))
              .setHeaderEntity("BeneficiaryHistory")
              .setHeaderTable("beneficiaries_history")
              .setHeaderEntityGeneratedIdField("bene_history_id")
              .setSequenceNumberGeneratorName("beneficiaryhistory_beneficiaryhistoryid_seq")
              .setHeaderEntityTransientFields(
                  "STATE_CODE",
                  "BENE_COUNTY_CD",
                  "BENE_ZIP_CD",
                  "BENE_RACE_CD",
                  "BENE_ENTLMT_RSN_ORIG",
                  "BENE_ENTLMT_RSN_CURR",
                  "BENE_ESRD_IND",
                  "BENE_MDCR_STATUS_CD",
                  "BENE_PTA_TRMNTN_CD",
                  "BENE_PTB_TRMNTN_CD",
                  "BENE_SRNM_NAME",
                  "BENE_GVN_NAME",
                  "BENE_MDL_NAME")
              .setHeaderEntityAdditionalDatabaseFields(
                  createDetailsForAdditionalDatabaseFields(
                      Arrays.asList("HICN_UNHASHED", "MBI_HASH", "LAST_UPDATED")))
              .setIsBeneficiaryEntity(false));

      mappingSpecs.add(
          new MappingSpec(packageName)
              .setRifLayout(RifLayout.parse(spreadsheetWorkbook, medicareBeneficiaryIdSheet))
              .setHeaderEntity("MedicareBeneficiaryIdHistory")
              .setHeaderTable("medicare_beneficiaryid_history")
              .setHeaderEntityIdField("bene_mbi_id")
              .setHeaderEntityAdditionalDatabaseFields(
                  createDetailsForAdditionalDatabaseFields(Arrays.asList("LAST_UPDATED"))));

      mappingSpecs.add(
          new MappingSpec(packageName)
              .setRifLayout(RifLayout.parse(spreadsheetWorkbook, pdeSheet))
              .setHeaderEntity("PartDEvent")
              .setHeaderTable("partd_events")
              .setHeaderEntityIdField("PDE_ID")
              .setHeaderEntityAdditionalDatabaseFields(
                  createDetailsForAdditionalDatabaseFields(Arrays.asList("LAST_UPDATED"))));

      mappingSpecs.add(
          new MappingSpec(packageName)
              .setRifLayout(RifLayout.parse(spreadsheetWorkbook, carrierSheet))
              .setHeaderEntity("CarrierClaim")
              .setHeaderTable("carrier_claims")
              .setHeaderEntityIdField("CLM_ID")
              .setHasLines(true)
              .setLineTable("carrier_claim_lines")
              .setLineEntityLineNumberField("LINE_NUM")
              .setHeaderEntityAdditionalDatabaseFields(
                  createDetailsForAdditionalDatabaseFields(Arrays.asList("LAST_UPDATED"))));

      mappingSpecs.add(
          new MappingSpec(packageName)
              .setRifLayout(RifLayout.parse(spreadsheetWorkbook, inpatientSheet))
              .setHeaderEntity("InpatientClaim")
              .setHeaderTable("inpatient_claims")
              .setHeaderEntityIdField("CLM_ID")
              .setHasLines(true)
              .setLineTable("inpatient_claim_lines")
              .setLineEntityLineNumberField("CLM_LINE_NUM")
              .setHeaderEntityAdditionalDatabaseFields(
                  createDetailsForAdditionalDatabaseFields(Arrays.asList("LAST_UPDATED"))));

      mappingSpecs.add(
          new MappingSpec(packageName)
              .setRifLayout(RifLayout.parse(spreadsheetWorkbook, outpatientSheet))
              .setHeaderEntity("OutpatientClaim")
              .setHeaderTable("outpatient_claims")
              .setHeaderEntityIdField("CLM_ID")
              .setHasLines(true)
              .setLineTable("outpatient_claim_lines")
              .setLineEntityLineNumberField("CLM_LINE_NUM")
              .setHeaderEntityAdditionalDatabaseFields(
                  createDetailsForAdditionalDatabaseFields(Arrays.asList("LAST_UPDATED"))));

      mappingSpecs.add(
          new MappingSpec(packageName)
              .setRifLayout(RifLayout.parse(spreadsheetWorkbook, hhaSheet))
              .setHeaderEntity("HHAClaim")
              .setHeaderTable("hha_claims")
              .setHeaderEntityIdField("CLM_ID")
              .setHasLines(true)
              .setLineTable("hha_claim_lines")
              .setLineEntityLineNumberField("CLM_LINE_NUM")
              .setHeaderEntityAdditionalDatabaseFields(
                  createDetailsForAdditionalDatabaseFields(Arrays.asList("LAST_UPDATED"))));

      mappingSpecs.add(
          new MappingSpec(packageName)
              .setRifLayout(RifLayout.parse(spreadsheetWorkbook, dmeSheet))
              .setHeaderEntity("DMEClaim")
              .setHeaderTable("dme_claims")
              .setHeaderEntityIdField("CLM_ID")
              .setHasLines(true)
              .setLineTable("dme_claim_lines")
              .setLineEntityLineNumberField("LINE_NUM")
              .setHeaderEntityAdditionalDatabaseFields(
                  createDetailsForAdditionalDatabaseFields(Arrays.asList("LAST_UPDATED"))));

      mappingSpecs.add(
          new MappingSpec(packageName)
              .setRifLayout(RifLayout.parse(spreadsheetWorkbook, hospiceSheet))
              .setHeaderEntity("HospiceClaim")
              .setHeaderTable("hospice_claims")
              .setHeaderEntityIdField("CLM_ID")
              .setHasLines(true)
              .setLineTable("hospice_claim_lines")
              .setLineEntityLineNumberField("CLM_LINE_NUM")
              .setHeaderEntityAdditionalDatabaseFields(
                  createDetailsForAdditionalDatabaseFields(Arrays.asList("LAST_UPDATED"))));

      mappingSpecs.add(
          new MappingSpec(packageName)
              .setRifLayout(RifLayout.parse(spreadsheetWorkbook, snfSheet))
              .setHeaderEntity("SNFClaim")
              .setHeaderTable("snf_claims")
              .setHeaderEntityIdField("CLM_ID")
              .setHasLines(true)
              .setLineTable("snf_claim_lines")
              .setLineEntityLineNumberField("CLM_LINE_NUM")
              .setHeaderEntityAdditionalDatabaseFields(
                  createDetailsForAdditionalDatabaseFields(Arrays.asList("LAST_UPDATED"))));
    } finally {
      if (spreadsheetWorkbook != null) spreadsheetWorkbook.close();
    }
    log("Generated mapping specification: '%s'", mappingSpecs);

    /* Generate the code for each layout. */
    for (MappingSpec mappingSpec : mappingSpecs) {
      generateCode(mappingSpec);
    }
  }

  /**
   * Generates the code for the specified {@link RifLayout}.
   *
   * @param mappingSpec the {@link MappingSpec} to generate code for
   * @throws IOException An {@link IOException} may be thrown if errors are encountered trying to
   *     generate source files.
   */
  private void generateCode(MappingSpec mappingSpec) throws IOException {
    /*
     * First, create the Java enum for the RIF columns.
     */
    TypeSpec columnEnum = generateColumnEnum(mappingSpec);

    /*
     * Then, create the JPA Entity for the "line" fields, containing: fields
     * and accessors.
     */
    Optional<TypeSpec> lineEntity =
        mappingSpec.getHasLines() ? Optional.of(generateLineEntity(mappingSpec)) : Optional.empty();

    /*
     * Then, create the JPA Entity for the "grouped" fields, containing:
     * fields, accessors, and a RIF-to-JPA-Entity parser.
     */
    TypeSpec headerEntity = generateHeaderEntity(mappingSpec);

    if (mappingSpec.isBeneficiaryEntity()) {
      generateBeneficiaryMonthlyEntity(mappingSpec);
    }

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
    writeFile(columnsEnumFile);

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
    log(
        "\n%s\nGenerating LineEntity code for %s\n%s\n%s",
        "===============================================",
        mappingSpec.getLineTable(),
        mappingSpec.toString(),
        "===============================================");
    RifLayout rifLayout = mappingSpec.getRifLayout();

    // Create the Entity class.
    AnnotationSpec entityAnnotation = AnnotationSpec.builder(Entity.class).build();
    AnnotationSpec tableAnnotation =
        AnnotationSpec.builder(Table.class)
            .addMember("name", "$S", mappingSpec.getLineTable().toLowerCase())
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

    // find associated RifField(s) to imbue into @IdClass
    RifField parentClaimRifField =
        rifLayout.getRifFields().stream()
            .filter(
                f ->
                    f.getRifColumnName()
                        .equalsIgnoreCase(mappingSpec.getHeaderEntityIdField().toLowerCase()))
            .findAny()
            .get();
    RifField lineNumberRifField =
        rifLayout.getRifFields().stream()
            .filter(
                f ->
                    f.getRifColumnName()
                        .equalsIgnoreCase(mappingSpec.getLineEntityLineNumberField().toLowerCase()))
            .findFirst()
            .get();

    // setup field types for associated RifField(s)
    TypeName parentClaimIdFieldType =
        selectJavaFieldType(
            parentClaimRifField.getRifColumnType(),
            parentClaimRifField.isRifColumnOptional(),
            parentClaimRifField.getRifColumnLength(),
            parentClaimRifField.getRifColumnScale());
    TypeName lineNumberFieldType =
        selectJavaFieldType(
            lineNumberRifField.getRifColumnType(),
            lineNumberRifField.isRifColumnOptional(),
            lineNumberRifField.getRifColumnLength(),
            lineNumberRifField.getRifColumnScale());

    // create fields to be added to the @IdClass object
    FieldSpec.Builder parentIdField =
        FieldSpec.builder(parentClaimIdFieldType, PARENT_CLAIM, Modifier.PRIVATE);
    FieldSpec.Builder lineNumberIdField =
        FieldSpec.builder(
            lineNumberFieldType, lineNumberRifField.getJavaFieldName(), Modifier.PRIVATE);

    // Add fields to that @IdClass class
    lineIdClass.addField(parentIdField.build());
    lineIdClass.addField(lineNumberIdField.build());

    // add getter methods to access the ID fields
    MethodSpec.Builder parentGetter =
        MethodSpec.methodBuilder("getParentClaim")
            .addStatement("return $N", PARENT_CLAIM)
            .returns(parentClaimIdFieldType);
    MethodSpec.Builder lineNumberGetter =
        MethodSpec.methodBuilder("getLineNumber")
            .addStatement("return $N", lineNumberRifField.getJavaFieldName())
            .returns(lineNumberFieldType);

    // Add getter, hashCode() and equals(...) to @IdClass.
    lineIdClass.addMethod(parentGetter.build());
    lineIdClass.addMethod(lineNumberGetter.build());
    lineIdClass.addMethod(generateHashCodeMethod(parentIdField.build(), lineNumberIdField.build()));
    lineIdClass.addMethod(
        generateEqualsMethod(
            mappingSpec.getLineEntity(), parentIdField.build(), lineNumberIdField.build()));

    // Finalize the @IdClass and nest it inside the Entity class.
    lineEntity.addType(lineIdClass.build());

    // Add a field and accessor to the "line" Entity for the parent.
    FieldSpec parentClaimField =
        FieldSpec.builder(mappingSpec.getHeaderEntity(), PARENT_CLAIM, Modifier.PRIVATE)
            .addAnnotation(Id.class)
            .addAnnotation(AnnotationSpec.builder(ManyToOne.class).build())
            .addAnnotation(
                AnnotationSpec.builder(JoinColumn.class)
                    .addMember("name", "$S", mappingSpec.getHeaderEntityIdField().toLowerCase())
                    .addMember(
                        "foreignKey",
                        "@$T(name = $S)",
                        ForeignKey.class,
                        String.format(
                                "%s_%s_to_%s",
                                mappingSpec.getLineTable(),
                                mappingSpec.getHeaderEntityIdField().toLowerCase(),
                                mappingSpec.getHeaderTable())
                            .toLowerCase())
                    .build())
            .build();

    lineEntity.addField(parentClaimField);

    // setup parentClaim setter/getter
    MethodSpec parentClaimGetter =
        MethodSpec.methodBuilder(calculateGetterName(parentClaimField))
            .addModifiers(Modifier.PUBLIC)
            .addStatement("return $N", PARENT_CLAIM)
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
                  selectJavaFieldType(
                      rifField.getRifColumnType(),
                      rifField.isRifColumnOptional(),
                      rifField.getRifColumnLength(),
                      rifField.getRifColumnScale()),
                  rifField.getJavaFieldName(),
                  Modifier.PRIVATE)
              .addAnnotations(createAnnotations(mappingSpec, rifField))
              .build();
      lineEntity.addField(lineField);

      MethodSpec.Builder lineFieldGetter =
          MethodSpec.methodBuilder(calculateGetterName(lineField))
              .addModifiers(Modifier.PUBLIC)
              .returns(
                  selectJavaPropertyType(
                      rifField.getRifColumnType(),
                      rifField.isRifColumnOptional(),
                      rifField.getRifColumnLength(),
                      rifField.getRifColumnScale()));
      addGetterStatement(rifField, lineField, lineFieldGetter);
      lineEntity.addMethod(lineFieldGetter.build());

      MethodSpec.Builder lineFieldSetter =
          MethodSpec.methodBuilder(calculateSetterName(lineField))
              .addModifiers(Modifier.PUBLIC)
              .returns(void.class)
              .addParameter(
                  selectJavaPropertyType(
                      rifField.getRifColumnType(),
                      rifField.isRifColumnOptional(),
                      rifField.getRifColumnLength(),
                      rifField.getRifColumnScale()),
                  lineField.name);
      addSetterStatement(rifField, lineField, lineFieldSetter);
      lineEntity.addMethod(lineFieldSetter.build());
    }

    TypeSpec lineEntityFinal = lineEntity.build();
    JavaFile lineEntityClassFile =
        JavaFile.builder(mappingSpec.getPackageName(), lineEntityFinal).build();
    writeFile(lineEntityClassFile);

    return lineEntityFinal;
  }

  private TypeSpec generateBeneficiaryMonthlyEntity(MappingSpec mappingSpec) throws IOException {

    // Create the Entity class.
    AnnotationSpec entityAnnotation = AnnotationSpec.builder(Entity.class).build();
    AnnotationSpec tableAnnotation =
        AnnotationSpec.builder(Table.class).addMember("name", "$S", "beneficiary_monthly").build();

    TypeSpec.Builder beneficiaryMonthlyEntity =
        TypeSpec.classBuilder("BeneficiaryMonthly")
            .addAnnotation(entityAnnotation)
            .addAnnotation(
                AnnotationSpec.builder(IdClass.class)
                    .addMember(
                        "value",
                        "$T.class",
                        ClassName.get("gov.cms.bfd.model.rif", "BeneficiaryMonthly")
                            .nestedClass("BeneficiaryMonthlyId"))
                    .build())
            .addAnnotation(tableAnnotation)
            .addModifiers(Modifier.PUBLIC);

    // Create the @IdClass needed for the composite primary key.
    TypeSpec.Builder beneficiaryMonthlyIdClass =
        TypeSpec.classBuilder("BeneficiaryMonthlyId")
            .addSuperinterface(Serializable.class)
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC);
    beneficiaryMonthlyIdClass.addField(
        FieldSpec.builder(
                long.class, "serialVersionUID", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
            .initializer("$L", 1L)
            .build());

    TypeName parentBeneficiaryIdFieldType = ClassName.get(String.class);
    FieldSpec.Builder parentIdField =
        FieldSpec.builder(parentBeneficiaryIdFieldType, PARENT_BENEFICIARY, Modifier.PRIVATE);

    // Add a field to that @IdClass class for the month.
    TypeName yearMonthFieldType = ClassName.get(LocalDate.class);
    FieldSpec.Builder yearMonthIdField =
        FieldSpec.builder(yearMonthFieldType, "yearMonth", Modifier.PRIVATE);

    beneficiaryMonthlyIdClass.addField(parentIdField.build());
    beneficiaryMonthlyIdClass.addField(yearMonthIdField.build());

    MethodSpec.Builder parentGetter =
        MethodSpec.methodBuilder("getParentBeneficiary")
            .addStatement("return $N", PARENT_BENEFICIARY)
            .returns(parentBeneficiaryIdFieldType);
    beneficiaryMonthlyIdClass.addMethod(parentGetter.build());
    MethodSpec.Builder yearMonthGetter =
        MethodSpec.methodBuilder("getYearMonth")
            .addStatement("return $N", "yearMonth")
            .returns(yearMonthFieldType);
    beneficiaryMonthlyIdClass.addMethod(yearMonthGetter.build());

    // Add hashCode() and equals(...) to that @IdClass.
    beneficiaryMonthlyIdClass.addMethod(
        generateHashCodeMethod(parentIdField.build(), yearMonthIdField.build()));
    beneficiaryMonthlyIdClass.addMethod(
        generateEqualsMethod(
            mappingSpec.getBeneficiaryMonthlyEntity(),
            parentIdField.build(),
            yearMonthIdField.build()));

    // Finalize the @IdClass and nest it inside the Entity class.
    beneficiaryMonthlyEntity.addType(beneficiaryMonthlyIdClass.build());

    // Add a field and accessor to the "line" Entity for the parent.
    FieldSpec parentBeneficiaryField =
        FieldSpec.builder(
                ClassName.get("gov.cms.bfd.model.rif", "Beneficiary"),
                PARENT_BENEFICIARY,
                Modifier.PRIVATE)
            .addAnnotation(Id.class)
            .addAnnotation(AnnotationSpec.builder(ManyToOne.class).build())
            .addAnnotation(
                AnnotationSpec.builder(JoinColumn.class)
                    .addMember("name", "$S", "bene_id")
                    .addMember(
                        "foreignKey",
                        "@$T(name = $S)",
                        ForeignKey.class,
                        String.format(
                                "%s_%s_to_%s", "beneficiary_monthly", "bene_id", "beneficiary")
                            .toLowerCase())
                    .build())
            .build();

    beneficiaryMonthlyEntity.addField(parentBeneficiaryField);

    MethodSpec parentBeneficiaryGetter =
        MethodSpec.methodBuilder(calculateGetterName(parentBeneficiaryField))
            .addModifiers(Modifier.PUBLIC)
            .addStatement("return $N", PARENT_BENEFICIARY)
            .returns(ClassName.get("gov.cms.bfd.model.rif", "Beneficiary"))
            .build();
    beneficiaryMonthlyEntity.addMethod(parentBeneficiaryGetter);

    MethodSpec.Builder parentBeneficiarySetter =
        MethodSpec.methodBuilder(calculateSetterName(parentBeneficiaryField))
            .addModifiers(Modifier.PUBLIC)
            .returns(void.class)
            .addParameter(
                ClassName.get("gov.cms.bfd.model.rif", "Beneficiary"), parentBeneficiaryField.name);
    addSetterStatement(false, parentBeneficiaryField, parentBeneficiarySetter);
    beneficiaryMonthlyEntity.addMethod(parentBeneficiarySetter.build());

    // These aren't "real" RifFields, as they're not in the spreadsheet; representing them here as
    // such, to make
    // it easier to add them into the spreadsheet in the future.
    RifField rifField =
        new RifField(
            "YEAR_MONTH",
            RifColumnType.DATE,
            Optional.of(8),
            Optional.empty(),
            false,
            null,
            null,
            "yearMonth");
    createBeneficiaryMonthlyFields(beneficiaryMonthlyEntity, true, rifField);

    rifField =
        new RifField(
            "FIPS_STATE_CNTY_CODE",
            RifColumnType.CHAR,
            Optional.of(5),
            Optional.empty(),
            true,
            null,
            null,
            "fipsStateCntyCode");
    createBeneficiaryMonthlyFields(beneficiaryMonthlyEntity, false, rifField);

    rifField =
        new RifField(
            "MEDICARE_STATUS_CODE",
            RifColumnType.CHAR,
            Optional.of(2),
            Optional.empty(),
            true,
            null,
            null,
            "medicareStatusCode");
    createBeneficiaryMonthlyFields(beneficiaryMonthlyEntity, false, rifField);

    rifField =
        new RifField(
            "ENTITLEMENT_BUY_IN_IND",
            RifColumnType.CHAR,
            Optional.of(1),
            Optional.empty(),
            true,
            null,
            null,
            "entitlementBuyInInd");
    createBeneficiaryMonthlyFields(beneficiaryMonthlyEntity, false, rifField);

    rifField =
        new RifField(
            "HMO_INDICATOR_IND",
            RifColumnType.CHAR,
            Optional.of(1),
            Optional.empty(),
            true,
            null,
            null,
            "hmoIndicatorInd");
    createBeneficiaryMonthlyFields(beneficiaryMonthlyEntity, false, rifField);

    rifField =
        new RifField(
            "PARTC_CONTRACT_NUMBER_ID",
            RifColumnType.CHAR,
            Optional.of(5),
            Optional.empty(),
            true,
            null,
            null,
            "partCContractNumberId");
    createBeneficiaryMonthlyFields(beneficiaryMonthlyEntity, false, rifField);

    rifField =
        new RifField(
            "PARTC_PBP_NUMBER_ID",
            RifColumnType.CHAR,
            Optional.of(3),
            Optional.empty(),
            true,
            null,
            null,
            "partCPbpNumberId");
    createBeneficiaryMonthlyFields(beneficiaryMonthlyEntity, false, rifField);

    rifField =
        new RifField(
            "PARTC_PLAN_TYPE_CODE",
            RifColumnType.CHAR,
            Optional.of(3),
            Optional.empty(),
            true,
            null,
            null,
            "partCPlanTypeCode");
    createBeneficiaryMonthlyFields(beneficiaryMonthlyEntity, false, rifField);

    rifField =
        new RifField(
            "PARTD_CONTRACT_NUMBER_ID",
            RifColumnType.CHAR,
            Optional.of(5),
            Optional.empty(),
            true,
            null,
            null,
            "partDContractNumberId");
    createBeneficiaryMonthlyFields(beneficiaryMonthlyEntity, false, rifField);

    rifField =
        new RifField(
            "PARTD_PBP_NUMBER_ID",
            RifColumnType.CHAR,
            Optional.of(3),
            Optional.empty(),
            true,
            null,
            null,
            "partDPbpNumberId");
    createBeneficiaryMonthlyFields(beneficiaryMonthlyEntity, false, rifField);

    rifField =
        new RifField(
            "PARTD_SEGMENT_NUMBER_ID",
            RifColumnType.CHAR,
            Optional.of(3),
            Optional.empty(),
            true,
            null,
            null,
            "partDSegmentNumberId");
    createBeneficiaryMonthlyFields(beneficiaryMonthlyEntity, false, rifField);

    rifField =
        new RifField(
            "PARTD_RETIREE_DRUG_SUBSIDY_IND",
            RifColumnType.CHAR,
            Optional.of(1),
            Optional.empty(),
            true,
            null,
            null,
            "partDRetireeDrugSubsidyInd");
    createBeneficiaryMonthlyFields(beneficiaryMonthlyEntity, false, rifField);

    rifField =
        new RifField(
            "MEDICAID_DUAL_ELIGIBILITY_CODE",
            RifColumnType.CHAR,
            Optional.of(2),
            Optional.empty(),
            true,
            null,
            null,
            "medicaidDualEligibilityCode");
    createBeneficiaryMonthlyFields(beneficiaryMonthlyEntity, false, rifField);

    rifField =
        new RifField(
            "PARTD_LOW_INCOME_COST_SHARE_GROUP_CODE",
            RifColumnType.CHAR,
            Optional.of(2),
            Optional.empty(),
            true,
            null,
            null,
            "partDLowIncomeCostShareGroupCode");
    createBeneficiaryMonthlyFields(beneficiaryMonthlyEntity, false, rifField);

    TypeSpec beneficiaryMonthlyEntityFinal = beneficiaryMonthlyEntity.build();
    JavaFile beneficiaryMonthlyClassFile =
        JavaFile.builder("gov.cms.bfd.model.rif", beneficiaryMonthlyEntityFinal).build();
    writeFile(beneficiaryMonthlyClassFile);

    return beneficiaryMonthlyEntityFinal;
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
    log(
        "\n%s\nGenerating code for %s\n%s\n%s",
        "===============================================",
        mappingSpec.getHeaderTable(),
        mappingSpec.toString(),
        "===============================================");

    // Create the Entity class.
    AnnotationSpec entityAnnotation = AnnotationSpec.builder(Entity.class).build();
    AnnotationSpec tableAnnotation =
        AnnotationSpec.builder(Table.class)
            .addMember("name", "$S", mappingSpec.getHeaderTable().toLowerCase())
            .build();
    TypeSpec.Builder headerEntityClass =
        TypeSpec.classBuilder(mappingSpec.getHeaderEntity())
            .addAnnotation(entityAnnotation)
            .addAnnotation(tableAnnotation)
            .addSuperinterface(ClassName.get("gov.cms.bfd.model.rif", "RifRecordBase"))
            .addModifiers(Modifier.PUBLIC);

    // Create an Entity field with accessors for the generated-ID field (if any).
    if (mappingSpec.getHeaderEntityGeneratedIdField() != null) {
      FieldSpec.Builder idFieldBuilder =
          FieldSpec.builder(
              TypeName.LONG, mappingSpec.getHeaderEntityGeneratedIdField(), Modifier.PRIVATE);
      idFieldBuilder.addAnnotation(Id.class);
      idFieldBuilder.addAnnotation(
          AnnotationSpec.builder(Column.class)
              .addMember("name", "$S", mappingSpec.getHeaderEntityGeneratedIdField().toLowerCase())
              .addMember("nullable", "$L", false)
              .addMember("updatable", "$L", false)
              .build());

      String sequenceName = mappingSpec.getSequenceNumberGeneratorName().toLowerCase();
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
    int entityLastHeaderFieldIx = mappingSpec.calculateLastHeaderFieldIndex();
    log("entityLastHeaderFieldIx=%d", entityLastHeaderFieldIx);
    for (int fieldIndex = 0; fieldIndex <= entityLastHeaderFieldIx; fieldIndex++) {
      RifField rifField = mappingSpec.getRifLayout().getRifFields().get(fieldIndex);

      FieldSpec headerField =
          FieldSpec.builder(
                  selectJavaFieldType(
                      rifField.getRifColumnType(),
                      rifField.isRifColumnOptional(),
                      rifField.getRifColumnLength(),
                      rifField.getRifColumnScale()),
                  rifField.getJavaFieldName(),
                  Modifier.PRIVATE)
              .addAnnotations(createAnnotations(mappingSpec, rifField))
              .build();
      headerEntityClass.addField(headerField);

      MethodSpec.Builder headerFieldGetter =
          MethodSpec.methodBuilder(calculateGetterName(headerField))
              .addModifiers(Modifier.PUBLIC)
              .returns(
                  selectJavaPropertyType(
                      rifField.getRifColumnType(),
                      rifField.isRifColumnOptional(),
                      rifField.getRifColumnLength(),
                      rifField.getRifColumnScale()));
      addGetterStatement(rifField, headerField, headerFieldGetter);
      headerEntityClass.addMethod(headerFieldGetter.build());

      MethodSpec.Builder headerFieldSetter =
          MethodSpec.methodBuilder(calculateSetterName(headerField))
              .addModifiers(Modifier.PUBLIC)
              .returns(void.class)
              .addParameter(
                  selectJavaPropertyType(
                      rifField.getRifColumnType(),
                      rifField.isRifColumnOptional(),
                      rifField.getRifColumnLength(),
                      rifField.getRifColumnScale()),
                  headerField.name);
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
                  selectJavaFieldType(
                      addlDatabaseField.getRifColumnType(),
                      addlDatabaseField.isRifColumnOptional(),
                      addlDatabaseField.getRifColumnLength(),
                      addlDatabaseField.getRifColumnScale()),
                  addlDatabaseField.getJavaFieldName(),
                  Modifier.PRIVATE)
              .addAnnotations(createAnnotations(mappingSpec, addlDatabaseField))
              .build();
      headerEntityClass.addField(headerField);

      MethodSpec.Builder headerFieldGetter =
          MethodSpec.methodBuilder(calculateGetterName(headerField))
              .addModifiers(Modifier.PUBLIC)
              .returns(
                  selectJavaPropertyType(
                      addlDatabaseField.getRifColumnType(),
                      addlDatabaseField.isRifColumnOptional(),
                      addlDatabaseField.getRifColumnLength(),
                      addlDatabaseField.getRifColumnScale()));
      addGetterStatement(addlDatabaseField, headerField, headerFieldGetter);
      headerEntityClass.addMethod(headerFieldGetter.build());

      MethodSpec.Builder headerFieldSetter =
          MethodSpec.methodBuilder(calculateSetterName(headerField))
              .addModifiers(Modifier.PUBLIC)
              .returns(void.class)
              .addParameter(
                  selectJavaPropertyType(
                      addlDatabaseField.getRifColumnType(),
                      addlDatabaseField.isRifColumnOptional(),
                      addlDatabaseField.getRifColumnLength(),
                      addlDatabaseField.getRifColumnScale()),
                  headerField.name);
      addSetterStatement(addlDatabaseField, headerField, headerFieldSetter);
      headerEntityClass.addMethod(headerFieldSetter.build());
      log("addlDatabaseField added, %s", addlDatabaseField);
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

    // Add the parent-to-child join field and accessor, if appropriate.
    if (mappingSpec.isBeneficiaryEntity()) {

      ParameterizedTypeName childFieldType =
          ParameterizedTypeName.get(
              ClassName.get(List.class), mappingSpec.getBeneficiaryMonthlyEntity());

      FieldSpec.Builder childField =
          FieldSpec.builder(childFieldType, "beneficiaryMonthlys", Modifier.PRIVATE)
              .initializer("new $T<>()", LinkedList.class);

      childField.addAnnotation(
          AnnotationSpec.builder(OneToMany.class)
              .addMember("mappedBy", "$S", mappingSpec.getBeneficiaryMonthlyEntityParentField())
              .addMember("orphanRemoval", "$L", true)
              .addMember("fetch", "$T.LAZY", FetchType.class)
              .addMember("cascade", "$T.ALL", CascadeType.class)
              .build());
      childField.addAnnotation(
          AnnotationSpec.builder(OrderBy.class)
              .addMember("value", "$S", mappingSpec.getEntityBeneficiaryMonthlyField() + " ASC")
              .build());
      headerEntityClass.addField(childField.build());

      MethodSpec childGetter =
          MethodSpec.methodBuilder("getBeneficiaryMonthlys")
              .addModifiers(Modifier.PUBLIC)
              .addStatement("return $N", "beneficiaryMonthlys")
              .returns(childFieldType)
              .build();
      headerEntityClass.addMethod(childGetter);

      MethodSpec childSetter =
          MethodSpec.methodBuilder("setBeneficiaryMonthlys")
              .addModifiers(Modifier.PUBLIC)
              .returns(void.class)
              .addParameter(childFieldType, "beneficiaryMonthlys")
              .addStatement(
                  "this.$N = ($T)$N", "beneficiaryMonthlys", childFieldType, "beneficiaryMonthlys")
              .build();
      headerEntityClass.addMethod(childSetter);
    }

    // Add a hardcoded "Beneficiary.skippedRifRecords" field, if appropriate.
    if (mappingSpec.isBeneficiaryEntity()) {
      ParameterizedTypeName childFieldType =
          ParameterizedTypeName.get(
              ClassName.get(Set.class),
              ClassName.get(mappingSpec.getPackageName(), "SkippedRifRecord"));

      FieldSpec.Builder childField =
          FieldSpec.builder(childFieldType, "skippedRifRecords", Modifier.PRIVATE)
              .initializer("new $T<>()", HashSet.class);

      childField.addAnnotation(
          AnnotationSpec.builder(OneToMany.class)
              .addMember("mappedBy", "$S", "beneId")
              .addMember("orphanRemoval", "$L", false)
              .addMember("fetch", "$T.LAZY", FetchType.class)
              .addMember("cascade", "$T.ALL", CascadeType.class)
              .build());
      headerEntityClass.addField(childField.build());

      MethodSpec childGetter =
          MethodSpec.methodBuilder("getSkippedRifRecords")
              .addModifiers(Modifier.PUBLIC)
              .addStatement("return $N", "skippedRifRecords")
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
    TypeSpec headerEntityFinal = headerEntityClass.build();
    JavaFile headerEntityFile =
        JavaFile.builder(mappingSpec.getPackageName(), headerEntityFinal).build();
    writeFile(headerEntityFile);

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
    int rifFieldsSize = mappingSpec.getRifLayout().getRifFields().size();
    int firstLineFieldIx =
        mappingSpec.getHasLines() ? mappingSpec.calculateFirstLineFieldIndex() : -1;
    log(
        "generateParser, # of RifFields: %d, line field starts at: %d",
        rifFieldsSize, firstLineFieldIx);

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
    for (int fieldIndex = 0; fieldIndex < rifFieldsSize; fieldIndex++) {
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
      // log("create code for: %s", entityField.toString());

      // Are we starting the header parsing?
      if (fieldIndex == 0) {
        parseMethod.addCode("\n// Parse the header fields.\n");
        parseMethod.addCode("$T headerRecord = csvRecords.get(0);\n", csvRecordType);
      }

      // Are we starting the line parsing?
      if (fieldIndex == firstLineFieldIx) {
        parseMethod.addCode("\n// Parse the line fields.\n");
        parseMethod.beginControlFlow(
            "for (int lineIndex = 0; lineIndex < csvRecords.size(); lineIndex++)");
        parseMethod.addStatement("$T lineRecord = csvRecords.get(lineIndex)", csvRecordType);
        parseMethod.addStatement("$1T line = new $1T()", mappingSpec.getLineEntity());

        FieldSpec lineEntityParentField =
            lineEntity.get().fieldSpecs.stream()
                .filter(f -> f.name.equalsIgnoreCase(mappingSpec.getLineEntityParentField()))
                .findAny()
                .get();
        parseMethod.addCode("line.$L(header);\n\n", calculateSetterName(lineEntityParentField));
      }

      // Determine which variables to use in assignment statement.
      String entityName;
      String recordName;
      if (mappingSpec.getHasLines() && fieldIndex >= firstLineFieldIx) {
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
    log("parsingClass: %s", parsingClassFinal.name);
    JavaFile parsingClassFile =
        JavaFile.builder(mappingSpec.getPackageName(), parsingClassFinal).build();
    writeFile(parsingClassFile);

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

    String headerColumnsList = calculateCsvColumns(headerEntity.fieldSpecs, mappingSpec);

    if (isDebug()) {
      logDebug(
          "headerColumnsList\n=====================\n%s",
          headerColumnsList.replaceAll(", ", ",\n"));
    }
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
              .filter(f -> f.name.equalsIgnoreCase(mappingSpec.getHeaderEntityLinesField()))
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

      String lineColumnsList = calculateCsvColumns(lineEntity.get().fieldSpecs, mappingSpec);

      if (isDebug()) {
        logDebug(
            "lineColumnsList\n=====================\n%s", lineColumnsList.replaceAll(", ", ",\n"));
      }

      csvWriterMethod.addStatement("lineRecords[0] = new $1T{ $2L }", recordType, lineColumnsList);
      csvWriterMethod.beginControlFlow(
          "for (int lineIndex = 0; lineIndex < entity.$L().size();lineIndex++)", linesFieldGetter);
      csvWriterMethod.addStatement(
          "$T lineEntity = entity.$L().get(lineIndex)",
          mappingSpec.getLineEntity(),
          linesFieldGetter);

      FieldSpec parentField =
          lineEntity.get().fieldSpecs.stream()
              .filter(f -> f.name.equalsIgnoreCase(PARENT_CLAIM))
              .findAny()
              .get();
      FieldSpec headerIdField =
          headerEntity.fieldSpecs.stream()
              .filter(f -> f.name.equalsIgnoreCase("claimId"))
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
    writeFile(parsingClassFile);
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
    StringBuilder sb = new StringBuilder();
    if (isDebug()) {
      sb.append("calculateFieldToCsvValueCode: [ ")
          .append("instanceName=")
          .append(instanceName)
          .append(", field=")
          .append(field.name);
      if (parentField != null) {
        sb.append(", parentField=").append(parentField.name);
      }
      if (headerIdField != null) {
        sb.append(", headerIdField=").append(headerIdField.name);
      }
    }
    StringBuilder code = new StringBuilder(instanceName);
    code.append(".");

    Optional<RifField> rifField =
        mappingSpec.getRifLayout().getRifFields().stream()
            .filter(f -> field.name.equals(f.getJavaFieldName()))
            .findAny();

    if (field == parentField) {
      // This is the line-level "parent" field.
      code.append(calculateGetterName(parentField)).append("().");
      code.append(calculateGetterName(headerIdField)).append("()");
    } else if (rifField.isPresent() && rifField.get().isRifColumnOptional()) {
      code.append(calculateGetterName(field)).append("().orElse(null)");
    } else {
      code.append(calculateGetterName(field)).append("()");
    }
    sb.append(", code=").append(code).append(" ]");
    log(sb.toString());
    return code.toString();
  }

  /**
   * Used in {@link #calculateCsvColumns(List<FieldSpec>, MappingSpec)} and generates the
   * field-to-CSV-value header.
   *
   * @param fields {@link List<FieldSpec>} to process
   * @param mappingSpec the {@link MappingSpec} of the field to generate conversion code for
   * @return the string header of column names
   */
  private String calculateCsvColumns(List<FieldSpec> fields, MappingSpec mappingSpec) {
    StringBuilder sb = new StringBuilder();
    int cnt = 0;
    for (FieldSpec field : fields) {
      Optional<RifField> rifField =
          mappingSpec.getRifLayout().getRifFields().stream()
              .filter(f -> field.name.equalsIgnoreCase(f.getJavaFieldName()))
              .findAny();

      if (!rifField.isPresent()) {
        rifField =
            mappingSpec.getHeaderEntityAdditionalDatabaseFields().stream()
                .filter(f -> field.name.equalsIgnoreCase(f.getJavaFieldName()))
                .findAny();
      }
      if (rifField.isPresent()) {
        sb.append(cnt > 0 ? ", \"" : "\"").append(rifField.get().getRifColumnName()).append("\"");
        cnt++;
      }
    }
    return sb.toString();
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
   * @param mappingSpec the {@link MappingSpec} for the specified {@link RifField}
   * @param rifField the {@link RifField} to create the corresponding {@link AnnotationSpec}s for
   * @return an ordered {@link List} of {@link AnnotationSpec}s representing the JPA, etc.
   *     annotations that should be applied to the specified {@link RifField}
   */
  private static List<AnnotationSpec> createAnnotations(
      MappingSpec mappingSpec, RifField rifField) {
    LinkedList<AnnotationSpec> annotations = new LinkedList<>();

    // Add an @Id annotation, if appropriate.
    if (rifField.getRifColumnName().equalsIgnoreCase(mappingSpec.getHeaderEntityIdField())
        || (mappingSpec.getHasLines()
            && rifField
                .getRifColumnName()
                .equalsIgnoreCase(mappingSpec.getLineEntityLineNumberField()))) {
      AnnotationSpec.Builder idAnnotation = AnnotationSpec.builder(Id.class);
      annotations.add(idAnnotation.build());
    }

    // Add an @Column annotation to every non-transient column.
    boolean isTransient =
        mappingSpec.getHeaderEntityTransientFields().contains(rifField.getRifColumnName());
    if (!isTransient) {
      AnnotationSpec.Builder columnAnnotation =
          AnnotationSpec.builder(Column.class)
              .addMember("name", "$S", rifField.getRifColumnName().toLowerCase())
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
      if (additionalDatabaseField.contentEquals("HICN_UNHASHED")) {
        RifField hicnUnhashed =
            new RifField(
                "HICN_UNHASHED",
                RifColumnType.CHAR,
                Optional.of(64),
                Optional.of(0),
                Boolean.TRUE,
                new URL(DATA_DICTIONARY_LINK + "hicnUnhashed"),
                "HICN_UNHASHED",
                "hicnUnhashed");
        addlDatabaseFields.add(hicnUnhashed);
        continue;
      }
      if (additionalDatabaseField.contentEquals("MBI_HASH")) {
        RifField mbiHash =
            new RifField(
                "MBI_HASH",
                RifColumnType.CHAR,
                Optional.of(64),
                Optional.of(0),
                Boolean.TRUE,
                new URL(DATA_DICTIONARY_LINK + "mbiHash"),
                "MBI_HASH",
                "mbiHash");
        addlDatabaseFields.add(mbiHash);
        continue;
      }
      if (additionalDatabaseField.contentEquals("LAST_UPDATED")) {
        RifField lastUpdated =
            new RifField(
                "LAST_UPDATED",
                RifColumnType.TIMESTAMP,
                Optional.of(20),
                Optional.of(0),
                Boolean.TRUE,
                new URL(DATA_DICTIONARY_LINK + "lastUpdated"),
                "LAST_UPDATED",
                "lastUpdated");
        addlDatabaseFields.add(lastUpdated);
        continue;
      }
    }
    return addlDatabaseFields;
  }

  /**
   * @param fieldName the JPA entity field name to convert from snake case to camel case
   * @return the input string converted to camel case
   */
  public static String convertToCamelCase(String fieldName) {
    if (!fieldName.contains("_")) {
      return fieldName;
    }
    // Capitalize first letter of string
    String camelCaseResult = fieldName.toLowerCase();
    camelCaseResult = camelCaseResult.substring(0, 1).toUpperCase() + camelCaseResult.substring(1);

    // iterate over string looking for '_' (underscore)
    while (camelCaseResult.contains("_")) {
      camelCaseResult =
          camelCaseResult.replaceFirst(
              "_[a-z]",
              String.valueOf(
                  Character.toUpperCase(camelCaseResult.charAt(camelCaseResult.indexOf("_") + 1))));
    }
    return camelCaseResult;
  }

  /**
   * @param entityField the JPA entity {@link FieldSpec} for the field that the desired getter will
   *     wrap
   * @return the name of the Java "getter" for the specified {@link FieldSpec}
   */
  private static String calculateGetterName(FieldSpec entityField) {
    String name = capitalize(convertToCamelCase(entityField.name));

    return entityField.type.equals(TypeName.BOOLEAN)
            || entityField.type.equals(ClassName.get(Boolean.class))
        ? "is" + name
        : "get" + name;
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
    if (!optional) {
      entityGetter.addStatement("return $N", entityField);
    } else {
      entityGetter.addStatement("return $T.ofNullable($N)", Optional.class, entityField);
    }
  }

  /**
   * @param entityField the JPA entity {@link FieldSpec} for the field that the desired setter will
   *     wrap @Param overrideName allow flexibility in not using JPA entity name as the basis for
   *     setter
   * @return the name of the Java "setter" for the specified {@link FieldSpec}
   */
  private static String calculateSetterName(FieldSpec entityField) {
    return "set" + capitalize(convertToCamelCase(entityField.name));
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
    if (!optional) {
      entitySetter.addStatement("this.$N = $N", entityField, entityField);
    } else {
      entitySetter.addStatement("this.$N = $N.orElse(null)", entityField, entityField);
    }
  }

  /**
   * @param name the {@link String} to capitalize the first letter of
   * @return a capitalized {@link String}
   */
  private static String capitalize(String name) {
    return String.format("%s%s", Character.toUpperCase(name.charAt(0)), name.substring(1));
  }

  /**
   * Creates the fields for the BeneficiaryMonthly class in the model rif
   *
   * @param lineEntity helps build the entity {@link TypeSpec.Builder}
   * @param isId determines if the field is an id field
   * @param rifField {@link RifField} to create
   */
  private static void createBeneficiaryMonthlyFields(
      TypeSpec.Builder lineEntity, boolean isId, RifField rifField) {

    List<AnnotationSpec> annotSpecs = createBeneficiaryMonthlyAnnotations(isId, rifField);
    TypeName javaFieldType =
        selectJavaFieldType(
            rifField.getRifColumnType(), rifField.isRifColumnOptional(),
            rifField.getRifColumnLength(), rifField.getRifColumnScale());
    TypeName javaPropType =
        selectJavaPropertyType(
            rifField.getRifColumnType(), rifField.isRifColumnOptional(),
            rifField.getRifColumnLength(), rifField.getRifColumnScale());
    FieldSpec lineField =
        FieldSpec.builder(javaFieldType, rifField.getJavaFieldName(), Modifier.PRIVATE)
            .addAnnotations(annotSpecs)
            .build();
    lineEntity.addField(lineField);

    MethodSpec.Builder lineFieldGetter =
        MethodSpec.methodBuilder(calculateGetterName(lineField))
            .addModifiers(Modifier.PUBLIC)
            .returns(javaPropType);
    addGetterStatement(rifField.isRifColumnOptional(), lineField, lineFieldGetter);
    lineEntity.addMethod(lineFieldGetter.build());

    MethodSpec.Builder lineFieldSetter =
        MethodSpec.methodBuilder(calculateSetterName(lineField))
            .addModifiers(Modifier.PUBLIC)
            .returns(void.class)
            .addParameter(javaPropType, lineField.name);
    addSetterStatement(rifField.isRifColumnOptional(), lineField, lineFieldSetter);
    lineEntity.addMethod(lineFieldSetter.build());
  }

  /**
   * Creates the fields for the BeneficiaryMonthly annotations in the model rif
   *
   * @param isId determines if the field is an id field
   * @param rifField {@link RifField} to create
   */
  private static List<AnnotationSpec> createBeneficiaryMonthlyAnnotations(
      boolean isId, RifField rifField) {

    LinkedList<AnnotationSpec> annotations = new LinkedList<>();
    // Add an @Id annotation, if appropriate.
    if (isId) {
      AnnotationSpec.Builder idAnnotation = AnnotationSpec.builder(Id.class);
      annotations.add(idAnnotation.build());
    }
    // Add an @Column annotation to every column.
    AnnotationSpec.Builder columnAnnotation =
        AnnotationSpec.builder(Column.class)
            .addMember("name", "$S", rifField.getRifColumnName().toLowerCase())
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

      if (rifField.getRifColumnLength().isPresent()) {
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
        StringBuilder columnDefinition = new StringBuilder("numeric");
        if (rifField.getRifColumnLength().isPresent()) {
          columnDefinition.append("(").append(rifField.getRifColumnLength().get());

          if (rifField.getRifColumnScale().isPresent()) {
            columnDefinition.append(", ").append(rifField.getRifColumnScale().get());
          }
          columnDefinition.append(")");
        }
        columnAnnotation.addMember("columnDefinition", "$S", columnDefinition.toString());
      }
    }
    annotations.add(columnAnnotation.build());
    return annotations;
  }

  /**
   * Selects the java field type
   *
   * @param type specifies the field type {@link RifColumnType}
   * @param isColumnOptional determines if the field is optional {@link boolean}
   * @param columnLength specifies the column length {@link Optional<Integer>}, for numeric types
   *     this represents the total number of digits that can be stored
   * @param columnScale specifies the column scale {@link Optional<Integer>}, for numeric types this
   *     represents how many of the total digits (see `columnLength`) are to the right of the
   *     decimal point
   */
  private static TypeName selectJavaFieldType(
      RifColumnType type,
      boolean isColumnOptional,
      Optional<Integer> columnLength,
      Optional<Integer> columnScale) {
    if (type == RifColumnType.CHAR
        && columnLength.orElse(Integer.MAX_VALUE) == 1
        && !isColumnOptional) return TypeName.CHAR;
    else if (type == RifColumnType.CHAR
        && columnLength.orElse(Integer.MAX_VALUE) == 1
        && isColumnOptional) return ClassName.get(Character.class);
    else if (type == RifColumnType.CHAR) return ClassName.get(String.class);
    else if (type == RifColumnType.DATE && columnLength.orElse(0) == 8)
      return ClassName.get(LocalDate.class);
    else if (type == RifColumnType.TIMESTAMP && columnLength.orElse(0) == 20)
      return ClassName.get(Instant.class);
    else if (type == RifColumnType.NUM && columnScale.orElse(Integer.MAX_VALUE) > 0)
      return ClassName.get(BigDecimal.class);
    else if (type == RifColumnType.NUM
        && columnScale.orElse(Integer.MAX_VALUE) == 0
        && !isColumnOptional) return TypeName.INT;
    else if (type == RifColumnType.NUM
        && columnScale.orElse(Integer.MAX_VALUE) == 0
        && isColumnOptional) return ClassName.get(Integer.class);
    else throw new IllegalArgumentException("Unhandled field type: " + type.name());
  }

  /**
   * Selects the java property type
   *
   * @param type specifies the field type {@link RifColumnType}
   * @param isColumnOptional determines if the field is optional {@link boolean}
   * @param columnLength specifies the column length {@link Optional<Integer>}, for numeric types
   *     this represents the total number of digits that can be stored
   * @param columnScale specifies the column scale {@link Optional<Integer>}, for numeric types this
   *     represents how many of the total digits (see `columnLength`) are to the right of the
   *     decimal point
   */
  private static TypeName selectJavaPropertyType(
      RifColumnType type,
      boolean isColumnOptional,
      Optional<Integer> columnLength,
      Optional<Integer> columnScale) {
    if (!isColumnOptional)
      return selectJavaFieldType(type, isColumnOptional, columnLength, columnScale);
    else
      return ParameterizedTypeName.get(
          ClassName.get(Optional.class),
          selectJavaFieldType(type, isColumnOptional, columnLength, columnScale));
  }

  /**
   * Reports the specified log message at info log level.
   *
   * @param messageFormat the log message format {@link String}
   * @param messageArguments the log message format arguments
   */
  private void log(String messageFormat, Object... messageArguments) {
    String logMessage = String.format(messageFormat, messageArguments);
    getLog().info(logMessage);
  }

  /**
   * Reports the specified log message at debug log level.
   *
   * @param messageFormat the log message format {@link String}
   * @param messageArguments the log message format arguments
   */
  private void logDebug(String messageFormat, Object... messageArguments) {
    String logMessage = String.format(messageFormat, messageArguments);
    getLog().info(logMessage);
  }

  /**
   * Reports the specified log message at error log level.
   *
   * @param messageFormat the log message format {@link String}
   * @param messageArguments the log message format arguments
   */
  private void logError(String messageFormat, Object... messageArguments) {
    String logMessage = String.format(messageFormat, messageArguments);
    getLog().error(logMessage);
  }

  private boolean isDebug() {
    return getLog().isDebugEnabled();
  }

  private void writeFile(JavaFile javaFile) throws IOException {
    final var filePath = javaFile.toJavaFileObject().getName();
    final var sourceFile = new File(outputDir, filePath);
    log("writing source file: %s", sourceFile.getPath());
    javaFile.writeTo(outputDir);
  }

  /**
   * There is a bug in hibernate that causes compile to fail if meta data annotation classes already
   * exist and there is no setting to disable this behavior. As a result if a build is done without
   * an intervening {@code mvn clean} the build will fail. This method checks for the existence of
   * the annotations directory and deletes all existing java files within the directory to clear the
   * decks for the hibernate plugin's code generation.
   */
  private void deleteAnnotationsDirectory() {
    final var directory = new File(annotationsDirectory, packageName.replaceAll("\\.", "/"));
    log("checking annotations directory: %s", directory.getPath());
    if (directory.isDirectory()) {
      File[] sourceFiles = directory.listFiles();
      if (sourceFiles != null) {
        for (File file : sourceFiles) {
          if (file.getName().endsWith("_.java")) {
            log("deleting existing annotations file: %s", file.getPath());
            file.delete();
          }
        }
      }
    }
  }
}
