package gov.cms.bfd.pipeline.bridge.etl;

import com.google.protobuf.MessageOrBuilder;
import gov.cms.bfd.pipeline.bridge.util.DataSampler;
import gov.cms.bfd.pipeline.bridge.util.WrappedCounter;
import gov.cms.bfd.pipeline.bridge.util.WrappedMessage;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/** Abstract class for the transformer. */
public abstract class AbstractTransformer {

  /**
   * Abstract function for the transform method.
   *
   * <p>The implementation would parse the given {@link Parser.Data} into an associated claim object
   *
   * @param wrappedMessage the wrappedMessage
   * @param sequenceNumber the sequenceNumber
   * @param data the data
   * @param mbiSampler the samples of mbis
   * @param sampleId the samples of Ids
   * @param fileName the name of the file the claim came from
   * @return {@link MessageOrBuilder}
   */
  public abstract Optional<MessageOrBuilder> transform(
      WrappedMessage wrappedMessage,
      WrappedCounter sequenceNumber,
      Parser.Data<String> data,
      DataSampler<String> mbiSampler,
      int sampleId,
      String fileName);

  /**
   * Returns the computed result of {@link Supplier} if value is null, otherwise returns value.
   *
   * @param value The value to check against
   * @param action The {@link Supplier} for computing the fallback value if value is null
   * @param <T> The type of value being handled
   * @return Returns the computed result of the given {@link Supplier} if value was null, otherwise
   *     just returns value
   */
  public <T> T ifNull(T value, Supplier<T> action) {
    return ifNotNull(value, v -> v, action);
  }

  /**
   * Returns the computed result of the given {@link UnaryOperator} if value is not null, otherwise
   * returns the computed result of {@link Supplier}.
   *
   * @param value The value to check against
   * @param action The {@link UnaryOperator} for computing the result if value is not null
   * @param fallback The {@link Supplier} for computing the fallback result if value was null
   * @param <T> The type of value being handled
   * @return Returns the computed result of the given {@link UnaryOperator} if value is not null,
   *     otherwise returns the computed result of {@link Supplier}
   */
  public <T> T ifNotNull(T value, UnaryOperator<T> action, Supplier<T> fallback) {
    return value != null ? action.apply(value) : fallback.get();
  }

  /**
   * Runs the given value through the given {@link Consumer} if the value meets the condition of the
   * given {@link Predicate}, otherwise nothing happens.
   *
   * @param value The value to check against
   * @param condition The condition to check against
   * @param consumer The {@link Consumer} to run the given value through if the condition was true
   * @param <T> The type of value being processed
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
   * @param value The value to check against
   * @param consumer The {@link Consumer} to run the given value through if the condition was true
   * @param <T> The type of value being processed
   */
  public <T> void consumeIfNotNull(T value, Consumer<T> consumer) {
    consumeIf(value, Objects::nonNull, consumer);
  }

  /**
   * Gets the line number from the given {@link Parser.Data} object.
   *
   * @param data The {@link Parser.Data} object to pull the line number from
   * @param identifier The reference to use to pull the line number from the {@link Parser.Data}
   *     object
   * @return The line number pulled from the {@link Parser.Data} object
   */
  protected int getLineNumber(Parser.Data<String> data, String identifier) {
    int lineNumber;

    Optional<String> lineNumberString = data.get(identifier);

    if (lineNumberString.isPresent()) {
      try {
        lineNumber = Integer.parseInt(lineNumberString.get());
      } catch (NumberFormatException e) {
        throw new IllegalStateException(
            "(entry "
                + data.getEntryNumber()
                + ") Line number expected to be a valid numeric value");
      }
    } else {
      throw new IllegalStateException(
          "(entry " + data.getEntryNumber() + ") Line number expected to be a valid numeric value");
    }

    return lineNumber;
  }
}
