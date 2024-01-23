package gov.cms.bfd.pipeline.bridge.util;

import com.google.protobuf.MessageOrBuilder;
import lombok.Getter;
import lombok.Setter;

/**
 * Helper class for carrying claims between processing iterations so additional line items can be
 * added to the claim if any are found.
 */
@Getter
@Setter
public class WrappedMessage {

  /** The currently stored claim still being processed. */
  private MessageOrBuilder message;

  /** Tracks the last line number processed so sequence order integrity can be verified. */
  private long lineNumber = 0;
}
