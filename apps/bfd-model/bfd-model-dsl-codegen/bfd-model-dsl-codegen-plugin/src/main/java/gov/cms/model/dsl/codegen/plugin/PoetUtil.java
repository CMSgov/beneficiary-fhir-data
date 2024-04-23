package gov.cms.model.dsl.codegen.plugin;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import jakarta.annotation.Nonnull;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import javax.lang.model.element.Modifier;

/** Utility methods for working with the java poet library. */
public class PoetUtil {
  /**
   * Shortcut for an instance of {@link ClassName} for {@link Optional} since the library doesn't
   * provide one.
   */
  public static final ClassName OptionalClassName = ClassName.get(Optional.class);

  /**
   * Shortcut for an instance of {@link ClassName} for {@link String} since the library doesn't
   * provide one.
   */
  public static final ClassName StringClassName = ClassName.get(String.class);

  /**
   * Shortcut for an instance of {@link ClassName} for {@link Long} since the library doesn't
   * provide one.
   */
  public static final ClassName LongClassName = ClassName.get(Long.class);

  /**
   * Shortcut for an instance of {@link ClassName} for {@link java.util.Map} since the library
   * doesn't provide one.
   */
  public static final ClassName MapClassName = ClassName.get("java.util", "Map");

  /** Prevent instantiation of utility class. */
  private PoetUtil() {}

  /**
   * Generates a setter method for a given property name, field type, and accessor type. The latter
   * is the type passed into the setter. This is usually the same as the field type but in some
   * cases we support a {@link String} field in the database entity being treated as a {@link Long}
   * in the setter and getter methods.
   *
   * @param propertyName the java property name
   * @param fieldType the type of the property in the class
   * @param accessorType the type of the argument to the setter
   * @return a {@link MethodSpec} that generates the setter method
   */
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

  /**
   * Generates a getter method for a given property name, field type, and accessor type. The latter
   * is the type returned by the getter. This is usually the same as the field type but in some
   * cases we support a {@link String} field in the database entity being treated as a {@link Long}
   * in the setter and getter methods.
   *
   * @param propertyName the java property name
   * @param fieldType the type of the property in the class
   * @param accessorType the type of the getter return value
   * @return a {@link MethodSpec} that generates the getter method
   */
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

  /**
   * Generates a setter method for a given property name, field type, and accessor type. The latter
   * is the type passed into the setter. This is usually the same as the field type but in some
   * cases we support a {@link String} field in the database entity being treated as a {@link Long}
   * in the setter and getter methods.
   *
   * <p>The argument to the setter is actually an {@link Optional} containing the value. The actual
   * field type is just a raw value and will be mapped to null if the {@link Optional} is empty.
   *
   * @param propertyName the java property name
   * @param fieldType the type of the property in the class
   * @param accessorType the type of the argument to the setter
   * @return a {@link MethodSpec} that generates the setter method
   */
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

  /**
   * Generates a getter method for a given property name, field type, and accessor type. The latter
   * is the type returned by the getter. This is usually the same as the field type but in some
   * cases we support a {@link String} field in the database entity being treated as a {@link Long}
   * in the setter and getter methods.
   *
   * <p>The value returned by the getter is actually an {@link Optional} containing the value. The
   * actual field type is just a raw value and will be returned as an empty {@link Optional} if it
   * is null.
   *
   * @param propertyName the java property name
   * @param fieldType the type of the property in the class
   * @param accessorType the type of the getter return value
   * @return a {@link MethodSpec} that generates the getter method
   */
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

  /**
   * Generates a getter method that returns either the value of a joined object's property (if the
   * field containing the joined field is non-null) or null (if the field containing the joined
   * field is null).
   *
   * @param getterPropertyName the java property name used to name the getter method
   * @param getterResultType the type used to define the getter's return type
   * @param joinFieldName the name of the field containing the joined object
   * @param joinPropertyName the name of the property in the joined object to return
   * @return a {@link MethodSpec} that generates the getter method
   */
  public static MethodSpec createJoinPropertyGetter(
      String getterPropertyName,
      TypeName getterResultType,
      String joinFieldName,
      String joinPropertyName) {
    return MethodSpec.methodBuilder(fieldToMethodName("get", getterPropertyName))
        .addModifiers(Modifier.PUBLIC)
        .returns(getterResultType)
        .addStatement(
            "return $L == null ? null : $L.$L()",
            joinFieldName,
            joinFieldName,
            fieldToMethodName("get", joinPropertyName))
        .build();
  }

  /**
   * Generates a getter method that returns a mapping of fields by group, whose keys are the fields'
   * names and whose values are the fields' values.
   *
   * @param groupedPropertiesName the shared group name of the fields
   * @param propertyNames the fields for the given group
   * @param getterResultType the return type of the grouped fields
   * @return a {@link MethodSpec} that generates the getter method
   */
  public static MethodSpec createGroupedPropertiesGetter(
      String groupedPropertiesName, List<String> propertyNames, TypeName getterResultType) {
    ParameterizedTypeName returnType =
        ParameterizedTypeName.get(MapClassName, StringClassName, getterResultType);
    MethodSpec.Builder methodSpecBuilder =
        MethodSpec.methodBuilder(fieldToMethodName("get", groupedPropertiesName))
            .addModifiers(Modifier.PUBLIC)
            .returns(returnType)
            .addStatement("$T $L = new $T<>()", returnType, groupedPropertiesName, HashMap.class);
    propertyNames.stream()
        .forEach(
            property ->
                methodSpecBuilder.addStatement(
                    "$L.put($S, $L())",
                    groupedPropertiesName,
                    property,
                    fieldToMethodName("get", property)));
    methodSpecBuilder.addStatement("return $L", groupedPropertiesName);
    return methodSpecBuilder.build();
  }

  /**
   * Creates a setter/getter name by adding the capitalized field name to a prefix (set or get).
   *
   * @param prefix set or get
   * @param fieldName field name
   * @return the combined method name
   */
  public static String fieldToMethodName(String prefix, String fieldName) {
    return prefix + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
  }

  /**
   * Helper method to create a non-checked exception for cases where an unsupported combination of
   * types was passed.
   *
   * @param propertyName the java property name
   * @param fieldType the type of the property in the class
   * @param accessorType the type of the getter return value or setter parameter
   * @return an {@link IllegalArgumentException} with appropriate error message
   */
  @Nonnull
  private static IllegalArgumentException createInvalidTypeCombinationException(
      String propertyName, TypeName fieldType, TypeName accessorType) {
    return new IllegalArgumentException(
        String.format(
            "unsupported combination of java types for property: fieldType=%s accessorType=%s property=%s",
            fieldType, accessorType, propertyName));
  }
}
