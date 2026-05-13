package gov.cms.bfd.server.ng.interceptor;

import static gov.cms.bfd.server.ng.util.LoggerConstants.*;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import gov.cms.bfd.server.ng.log.RequestTelemetryLogger;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** HAPI FHIR interceptor that records request lifecycle timing metrics. */
@Interceptor
@Component
@RequiredArgsConstructor
public class RequestMetricsInterceptor {

  private final RequestTelemetryLogger requestTelemetryLogger;

  /** Pointcut to log timestamp in milliseconds when a request is pre-processed. */
  @Hook(Pointcut.SERVER_INCOMING_REQUEST_PRE_PROCESSED)
  public void requestPreProcessed() {
    requestTelemetryLogger.recordTimestamp(HAPI_INCOMING_PRE_PROCESS);
  }

  /** Pointcut to log timestamp in milliseconds when a request is post-processed. */
  @Hook(Pointcut.SERVER_INCOMING_REQUEST_POST_PROCESSED)
  public void requestPostProcessed() {
    requestTelemetryLogger.recordTimestamp(HAPI_INCOMING_POST_PROCESS);
  }

  /** Pointcut to log timestamp in milliseconds when a request is pre-handled. */
  @Hook(Pointcut.SERVER_INCOMING_REQUEST_PRE_HANDLED)
  public void requestPreHandled() {
    requestTelemetryLogger.recordTimestamp(HAPI_INCOMING_PRE_HANDLE);
  }

  /** Pointcut to log timestamp in milliseconds when a request has an outgoing response. */
  @Hook(Pointcut.SERVER_OUTGOING_RESPONSE)
  public void serverOutgoingResponse() {
    requestTelemetryLogger.recordTimestamp(HAPI_OUTGOING_RESPONSE);
  }

  /** Pointcut to log timestamp in milliseconds when a request has completed processing normally. */
  @Hook(Pointcut.SERVER_PROCESSING_COMPLETED_NORMALLY)
  public void processingCompletedNormally() {
    requestTelemetryLogger.recordTimestamp(HAPI_PROCESS_COMPLETED_NORMALLY);
  }

  /** Pointcut to log timestamp in milliseconds when a request has completed processing. */
  @Hook(Pointcut.SERVER_PROCESSING_COMPLETED)
  public void serverProcessCompleted() {
    requestTelemetryLogger.recordTimestamp(HAPI_PROCESS_COMPLETED);
  }
}
