package gov.cms.model.dsl.codegen.plugin.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.base.Strings;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/** Utility methods for use by and with the various model classes. */
public class ModelUtil {
  /** Shared {@link ObjectMapper} instance used for mapping YAML to POJOs. */
  private static final ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());

  /**
   * Map mapping id and table type to categories in appliesTo collection in FhirElementBean.
   *
   * @param mappingId s
   * @param fhirElementBean s
   * @param tableType stuff
   * @return boolean stuff
   */
  private static boolean map(String mappingId, FhirElementBean fhirElementBean, String tableType) {
    List<String> appliesTo = fhirElementBean.getAppliesTo();
    if ((mappingId.equals("InpatientClaim") && tableType.equals("CLAIM"))
        || (mappingId.equals("InpatientClaimLine") && tableType.equals("CLAIM LINE"))) {
      return appliesTo.contains("Inpatient");
    } else if ((mappingId.equals("CarrierClaim")
            && (tableType.equals("CLAIM") || tableType.equals("")))
        || (mappingId.equals("CarrierClaimLine") && tableType.equals("CLAIM LINE"))) {
      return appliesTo.contains("Carrier");
    } else if ((mappingId.equals("HHAClaim") && tableType.equals("CLAIM"))
        || (mappingId.equals("HHAClaimLine") && tableType.equals("CLAIM LINE"))) {
      return appliesTo.contains("HHA");
    } else if ((mappingId.equals("DMEClaim") && tableType.equals("CLAIM"))
        || (mappingId.equals("DMEClaimLine") && tableType.equals("CLAIM LINE"))) {
      return appliesTo.contains("DME");
    } else if ((mappingId.equals("OutpatientClaim") && tableType.equals("CLAIM"))
        || (mappingId.equals("OutpatientClaimLine") && tableType.equals("CLAIM LINE"))) {
      return appliesTo.contains("Outpatient");
    } else if ((mappingId.equals("HospiceClaim") && tableType.equals("CLAIM"))
        || (mappingId.equals("HospiceClaimLine") && tableType.equals("CLAIM LINE"))) {
      return appliesTo.contains("Hospice");
    } else if ((mappingId.equals("SNFClaim") && tableType.equals("CLAIM"))
        || (mappingId.equals("SNFClaimLine") && tableType.equals("CLAIM LINE"))) {
      return appliesTo.contains("SNF");
    } else if (mappingId.equals("PartDEvent")) {
      return appliesTo.contains("PDE");
    } else if (mappingId.equals("Beneficiary") && tableType.equals("BENEFICIARIES")) {
      return appliesTo.contains("")
          || appliesTo.contains("Part-A")
          || appliesTo.contains("Part-B")
          || appliesTo.contains("Part-C")
          || appliesTo.contains("Part-D");
    } else if (mappingId.equals("BeneficiaryMonthly") && tableType.equals("BENEFICIARYMONTHLY")) {
      return appliesTo.contains("Part-D");
    } else if (mappingId.equals("BeneficiaryHistory")
        && appliesTo.contains("")
        && (tableType.equals("BENEFICIARIESHISTORY")
            || tableType.equals("MEDICAREBENEFICIARYIDHISTORY"))) {
      return true;
    } /*else if (appliesTo.contains("")
          && tableType.equals("BENEFICIARIES;MEDICAREBENEFICIARYIDHISTORY")) {
        return true;
      } else if ((appliesTo.contains("") || appliesTo.contains("Part-A")) && tableType.isEmpty()) {
        // patient resource profile, profile, Contract Identifier, Contract Reference
        return true;
      } else if (fhirElementBean.getName().contains("Coverage") && tableType.isEmpty()) {
        // Coverage Class - Group, Coverage Class - Plan, Coverage Grouping - Sub Group, Coverage
        // Grouping - Sub Plan
        // Coverage Payer, Coverage Resource Profile, CoverageType
        return true;
      }*/
    // TODO: ask about how to handle these cases ^

    return false;
  }

  /** Prevent instantiating instances. */
  private ModelUtil() {}

  /**
   * Load all mappings from the provided file or directory. If a directory is provided all YAML
   * files in that directory will be loaded.
   *
   * @param mappingPath path to a file or directory containing mappings
   * @return consolidated {@link RootBean} containing all mappings
   * @throws IOException if any I/O errors prevent loading
   */
  public static RootBean loadModelFromYamlFileOrDirectory(String mappingPath) throws IOException {
    if (!isValidMappingSource(mappingPath)) {
      throw new IOException("mappingPath not defined or does not exist");
    }

    return loadMappingsFromYamlFileOrDirectory(mappingPath);
  }

  /**
   * Load all mappings from the provided file or directory. If a directory is provided all YAML
   * files in that directory will be loaded.
   *
   * @param mappingPath path to a file or directory containing mappings
   * @param fhirMappingPath stuff
   * @return consolidated {@link RootBean} containing all mappings
   * @throws IOException if any I/O errors prevent loading
   */
  public static RootBean loadModelFromYamlFileAndJsonFiles(
      String mappingPath, String fhirMappingPath) throws IOException {
    if (!isValidMappingSource(mappingPath)) {
      throw new IOException("mappingPath not defined or does not exist");
    }

    return loadMappingsFromYamlFileAndJsonFiles(mappingPath, fhirMappingPath);
  }

  /**
   * Extract the java package name for the transformer.
   *
   * @param fullClassName full class name including package
   * @return the java package name for the transformer.
   */
  public static String packageName(String fullClassName) {
    return fullClassName.substring(0, fullClassName.lastIndexOf("."));
  }

  /**
   * Extract just the java class name for the entity.
   *
   * @param fullClassName full class name including package
   * @return the java class name for the entity.
   */
  public static String className(String fullClassName) {
    return fullClassName.substring(1 + fullClassName.lastIndexOf("."));
  }

  /**
   * Create a {@link ClassName} from the given full class name.
   *
   * @param fullClassName full class name including package
   * @return a {@link ClassName} for the specified class
   */
  public static ClassName classType(String fullClassName) {
    return ClassName.get(packageName(fullClassName), className(fullClassName));
  }

  /**
   * Determines if the given file/directory name can be used to load one or more mappings.
   *
   * @param path path to a file or directory
   * @return true if the path references an existing file or directory
   */
  private static boolean isValidMappingSource(String path) {
    if (Strings.isNullOrEmpty(path)) {
      return false;
    }
    var file = new File(path);
    return file.exists() && (file.isDirectory() || file.isFile());
  }

  /**
   * Determine if the given class name is a valid full name including package as well as class.
   *
   * @param fullClassName class name to test
   * @return true if the class name is a valid full name including package as well as class
   */
  public static boolean isValidFullClassName(String fullClassName) {
    return fullClassName != null && fullClassName.indexOf('.') > 0;
  }

  /**
   * Load mappings from the given path.
   *
   * @param mappingPath path to a file or directory
   * @return a {@link RootBean} containing the mappings loaded from path
   * @throws IOException if anything could not be loaded
   */
  private static RootBean loadMappingsFromYamlFileOrDirectory(String mappingPath)
      throws IOException {
    final var file = new File(mappingPath);
    final var fileAttributes = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
    if (fileAttributes.isRegularFile()) {
      return objectMapper.readValue(file, RootBean.class);
    }
    if (!fileAttributes.isDirectory()) {
      throw new IOException("expected a file or directory: " + mappingPath);
    }

    var combinedRoot = new RootBean(new ArrayList<>());
    var mappingFiles = file.listFiles(f -> f.getName().endsWith(".yaml"));
    if (mappingFiles != null) {
      for (File mappingFile : mappingFiles) {
        combinedRoot.addMappingsFrom(objectMapper.readValue(mappingFile, RootBean.class));
      }
    }
    return combinedRoot;
  }

  /**
   * Load mappings from the given path.
   *
   * @param mappingPath path to a file or directory
   * @param fhirMappingPath stuff
   * @return a {@link RootBean} containing the mappings loaded from path
   * @throws IOException if anything could not be loaded
   */
  private static RootBean loadMappingsFromYamlFileAndJsonFiles(
      String mappingPath, String fhirMappingPath) throws IOException {
    final var file = new File(mappingPath);
    final var fileAttributes = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
    if (fileAttributes.isRegularFile()) {
      return objectMapper.readValue(file, RootBean.class);
    }
    if (!fileAttributes.isDirectory()) {
      throw new IOException("expected a file or directory: " + mappingPath);
    }

    var combinedRoot = new RootBean(new ArrayList<>());
    var mappingFiles = file.listFiles(f -> f.getName().endsWith(".yaml"));
    if (mappingFiles != null) {
      List<FhirElementBean> allFhirElements = convertJsonFilesToFhirElementBeans(fhirMappingPath);
      for (File mappingFile : mappingFiles) {
        RootBean rootBean = objectMapper.readValue(mappingFile, RootBean.class);
        List<MappingBean> mappingBeans =
            rootBean.getMappings().stream()
                .map(
                    mappingBean -> {
                      // collect corresponding fhir elements for mapping
                      List<FhirElementBean> fhirElements =
                          allFhirElements.stream()
                              .filter(
                                  fhirElementBean ->
                                      map(
                                          mappingBean.getId(),
                                          fhirElementBean,
                                          fhirElementBean.getBfdTableType()))
                              .map(
                                  fhirElementBean -> {
                                    // identify column that contains data corresponding to json file
                                    TableBean tableBean = mappingBean.getTable();
                                    String tableName = tableBean.getName();
                                    Optional<ColumnBean> columnFound =
                                        findColumn(fhirElementBean, tableBean);

                                    // Update Fhir Element fields
                                    fhirElementBean.setBfdTableType(tableName);
                                    updateFhirElements(fhirElementBean, mappingBean, columnFound);
                                    return fhirElementBean;
                                  })
                              .collect(Collectors.toList());

                      mappingBean.setR4FhirElements(fhirElements);
                      return mappingBean;
                    })
                .collect(Collectors.toList());
        // rootBean.setMappings(mappingBeans);

        combinedRoot.addMappingsFrom(rootBean);
        // combinedRoot.addMappings(mappingBeans);
      }
    }
    return combinedRoot;
  }

  /**
   * hi.
   *
   * @param fhirMappingPath stuff
   * @return stuff
   */
  private static List<FhirElementBean> convertJsonFilesToFhirElementBeans(String fhirMappingPath)
      throws IOException {
    final var file = new File(fhirMappingPath);
    var fhirMappingFiles = file.listFiles(f -> f.getName().endsWith(".json"));
    List<FhirElementBean> fhirElementsToAdd = new ArrayList<>();
    for (var fhirMappingFile : fhirMappingFiles) {
      FhirElementBean fhirElementBean =
          objectMapper.readValue(fhirMappingFile, FhirElementBean.class);
      fhirElementsToAdd.add(fhirElementBean);
    }
    return fhirElementsToAdd;
  }

  /**
   * hi.
   *
   * @param fhirElementBean stuff
   * @param tableBean stuff
   * @return stuff
   */
  private static Optional<ColumnBean> findColumn(
      FhirElementBean fhirElementBean, TableBean tableBean) {
    return tableBean.getColumns().stream()
        .filter(
            column -> {
              String columnName = column.getColumnName().toLowerCase();
              String name = column.getName().toLowerCase();
              return columnName.equals(fhirElementBean.getBfdColumnName().toLowerCase())
                  || name.equals(fhirElementBean.getBfdColumnName())
                  || name.equals(fhirElementBean.getBfdJavaFieldName().toLowerCase());
            })
        .findFirst();
  }

  /**
   * hi.
   *
   * @param fhirElementBean stuff
   * @param mappingBean stuff
   * @param columnBean stuff
   * @return stuff
   */
  private static FhirElementBean updateFhirElements(
      FhirElementBean fhirElementBean, MappingBean mappingBean, Optional<ColumnBean> columnBean) {
    if (columnBean.isPresent()) {
      ColumnBean column = columnBean.get();
      fhirElementBean.setBfdColumnName(column.getColumnName());
      fhirElementBean.setBfdDbType(column.getSqlType());
      fhirElementBean.setBfdJavaFieldName(column.getName());

      List<TransformationBean> transformationsFound =
          mappingBean.getTransformations().stream()
              .filter(transformationBean -> transformationBean.getTo().equals(column.getName()))
              .toList();
      if (!transformationsFound.isEmpty()) {
        List<String> ccwMappings = new ArrayList<>();
        transformationsFound.forEach(
            transformationBean -> ccwMappings.add(transformationBean.getFrom()));
        fhirElementBean.setCcwMapping(ccwMappings);
      }
    } else {
      System.out.println(
          "Couldn't find column based off bfdColumnName. Defaults to using values in json");
    }
    return fhirElementBean;
  }

  /**
   * Compute the appropriate {@link TypeName} to use for the given {@code javaType}.
   *
   * @param javaType either {@link ColumnBean#javaType} or {@link ColumnBean#javaAccessorType}
   * @return an {@link Optional} containing an appropriate {@link TypeName} or empty if no mapping
   *     could be found
   */
  public static Optional<TypeName> mapJavaTypeToTypeName(String javaType) {
    switch (javaType) {
      case "char":
        return Optional.of(TypeName.CHAR);
      case "Character":
        return Optional.of(ClassName.get(Character.class));
      case "int":
        return Optional.of(TypeName.INT);
      case "Integer":
        return Optional.of(ClassName.get(Integer.class));
      case "short":
        return Optional.of(TypeName.SHORT);
      case "Short":
        return Optional.of(ClassName.get(Short.class));
      case "long":
        return Optional.of(TypeName.LONG);
      case "Long":
        return Optional.of(ClassName.get(Long.class));
      case "String":
        return Optional.of(ClassName.get(String.class));
      default:
        try {
          return Optional.of(ClassName.get(Class.forName(javaType)));
        } catch (ClassNotFoundException ex) {
          // just report that no valid mapping was found
          return Optional.empty();
        }
    }
  }

  /**
   * Determines an appropriate java type to use for the given sql type name.
   *
   * @param sqlType SQL type name to map
   * @return an {@link Optional} containing an appropriate {@link TypeName} or empty if no mapping
   *     could be found
   */
  public static Optional<TypeName> mapSqlTypeToTypeName(String sqlType) {
    if (sqlType.contains("char")) {
      return Optional.of(ClassName.get(String.class));
    }
    if (sqlType.contains("smallint")) {
      return Optional.of(ClassName.get(Short.class));
    }
    if (sqlType.equals("bigint")) {
      return Optional.of(ClassName.get(Long.class));
    }
    if (sqlType.equals("int") || sqlType.equals("integer")) {
      return Optional.of(ClassName.get(Integer.class));
    }
    if (sqlType.contains("decimal") || sqlType.contains("numeric")) {
      return Optional.of(ClassName.get(BigDecimal.class));
    }
    if (sqlType.contains("date")) {
      return Optional.of(ClassName.get(LocalDate.class));
    }
    if (sqlType.contains("timestamp")) {
      return Optional.of(ClassName.get(Instant.class));
    }
    return Optional.empty();
  }
}
