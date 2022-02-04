package gov.cms.bfd.pipeline.bridge.etl;

import com.google.protobuf.MessageOrBuilder;
import gov.cms.bfd.pipeline.bridge.util.WrappedCounter;
import gov.cms.bfd.pipeline.bridge.util.WrappedMessage;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public abstract class AbstractTransformer {

  public abstract Optional<MessageOrBuilder> transform(
      WrappedCounter sequenceNumber, Parser.Data<String> data, WrappedMessage wrappedMessage);

  /**
   * Returns the computed result of {@link Supplier} if value is null, otherwise returns value.
   *
   * @param value The value to check against
   * @param action The {@link Supplier} for computing the fallback value if value is null.
   * @param <T> The type of value being handled
   * @return Returns the computed result of the given {@link Supplier} if value was null, otherwise
   *     just returns value.
   */
  public <T> T ifNull(T value, Supplier<T> action) {
    return ifNotNull(value, v -> v, action);
  }

  /**
   * Returns the computed result of the given {@link UnaryOperator} if the given value is not null,
   * otherwise returns null.
   *
   * @param value The value to check against
   * @param action The {@link UnaryOperator} for computing the result if value is not null.
   * @param <T> The type of value being handled
   * @return Returns the computed result of the given {@link UnaryOperator} if the given value is
   *     not null, otherwise returns null.
   */
  public <T> T ifNotNull(T value, UnaryOperator<T> action) {
    return ifNotNull(value, action, () -> null);
  }

  /**
   * Returns the computed result of the given {@link UnaryOperator} if value is not null, otherwise
   * returns the computed result of {@link Supplier}.
   *
   * @param value The value to check against.
   * @param action The {@link UnaryOperator} for computing the result if value is not null.
   * @param fallback The {@link Supplier} for computing the fallback result if value was null.
   * @param <T> The type of value being handled
   * @return Returns the computed result of the given {@link UnaryOperator} if value is not null,
   *     otherwise returns the computed result of {@link Supplier}.
   */
  public <T> T ifNotNull(T value, UnaryOperator<T> action, Supplier<T> fallback) {
    return value != null ? action.apply(value) : fallback.get();
  }

  /**
   * Runs the given value through the given {@link Consumer} if the value meets the condition of the
   * given {@link Predicate}, otherwise nothing happens.
   *
   * @param value The value to check against.
   * @param condition The condition to check against.
   * @param consumer The {@link Consumer} to run the given value through if the condition was true.
   * @param <T> The type of value being processed.
   */
  public <T> void consumeIf(T value, Predicate<T> condition, Consumer<T> consumer) {
    if (condition.test(value)) {
      consumer.accept(value);
    }
  }

  /**
   * Runs the given value through the given {@link Consumer} if the value is not null, otherwise
   * nothing happens.
   *
   * @param value The value to check against.
   * @param consumer The {@link Consumer} to run the given value through if the condition was true.
   * @param <T> The type of value being processed.
   */
  public <T> void consumeIfNotNull(T value, Consumer<T> consumer) {
    consumeIf(value, Objects::nonNull, consumer);
  }
}
