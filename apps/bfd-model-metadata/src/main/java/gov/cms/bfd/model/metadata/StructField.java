package gov.cms.bfd.model.metadata;

/** Models a field in a {@link Struct}. */
public interface StructField {
  /**
   * @return the ID of this {@link StructField}, which must be unique within the {@link Struct} that
   *     contains it
   */
  String getId();
}
