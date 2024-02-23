package gov.cms.model.dsl.codegen.plugin.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import org.yaml.snakeyaml.Yaml;

/** Utility methods for use by and with the various model classes. */
public class ModelUtil {
  /** Shared {@link ObjectMapper} instance used for mapping YAML to POJOs. */
  private static final ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());

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
   * @return consolidated {@link RootBean} containing all mappings
   * @throws IOException if any I/O errors prevent loading
   */
  public static RootBean loadModelFromYamlFileWithAliasesOrDirectory(String mappingPath)
      throws IOException {
    if (!isValidMappingSource(mappingPath)) {
      throw new IOException("mappingPath not defined or does not exist");
    }

    return loadMappingsFromYamlFileWithAliasesOrDirectory(mappingPath);
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
   * @return a {@link RootBean} containing the mappings loaded from path
   * @throws IOException if anything could not be loaded
   */
  private static RootBean loadMappingsFromYamlFileWithAliasesOrDirectory(String mappingPath)
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
        // Jackson yaml does not support anchors so use SnakeYaml to parse the YAML file
        // & then convert it to Jackson's format
        var yaml = new Yaml();
        InputStream mappingFileStream = new FileInputStream(mappingFile);
        Map<String, Object> yamlObj = yaml.load(mappingFileStream);
        Gson gson = new Gson();
        String json = gson.toJson(yamlObj);
        combinedRoot.addMappingsFrom(objectMapper.readValue(json, RootBean.class));
      }
    }
    return combinedRoot;
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
