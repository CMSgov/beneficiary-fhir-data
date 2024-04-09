package gov.cms.model.dsl.codegen.plugin.transformer;

import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import gov.cms.model.dsl.codegen.library.EnumStringExtractor;
import gov.cms.model.dsl.codegen.plugin.accessor.Getter;
import gov.cms.model.dsl.codegen.plugin.accessor.Setter;
import gov.cms.model.dsl.codegen.plugin.model.ColumnBean;
import gov.cms.model.dsl.codegen.plugin.model.MappingBean;
import gov.cms.model.dsl.codegen.plugin.model.TransformationBean;
import jakarta.annotation.Nonnull;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.Modifier;

/**
 * Implementation of {@link FieldTransformer} for use with enum values mapped to strings in the
 * database.
 */
public class MessageEnumFieldTransformer implements FieldTransformer {
  /** Name for the {@link TransformationBean#findTransformerOption} that defines the enum class. */
  public static final String ENUM_CLASS_OPT = "enumClass";

  /**
   * Name for the {@link TransformationBean#findTransformerOption} that defines whether the source
   * provides an unrecognized value property as well as the enum property.
   */
  public static final String HAS_UNRECOGNIZED_OPT = "hasUnrecognized";

  /**
   * Name for the {@link TransformationBean#findTransformerOption} that defines certain enum values
   * that should be rejected during transformation.
   */
  public static final String UNSUPPORTED_ENUM_VALUES_OPT = "unsupportedEnumValues";

  /**
   * Name for the {@link TransformationBean#findTransformerOption} that defines extra options to
   * configure the {@link EnumStringExtractor}.
   */
  public static final String EXTRACTOR_OPTIONS_OPT = "extractorOptions";

  /**
   * Name for the {@link TransformationBean#findTransformerOption} that defines the suffix used to
   * find the enum field in the source object.
   */
  public static final String ENUM_NAME_SUFFIX = "enumNameSuffix";

  /**
   * Name for the {@link TransformationBean#findTransformerOption} that defines the suffix used to
   * find the unrecognized field in the source object.
   */
  public static final String UNRECOGNIZED_NAME_SUFFIX = "unrecognizedNameSuffix";

  /** Default value for the {@link MessageEnumFieldTransformer#ENUM_NAME_SUFFIX} option. */
  public static final String DEFAULT_ENUM_NAME_SUFFIX = "Enum";

  /** Default value for the {@link MessageEnumFieldTransformer#UNRECOGNIZED_NAME_SUFFIX} option. */
  public static final String DEFAULT_UNRECOGNIZED_NAME_SUFFIX = "Unrecognized";

  /**
   * {@inheritDoc}
   *
   * <p>Generate code to call either {@link
   * gov.cms.model.dsl.codegen.library.DataTransformer#copyEnumAsCharacter} or {@link
   * gov.cms.model.dsl.codegen.library.DataTransformer#copyEnumAsString} depending on whether the
   * column field is of type character or string.
   *
   * @param mapping The mapping that contains the field.
   * @param column model object describing the database column
   * @param transformation model object describing the transformation to apply
   * @param getter {@link Getter} implementation used to generate code to read from source field
   * @param setter {@link Setter} implementation used to generate code to write to the destination
   *     field
   * @return a statement calling a method to copy the value
   */
  @Override
  public CodeBlock generateCodeBlock(
      MappingBean mapping,
      ColumnBean column,
      TransformationBean transformation,
      Getter getter,
      Setter setter) {
    CodeBlock.Builder builder = CodeBlock.builder();
    if (column.isChar()) {
      builder.addStatement(
          "$L.copyEnumAsCharacter($L, $L.getEnumString($L), $L)",
          TRANSFORMER_VAR,
          TransformerUtil.createFieldNameForErrorReporting(mapping, column),
          extractorName(mapping, transformation),
          SOURCE_VAR,
          setter.createSetRef(column));
    } else {
      builder.addStatement(
          "$L.copyEnumAsString($L, $L, $L, $L, $L.getEnumString($L), $L)",
          TRANSFORMER_VAR,
          TransformerUtil.createFieldNameForErrorReporting(mapping, column),
          column.isNullable(),
          column.computeMinLength(0),
          column.computeLength(),
          extractorName(mapping, transformation),
          SOURCE_VAR,
          setter.createSetRef(column));
    }
    return builder.build();
  }

  /**
   * {@inheritDoc}
   *
   * <p>The generated list will contain a single field to hold a {@link EnumStringExtractor}.
   *
   * @param mapping The mapping that contains the field.
   * @param column model object describing the database column
   * @param transformation model object describing the transformation to apply
   * @return a list containing the {@link FieldSpec}
   */
  @Override
  public List<FieldSpec> generateFieldSpecs(
      MappingBean mapping, ColumnBean column, TransformationBean transformation) {
    final ClassName messageClass = TransformerUtil.toClassName(mapping.getMessageClassName());
    final ClassName enumClass = getEnumClass(transformation);
    FieldSpec.Builder builder =
        FieldSpec.builder(
            ParameterizedTypeName.get(
                ClassName.get(EnumStringExtractor.class), messageClass, enumClass),
            extractorName(mapping, transformation),
            Modifier.PRIVATE,
            Modifier.FINAL);
    return ImmutableList.of(builder.build());
  }

