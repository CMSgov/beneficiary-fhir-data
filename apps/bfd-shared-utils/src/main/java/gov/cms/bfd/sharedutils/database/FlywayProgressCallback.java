package gov.cms.bfd.sharedutils.database;

import com.google.common.annotations.VisibleForTesting;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import java.util.Map;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.flywaydb.core.api.callback.Callback;
import org.flywaydb.core.api.callback.Context;
import org.flywaydb.core.api.callback.Event;

/**
 * Implementation of {@link Callback} that converts flyway {@link Event} objects into {@link
 * DatabaseMigrationProgress} objects and passes them to a callback function. Used by the migrator
 * app to send updates to an SQS queue or a log file during processing.
 */
@AllArgsConstructor
public class FlywayProgressCallback implements Callback {
  /**
   * We only care about events that we report on. All others will be ignored. This maps each
   * supported event to its equivalent {@link DatabaseMigrationProgress}.
   */
  @VisibleForTesting
  static final Map<Event, DatabaseMigrationProgress.Stage> SUPPORTED_EVENTS_MAPPING =
      Map.of(
          Event.BEFORE_MIGRATE, DatabaseMigrationProgress.Stage.Preparing,
          Event.AFTER_MIGRATE, DatabaseMigrationProgress.Stage.Completed,
          Event.BEFORE_EACH_MIGRATE, DatabaseMigrationProgress.Stage.BeforeFile,
          Event.AFTER_EACH_MIGRATE, DatabaseMigrationProgress.Stage.AfterFile,
          Event.BEFORE_VALIDATE, DatabaseMigrationProgress.Stage.BeforeValidate,
          Event.AFTER_VALIDATE, DatabaseMigrationProgress.Stage.AfterValidate);

  /** Function to call with updates. */
  private final Consumer<DatabaseMigrationProgress> progressConsumer;

  /**
   * Converts the {@link Event} into a {@link DatabaseMigrationProgress} and passes that to the
   * callback function.
   *
   * <p>{@inheritDoc}
   *
   * @param event The event to handle.
   * @param context The context for this event.
   */
  @Override
  public void handle(Event event, Context context) {
    final var migrationInfo = context.getMigrationInfo();
    final var stage = SUPPORTED_EVENTS_MAPPING.get(event);
    if (stage == null) {
      throw new BadCodeMonkeyException("unsupported Event type: " + event);
    }
    final var progress = new DatabaseMigrationProgress(stage, migrationInfo);
    progressConsumer.accept(progress);
  }

  @Override
  public boolean supports(Event event, Context context) {
    return SUPPORTED_EVENTS_MAPPING.containsKey(event);
  }

  @Override
  public boolean canHandleInTransaction(Event event, Context context) {
    return true;
  }

  @Override
  public String getCallbackName() {
    return getClass().getName();
  }
}
