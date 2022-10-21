package gov.cms.bfd.server.war;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import gov.cms.bfd.server.sharedutils.BfdMDC;
import java.sql.Timestamp;
import org.slf4j.MDC;

@Interceptor
public class TimerInterceptor {
  http_access.response.server_outgoing_response_duration
  @Hook(Pointcut.SERVER_INCOMING_REQUEST_PRE_HANDLED)
  public void requestPreHandled(ServletRequestDetails theRequestDetails) {
    // Record the time before the BFD Handler is called.
    Long requestStartMilliseconds =
        (Long) theRequestDetails.getServletRequest().getAttribute(BfdMDC.REQUEST_START_KEY);
    if (requestStartMilliseconds != null) {
      // convert requestStartMilliseconds to timestamp
      Timestamp requestStart = new Timestamp(requestStartMilliseconds);
      BfdMDC.put(
          "http_access.request.server_incoming_request_pre_handle_time", requestStart.toString());
    }
  }

  @Hook(Pointcut.SERVER_OUTGOING_RESPONSE)
  public void serverOutgoingResponse(ServletRequestDetails theRequestDetails) {
    // Record the response duration.
    Long requestStartMilliseconds =
        (Long) theRequestDetails.getServletRequest().getAttribute(BfdMDC.REQUEST_START_KEY);
    if (requestStartMilliseconds != null)
      BfdMDC.put(
          "http_access.response.server_outgoing_response_duration",
          Long.toString(System.currentTimeMillis() - requestStartMilliseconds));
  }
}
