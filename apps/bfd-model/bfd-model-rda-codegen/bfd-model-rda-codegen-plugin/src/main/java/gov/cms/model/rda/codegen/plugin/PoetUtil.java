package gov.cms.model.rda.codegen.plugin;

import com.squareup.javapoet.ClassName;

public class PoetUtil {
  public static ClassName toClassName(String fullClassName) {
    final int lastComponentDotIndex = fullClassName.lastIndexOf('.');
    if (lastComponentDotIndex <= 0) {
      throw new IllegalArgumentException("expected a full class name but there was no .");
    }
    return ClassName.get(
        fullClassName.substring(0, lastComponentDotIndex),
        fullClassName.substring(lastComponentDotIndex + 1));
  }

  private static String fieldToMethodName(String prefix, String fieldName) {
    return prefix + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
  }
}
