package gov.cms.bfd.model.rda;

/**
 * Concrete instance of {@link AbstractJsonConverter} that maps JSON array strings to and from
 * instances of {@link StringList}. Converts null string values into empty list instances when
 * deserializing value from the database.
 */
public class StringListConverter extends AbstractJsonConverter<StringList> {
  /** StringListConverter Constructor. */
  public StringListConverter() {
    super(StringList.class, StringList::new);
  }
}