  /**
   * {@inheritDoc}
   *
   * <p>The generated list will contain code to initialize the single field that holds a {@link
   * EnumStringExtractor}.
   *
   * @param mapping The mapping that contains the field.
   * @param column model object describing the database column
   * @param transformation model object describing the transformation to apply
   * @return a list containing the {@link CodeBlock}
   */
  @Override
  public List<CodeBlock> generateFieldInitializers(
      MappingBean mapping, ColumnBean column, TransformationBean transformation) {
    final ClassName messageClass = TransformerUtil.toClassName(mapping.getMessageClassName());
    final ClassName enumClass = getEnumClass(transformation);
    final boolean hasUnrecognized =
        transformation
            .transformerOption(HAS_UNRECOGNIZED_OPT)
            .map(Boolean::parseBoolean)
            .orElse(true);
    CodeBlock initializer =
        CodeBlock.builder()
            .addStatement(
                "$L = $L.createEnumStringExtractor($L,$L,$L,$L,$T.UNRECOGNIZED,$L,$L)",
                extractorName(mapping, transformation),
                ENUM_FACTORY_VAR,
                sourceEnumHasValueMethod(messageClass, transformation),
                sourceEnumGetValueMethod(messageClass, transformation),
                sourceHasUnrecognizedMethod(hasUnrecognized, messageClass, transformation),
                sourceGetUnrecognizedMethod(hasUnrecognized, messageClass, transformation),
                enumClass,
                unsupportedEnumValues(enumClass, transformation),
                extractorOptions(transformation))
            .build();
    return ImmutableList.of(initializer);
  }

  /**
   * Combines the {@code baseFieldName} and the value (or default) of the {@link
   * MessageEnumFieldTransformer#ENUM_NAME_SUFFIX} option to produce a name for the enum field in
   * the source object.
   *
   * @param transformation model object describing the transformation to apply
   * @param baseFieldName prefix to add to the field name
   * @return field name for the enum field
   */
  private String enumFieldName(TransformationBean transformation, String baseFieldName) {
    return baseFieldName
        + transformation.transformerOption(ENUM_NAME_SUFFIX).orElse(DEFAULT_ENUM_NAME_SUFFIX);
  }

  /**
   * Combines the {@code baseFieldName} and the value (or default) of the {@link
   * MessageEnumFieldTransformer#UNRECOGNIZED_NAME_SUFFIX} option to produce a name for the enum's
   * unrecognized value field in the source object.
   *
   * @param transformation model object describing the transformation to apply
   * @param baseFieldName prefix to add to the field name
   * @return field name for the enum's unrecognized value field
   */
  private String unrecognizedFieldName(TransformationBean transformation, String baseFieldName) {
    return baseFieldName
        + transformation
            .transformerOption(UNRECOGNIZED_NAME_SUFFIX)
            .orElse(DEFAULT_UNRECOGNIZED_NAME_SUFFIX);
  }

  /**
   * Creates an expression that returns true if the source object's enum field has a value.
   *
   * @param sourceClass the source object's class
   * @param transformation model object describing the transformation to apply
   * @return expression to compute whether an enum value is present in the source object
   */
  private CodeBlock sourceEnumHasValueMethod(
      ClassName sourceClass, TransformationBean transformation) {
    return TransformerUtil.createPropertyAccessCodeBlock(
        transformation,
        fieldName ->
            CodeBlock.of("$T::has$L", sourceClass, enumFieldName(transformation, fieldName)),
        (fieldName, propertyName) ->
            CodeBlock.of(
                "message -> message.has$L() && message.get$L().has$L()",
                fieldName,
                fieldName,
                enumFieldName(transformation, propertyName)));
  }

  /**
   * Creates an expression that returns the value of the source object's enum field.
   *
   * @param sourceClass the source object's class
   * @param transformation model object describing the transformation to apply
   * @return expression to extract the value from the source object's enum field
   */
  private CodeBlock sourceEnumGetValueMethod(
      ClassName sourceClass, TransformationBean transformation) {
    return TransformerUtil.createPropertyAccessCodeBlock(
        transformation,
        fieldName ->
            CodeBlock.of("$T::get$L", sourceClass, enumFieldName(transformation, fieldName)),
        (fieldName, propertyName) ->
            CodeBlock.of(
                "message -> message.get$L().get$L()",
                fieldName,
                enumFieldName(transformation, propertyName)));
  }

