package gov.cms.bfd.server.war.commons.adapter;

import gov.cms.bfd.model.rif.PartDEvent;
import java.util.Optional;

public class PartDEventClaimLineAdaptor extends ClaimLineAdaptor {
  PartDEvent pde;

  public PartDEventClaimLineAdaptor(PartDEvent pde) {
    this.pde = pde;
  }

  @Override
  public Optional<String> getNationalDrugCode() {
    return Optional.of(pde.getNationalDrugCode());
  }
}
