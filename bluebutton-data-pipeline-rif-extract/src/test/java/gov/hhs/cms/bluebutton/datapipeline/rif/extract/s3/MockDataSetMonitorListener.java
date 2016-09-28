package gov.hhs.cms.bluebutton.datapipeline.rif.extract.s3;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import gov.hhs.cms.bluebutton.datapipeline.rif.model.RifFilesEvent;

/**
 * A mock {@link DataSetMonitorListener} that tracks the events it receives.
 */
final class MockDataSetMonitorListener implements DataSetMonitorListener {
	private int noDataAvailableEvents = 0;
	private final List<RifFilesEvent> dataEvents = new LinkedList<>();
	final List<Throwable> errorEvents = new LinkedList<>();

	/**
	 * @see gov.hhs.cms.bluebutton.datapipeline.rif.extract.s3.DataSetMonitorListener#noDataAvailable()
	 */
	@Override
	public void noDataAvailable() {
		noDataAvailableEvents++;
	}

	/**
	 * @return the number of times the {@link #noDataAvailable()} event has been
	 *         fired
	 */
	public int getNoDataAvailableEvents() {
		return noDataAvailableEvents;
	}

	/**
	 * @see gov.hhs.cms.bluebutton.datapipeline.rif.extract.s3.DataSetMonitorListener#dataAvailable(gov.hhs.cms.bluebutton.datapipeline.rif.model.RifFilesEvent)
	 */
	@Override
	public void dataAvailable(RifFilesEvent rifFilesEvent) {
		dataEvents.add(rifFilesEvent);
	}

	/**
	 * @return the {@link List} of {@link RifFilesEvent}s that have been passed
	 *         to {@link #dataAvailable(RifFilesEvent)}
	 */
	public List<RifFilesEvent> getDataEvents() {
		return Collections.unmodifiableList(dataEvents);
	}

	/**
	 * @see gov.hhs.cms.bluebutton.datapipeline.rif.extract.s3.DataSetMonitorListener#errorOccurred(java.lang.Throwable)
	 */
	@Override
	public void errorOccurred(Throwable error) {
		errorEvents.add(error);
	}

	/**
	 * @return the {@link List} of {@link Throwable}s that have been passed to
	 *         {@link #errorOccurred(Throwable)}
	 */
	public List<Throwable> getErrorEvents() {
		return Collections.unmodifiableList(errorEvents);
	}
}