package gov.cms.bfd.pipeline.ccw.rif;

import gov.cms.bfd.model.rif.RifFilesEvent;

/** Implementations of this interface process data sets produced by {@link CcwRifLoadJob}. */
public interface DataSetProcessor {
  /**
   * Called when the {@link CcwRifLoadJob} has checked the S3 bucket for a new data set, and not
   * found one.
   */
  void noDataToProcess();

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
  void processDataSet(RifFilesEvent rifFilesEvent) throws Exception;
}