  /**
   * Creates an expression that returns true if the source object's unrecognized value field has a
   * value.
   *
   * @param hasMethod true if the source object as a method to access an unrecognized field value
   * @param sourceClass the source object's class
   * @param transformation model object describing the transformation to apply
   * @return expression to compute whether an unrecognized value is present in the source object
   */
  private CodeBlock sourceHasUnrecognizedMethod(
      boolean hasMethod, ClassName sourceClass, TransformationBean transformation) {
    if (hasMethod) {
      return TransformerUtil.createPropertyAccessCodeBlock(
          transformation,
          fieldName ->
              CodeBlock.of(
                  "$T::has$L", sourceClass, unrecognizedFieldName(transformation, fieldName)),
          (fieldName, propertyName) ->
              CodeBlock.of(
                  "message -> message.has$L() && message.get$L().has$L()",
                  fieldName,
                  fieldName,
                  unrecognizedFieldName(transformation, propertyName)));
    } else {
      return CodeBlock.of("ignored -> false");
    }
  }

  /**
   * Creates an expression that returns the value of the source object's unrecognized value field.
   *
   * @param hasMethod true if the source object as a method to access an unrecognized field value
   * @param sourceClass the source object's class
   * @param transformation model object describing the transformation to apply
   * @return expression to extract the value from the source object's enum field
   */
  private CodeBlock sourceGetUnrecognizedMethod(
      boolean hasMethod, ClassName sourceClass, TransformationBean transformation) {
    if (hasMethod) {
      return TransformerUtil.createPropertyAccessCodeBlock(
          transformation,
          fieldName ->
              CodeBlock.of(
                  "$T::get$L", sourceClass, unrecognizedFieldName(transformation, fieldName)),
          (fieldName, propertyName) ->
              CodeBlock.of(
                  "message -> message.get$L().get$L()",
                  fieldName,
                  unrecognizedFieldName(transformation, propertyName)));
    } else {
      return CodeBlock.of("ignored -> null");
    }
  }

  /**
   * Creates an expression that returns a {@link Set} containing any unsupported enum values.
   *
   * @param enumClass the enum's class
   * @param transformation model object describing the transformation to apply
   * @return expression to extract create a {@link Set} of unsupported enum values
   */
  private CodeBlock unsupportedEnumValues(ClassName enumClass, TransformationBean transformation) {
    return createOptionsSet(enumClass, transformation, UNSUPPORTED_ENUM_VALUES_OPT);
  }

  /**
   * Creates an expression that returns a {@link Set} containing any extractor options that have
   * been configured for this transformation.
   *
   * @param field field object describing the transformation to apply
   * @return expression to extract create a {@link Set} of configured extractor options
   */
  private CodeBlock extractorOptions(TransformationBean field) {
    return createOptionsSet(
        ClassName.get(EnumStringExtractor.Options.class), field, EXTRACTOR_OPTIONS_OPT);
  }

  /**
   * Creates an expression that defines a {@link Set} of enum values parsed from the specified enum
   * option.
   *
   * @param enumClass the enum's class
   * @param transformation model object describing the transformation to apply
   * @param enumOptionName name of the enum option
   * @return expression that creates the {@link Set}
   */
  private CodeBlock createOptionsSet(
      TypeName enumClass, TransformationBean transformation, String enumOptionName) {
    final List<String> enumValues =
        transformation.transformerListOption(enumOptionName).orElse(List.of());
    boolean first = true;
    CodeBlock.Builder builder = CodeBlock.builder();
    builder.add("$T.of(", Set.class);
    for (String enumValue : enumValues) {
      builder.add("$L$T.$L", first ? "" : ",", enumClass, enumValue);
      first = false;
    }
    builder.add(")");
    return builder.build();
  }

  /**
   * Extract the value of the {@link MessageEnumFieldTransformer#ENUM_CLASS_OPT} option and convert
   * it into a {@link ClassName}.
   *
   * @param transformation model object describing the transformation being applied
   * @return correct {@link ClassName}
   * @throws IllegalArgumentException if no class was defined in the {@link TransformationBean}
   */
  @Nonnull
  private ClassName getEnumClass(TransformationBean transformation) {
    return TransformerUtil.toClassName(
        transformation
            .transformerOption(ENUM_CLASS_OPT)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        String.format(
                            "missing value for option %s in field %s",
                            ENUM_CLASS_OPT, transformation.getFrom()))));
  }

  /**
   * Create a unique field name for use in defining an enum extractor field for this specific
   * transformation in the generated transformer class.
   *
   * @param mapping {@link MappingBean} that contains this transformation
   * @param transformation {@link TransformationBean} that needs an extractor field
   * @return unique field name for the extractor
   */
  private static String extractorName(MappingBean mapping, TransformationBean transformation) {
    final String fromName = transformation.getFrom().replace(".", "_");
    return String.format("%s_%s_Extractor", mapping.getEntityClassSimpleName(), fromName);
  }
}
