package gov.cms.bfd.datadictionary.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import gov.cms.bfd.datadictionary.model.FhirElement;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;

/** Spiterator used by the FhirElementStream. */
public class FhirElementSpliterator implements Spliterator<FhirElement> {

  /** ObjectMapper to deserialize the JSON files. */
  private static final ObjectMapper objectMapper = JsonMapper.builder().build();

  /** Ordered list of files. */
  private final List<File> files;

  /** Current index into the list of files. */
  private int currentIndex;

  /**
   * Constructs a FhirElementSpliterator from a list of files.
   *
   * @param files the list of files to iterate over
   */
  public FhirElementSpliterator(List<File> files) {
    this.files = files;
  }

  /**
   * Deserializes a FhirElement from the current file and invokes the consumer function.
   *
   * @param consumer a consumer function that takes a FhirElement
   * @return true if can advance and false otherwise
   */
  @Override
  public boolean tryAdvance(Consumer<? super FhirElement> consumer) {
    if (currentIndex < files.size()) {
      try {
        FhirElement element = objectMapper.readValue(files.get(currentIndex), FhirElement.class);
        consumer.accept(element);
        currentIndex++;
      } catch (IOException e) {
        throw new RuntimeException(
            String.format(
                "Error deserializing FhirElement from file %s", files.get(currentIndex).getName()),
            e);
      }
      return true;
    }
    return false;
  }

  /**
   * Not supported.
   *
   * @return null
   */
  @Override
  public Spliterator<FhirElement> trySplit() {
    // not supported
    return null;
  }

  /**
   * Retrieves the count of remaining elements in the stream.
   *
   * @return count of the remaining elements.
   */
  @Override
  public long estimateSize() {
    return files.size() - currentIndex;
  }

  /**
   * Retrieves the characteristics of this stream.
   *
   * @return the characteristics
   * @see <a
   *     href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/Spliterator.html">java.util.Spliterator</a>
   */
  @Override
  public int characteristics() {
    return ORDERED | SIZED | NONNULL;
  }
}
