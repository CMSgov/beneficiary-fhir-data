package gov.cms.bfd.pipeline.bridge.util;

import com.google.protobuf.MessageOrBuilder;

public class WrappedMessage {

  private MessageOrBuilder message;
  private long lineNumber = 0;

  public MessageOrBuilder getMessage() {
    return message;
  }

  public void setMessage(MessageOrBuilder message) {
    this.message = message;
  }

  public long getLineNumber() {
    return lineNumber;
  }

  public void setLineNumber(long lineNumber) {
    this.lineNumber = lineNumber;
  }
}
