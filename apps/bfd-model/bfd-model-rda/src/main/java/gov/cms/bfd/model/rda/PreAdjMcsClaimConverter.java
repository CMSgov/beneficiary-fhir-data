package gov.cms.bfd.model.rda;

/**
 * Concrete subclass of {@code AbstractJsonConverter} to handle round trip conversion of {@code
 * PreAdjMcsClaim} objects to JSON.
 */
public class PreAdjMcsClaimConverter extends AbstractJsonConverter<PreAdjMcsClaim> {
  public PreAdjMcsClaimConverter() {
    super(PreAdjMcsClaim.class);
  }
}
