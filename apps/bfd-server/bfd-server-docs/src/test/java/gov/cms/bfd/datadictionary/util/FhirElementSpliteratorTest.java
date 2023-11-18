package gov.cms.bfd.datadictionary.util;

import static java.util.Spliterator.NONNULL;
import static java.util.Spliterator.ORDERED;
import static java.util.Spliterator.SIZED;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Test class for the FhirElementSpliterator. */
class FhirElementSpliteratorTest {

  /** FhirElementSpliterator under test. */
  FhirElementSpliterator spliterator;

  /** Test setup for each test case. */
  @BeforeEach
  void setup() {
    ClassLoader classLoader = FhirElementStream.class.getClassLoader();
    var url = classLoader.getResource("dd/data");
    assert url != null;
    var dir = new File(url.getPath());
    var files = dir.listFiles();
    assert files != null;
    Arrays.sort(files, Comparator.comparing(File::getName));
    List<File> fileList = Arrays.stream(files).toList();
    spliterator = new FhirElementSpliterator(fileList);
  }

  /** Test the tryAdvance method. */
  @Test
  void tryAdvance() {
    // assert three elements yielded
    assertTrue(spliterator.tryAdvance(Assertions::assertNotNull));
    assertTrue(spliterator.tryAdvance(Assertions::assertNotNull));
    assertTrue(spliterator.tryAdvance(Assertions::assertNotNull));
    assertFalse(spliterator.tryAdvance(fe -> {}));
  }

  /** Verify that the trySplit method returns null. */
  @Test
  void trySplit() {
    assertNull(spliterator.trySplit());
  }

  /** Test that the estimateSize method is correct. */
  @Test
  void estimateSize() {
    assertEquals(3, spliterator.estimateSize());
  }

  /** Test that the proper characteristics are returned. */
  @Test
  void characteristics() {
    assertEquals(ORDERED | SIZED | NONNULL, spliterator.characteristics());
  }
}
