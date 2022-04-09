package gov.cms.model.dsl.codegen.plugin.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.base.Strings;
import com.squareup.javapoet.ClassName;
import gov.cms.model.dsl.codegen.plugin.MojoUtil;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import org.apache.maven.plugin.MojoExecutionException;

public class ModelUtil {
  private static final ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());

  public static RootBean loadModelFromYamlFileOrDirectory(String mappingFile)
      throws IOException, MojoExecutionException {
    if (!isValidMappingSource(mappingFile)) {
      throw MojoUtil.createException("mappingFile not defined or does not exist");
    }

    return loadMappingsFromYamlFileOrDirectory(mappingFile);
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

  private static boolean isValidMappingSource(String filename) {
    if (Strings.isNullOrEmpty(filename)) {
      return false;
    }
    var file = new File(filename);
    return file.exists() && (file.isDirectory() || file.isFile());
  }

  private static RootBean loadMappingsFromYamlFileOrDirectory(String filename) throws IOException {
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
}
