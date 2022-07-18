package gov.cms.model.dsl.codegen.plugin.transformer;

import static java.lang.Boolean.FALSE;

import com.squareup.javapoet.CodeBlock;
import gov.cms.model.dsl.codegen.plugin.accessor.Getter;
import gov.cms.model.dsl.codegen.plugin.accessor.Setter;
import gov.cms.model.dsl.codegen.plugin.model.ColumnBean;
import gov.cms.model.dsl.codegen.plugin.model.MappingBean;
import gov.cms.model.dsl.codegen.plugin.model.TransformationBean;

/** Implementation of {@link FieldTransformer} for use with string fields. */
public class StringFieldTransformer implements FieldTransformer {
  /**
   * Transformer option name to control whether optional string copying will ignore empty strings.
   */
  public static final String IgnoreEmptyStringOption = "ignoreEmptyString";

  /**
   * {@inheritDoc}
   *
   * <p>Generate code to call either {@link
   * gov.cms.model.dsl.codegen.library.DataTransformer#copyString} or {@link
   * gov.cms.model.dsl.codegen.library.DataTransformer#copyOptionalString} depending on whether the
   * field is optional.
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
    return transformation.isOptional()
        ? CodeBlock.builder()
            .addStatement(
                "$L.$L($L, $L, $L, $L, $L, $L)",
                TRANSFORMER_VAR,
                selectOptionalStringCopyMethodName(transformation),
                TransformerUtil.createFieldNameForErrorReporting(mapping, column),
                column.computeMinLength(mapping.getMinStringLength()),
                column.computeLength(),
                getter.createHasRef(transformation),
                getter.createGetRef(transformation),
                setter.createSetRef(column))
            .build()
        : CodeBlock.builder()
            .addStatement(
                "$L.copyString($L, $L, $L, $L, $L, $L)",
                TRANSFORMER_VAR,
                TransformerUtil.createFieldNameForErrorReporting(mapping, column),
                column.isNullable(),
                column.computeMinLength(mapping.getMinStringLength()),
                column.computeLength(),
                getter.createGetCall(transformation),
                setter.createSetRef(column))
            .build();
  }

  /**
   * Selects the appropriate method name for copying an optional string. Which method to use is
   * determined by checking for the boolean option {@link IgnoreEmptyStringOption}.
   *
   * @param transformation model object describing the transformation to apply
   * @return correct method name
   */
  private String selectOptionalStringCopyMethodName(TransformationBean transformation) {
    return transformation
            .transformerOption(IgnoreEmptyStringOption)
            .map(Boolean::valueOf)
            .orElse(FALSE)
        ? "copyOptionalNonEmptyString"
        : "copyOptionalString";
  }
}
