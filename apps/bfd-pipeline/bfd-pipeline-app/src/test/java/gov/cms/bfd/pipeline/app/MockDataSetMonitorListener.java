package gov.cms.bfd.pipeline.app;

import gov.cms.bfd.model.rif.RifFilesEvent;
import gov.cms.bfd.pipeline.ccw.rif.extract.s3.DataSetMonitorListener;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A mock {@link DataSetMonitorListener} that tracks the events it receives. */
public final class MockDataSetMonitorListener implements DataSetMonitorListener {
  private static final Logger LOGGER = LoggerFactory.getLogger(MockDataSetMonitorListener.class);

  /** Counter for how many times the {@link #noDataAvailable()} has fired. */
  private int noDataAvailableEvents = 0;

  /** A list of data events that have fired. */
  private final List<RifFilesEvent> dataEvents = new LinkedList<>();

  /** A list of error events that have fired. */
  final List<Throwable> errorEvents = new LinkedList<>();

  /** {@inheritDoc} */
  @Override
  public void noDataAvailable() {
    noDataAvailableEvents++;
  }

  /**
   * Gets the number of times the {@link #noDataAvailable()} event has fired.
   *
   * @return the number of times the {@link #noDataAvailable()} event has been fired.
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
   * Gets an immutable list of the data events.
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
   * Gets an immutable list of the error events.
   *
   * @return the {@link List} of {@link Throwable}s that have been passed to {@link
   *     #errorOccurred(Throwable)}
   */
  public List<Throwable> getErrorEvents() {
    return Collections.unmodifiableList(errorEvents);
  }
}
