package gov.cms.bfd.sharedutils.database;

import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import java.util.Set;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.flywaydb.core.api.callback.Callback;
import org.flywaydb.core.api.callback.Context;
import org.flywaydb.core.api.callback.Event;

@AllArgsConstructor
public class FlywayProgressCallback implements Callback {
  private static final Set<Event> SUPPORTED_EVENTS =
      Set.of(
          Event.BEFORE_MIGRATE,
          Event.AFTER_MIGRATE,
          Event.BEFORE_EACH_MIGRATE,
          Event.AFTER_EACH_MIGRATE,
          Event.BEFORE_VALIDATE,
          Event.AFTER_VALIDATE);

  private final Consumer<DatabaseMigrationStage> progressConsumer;

  @Override
  public boolean supports(Event event, Context context) {
    return SUPPORTED_EVENTS.contains(event);
  }

  @Override
  public boolean canHandleInTransaction(Event event, Context context) {
    return true;
  }

  @Override
  public void handle(Event event, Context context) {
    var progress = createProgress(event, context);
    progressConsumer.accept(progress);
  }

  DatabaseMigrationStage createProgress(Event event, Context context) {
    return switch (event) {
      case BEFORE_MIGRATE -> new DatabaseMigrationStage(DatabaseMigrationStage.Stage.Preparing, "");
      case AFTER_MIGRATE -> new DatabaseMigrationStage(DatabaseMigrationStage.Stage.Finished, "");
      case BEFORE_EACH_MIGRATE -> new DatabaseMigrationStage(
          DatabaseMigrationStage.Stage.BeforeFile, context.getMigrationInfo().getScript());
      case AFTER_EACH_MIGRATE -> new DatabaseMigrationStage(
          DatabaseMigrationStage.Stage.AfterFile, context.getMigrationInfo().getScript());
      case BEFORE_VALIDATE -> new DatabaseMigrationStage(
          DatabaseMigrationStage.Stage.BeforeValidate, "");
      case AFTER_VALIDATE -> new DatabaseMigrationStage(
          DatabaseMigrationStage.Stage.AfterValidate, "");
      default -> throw new BadCodeMonkeyException("unsupported Event type: " + event);
    };
  }

  @Override
  public String getCallbackName() {
    return getClass().getName();
  }
}
