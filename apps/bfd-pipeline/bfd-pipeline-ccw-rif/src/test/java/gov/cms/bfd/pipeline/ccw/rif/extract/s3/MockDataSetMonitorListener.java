package gov.cms.bfd.pipeline.ccw.rif.extract.s3;

import gov.cms.bfd.model.rif.RifFilesEvent;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A mock {@link DataSetMonitorListener} that tracks the events it receives. */
public final class MockDataSetMonitorListener implements DataSetMonitorListener {
  private static final Logger LOGGER = LoggerFactory.getLogger(MockDataSetMonitorListener.class);

  /** Tracks the number of events where no data was available; primarily used for testing. */
  private int noDataAvailableEvents = 0;

  /** The list of data available events. */
  private final List<RifFilesEvent> dataEvents = new LinkedList<>();

  /** The list of load error events. */
  final List<Throwable> errorEvents = new LinkedList<>();

  /** {@inheritDoc} */
  @Override
  public void noDataAvailable() {
    noDataAvailableEvents++;
  }

  /**
   * Gets the {@link #noDataAvailableEvents}.
   *
   * @return the number of times the {@link #noDataAvailable()} event has been fired
   */
  public int getNoDataAvailableEvents() {
    return noDataAvailableEvents;
  }

  /** {@inheritDoc} */
  @Override
  public void dataAvailable(RifFilesEvent rifFilesEvent) {
    dataEvents.add(rifFilesEvent);
  }

  /**
   * Gets the {@link #dataEvents} as an immutable list.
   *
   * @return the {@link List} of {@link RifFilesEvent}s that have been passed to {@link
   *     #dataAvailable(RifFilesEvent)}
   */
  public List<RifFilesEvent> getDataEvents() {
    return Collections.unmodifiableList(dataEvents);
  }

  /** {@inheritDoc} */
  @Override
  public void errorOccurred(Throwable error) {
    LOGGER.warn("Error received.", error);
    errorEvents.add(error);
  }

  /**
   * Gets the {@link #errorEvents} as an immutable list.
   *
   * @return the {@link List} of {@link Throwable}s that have been passed to {@link
   *     #errorOccurred(Throwable)}
   */
  public List<Throwable> getErrorEvents() {
    return Collections.unmodifiableList(errorEvents);
  }
}
