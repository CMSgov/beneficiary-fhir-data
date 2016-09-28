package gov.hhs.cms.bluebutton.datapipeline.rif.extract.s3;

import gov.hhs.cms.bluebutton.datapipeline.rif.model.RifFilesEvent;

/**
 * Implementations of this event/callback interface can receive the events fired
 * by a {@link DataSetMonitor} and its {@link DataSetMonitorWorker}s.
 */
public interface DataSetMonitorListener {
	/**
	 * This callback will be fired when the {@link DataSetMonitorWorker} has
	 * checked the S3 bucket for a new data set, and not found one.
	 */
	default void noDataAvailable() {
		// Default is a no-op, as this is really only used in tests.
	}

	/**
	 * <p>
	 * This callback will be fired when a new {@link RifFilesEvent} data set is
	 * available for processing. It's this method's responsibility to actually
	 * <em>do</em> that processing, presumably by transforming the data and
	 * pushing it to a FHIR server.
	 * </p>
	 * <p>
	 * <strong>It is very important</strong> that this method block until the
	 * data set has finished processing. If it did not, the application might
	 * start processing another data set at the same time, which will likely
	 * lead to updates being applied in the wrong order, and an inconsistent
	 * resulting database. That would be very bad, and unrecoverable.
	 * </p>
	 * 
	 * @param rifFilesEvent
	 *            the new {@link RifFilesEvent} data set to be processed
	 */
	void dataAvailable(RifFilesEvent rifFilesEvent);

	/**
	 * This callback will be fired when an unrecoverable error has occurred. It
	 * is this method's responsibility to call {@link DataSetMonitor#stop()}, if
	 * the processing should be halted as a result of the error.
	 * 
	 * @param error
	 *            the error that was encountered and couldn't be handled within
	 *            the {@link DataSetMonitor} or {@link DataSetMonitorWorker}
	 */
	void errorOccurred(Throwable error);
}
