package gov.cms.bfd.server.war.commons;

import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import java.util.List;

/** Abstract resource provider for common functionality. */
public class AbstractResourceProvider {

  /** A constant for excludeSAMHSA. */
  public static final String EXCLUDE_SAMHSA = "excludeSAMHSA";
}
