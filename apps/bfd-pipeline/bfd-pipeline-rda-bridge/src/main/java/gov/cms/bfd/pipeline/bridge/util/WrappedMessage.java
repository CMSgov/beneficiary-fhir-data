package gov.cms.bfd.pipeline.bridge.util;

import com.google.protobuf.MessageOrBuilder;

/**
 * Helper class for carrying claims between processing iterations so additional line items can be
 * added to the claim if any are found.
 */
public class WrappedMessage {

  /** The currently stored claim still being processed. */
  private MessageOrBuilder message;
  /** Tracks the last line number processed so sequence order integrity can be verified. */
  private long lineNumber = 0;

  /**
   * Gets the {@link #message}.
   *
   * @return the message
   */
  public MessageOrBuilder getMessage() {
    return message;
  }

  /**
   * Sets the {@link #message}.
   *
   * @param message sets the message
   */
  public void setMessage(MessageOrBuilder message) {
    this.message = message;
  }

  /**
   * Gets the {@link #lineNumber}.
   *
   * @return the line number
   */
  public long getLineNumber() {
    return lineNumber;
  }

  /**
   * Sets the {@link #lineNumber}.
   *
   * @param lineNumber sets the line number
   */
  public void setLineNumber(long lineNumber) {
    this.lineNumber = lineNumber;
  }
}
