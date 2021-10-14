package gov.cms.bfd.sharedutils.generators.exceptions;

/** Thrown when a parsing specific error was encountered. */
public class ParsingException extends RuntimeException {

  public ParsingException(String message) {
    super(message);
  }
}
