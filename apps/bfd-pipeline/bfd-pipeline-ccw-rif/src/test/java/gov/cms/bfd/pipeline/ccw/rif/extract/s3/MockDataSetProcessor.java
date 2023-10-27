package gov.cms.bfd.pipeline.ccw.rif.extract.s3;

import gov.cms.bfd.model.rif.RifFilesEvent;
import gov.cms.bfd.pipeline.ccw.rif.DataSetProcessor;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A mock {@link DataSetProcessor} that tracks the events it receives. */
public final class MockDataSetProcessor implements DataSetProcessor {
  private static final Logger LOGGER = LoggerFactory.getLogger(MockDataSetProcessor.class);

  /** Tracks the number of events where no data was available; primarily used for testing. */
  private int noDataAvailableEvents = 0;

  /** The list of data available events. */
  private final List<RifFilesEvent> dataEvents = new LinkedList<>();

  @Override
  public void noDataToProcess() {
    noDataAvailableEvents++;
  }

  /**
   * Gets the {@link #noDataAvailableEvents}.
   *
   * @return the number of times the {@link #noDataToProcess()} event has been fired
   */
  public int getNoDataAvailableEvents() {
    return noDataAvailableEvents;
  }

  @Override
  public void processDataSet(RifFilesEvent rifFilesEvent) {
    dataEvents.add(rifFilesEvent);
  }

  /**
   * Gets the {@link #dataEvents} as an immutable list.
   *
   * @return the {@link List} of {@link RifFilesEvent}s that have been passed to {@link
   *     #processDataSet(RifFilesEvent)}
   */
  public List<RifFilesEvent> getDataEvents() {
    return Collections.unmodifiableList(dataEvents);
  }
}
