package gov.cms.model.dsl.codegen.plugin.transformer;

import com.squareup.javapoet.CodeBlock;
import gov.cms.model.dsl.codegen.library.DataTransformer;
import gov.cms.model.dsl.codegen.plugin.accessor.Getter;
import gov.cms.model.dsl.codegen.plugin.accessor.Setter;
import gov.cms.model.dsl.codegen.plugin.model.ColumnBean;
import gov.cms.model.dsl.codegen.plugin.model.MappingBean;
import gov.cms.model.dsl.codegen.plugin.model.TransformationBean;
import java.util.function.Consumer;

/** Implementation of {@link FieldTransformer} for use with fields needing to be Base64 encoded. */
public class Base64FieldTransformer implements FieldTransformer {
  /**
   * Name for the {@link TransformationBean#findTransformerOption} that defines the decoded length.
   */
  public static final String DECODED_LENGTH_OPT = "decodedLength";

  /**
   * {@inheritDoc}
   *
   * <p>Generate code to call {@link DataTransformer#copyBase64String(String, boolean, int, int,
   * int, String, Consumer)}.
   *
   * @param mapping The mapping that contains the field.
   * @param column model object describing the database column
   * @param transformation model object describing the transformation to apply
   * @param getter {@link Getter} implementation used to generate code to read from source field
   * @param setter {@link Setter} implementation used to generate code to write to the destination
   *     field
   * @return a statement calling a method to copy the value
   * @throws IllegalArgumentException if the field is not optional
   */
  @Override
  public CodeBlock generateCodeBlock(
      MappingBean mapping,
      ColumnBean column,
      TransformationBean transformation,
      Getter getter,
      Setter setter) {
    int columnLength = column.computeLength();

    return CodeBlock.builder()
        .addStatement(
            "$L.copyBase64String($L, $L, $L, $L, $L, $L, $L)",
            TRANSFORMER_VAR,
            TransformerUtil.createFieldNameForErrorReporting(mapping, column),
            column.isNullable(),
            column.computeMinLength(mapping.getMinStringLength()),
            column.computeLength(),
            transformation
                .transformerOption(DECODED_LENGTH_OPT)
                .orElse(String.valueOf(columnLength)),
            getter.createGetCall(transformation),
            setter.createSetRef(column))
        .build();
  }
}
