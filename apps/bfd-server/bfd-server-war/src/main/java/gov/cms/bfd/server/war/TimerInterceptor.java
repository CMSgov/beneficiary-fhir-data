package gov.cms.bfd.server.war;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import gov.cms.bfd.server.sharedutils.BfdMDC;

/**
 * With the use of HAPI's pointcut and interceptors, time metrics at various instances i.e.
 * Pre-handling and outgoing response in the BFD API call lifecycle can be generated and logged
 * {@link BfdMDC}. For more info on server pointcuts:
 * https://hapifhir.io/hapi-fhir/docs/interceptors/server_pointcuts.html
 */
@Interceptor
public class TimerInterceptor {

  /** Pointcut to log time in milliseconds when a request is pre-processed. */
  @Hook(Pointcut.SERVER_INCOMING_REQUEST_PRE_PROCESSED)
  public void requestPreProcessed() {
    // log current instance to MDC
    Long currentTime = System.currentTimeMillis();
    BfdMDC.put(
        "hapi.server_incoming_request_pre_process_time_in_millis", Long.toString(currentTime));
  }

  /** Pointcut to log time in milliseconds when a request is post-processed. */
  @Hook(Pointcut.SERVER_INCOMING_REQUEST_POST_PROCESSED)
  public void requestPostProcessed() {
    // log current instance to MDC
    Long currentTime = System.currentTimeMillis();
    BfdMDC.put(
        "hapi.server_incoming_request_post_process_time_in_millis", Long.toString(currentTime));
  }

  /** Pointcut to log time in milliseconds when a request is pre-handled. */
  @Hook(Pointcut.SERVER_INCOMING_REQUEST_PRE_HANDLED)
  public void requestPreHandled() {
    // log current instance to MDC
    Long currentTime = System.currentTimeMillis();
    BfdMDC.put(
        "hapi.server_incoming_request_pre_handle_time_in_millis", Long.toString(currentTime));
  }

  /** Pointcut to log time in milliseconds when a request has an outgoing response. */
  @Hook(Pointcut.SERVER_OUTGOING_RESPONSE)
  public void serverOutgoingResponse() {
    // log current instance to MDC
    Long currentTime = System.currentTimeMillis();
    BfdMDC.put("hapi.server_outgoing_response_time_in_millis", Long.toString(currentTime));
  }

  /** Pointcut to log time in milliseconds when a request has completed processing normally */
  @Hook(Pointcut.SERVER_PROCESSING_COMPLETED_NORMALLY)
  public void processingCompletedNormally() {
    // log current instance to MDC
    Long currentTime = System.currentTimeMillis();
    BfdMDC.put(
        "hapi.server_processing_completed_normally_time_in_millis", Long.toString(currentTime));
  }

  /** Pointcut to log time in milliseconds when a request has completed processing */
  @Hook(Pointcut.SERVER_PROCESSING_COMPLETED)
  public void serverProcessCompleted() {
    // log current instance to MDC
    Long currentTime = System.currentTimeMillis();
    BfdMDC.put("hapi.server_processing_completed_time_in_millis", Long.toString(currentTime));
  }

  /**
   * Pointcut to log time in milliseconds when a request in pre-processing has an outgoing
   * exception.
   */
  @Hook(Pointcut.SERVER_PRE_PROCESS_OUTGOING_EXCEPTION)
  public void preProcessOutgoingException() {
    // log current instance to MDC
    Long currentTime = System.currentTimeMillis();
    BfdMDC.put(
        "hapi.server_pre_process_outgoing_exception_time_in_millis", Long.toString(currentTime));
  }

  /** Pointcut to log time in milliseconds when a request has an outgoing failure. */
  @Hook(Pointcut.SERVER_OUTGOING_FAILURE_OPERATIONOUTCOME)
  public void serverOutgoingFailureOperationOutcome() {
    // log current instance to MDC
    Long currentTime = System.currentTimeMillis();
    BfdMDC.put(
        "hapi.server_outgoing_failure_operation_outcome_time_in_millis",
        Long.toString(currentTime));
  }

  /** Pointcut to log time in milliseconds when a request has a server handle exception. */
  @Hook(Pointcut.SERVER_HANDLE_EXCEPTION)
  public void serverHandleException() {
    // log current instance to MDC
    Long currentTime = System.currentTimeMillis();
    BfdMDC.put("hapi.server_handle_exception_time_in_millis", Long.toString(currentTime));
  }
}
