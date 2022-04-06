package gov.cms.model.rda.codegen.plugin;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.lang.model.element.Modifier;

public class PoetUtil {
  public static final ClassName OptionalClassName = ClassName.get(Optional.class);
  private static final ClassName StringClassName = ClassName.get(String.class);
  private static final ClassName LongClassName = ClassName.get(Long.class);

  public static ClassName toClassName(String fullClassName) {
    final int lastComponentDotIndex = fullClassName.lastIndexOf('.');
    if (lastComponentDotIndex <= 0) {
      throw new IllegalArgumentException("expected a full class name but there was no .");
    }
    return ClassName.get(
        fullClassName.substring(0, lastComponentDotIndex),
        fullClassName.substring(lastComponentDotIndex + 1));
  }

  public static MethodSpec createStandardSetter(
      String propertyName, TypeName fieldType, TypeName accessorType) {
    if (fieldType.equals(accessorType)) {
      return MethodSpec.methodBuilder(fieldToMethodName("set", propertyName))
          .addModifiers(Modifier.PUBLIC)
          .addParameter(fieldType, propertyName)
          .addStatement("this.$N = $N", propertyName, propertyName)
          .build();
    } else if (fieldType.equals(StringClassName) && accessorType.equals(TypeName.LONG)) {
      return MethodSpec.methodBuilder(fieldToMethodName("set", propertyName))
          .addModifiers(Modifier.PUBLIC)
          .addParameter(accessorType, propertyName)
          .addStatement("this.$N = $T.valueOf($N)", propertyName, fieldType, propertyName)
          .build();
    } else {
      throw createInvalidTypeCombinationException(propertyName, fieldType, accessorType);
    }
  }

  public static MethodSpec createStandardGetter(
      String propertyName, TypeName fieldType, TypeName accessorType) {
    if (fieldType.equals(accessorType)) {
      return MethodSpec.methodBuilder(fieldToMethodName("get", propertyName))
          .addModifiers(Modifier.PUBLIC)
          .returns(fieldType)
          .addStatement("return $N", propertyName)
          .build();
    } else if (fieldType.equals(StringClassName) && accessorType.equals(TypeName.LONG)) {
      return MethodSpec.methodBuilder(fieldToMethodName("get", propertyName))
          .addModifiers(Modifier.PUBLIC)
          .returns(accessorType)
          .addStatement("return $T.parseLong($N)", LongClassName, propertyName)
          .build();
    } else {
      throw createInvalidTypeCombinationException(propertyName, fieldType, accessorType);
    }
  }

  public static MethodSpec createOptionalSetter(
      String propertyName, TypeName fieldType, TypeName accessorType) {
    if (fieldType.equals(accessorType)) {
      return MethodSpec.methodBuilder(fieldToMethodName("set", propertyName))
          .addModifiers(Modifier.PUBLIC)
          .addParameter(ParameterizedTypeName.get(OptionalClassName, fieldType), propertyName)
          .addStatement("this.$N = $N.orElse(null)", propertyName, propertyName)
          .build();
    } else if (fieldType.equals(StringClassName) && accessorType.equals(TypeName.LONG)) {
      return MethodSpec.methodBuilder(fieldToMethodName("set", propertyName))
          .addModifiers(Modifier.PUBLIC)
          .addParameter(ParameterizedTypeName.get(OptionalClassName, LongClassName), propertyName)
          .addStatement(
              "this.$N = $N.map($T::valueOf).orElse(null)",
              propertyName,
              propertyName,
              String.class)
          .build();
    } else {
      throw createInvalidTypeCombinationException(propertyName, fieldType, accessorType);
    }
  }

  public static MethodSpec createOptionalGetter(
      String propertyName, TypeName fieldType, TypeName accessorType) {
    if (fieldType.equals(accessorType)) {
      return MethodSpec.methodBuilder(fieldToMethodName("get", propertyName))
          .addModifiers(Modifier.PUBLIC)
          .returns(ParameterizedTypeName.get(OptionalClassName, fieldType))
          .addStatement("return $T.ofNullable($N)", Optional.class, propertyName)
          .build();
    } else if (fieldType.equals(StringClassName) && accessorType.equals(TypeName.LONG)) {
      return MethodSpec.methodBuilder(fieldToMethodName("get", propertyName))
          .addModifiers(Modifier.PUBLIC)
          .returns(ParameterizedTypeName.get(OptionalClassName, LongClassName))
          .addStatement(
              "return $T.ofNullable($N).map($T::parseLong)",
              Optional.class,
              propertyName,
              Long.class)
          .build();
    } else {
      throw createInvalidTypeCombinationException(propertyName, fieldType, accessorType);
    }
  }

  public static String fieldToMethodName(String prefix, String fieldName) {
    return prefix + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
  }

  @Nonnull
  private static IllegalArgumentException createInvalidTypeCombinationException(
      String propertyName, TypeName fieldType, TypeName accessorType) {
    return new IllegalArgumentException(
        String.format(
            "unsupported combination of java types for property: fieldType=%s accessorType=%s property=%s",
            fieldType, accessorType, propertyName));
  }
}
