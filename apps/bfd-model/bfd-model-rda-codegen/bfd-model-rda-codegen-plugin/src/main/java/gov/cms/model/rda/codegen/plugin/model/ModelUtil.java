package gov.cms.model.rda.codegen.plugin.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.base.Strings;
import com.squareup.javapoet.ClassName;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class ModelUtil {
  private static final ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());

  public static boolean isValidMappingSource(String filename) {
    if (Strings.isNullOrEmpty(filename)) {
      return false;
    }
    var file = new File(filename);
    return file.exists() && (file.isDirectory() || file.isFile());
  }

  public static RootBean loadMappingsFromYamlFile(String filename) throws IOException {
    var file = new File(filename);
    if (file.isFile()) {
      return objectMapper.readValue(file, RootBean.class);
    }
    if (!file.isDirectory()) {
      throw new IllegalArgumentException("expected a file or directory: " + filename);
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

  public static String packageName(String fullClassName) {
    return fullClassName.substring(0, fullClassName.lastIndexOf("."));
  }

  public static String className(String fullClassName) {
    return fullClassName.substring(1 + fullClassName.lastIndexOf("."));
  }

  public static ClassName classType(String fullClassName) {
    return ClassName.get(packageName(fullClassName), className(fullClassName));
  }
}
