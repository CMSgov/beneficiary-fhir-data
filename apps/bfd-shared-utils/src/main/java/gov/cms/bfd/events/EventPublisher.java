package gov.cms.bfd.events;

/**
 * An object capable of publishing events to some external event system. Implementations might
 * publish events to an SQS queue, SNS topic, or even a database table. Events are just java beans
 * that can be converted into JSON.
 */
public interface EventPublisher {
  /**
   * Publish the event object in a manner appropriate to this implementation. For example,
   * serializing the object to JSON and publishing it as a string to an SQS queue or SNS topic.
   *
   * @param event an object to be published
   */
  void publishEvent(Object event);
}
