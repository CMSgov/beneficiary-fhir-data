package gov.cms.bfd.sharedutils.exceptions;

import javax.xml.bind.JAXBException;

/**
 * Wraps a checked {@link JAXBException} in an unchecked {@link RuntimeException} derivative, so
 * that error handling can be deferred to elsewhere in the call stack, without requiring all of the
 * method signature noise that checked exceptions mandate.
 */
public final class UncheckedJaxbException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  /**
   * Constructs a new {@link UncheckedJaxbException}.
   *
   * @param cause the checked {@link JAXBException} that caused the exception
   */
  public UncheckedJaxbException(JAXBException cause) {
    super(cause);
  }
}
