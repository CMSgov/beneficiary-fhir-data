package gov.cms.model.dsl.codegen.plugin.transformer;

import com.squareup.javapoet.CodeBlock;

/** Implementation. */
public class IdentifierTransformer implements FhirElementTransformer {

  /**
   * Sets identifier on FHIR Resource.
   *
   * @param javaFieldName identifierValue
   * @param ccwMapping ccwMapping
   */
  @Override
  public CodeBlock generateCodeBlock(String javaFieldName, String ccwMapping) {
    return CodeBlock.builder()
        .addStatement(
            "$L.setIdentifier($L, $L, $S)",
            FhirElementTransformer.TRANSFORMER_VAR,
            FhirElementTransformer.DEST_VAR,
            createGetRef(javaFieldName),
            ccwMapping)
        .build();
  }

  /**
   * {@inheritDoc}
   *
   * <p>This implementation accepts a value and passes it to the getter as-is.
   */
  public CodeBlock createGetRef(String javaFieldName) {
    return CodeBlock.of(
        "$L::get$L", FhirElementTransformer.SOURCE_VAR, TransformerUtil.capitalize(javaFieldName));
  }
}
