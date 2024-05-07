package gov.cms.model.dsl.codegen.plugin.transformer;

import com.squareup.javapoet.CodeBlock;

/**
 * A FhirElementTransformer is an object that generates java code for a specific type of FHIR
 * element transformation. Note the plugin does not do any data movement so these objects don't
 * actually interact with data. Instead they interact with objects from the DSL model that define
 * classes and entity types, etc and generate code for methods that implement the data movement.
 *
 * <p>The plugin will use a mapping of names to instances of this interface to select the proper
 * generator to use for each field in the model. See {@link
 * TransformerUtil#selectFhirElementTransformer} for details.
 */
public interface FhirElementTransformer {
  /**
   * Variable/argument name used for holding the source object from which data is being extracted.
   */
  String SOURCE_VAR = "from";

  /**
   * Variable/argument name used for holding the destination object to which data is being copied.
   */
  String DEST_VAR = "to";

  /**
   * Variable/argument name used for holding the {@link FhirElementTransformer} object used to copy
   * data.
   */
  String TRANSFORMER_VAR = "fhirElementTransformer";

  /**
   * Generate a code block that will copy the field from the source object to the dest object using
   * a {@link FhirElementTransformer} object. The returned {@link CodeBlock} will be inserted into a
   * method definition by our caller. That method will have variables for the source, destination,
   * and transformer objects that use the standard names defined in this interface as constants. For
   * example, the generated code can use {@link FhirElementTransformer#SOURCE_VAR} to reference the
   * DMEClaim source object, {@link FieldTransformer#DEST_VAR} to reference the destination entity
   * object, and {@link FhirElementTransformer#TRANSFORMER_VAR} to reference the
   * FhirElementTransformer.
   *
   * @param javaFieldName javaFieldName
   * @param ccwMapping ccwMapping
   * @return {@link CodeBlock} containing code to perform the transformation
   */
  CodeBlock generateCodeBlock(String javaFieldName, String ccwMapping);
}
