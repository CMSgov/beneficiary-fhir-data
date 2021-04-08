package gov.cms.bfd.server.war.commons.adapter;

import java.util.Optional;

public abstract class ClaimLineAdaptor implements ClaimLineAdaptorInterface {
  public Optional<String> getNationalDrugCode() {
    return Optional.empty();
  }
}
