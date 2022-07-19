package gov.cms.model.dsl.codegen.library;

/**
 * Defines the method signature for methods that are called to transform data for a field at
 * runtime.
 */
@FunctionalInterface
public interface ExternalTransformation<TRecord, TEntity> {
  /**
   * Perform some transformation and/or validation of data between the {@code record} and {@code
   * entity} objects using the provided {@link DataTransformer}.
   *
   * @param transformer used to perform transformations and/or report errors
   * @param namePrefix used to clarify which object is being transformed (for example in an array)
   * @param record object containing source data
   * @param entity object receiving transformed data
   */
  void transformField(
      DataTransformer transformer, String namePrefix, TRecord record, TEntity entity);
}
