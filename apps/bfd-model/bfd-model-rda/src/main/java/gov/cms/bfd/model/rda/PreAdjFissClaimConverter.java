package gov.cms.bfd.model.rda;

/**
 * Concrete subclass of {@code AbstractJsonConverter} to handle round trip conversion of {@code
 * PreAdjFissClaim} objects to JSON.
 */
public class PreAdjFissClaimConverter extends AbstractJsonConverter<PreAdjFissClaim> {
  public PreAdjFissClaimConverter() {
    super(PreAdjFissClaim.class);
  }
}
