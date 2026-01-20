package gov.cms.bfd.server.war.commons;

import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import java.util.List;

/** Abstract resource provider for common functionality. */
public class AbstractResourceProvider {

  /**
   * Ignored. Kept only to track usage of the {@code IncludeTaxNumbers} header/query parameter via
   * MDC.
   */
  public static final String HEADER_NAME_INCLUDE_TAX_NUMBERS = "IncludeTaxNumbers";

  /** A constant for excludeSAMHSA. */
  public static final String EXCLUDE_SAMHSA = "excludeSAMHSA";

  /**
   * Returns the boolean value of the "IncludeTaxNumbers" header or query parameter.
   *
   * @param requestDetails a {@link RequestDetails} containing the details of the request URL, used
   *     to parse out the HTTP header that controls this setting
   * @return {@code true} if {@code IncludeTaxNumbers} is {@code true}, else {@code false}
   */
  protected boolean returnIncludeTaxNumbers(RequestDetails requestDetails) {
    /*
     * Note: headers can be multi-valued and so calling the enticing-looking `getHeader(...)` method
     * is often a bad idea, as it will often do the wrong thing.
     */
    List<String> headerValues = requestDetails.getHeaders(HEADER_NAME_INCLUDE_TAX_NUMBERS);

    if (headerValues == null || headerValues.isEmpty()) {
      return false;
    } else if (headerValues.size() == 1) {
      String headerValue = headerValues.get(0);
      if ("true".equalsIgnoreCase(headerValue)) {
        return true;
      } else if ("false".equalsIgnoreCase(headerValue)) {
        return false;
      }
    }

    throw new InvalidRequestException(
        "Unsupported " + HEADER_NAME_INCLUDE_TAX_NUMBERS + " header value: " + headerValues);
  }
}
