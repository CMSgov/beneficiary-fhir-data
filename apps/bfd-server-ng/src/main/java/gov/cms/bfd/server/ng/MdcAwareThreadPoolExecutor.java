package gov.cms.bfd.server.ng;

import org.slf4j.MDC;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Custom task executor that preserves MDC info, ensuring all relevant info is logged for each
 * request.
 */
public class MdcAwareThreadPoolExecutor extends ThreadPoolTaskExecutor {

  static Runnable wrapWithMdcContext(Runnable task) {
    // save the current MDC context
    var contextMap = MDC.getCopyOfContextMap();
    return () -> {
      MDC.clear();
      MDC.setContextMap(contextMap);
      try {
        task.run();
      } finally {
        // once the task is complete, clear MDC
        MDC.clear();
      }
    };
  }

  @Override
  public void execute(Runnable command) {
    super.execute(wrapWithMdcContext(command));
  }
}
