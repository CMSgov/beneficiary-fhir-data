package gov.cms.bfd.server.war.commons.adapter;

import gov.cms.bfd.model.rif.OutpatientClaimLine;
import java.util.Optional;

public class OutpatientClaimLineAdaptor extends ClaimLineAdaptor {
  OutpatientClaimLine line;

  public OutpatientClaimLineAdaptor(OutpatientClaimLine line) {
    this.line = line;
  }

  @Override
  public Optional<String> getNationalDrugCode() {
    return line.getNationalDrugCode();
  }
}
