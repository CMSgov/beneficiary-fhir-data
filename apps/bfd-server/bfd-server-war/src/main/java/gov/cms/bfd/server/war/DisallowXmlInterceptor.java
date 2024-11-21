package gov.cms.bfd.server.war;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import gov.cms.bfd.server.sharedutils.BfdMDC;

/**
 * With the use of HAPI's pointcut and interceptors, time metrics at various instances i.e.
 * Pre-handling and outgoing response in the BFD API call lifecycle can be generated and logged
 * {@link BfdMDC}. For more info on server pointcuts:
 * https://hapifhir.io/hapi-fhir/docs/interceptors/server_pointcuts.html
 */
@Interceptor
public class DisallowXmlInterceptor {

  /**
   * Pointcut to log timestamp in milliseconds when a request is pre-processed.
   *
   * @param requestDetails value of the requestDetails
   */
  @Hook(Pointcut.SERVER_INCOMING_REQUEST_PRE_HANDLED)
  public void checkContentType(RequestDetails requestDetails) {
    // find a way to check if JSON or XML using the hapi fhir api instead of the content
    String contentType = requestDetails.getHeader(Constants.HEADER_CONTENT_TYPE);
    //    String formatParam = servletRequest.getParameter("_format");
    //    if ("xml".equalsIgnoreCase(formatParam)) {
    //      System.out.println("The requested format is XML.");
    //    } else {
    //      System.out.println("The requested format is not XML.");
    //    }
    // Check Accept header (for requested response format)
    String acceptHeader = requestDetails.getHeader(Constants.HEADER_ACCEPT);

    // Determine if request is XML
    boolean isXmlRequest =
        (contentType != null && contentType.contains(Constants.CT_FHIR_XML))
            || (acceptHeader != null && acceptHeader.contains(Constants.CT_FHIR_XML));

    if (isXmlRequest) {
      //            throw new InvalidRequestException("XML format is not supported.");
      throw new IllegalArgumentException("XML format is not supported by this server.");
    }

    // check JSON for debug, log the request if it is JSON
    boolean isJsonRequest =
        (contentType != null && contentType.contains(Constants.CT_FHIR_JSON))
            || (acceptHeader != null && acceptHeader.contains(Constants.CT_FHIR_JSON));
    if (isJsonRequest) {
      System.out.println("JSON request detected.");
    }
  }
}
