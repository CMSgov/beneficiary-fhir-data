package gov.cms.bfd.server.war;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import gov.cms.bfd.server.sharedutils.BfdMDC;
import java.sql.Timestamp;

/**
 * With the use of HAPI's pointcut and interceptors, time metrics at various instances i.e.
 * Pre-handling and outgoing response in the BFD API call lifecycle can be generated and logged
 * {@link BfdMDC}. For more info on server pointcuts:
 * https://hapifhir.io/hapi-fhir/docs/interceptors/server_pointcuts.html
 */
@Interceptor
public class TimerInterceptor {

  /**
   * Pointcut to log time when a request is pre-handled.
   *
   * @param theRequestDetails the {@link ServletRequestDetails} to get request start time
   */
  @Hook(Pointcut.SERVER_INCOMING_REQUEST_PRE_HANDLED)
  public void requestPreHandled(ServletRequestDetails theRequestDetails) {
    // Record the time before the BFD Handler is called.
    Long requestStartMilliseconds =
        (Long) theRequestDetails.getServletRequest().getAttribute(BfdMDC.REQUEST_START_KEY);
    if (requestStartMilliseconds != null) {
      // convert requestStartMilliseconds to timestamp
      Timestamp requestStart = new Timestamp(requestStartMilliseconds);
      BfdMDC.put("hapi.server_incoming_request_pre_handle_time", requestStart.toString());
    }
  }

  /**
   * Pointcut to log duration between outgoing response and request start.
   *
   * @param theRequestDetails the {@link ServletRequestDetails} to get request start time
   */
  @Hook(Pointcut.SERVER_OUTGOING_RESPONSE)
  public void serverOutgoingResponse(ServletRequestDetails theRequestDetails) {
    // Record the response duration.
    Long requestStartMilliseconds =
        (Long) theRequestDetails.getServletRequest().getAttribute(BfdMDC.REQUEST_START_KEY);
    if (requestStartMilliseconds != null)
      BfdMDC.put(
          "hapi.server_outgoing_response_duration",
          Long.toString(System.currentTimeMillis() - requestStartMilliseconds));
  }
}
