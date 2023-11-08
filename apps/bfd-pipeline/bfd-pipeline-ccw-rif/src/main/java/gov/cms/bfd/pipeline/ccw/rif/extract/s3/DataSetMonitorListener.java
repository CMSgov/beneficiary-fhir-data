package gov.cms.bfd.pipeline.ccw.rif.extract.s3;

import gov.cms.bfd.model.rif.RifFilesEvent;
import gov.cms.bfd.pipeline.ccw.rif.CcwRifLoadJob;

/**
 * Implementations of this event/callback interface can receive the events fired by a {@link
 * CcwRifLoadJob}.
 */
public interface DataSetMonitorListener {
  /**
   * Called when the {@link CcwRifLoadJob} has checked the S3 bucket for a new data set, and not
   * found one.
   */
  void noDataAvailable();

  /**
   * Called when a new {@link RifFilesEvent} data set is available for processing. It's this
   * method's responsibility to actually <em>do</em> that processing, presumably by transforming the
   * data and pushing it to a FHIR server.
   *
   * <p><strong>It is very important</strong> that this method block until the data set has finished
   * processing. If it did not, the application might start processing another data set at the same
   * time, which will likely lead to updates being applied in the wrong order, and an inconsistent
   * resulting database. That would be very bad, and unrecoverable.
   *
   * @param rifFilesEvent the new {@link RifFilesEvent} data set to be processed
   * @throws Exception any exception indicates that processing failed
   */
  void dataAvailable(RifFilesEvent rifFilesEvent) throws Exception;
}
