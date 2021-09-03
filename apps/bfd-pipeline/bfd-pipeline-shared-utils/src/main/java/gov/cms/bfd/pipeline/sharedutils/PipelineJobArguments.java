package gov.cms.bfd.pipeline.sharedutils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.UncheckedIOException;

/**
 * Implementations of this interface represent the arguments that can be passed to non-scheduled
 * executions of a {@link PipelineJob} implementation.
 */
public interface PipelineJobArguments {
  static ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  /**
   * @return a full-fidelity, round-trippable representation of these {@link PipelineJobArguments},
   *     serialized as a {@link String}
   */
  default String serialize() {
    try {
      return OBJECT_MAPPER.writeValueAsString(this);
    } catch (JsonProcessingException e) {
      throw new UncheckedIOException(e);
    }
  }

  /**
   * @param <A> the {@link Class} of this {@link PipelineJobArguments} implementation
   * @param serializedArguments a serialized representation of an instance of this {@link
   *     PipelineJobArguments} implementation, as produced by {@link #serialize()}
   * @param argumentsClass the {@link Class} of this {@link PipelineJobArguments} implementation
   * @return the deserialized {@link PipelineJobArguments} represented by the specified {@link
   *     String}
   */
  default <A extends PipelineJobArguments> PipelineJobArguments deserialize(
      String serializedArguments, Class<A> argumentsClass) {
    try {
      return OBJECT_MAPPER.readValue(serializedArguments, argumentsClass);
    } catch (JsonProcessingException e) {
      throw new UncheckedIOException(e);
    }
  }
}
