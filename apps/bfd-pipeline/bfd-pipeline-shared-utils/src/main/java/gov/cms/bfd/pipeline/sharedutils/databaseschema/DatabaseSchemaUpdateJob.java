package gov.cms.bfd.pipeline.sharedutils.databaseschema;

import gov.cms.bfd.model.rif.schema.DatabaseSchemaManager;
import gov.cms.bfd.pipeline.sharedutils.NullPipelineJobArguments;
import gov.cms.bfd.pipeline.sharedutils.PipelineJob;
import gov.cms.bfd.pipeline.sharedutils.PipelineJobOutcome;
import gov.cms.bfd.pipeline.sharedutils.PipelineJobSchedule;
import gov.cms.bfd.pipeline.sharedutils.PipelineJobType;
import java.util.Optional;
import javax.sql.DataSource;

/**
 * This {@link PipelineJob} is responsible for creating/updating the BFD database schema, via {@link
 * DatabaseSchemaManager}.
 */
public final class DatabaseSchemaUpdateJob implements PipelineJob<NullPipelineJobArguments> {
  public static final PipelineJobType<NullPipelineJobArguments> JOB_TYPE =
      new PipelineJobType<NullPipelineJobArguments>(DatabaseSchemaUpdateJob.class);
  private final DataSource dataSource;

  /**
   * Constructs a new {@link DatabaseSchemaUpdateJob} instance.
   *
   * @param dataSource the application's {@link DataSource}
   */
  public DatabaseSchemaUpdateJob(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  /** @see gov.cms.bfd.pipeline.sharedutils.PipelineJob#getSchedule() */
  @Override
  public Optional<PipelineJobSchedule> getSchedule() {
    return Optional.empty();
  }

  /** @see gov.cms.bfd.pipeline.sharedutils.PipelineJob#isInterruptible() */
  @Override
  public boolean isInterruptible() {
    /*
     * Our code for this is perfectly interruptible but it's not at all clear that Flyway is, and
     * even if it is, that's not documented as a guarantee anywhere.
     */
    return false;
  }

  /** @see gov.cms.bfd.pipeline.sharedutils.PipelineJob#call() */
  @Override
  public PipelineJobOutcome call() throws Exception {
    DatabaseSchemaManager.createOrUpdateSchema(dataSource);

    /*
     * DatabaseSchemaManager doesn't report back on whether or not it had any work to do, so we just
     * assume that it does for now. TODO enhance DatabaseSchemaManager to report back with info on
     * what it did
     */
    return PipelineJobOutcome.WORK_DONE;
  }
}
