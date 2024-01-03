package gov.cms.bfd.migrator.app;

import gov.cms.bfd.events.EventPublisher;
import gov.cms.bfd.json.JsonConverter;

/**
 * Implementation of {@link EventPublisher} that publishes events to an SQS queue as JSON strings.
 */
public class SqsEventPublisher implements EventPublisher {
  /** Used to communicate with SQS. */
  private final SqsDao sqsDao;

  /** URL of the queue we post messages to. */
  private final String queueUrl;

  /** Used to convert objects into JSON strings. */
  private final JsonConverter jsonConverter;

  /**
   * Initializes an instance using a minimal JSON converter.
   *
   * @param sqsDao Used to communicate with SQS.
   * @param queueUrl URL of the queue we post messages to.
   */
  public SqsEventPublisher(SqsDao sqsDao, String queueUrl) {
    this(sqsDao, queueUrl, JsonConverter.minimalInstance());
  }

  /**
   * Initializes an instance using a provided JSON converter.
   *
   * @param sqsDao Used to communicate with SQS.
   * @param queueUrl URL of the queue we post messages to.
   * @param jsonConverter Used to convert events into JSON strings.
   */
  public SqsEventPublisher(SqsDao sqsDao, String queueUrl, JsonConverter jsonConverter) {
    this.sqsDao = sqsDao;
    this.queueUrl = queueUrl;
    this.jsonConverter = jsonConverter;
  }

  @Override
  public void publishEvent(Object event) {
    final var eventJson = jsonConverter.objectToJson(event);
    sqsDao.sendMessage(queueUrl, eventJson);
  }
}
