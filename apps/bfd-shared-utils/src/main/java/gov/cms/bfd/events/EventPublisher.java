package gov.cms.bfd.events;

/**
 * An object capable of publishing events to some external event system. Implementations might
 * publish events to an SQS queue, SNS topic, or even a database table. Events are just java beans
 * that can be converted into JSON.
 */
public interface EventPublisher {
  void publishEvent(Object event);
}
