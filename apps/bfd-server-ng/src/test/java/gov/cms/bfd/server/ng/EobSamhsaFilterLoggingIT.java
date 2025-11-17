package gov.cms.bfd.server.ng;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import gov.cms.bfd.server.ng.eob.EobHandler;
import gov.cms.bfd.server.ng.input.DateTimeRange;
import gov.cms.bfd.server.ng.testUtil.ThreadSafeAppender;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;

/** Integration test to verify SAMHSA claim filtering logging is working correctly. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class EobSamhsaFilterLoggingIT extends IntegrationTestBase {

  private static final long CLAIM_ID_WITH_SAMHSA_DIAGNOSIS = 4146709784142L;
  private static final long CLAIM_ID_WITH_NO_SAMHSA = 566745788569L;
  private static final String SAMHSA_FILTERED_LOG_MESSAGE = "SAMHSA claim filtered";
  private final EobHandler eobHandler;

  /** Test that filtering a SAMHSA claim generates appropriate logs. */
  @Test
  void testSamhsaClaimLogging() {
    var events = ThreadSafeAppender.startRecord();

    eobHandler.searchById(
        CLAIM_ID_WITH_SAMHSA_DIAGNOSIS,
        new DateTimeRange(),
        new DateTimeRange(),
        SamhsaFilterMode.EXCLUDE);

    var samhsaLogs =
        events.stream()
            .filter(e -> e.getLoggerName().equals("gov.cms.bfd.server.ng.eob.EobHandler"))
            .filter(e -> e.getMessage().contains(SAMHSA_FILTERED_LOG_MESSAGE))
            .toList();

    assertEquals(1, samhsaLogs.size(), "Expected exactly one SAMHSA filtering log message");
    var logEvent = samhsaLogs.get(0);
    var formattedMessage = logEvent.getFormattedMessage();
    assertTrue(
        formattedMessage.contains(SAMHSA_FILTERED_LOG_MESSAGE),
        "Log message should contain SAMHSA claim filtered text");
    assertTrue(
        formattedMessage.contains("type="), "Log message should contain claim type information");
  }

  /** Test that non-SAMHSA claims do not generate filtering logs. */
  @Test
  void testNonSamhsaClaimNoLogging() {
    var events = ThreadSafeAppender.startRecord();

    eobHandler.searchById(
        CLAIM_ID_WITH_NO_SAMHSA,
        new DateTimeRange(),
        new DateTimeRange(),
        SamhsaFilterMode.EXCLUDE);

    var samhsaLogs =
        events.stream()
            .filter(e -> e.getLoggerName().equals("gov.cms.bfd.server.ng.eob.EobHandler"))
            .filter(e -> e.getMessage().contains(SAMHSA_FILTERED_LOG_MESSAGE))
            .toList();

    assertTrue(
        samhsaLogs.isEmpty(), "Expected no SAMHSA filtering log messages for non-SAMHSA claims");
  }
}
