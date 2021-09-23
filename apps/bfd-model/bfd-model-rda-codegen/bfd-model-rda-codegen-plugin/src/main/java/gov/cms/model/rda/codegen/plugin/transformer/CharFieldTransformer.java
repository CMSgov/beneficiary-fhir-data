package gov.cms.model.rda.codegen.plugin.transformer;

import com.squareup.javapoet.CodeBlock;
import gov.cms.model.rda.codegen.plugin.model.ColumnBean;
import gov.cms.model.rda.codegen.plugin.model.MappingBean;
import gov.cms.model.rda.codegen.plugin.model.TransformationBean;

public class CharFieldTransformer extends AbstractFieldTransformer {
  @Override
  public CodeBlock generateCodeBlock(
      MappingBean mapping, ColumnBean column, TransformationBean transformation) {
    return transformation.isOptional()
        ? generateBlockForOptional()
        : generateBlockForRequired(mapping, column, transformation);
  }

  private CodeBlock generateBlockForRequired(
      MappingBean mapping, ColumnBean column, TransformationBean transformation) {
    return CodeBlock.builder()
        .addStatement(
            "$L.copyCharacter($L, $L, $L)",
            TRANSFORMER_VAR,
            fieldNameReference(mapping, column),
            sourceValue(transformation),
            destSetRef(column))
        .build();
  }

  private CodeBlock generateBlockForOptional() {
    throw new IllegalArgumentException("optional chars are not currently supported");
  }
}
