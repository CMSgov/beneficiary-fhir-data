package gov.cms.model.rda.codegen.library;

/**
 * Defines the method signature for methods that are generated to transform data for a field at
 * runtime.
 */
@FunctionalInterface
public interface ExternalTransformation<TRecord, TEntity> {
  void transformField(DataTransformer transformer, TRecord record, TEntity entity);
}
