package gov.cms.bfd.model.codebook.extractor;

import java.util.Objects;
import javax.lang.model.element.Element;

/**
 * This unchecked exception type indicates that an unrecoverable error was encountered while running
 * {@link CodebookVariablesEnumProcessor}.
 */
public final class RifLayoutProcessingException extends RuntimeException {
  private static final long serialVersionUID = -7030975478260105688L;

  /** The field or package that was the source of the exception. */
  private final Element element;

  /**
   * Constructs a new {@link RifLayoutProcessingException}.
   *
   * @param element the value to use for {@link #getElement()}
   * @param message the format {@link String} for {@link #getMessage()}
   * @param args the format arguments for {@link #getMessage()}
   */
  public RifLayoutProcessingException(Element element, String message, Object... args) {
    super(String.format(message, args));

    Objects.requireNonNull(element);

    this.element = element;
  }

  /**
   * Gets the element.
   *
   * @return the element
   */
  public Element getElement() {
    return element;
  }
}
