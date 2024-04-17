package gov.cms.model.dsl.codegen.plugin.model;

import ca.uhn.fhir.model.api.annotation.Child;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.base.Strings;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import lombok.Builder;
import lombok.Data;
import org.hl7.fhir.r4.model.Coverage;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.Patient;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

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

  /*public static RootBean loadModelFromYamlFileAndJsonFiles(
      String mappingPath, String fhirMappingPath) throws IOException {
    if (!isValidMappingSource(mappingPath)) {
      throw new IOException("mappingPath not defined or does not exist");
    }

    return loadMappingsFromYamlFileAndJsonFiles(mappingPath, fhirMappingPath);
  }*/

  /**
   * Load all mappings from the provided file or directory. If a directory is provided all YAML
   * files in that directory will be loaded.
   *
   * @param mappingPath path to a file or directory containing mappings
   * @param fhirEntitiesDirectory stuff
   * @param fhirMappingPath stuff
   * @param codeBookPath stuff
   * @return consolidated {@link RootBean} containing all mappings
   * @throws IOException if any I/O errors prevent loading
   */
  public static RootBean appendFhirElementsToYamlFiles(
      String mappingPath, File fhirEntitiesDirectory, String fhirMappingPath, String codeBookPath)
      throws IOException {
    if (!isValidMappingSource(mappingPath)) {
      throw new IOException("mappingPath not defined or does not exist");
    }

    return loadFhirElementsFromJsonFiles(
        mappingPath, fhirEntitiesDirectory, fhirMappingPath, codeBookPath);
  }

  /**
   * Extracts descriptions of each variable from the xml files parsed from the code books.
   *
   * @param codeBookPath path of xml files parsed from the code books
   * @return map that maps variable IDs to descriptions
   */
  public static Map<String, String> getDescriptionsFromCodeBook(String codeBookPath) {
    Map<String, String> variableIdToDescription = new HashMap<>();
    File file = new File(codeBookPath);
    var codeBookFiles = file.listFiles(f -> f.getName().endsWith(".xml"));
    if (codeBookFiles != null) {
      for (File codeBookFile : codeBookFiles) {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = null;
        try {
          db = dbf.newDocumentBuilder();
          Document document = db.parse(codeBookFile);
          document.getDocumentElement().normalize();

          NodeList variables = document.getElementsByTagName("variable");
          for (int variableIndex = 0; variableIndex < variables.getLength(); variableIndex++) {
            Node variable = variables.item(variableIndex);
            if (variable.getNodeType() == Node.ELEMENT_NODE) {
              NodeList childNodes = variable.getChildNodes();
              for (int index = 0; index < childNodes.getLength(); index++) {
                Node childNode = childNodes.item(index);
                if (childNode.getNodeName().equals("description")) {
                  String description = childNode.getTextContent().replaceAll("\\s+", " ");
                  Element element = (Element) variable;
                  String variableId = element.getAttribute("id");
                  variableIdToDescription.put(variableId, description);
                }
              }
            }
          }
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
    }
    return variableIdToDescription;
  }

  /**
   * this.
   *
   * @param fhirElementBean this
   * @return FhirElementBean
   */
  public static FhirElementBean buildFhirPath(FhirElementBean fhirElementBean) {
    Map<String, Class<?>> fhirClassesMap =
        new HashMap<>() {
          {
            put("Coverage", Coverage.class);
            put("ExplanationOfBenefit", ExplanationOfBenefit.class);
            put("Patient", Patient.class);
          }
        };

    List<FhirMapping> fhirMappings = fhirElementBean.getFhirMapping();
    for (FhirMapping fhirMapping : fhirMappings) {
      String resource = fhirMapping.getResource();
      if (!resource.isEmpty()) {
        Set<String> fhirElements = findFields(fhirClassesMap.get(resource), Child.class);
        String currentElement = fhirMapping.getElement();
        if (!currentElement.isEmpty()) {
          String[] splitCurrentElement = currentElement.split("\\.");

          if (fhirElements.contains(splitCurrentElement[0])) {
            // fhir path is the expression under search parameters in specification
            fhirMapping.setFhirPath(currentElement);
          } else {
            // element is an extension
            System.out.println("element is an extension");
          }
        }
      }
    }
    fhirElementBean.setFhirMapping(fhirMappings);
    return fhirElementBean;
  }

  /**
   * this.
   *
   * @param classs this
   * @param ann this
   * @return elements of a resource
   */
  public static Set<String> findFields(Class<?> classs, Class<? extends Annotation> ann) {
    Set<String> set = new HashSet<>();
    Class<?> c = classs;
    while (c != null) {
      for (Field field : c.getDeclaredFields()) {
        if (field.isAnnotationPresent(ann)) {
          set.add(field.getName());
        }
      }
      c = c.getSuperclass();
    }
    return set;
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

  /*private static RootBean loadMappingsFromYamlFileAndJsonFiles(
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
        rootBean.getMappings().stream()
            .forEach(
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
                });

        combinedRoot.addMappingsFrom(rootBean);
      }
    }
    return combinedRoot;
  }*/

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
   * @param ccwMappingToDescriptionMap stuff
   * @return stuff
   */
  private static FhirElementBean updateFhirElements(
      FhirElementBean fhirElementBean,
      MappingBean mappingBean,
      Optional<ColumnBean> columnBean,
      Map<String, String> ccwMappingToDescriptionMap) {
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
        if (ccwMappingToDescriptionMap.containsKey(ccwMappings.getFirst())) {
          fhirElementBean.setDescription(ccwMappingToDescriptionMap.get(ccwMappings.getFirst()));
        }
      }

      return buildFhirPath(fhirElementBean);
    } else {
      System.out.println(
          "Couldn't find column based off bfdColumnName. Defaults to using values in json");
    }
    return fhirElementBean;
  }

  /**
   * Load mappings from the given path.
   *
   * @param mappingPath path to a file or directory
   * @param fhirEntitiesDirectory stuff
   * @param fhirMappingPath stuff
   * @param codeBookPath stuff
   * @return a {@link RootBean} containing the mappings loaded from path
   * @throws IOException if anything could not be loaded
   */
  private static RootBean loadFhirElementsFromJsonFiles(
      String mappingPath, File fhirEntitiesDirectory, String fhirMappingPath, String codeBookPath)
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
      List<FhirElementBean> allFhirElements = convertJsonFilesToFhirElementBeans(fhirMappingPath);
      Map<String, String> ccwMappingToDescriptionMap = getDescriptionsFromCodeBook(codeBookPath);

      Map<String, List<FhirElementBean>> fhirResources =
          new HashMap<>() {
            {
              put("", new ArrayList<>());
              put("Coverage", new ArrayList<>());
              put("ExplanationOfBenefit", new ArrayList<>());
              put("Patient", new ArrayList<>());
            }
          };

      for (File mappingFile : mappingFiles) {
        // collect corresponding fhir elements for mapping
        RootBean rootBean = objectMapper.readValue(mappingFile, RootBean.class);
        rootBean.getMappings().stream()
            .forEach(
                mappingBean -> {
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
                                fhirElementBean.setMappingId(mappingBean.getId());
                                fhirElementBean.setBfdTableType(tableName);
                                updateFhirElements(
                                    fhirElementBean,
                                    mappingBean,
                                    columnFound,
                                    ccwMappingToDescriptionMap);
                                return fhirElementBean;
                              })
                          .collect(Collectors.toList());

                  if (!fhirElements.isEmpty()) {
                    mappingBean.setR4FhirElements(fhirElements);
                    // fhirElements group by fhir resource
                    aggregateFhirResourceGroups(fhirElements, fhirResources);
                  }
                });
        combinedRoot.addMappingsFrom(rootBean);
      }

      // create yaml files
      for (Map.Entry<String, List<FhirElementBean>> entry : fhirResources.entrySet()) {
        FhirResourceMapping fhirResourceMapping =
            FhirResourceMapping.builder().fhirVersion("R4").fhirElements(entry.getValue()).build();
        objectMapper.writeValue(
            new File(fhirEntitiesDirectory + "/" + entry.getKey() + ".yaml"), fhirResourceMapping);
      }
    }
    return combinedRoot;
  }

  /** Utility methods for use by and with the various model classes. */
  @Builder
  @Data
  public static class FhirResourceMapping {
    /** FHIR Version. */
    private String fhirVersion;

    /** List of FHIR elements. */
    private List<FhirElementBean> fhirElements;
  }

  /**
   * Compute the appropriate {@link TypeName} to use for the given {@code javaType}.
   *
   * @param fhirElements hi
   * @param fhirResources hi
   */
  private static void aggregateFhirResourceGroups(
      List<FhirElementBean> fhirElements, Map<String, List<FhirElementBean>> fhirResources) {
    Map<String, List<FhirElementBean>> fhirResourceGroups =
        fhirElements.stream()
            .collect(
                Collectors.groupingBy(
                    element -> {
                      List<FhirMapping> fhirMappings = element.getFhirMapping();
                      if (!fhirMappings.isEmpty()) {
                        return element.getFhirMapping().getFirst().getResource();
                      }
                      return ""; // todo: investigate case where R4 resource not specified
                    }));

    for (Map.Entry<String, List<FhirElementBean>> entry : fhirResourceGroups.entrySet()) {
      List<FhirElementBean> currentFhirResources = fhirResources.get(entry.getKey());
      if (currentFhirResources != null) {
        currentFhirResources.addAll(entry.getValue());
        fhirResources.put(entry.getKey(), currentFhirResources);
      }
    }
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
