package gov.cms.model.dsl.codegen.library;

/**
 * Defines the method signature for methods that are generated to transform data for a field at
 * runtime.
 */
@FunctionalInterface
public interface ExternalTransformation<TRecord, TEntity> {
  void transformField(
      DataTransformer transformer, String namePrefix, TRecord record, TEntity entity);
}
