package gov.cms.bfd.server.war.commons.adapter;

import gov.cms.bfd.model.rif.InpatientClaimLine;

public class InpatientClaimLineAdaptor extends ClaimLineAdaptor {

  InpatientClaimLine line;

  public InpatientClaimLineAdaptor(InpatientClaimLine line) {
    this.line = line;
  }

  /* Nothing */
}
