package gov.cms.model.rda.codegen.plugin.transformer;

import com.squareup.javapoet.CodeBlock;
import gov.cms.model.rda.codegen.plugin.model.ColumnBean;
import gov.cms.model.rda.codegen.plugin.model.MappingBean;
import gov.cms.model.rda.codegen.plugin.model.TransformationBean;

public class IntFieldTransformer extends AbstractFieldTransformer {
  @Override
  public CodeBlock generateCodeBlock(
      MappingBean mapping, ColumnBean column, TransformationBean transformation) {
    return transformation.isOptional()
        ? generateBlockForOptional(mapping, column, transformation)
        : generateBlockForRequired();
  }

  private CodeBlock generateBlockForRequired() {
    throw new IllegalArgumentException("non-optional ints are not currently supported");
  }

  private CodeBlock generateBlockForOptional(
      MappingBean mapping, ColumnBean column, TransformationBean transformation) {
    return CodeBlock.builder()
        .addStatement(
            "$L.copyOptionalInt($L, $L, $L)",
            TRANSFORMER_VAR,
            sourceHasRef(transformation),
            sourceGetRef(transformation),
            destSetRef(column))
        .build();
  }
}
