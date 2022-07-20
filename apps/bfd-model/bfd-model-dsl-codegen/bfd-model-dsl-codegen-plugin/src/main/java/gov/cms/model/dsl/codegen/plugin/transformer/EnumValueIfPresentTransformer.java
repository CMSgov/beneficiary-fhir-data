package gov.cms.model.dsl.codegen.plugin.transformer;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import gov.cms.model.dsl.codegen.plugin.accessor.Getter;
import gov.cms.model.dsl.codegen.plugin.accessor.Setter;
import gov.cms.model.dsl.codegen.plugin.model.ColumnBean;
import gov.cms.model.dsl.codegen.plugin.model.EnumTypeBean;
import gov.cms.model.dsl.codegen.plugin.model.MappingBean;
import gov.cms.model.dsl.codegen.plugin.model.TransformationBean;

/**
 * Sets a column to a specific enum value if the input field is present in the message. Useful when
 * a single column is populated from one of several possible fields defined in a {@code oneof}
 * within the message.
 */
public class EnumValueIfPresentTransformer implements FieldTransformer {
  /**
   * Key for a {@link TransformationBean#transformerOptions} key/value pair. The value of this
   * option must be the name of an enum defined within the mapping.
   */
  public static final String EnumNameOption = "enumName";

  /**
   * Key for a {@link TransformationBean#transformerOptions} key/value pair. The value of this
   * option must be a valid name for one of the enum's values.
   */
  public static final String EnumValueOption = "enumValue";

  /**
   * {@inheritDoc}
   *
   * <p>Adds code that checks to see if the field is present in the source object and, if it is, to
   * set the enum field in the destination object to the specified enum value.
   *
   * @param mapping The mapping that contains the field.
   * @param column model object describing the database column
   * @param transformation model object describing the transformation to apply
   * @param getter {@link Getter} implementation used to generate code to read from source field
   * @param setter {@link Setter} implementation used to generate code to write to the destination
   *     field
   * @return a code block calling a method to copy the value if the field is present
   */
  @Override
  public CodeBlock generateCodeBlock(
      MappingBean mapping,
      ColumnBean column,
      TransformationBean transformation,
      Getter getter,
      Setter setter) {
    final String enumTypeName = transformation.findTransformerOption(EnumNameOption);
    EnumTypeBean enumType = mapping.findEnum(enumTypeName);
    String enumValue = enumType.findValue(transformation.findTransformerOption(EnumValueOption));
    ClassName enumClass =
        ClassName.get(
            mapping.getEntityClassPackage(),
            mapping.getEntityClassSimpleName(),
            enumType.getName());
    CodeBlock.Builder builder =
        CodeBlock.builder()
            .beginControlFlow("if ($L)", getter.createHasCall(transformation))
            .add(setter.createSetCall(column, CodeBlock.of("$T.$L", enumClass, enumValue)))
            .endControlFlow();
    return builder.build();
  }
}
