package gov.cms.model.rda.codegen.plugin.transformer;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import gov.cms.model.rda.codegen.plugin.model.ColumnBean;
import gov.cms.model.rda.codegen.plugin.model.EnumTypeBean;
import gov.cms.model.rda.codegen.plugin.model.MappingBean;
import gov.cms.model.rda.codegen.plugin.model.TransformationBean;

/**
 * Sets a column to a specific enum value if the input field is present in the message. Useful when
 * a single column is populated from one of several possible fields defined in a {@code oneof}
 * within the message.
 */
public class EnumValueIfPresentTransformer extends AbstractFieldTransformer {
  public static final String EnumNameOption = "enumName";
  public static final String EnumValueOption = "enumValue";

  @Override
  public CodeBlock generateCodeBlock(
      MappingBean mapping, ColumnBean column, TransformationBean transformation) {
    final String enumTypeName = transformation.findTransformerOption(EnumNameOption);
    EnumTypeBean enumType = mapping.findEnum(enumTypeName);
    String enumValue = enumType.findValue(transformation.findTransformerOption(EnumValueOption));
    ClassName enumClass =
        ClassName.get(mapping.entityPackageName(), mapping.entityClassName(), enumType.getName());
    CodeBlock.Builder builder =
        CodeBlock.builder()
            .beginControlFlow("if ($L)", sourceHasValue(transformation))
            .add(destSetter(column, CodeBlock.of("$T.$L", enumClass, enumValue)))
            .endControlFlow();
    return builder.build();
  }
}
