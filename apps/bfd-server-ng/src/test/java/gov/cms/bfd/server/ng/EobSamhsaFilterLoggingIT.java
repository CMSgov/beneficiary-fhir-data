package gov.cms.bfd.server.ng;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import gov.cms.bfd.server.ng.eob.EobHandler;
import gov.cms.bfd.server.ng.input.DateTimeRange;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/** Integration test to verify SAMHSA claim filtering logging is working correctly. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class EobSamhsaFilterLoggingIT extends IntegrationTestBase {

  private static final long CLAIM_ID_WITH_SAMHSA_DIAGNOSIS = 4146709784142L;
  private static final long CLAIM_ID_WITH_NO_SAMHSA = 566745788569L;

  private final EobHandler eobHandler;

  private ListAppender<ILoggingEvent> listAppender;
  private Logger logger;

  @BeforeEach
  void setUp() {
    // Get the logger for EobHandler or SAMHSA filtering
    logger = (Logger) LoggerFactory.getLogger("gov.cms.bfd.server.ng.eob");

    // Create and attach a ListAppender to capture log events
    listAppender = new ListAppender<>();
    listAppender.start();
    logger.addAppender(listAppender);
  }

  @AfterEach
  void tearDown() {
    // Remove the appender after each test
    logger.detachAppender(listAppender);
    listAppender.stop();
  }

  /** Test that filtering a SAMHSA claim generates appropriate logs. */
  @Test
  void testSamhsaClaimLogging() {
    listAppender.list.clear();

    eobHandler.searchById(
        CLAIM_ID_WITH_SAMHSA_DIAGNOSIS,
        new DateTimeRange(),
        new DateTimeRange(),
        SamhsaFilterMode.EXCLUDE);

    assertFalse(listAppender.list.isEmpty(), "Expected log messages during SAMHSA filtering");
  }

  /** Test that non-SAMHSA claims do not generate filtering logs. */
  @Test
  void testNonSamhsaClaimNoLogging() {
    listAppender.list.clear();

    eobHandler.searchById(
        CLAIM_ID_WITH_NO_SAMHSA,
        new DateTimeRange(),
        new DateTimeRange(),
        SamhsaFilterMode.EXCLUDE);

    assertTrue(listAppender.list.isEmpty(), "Expected no log messages for non-SAMHSA claims");
  }
}
