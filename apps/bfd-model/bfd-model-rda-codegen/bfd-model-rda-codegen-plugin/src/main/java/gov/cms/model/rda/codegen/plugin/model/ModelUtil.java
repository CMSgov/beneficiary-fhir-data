package gov.cms.model.rda.codegen.plugin.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.squareup.javapoet.ClassName;
import java.io.File;
import java.io.IOException;

public class ModelUtil {
  private static final ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());

  public static RootBean loadMappingsFromYamlFile(String filename) throws IOException {
    return objectMapper.readValue(new File(filename), RootBean.class);
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
