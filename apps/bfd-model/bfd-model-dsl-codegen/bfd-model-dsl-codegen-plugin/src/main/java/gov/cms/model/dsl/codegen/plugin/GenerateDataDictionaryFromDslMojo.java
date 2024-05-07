package gov.cms.model.dsl.codegen.plugin;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import gov.cms.model.dsl.codegen.library.FhirElementDataTransformer;
import gov.cms.model.dsl.codegen.plugin.mappers.CsvToExcel;
import gov.cms.model.dsl.codegen.plugin.mappers.FhirElementToCsv;
import gov.cms.model.dsl.codegen.plugin.mappers.FhirElementToJson;
import gov.cms.model.dsl.codegen.plugin.model.DataDictionary;
import gov.cms.model.dsl.codegen.plugin.model.FhirElementBean;
import gov.cms.model.dsl.codegen.plugin.model.FhirMapping;
import gov.cms.model.dsl.codegen.plugin.model.ModelUtil;
import gov.cms.model.dsl.codegen.plugin.transformer.FhirElementTransformer;
import gov.cms.model.dsl.codegen.plugin.transformer.TransformerUtil;
import gov.cms.model.dsl.codegen.plugin.util.Version;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.lang.model.element.Modifier;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/** A Maven Mojo that generates a data dictionary and FHIR Resource transformer classes. */
@Mojo(name = "dataDictionary", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class GenerateDataDictionaryFromDslMojo extends AbstractMojo {

  /** ExplanationOfBenefit FHIR Resource. */
  private final String FHIR_RESOURCE_EOB = "ExplanationOfBenefit";

  /** Path to a single mapping file or a directory containing one or more mapping files. */
  @Parameter(property = "mappingPath")
  private String mappingPath;

  /** Path to a directory containing one or more data dictionary json files. */
  @Parameter(property = "dataDictionaryDataPath")
  private String dataDictionaryDataPath;

  /** Path to directory containing code book files. */
  @Parameter(property = "codeBookPath")
  private String codeBookPath;

  /** Project version. */
  @Parameter(property = "projectVersion")
  private String projectVersion;

  /** Path to directory to contain generated data dictionary. */
  @Parameter(property = "destinationDirectory")
  private String destinationDirectory;

  /** Name of package that will contain the FhirResourceTransformers class. */
  @Parameter(property = "fhirResourceTransformerPackage")
  private String fhirResourceTransformerPackage;

  /** V1 CSV template file path. */
  @Parameter(property = "v1TemplateFilePath")
  private String v1TemplateFilePath;

  /** V2 CSV template file path. */
  @Parameter(property = "v2TemplateFilePath")
  private String v2TemplateFilePath;

  /**
   * Instance of {@link MavenProject} used to call {@link
   * MavenProject#addCompileSourceRoot(String)}.
   */
  @Parameter(property = "project", readonly = true)
  private MavenProject project;

  /** Path to directory to contain generated FHIR Resource mappings. */
  @Parameter(
      property = "fhirResourceMappingDirectory",
      defaultValue = "${project.build.directory}/generated-resource/fhirResources")
  private String fhirResourceMappingDirectory;

  /** Path to directory to contain generated code for Fhir resource transformers. */
  @Parameter(
      property = "fhirResourceTransformersDirectory",
      defaultValue = "${project.build.directory}/generated-sources/fhirResourceTransformers")
  private String fhirResourceTransformersDirectory;

  /** Parameterless constructor used by Maven to instantiate the plugin. */
  public GenerateDataDictionaryFromDslMojo() {}

  /**
   * Executed by maven to execute the mojo. Reads all data dictionary json files and generates an
   * updated data dictionary and generates a FHIR resource transformer class for every collection of
   * FhirElementBeans corresponding to a mapping
   *
   * @throws MojoExecutionException if the process fails due to some error
   */
  public void execute() throws MojoExecutionException {
    final File fhirResourceMappingDir =
        MojoUtil.initializeOutputDirectory(fhirResourceMappingDirectory);
    var templateFileMap = Map.of(Version.V1, v1TemplateFilePath, Version.V2, v2TemplateFilePath);
    var xlsxFilename =
        String.format("%s/data-dictionary-%s.xlsx", destinationDirectory, projectVersion);

    // Open Excel output here since both API versions are written to the same workbook
    try (var xlsxOutputStream = new FileOutputStream(xlsxFilename);
        var workbook = new XSSFWorkbook()) {

      // Process each data dictionary resource directory in turn
      for (Version version : List.of(Version.V1, Version.V2)) {
        var resourceDirPath = String.format("%s/%s", dataDictionaryDataPath, version.name());
        var templatePath = templateFileMap.get(version);
        DataDictionary dataDictionary = DataDictionary.builder().version(version).build();
        dataDictionary =
            ModelUtil.createDataDictionary(
                mappingPath, fhirResourceMappingDir, resourceDirPath, codeBookPath, dataDictionary);

        // Adds FHIR Resource yaml files
        if (project != null) {
          project.addCompileSourceRoot(fhirResourceMappingDirectory);
        }

        // Generates Data Dictionary
        generateDataDictionaryArtifact(
            dataDictionary, destinationDirectory, projectVersion, templatePath, workbook, version);

        // Generates FHIR resource transformers
        generateFhirResourceTransformerClasses(
            dataDictionary.getExplanationOfBenefitFhirElements(), FHIR_RESOURCE_EOB, version);
      }

      // save Excel workbook file
      workbook.write(xlsxOutputStream);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Function to encapsulate the processing of a single resource directory.
   *
   * @param dataDictionary data dictionary containing mappings of fhir elements
   * @param destinationDirectory the output file path
   * @param projectVersion the project version
   * @param csvTemplatePath the path within the module resources to the CSV template file
   * @param workbook the Excel workbook (shared across data dictionary versions)
   * @param version the BFD API version, e.g. V1, V2
   */
  public static void generateDataDictionaryArtifact(
      DataDictionary dataDictionary,
      String destinationDirectory,
      String projectVersion,
      String csvTemplatePath,
      XSSFWorkbook workbook,
      Version version) {

    // name output files
    var basePath = getOutputFileBaseName(destinationDirectory, projectVersion, version);
    var jsonPath = basePath + ".json";
    var csvPath = basePath + ".csv";

    try {
      // create transformers/writers and stream
      var elementToJson = FhirElementToJson.createInstance(new FileWriter(jsonPath));
      var elementToCsv = FhirElementToCsv.createInstance(new FileWriter(csvPath), csvTemplatePath);
      var csvToExcel = CsvToExcel.createInstance(workbook, version);

      dataDictionary
          .getAllFhirElements()
          .sort(
              new Comparator<FhirElementBean>() {
                @Override
                public int compare(
                    final FhirElementBean fhirElement1, FhirElementBean fhirElement2) {
                  return Integer.compare(fhirElement1.getId(), fhirElement2.getId());
                }
              });
      dataDictionary.getAllFhirElements().stream()
          .map(elementToJson)
          .map(elementToCsv)
          .flatMap(Collection::stream)
          .forEach(csvToExcel);
      csvToExcel.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Forms the base file name for output files.
   *
   * @param destinationDirectory output file destination directory path
   * @param projectVersion the project version
   * @param version the BFD API version
   * @return a String representing the base output file name
   */
  private static String getOutputFileBaseName(
      String destinationDirectory, String projectVersion, Version version) {
    return String.format(
        "%s/%s-data-dictionary-%s", destinationDirectory, version.name(), projectVersion);
  }

  /** Data transfer object for creating FHIR resource transformer methods. */
  @Data
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  public static class FhirTransformationDto {
    /** Path to a single mapping file or a directory containing one or more mapping files. */
    String element;

    /** Path to a single mapping file or a directory containing one or more mapping files. */
    String javaFieldName;

    /** Path to a single mapping file or a directory containing one or more mapping files. */
    String ccwMapping;

    /** Path to a single mapping file or a directory containing one or more mapping files. */
    String fhirElementTransformer;

    /** Source Entity Class Name. */
    String sourceEntityClassName;
  }

  /**
   * Generates a FHIR Resource transformer class for every collection of FhirElementBeans
   * corresponding to a mapping.
   *
   * @param fhirElementBeans fhirElementBeans
   * @param fhirResource fhirResource
   * @param version version
   */
  private void generateFhirResourceTransformerClasses(
      List<FhirElementBean> fhirElementBeans, String fhirResource, Version version)
      throws MojoExecutionException {
    Map<String, List<FhirElementBean>> fhirElementsGroupedByClaimType =
        fhirElementBeans.stream()
            .map(FhirElementBean::getAppliesTo)
            .flatMap(Collection::stream)
            .distinct()
            .collect(
                Collectors.toMap(
                    Function.identity(),
                    claimType ->
                        fhirElementBeans.stream()
                            .filter(
                                fhirElementBean ->
                                    fhirElementBean.getAppliesTo().contains(claimType))
                            .collect(Collectors.toList())));

    // All FHIR elements for a DME EOB. Note: Hard coded DME below for POC!
    List<FhirElementBean> DMEEobFhirElementBeans = fhirElementsGroupedByClaimType.get("DME");

    try {
      final File outputDir = MojoUtil.initializeOutputDirectory(fhirResourceTransformersDirectory);
      TypeSpec fhirResourceTransformerEntity =
          createFhirResourceTransformerClassForMapping(
              DMEEobFhirElementBeans, "DME", fhirResource, version);

      JavaFile javaFile =
          JavaFile.builder(fhirResourceTransformerPackage, fhirResourceTransformerEntity).build();
      javaFile.writeTo(outputDir);
      project.addCompileSourceRoot(fhirResourceTransformersDirectory);
    } catch (IOException ex) {
      throw new MojoExecutionException("I/O error during code generation", ex);
    }
  }

  /**
   * Creates a {@link TypeSpec} defining a transformer class for the given FhirElementBeans {@link
   * FhirElementBean}.
   *
   * @param fhirElementBeans fhirElementBeans
   * @param mapping mapping
   * @param fhirResource fhirResource
   * @param version version
   * @return the {@link TypeSpec}
   */
  private TypeSpec createFhirResourceTransformerClassForMapping(
      List<FhirElementBean> fhirElementBeans, String mapping, String fhirResource, Version version)
      throws MojoExecutionException {
    List<FhirTransformationDto> fhirElementsDaos =
        fhirElementBeans.stream()
            .map(
                fhirElementBean -> {
                  List<FhirMapping> fhirMappings = fhirElementBean.getFhirMapping();
                  String javaFieldName = fhirElementBean.getBfdJavaFieldName();
                  List<String> ccwMapping = fhirElementBean.getCcwMapping();
                  FhirMapping fhirMapping = fhirMappings.getFirst();
                  String sourceEntityClassName =
                      fhirElementBean.getSourceEntityClassNames().getOrDefault(mapping, "");
                  String[] cleanedElementPath =
                      fhirMapping
                          .getElement()
                          .replaceAll("[N]", "")
                          .replaceAll("\\[]", "")
                          .split("\\.");
                  String firstChildElement = cleanedElementPath[0];
                  return FhirTransformationDto.builder()
                      .element(fhirMapping.getElement())
                      .javaFieldName(javaFieldName)
                      .ccwMapping(ccwMapping.getFirst())
                      .fhirElementTransformer(TransformerUtil.capitalize(firstChildElement))
                      .sourceEntityClassName(sourceEntityClassName)
                      .build();
                })
            .toList();
    TypeSpec.Builder classBuilder =
        TypeSpec.classBuilder(createTransformerSimpleName(mapping, fhirResource))
            .addModifiers(Modifier.PUBLIC);
    MethodSpec.Builder constructor = MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC);
    classBuilder.addMethod(constructor.build());

    // Note: Hard coded to only return fhir element dao where transformer is Identifier for POC only
    fhirElementsDaos =
        fhirElementsDaos.stream()
            .filter(
                fhirElementsDao -> fhirElementsDao.getFhirElementTransformer().equals("Identifier"))
            .collect(Collectors.toList());

    classBuilder.addMethod(
        createTransformMethodForMapping(fhirElementsDaos, fhirResource, version));
    return classBuilder.build();
  }

  /**
   * Creates a method {@link MethodSpec} for a method that transforms a message instance into an
   * entity instance for a given {@link FhirTransformationDto}. Only the properties of the message
   * are transformed by the generated method.
   *
   * @param fhirTransformationDtos fhirTransformationDtos
   * @param fhirResource fhirResource
   * @param version version
   * @return the {@link MethodSpec}
   */
  private MethodSpec createTransformMethodForMapping(
      List<FhirTransformationDto> fhirTransformationDtos, String fhirResource, Version version)
      throws MojoExecutionException {
    final String sourceEntityClassName =
        fhirTransformationDtos.getFirst().getSourceEntityClassName();
    final TypeName sourceEntityClassType =
        ModelUtil.classType(
            sourceEntityClassName); // TODO: get sourceEntityClassName from fhir resource yaml
    // instead
    final TypeName fhirResourceType =
        ModelUtil.classType(ModelUtil.constructFhirResourceClassType(version, fhirResource));
    // Note: hard coded DME for now, otherwise would be mapping like SNF, PDE, ...
    final MethodSpec.Builder builder =
        MethodSpec.methodBuilder(createTransformMethodNameForMapping("DME", fhirResource))
            .returns(fhirResourceType)
            .addModifiers(Modifier.PUBLIC)
            .addParameter(sourceEntityClassType, FhirElementTransformer.SOURCE_VAR)
            .addParameter(FhirElementDataTransformer.class, FhirElementTransformer.TRANSFORMER_VAR)
            .addStatement(
                "final $T $L = new $T()",
                fhirResourceType,
                FhirElementTransformer.DEST_VAR,
                fhirResourceType);

    for (FhirTransformationDto fhirTransformationDto : fhirTransformationDtos) {
      final CodeBlock transformationCode =
          TransformerUtil.selectFhirElementTransformer(fhirTransformationDto)
              .map(
                  generator ->
                      generator.generateCodeBlock(
                          fhirTransformationDto.getJavaFieldName(),
                          fhirTransformationDto.getCcwMapping()))
              .orElseThrow(
                  () ->
                      MojoUtil.createException(
                          "No known transformation found for: mapping=%s from=%s to=%s",
                          fhirTransformationDto.getFhirElementTransformer(), "DME", fhirResource));
      builder.addCode(transformationCode);
    }
    builder.addStatement("return $L", FhirElementTransformer.DEST_VAR);
    return builder.build();
  }

  /**
   * Creates a unique method name for the generated method that transforms single objects of a type.
   *
   * @param fhirResource fhirResource
   * @param mapping for the object being transformed
   * @return unique method name
   */
  private String createTransformMethodNameForMapping(String mapping, String fhirResource) {
    return "transformFhirElementsTo" + mapping + fhirResource;
  }

  /**
   * Creates a unique class name for the generated class that transforms fhir elements into a fhir
   * resource.
   *
   * @param mapping for the object being transformed
   * @param fhirResource fhirResource
   * @return unique class name
   */
  private String createTransformerSimpleName(String mapping, String fhirResource) {
    return mapping + fhirResource + "Transformer";
  }
}
