package gov.cms.model.dsl.codegen.plugin.transformer;

import com.squareup.javapoet.CodeBlock;
import gov.cms.model.dsl.codegen.plugin.accessor.Getter;
import gov.cms.model.dsl.codegen.plugin.accessor.Setter;
import gov.cms.model.dsl.codegen.plugin.model.ColumnBean;
import gov.cms.model.dsl.codegen.plugin.model.MappingBean;
import gov.cms.model.dsl.codegen.plugin.model.TransformationBean;

/**
 * Implementation of {@link FieldTransformer} for use with string fields that need to be hashed
 * before being copied to the destination field.
 */
public class IdHashFieldTransformer implements FieldTransformer {
  /**
   * {@inheritDoc}
   *
   * <p>Generates code that uses the id hasher to hash the input string value and then copies the
   * hashed string to the output field.
   *
   * @return a code block containing statements to hash and copy the value
   */
  @Override
  public CodeBlock generateCodeBlock(
      MappingBean mapping,
      ColumnBean column,
      TransformationBean transformation,
      Getter getter,
      Setter setter) {
    if (transformation.isOptional()) {
      final String valueFunc =
          String.format("()-> %s.apply(%s)", HASHER_VAR, getter.createGetCall(transformation));
      return CodeBlock.builder()
          .addStatement(
              "$L.copyOptionalString($L, 1, $L, $L, $L, $L)",
              TRANSFORMER_VAR,
              TransformerUtil.createFieldNameForErrorReporting(mapping, column),
              column.computeLength(),
              getter.createHasRef(transformation),
              valueFunc,
              setter.createSetRef(column))
          .build();
    } else {
      final String value =
          String.format("%s.apply(%s)", HASHER_VAR, getter.createGetCall(transformation));
      return CodeBlock.builder()
          .addStatement(
              "$L.copyString($L, $L, 1, $L, $L, $L)",
              TRANSFORMER_VAR,
              TransformerUtil.createFieldNameForErrorReporting(mapping, column),
              column.isNullable(),
              column.computeLength(),
              value,
              setter.createSetRef(column))
          .build();
    }
  }
}
