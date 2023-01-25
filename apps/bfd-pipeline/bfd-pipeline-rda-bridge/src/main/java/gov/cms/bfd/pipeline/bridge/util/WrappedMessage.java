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
   * This method gets the message out of the MessageOrBuilder.
   *
   * @return {@link MessageOrBuilder} returns the message.
   */
  public MessageOrBuilder getMessage() {
    return message;
  }

  /**
   * This method sets the message in the MessageOrBuilder.
   *
   * @param message sets the message from {@link MessageOrBuilder}.
   */
  public void setMessage(MessageOrBuilder message) {
    this.message = message;
  }

  /**
   * * This method gets the lineNumber.
   *
   * @return lineNumber sets the line number from {@link long}.
   */
  public long getLineNumber() {
    return lineNumber;
  }

  /**
   * This method sets the lineNumber.
   *
   * @param lineNumber sets the line number from {@link long}.
   */
  public void setLineNumber(long lineNumber) {
    this.lineNumber = lineNumber;
  }
}
