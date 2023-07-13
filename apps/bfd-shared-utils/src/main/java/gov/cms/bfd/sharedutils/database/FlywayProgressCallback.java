package gov.cms.bfd.sharedutils.database;

import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import java.util.Set;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.callback.Callback;
import org.flywaydb.core.api.callback.Context;
import org.flywaydb.core.api.callback.Event;

/**
 * Implementation of {@link Callback} that converts flyway {@link Event} objects into {@link
 * DatabaseMigrationStage} objects and passes them to a callback function. Used by the migrator app
 * to send updates to an SQS queue or a log file during processing.
 */
@AllArgsConstructor
public class FlywayProgressCallback implements Callback {
  /** We only care about events that we report on. All others will be ignored. */
  private static final Set<Event> SUPPORTED_EVENTS =
      Set.of(
          Event.BEFORE_MIGRATE,
          Event.AFTER_MIGRATE,
          Event.BEFORE_EACH_MIGRATE,
          Event.AFTER_EACH_MIGRATE,
          Event.BEFORE_VALIDATE,
          Event.AFTER_VALIDATE);

  /** Function to call with updates. */
  private final Consumer<DatabaseMigrationStage> progressConsumer;

  /**
   * Converts the {@link Event} into a {@link DatabaseMigrationStage} and passes that to the
   * callback function.
   *
   * <p>{@inheritDoc}
   *
   * @param event The event to handle.
   * @param context The context for this event.
   */
  @Override
  public void handle(Event event, Context context) {
    var progress = createProgress(event, context);
    progressConsumer.accept(progress);
  }

  @Override
  public boolean supports(Event event, Context context) {
    return SUPPORTED_EVENTS.contains(event);
  }

  @Override
  public boolean canHandleInTransaction(Event event, Context context) {
    return true;
  }

  @Override
  public String getCallbackName() {
    return getClass().getName();
  }

  /**
   * Converts the {@link Event} into a {@link DatabaseMigrationStage}.
   *
   * @param event The event to handle.
   * @param context The context for this event.
   * @return the converted object
   */
  DatabaseMigrationStage createProgress(Event event, Context context) {
    final MigrationInfo migrationInfo = context.getMigrationInfo();
    return switch (event) {
      case BEFORE_MIGRATE -> new DatabaseMigrationStage(
          DatabaseMigrationStage.Stage.Preparing, migrationInfo);
      case AFTER_MIGRATE -> new DatabaseMigrationStage(
          DatabaseMigrationStage.Stage.Completed, migrationInfo);
      case BEFORE_EACH_MIGRATE -> new DatabaseMigrationStage(
          DatabaseMigrationStage.Stage.BeforeFile, migrationInfo);
      case AFTER_EACH_MIGRATE -> new DatabaseMigrationStage(
          DatabaseMigrationStage.Stage.AfterFile, migrationInfo);
      case BEFORE_VALIDATE -> new DatabaseMigrationStage(
          DatabaseMigrationStage.Stage.BeforeValidate, migrationInfo);
      case AFTER_VALIDATE -> new DatabaseMigrationStage(
          DatabaseMigrationStage.Stage.AfterValidate, migrationInfo);
      default -> throw new BadCodeMonkeyException("unsupported Event type: " + event);
    };
  }
}
