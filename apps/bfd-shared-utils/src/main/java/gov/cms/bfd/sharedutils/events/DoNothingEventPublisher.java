package gov.cms.bfd.sharedutils.events;

/**
 * Implementation of {@link EventPublisher} that does nothing when called. Useful when event
 * publication is an optional feature that has not been enabled by configuration.
 */
public class DoNothingEventPublisher implements EventPublisher {
  /**
   * Does nothing at all with the provided event. {@inheritDoc}
   *
   * @param event an object to be published
   */
  @Override
  public void publishEvent(Object event) {}
}
