package gov.cms.bfd.pipeline.rda.grpc.sink.concurrent;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link BlockingPublisher}. */
public class BlockingPublisherTest {
  /**
   * Verifies that all values submitted to be published are actually published.
   *
   * @throws InterruptedException pass through if thrown during test
   */
  @Test
  void shouldPublishAllValues() throws InterruptedException {
    var publisher = new BlockingPublisher<Integer>(100);

    assertEquals(100, publisher.getAvailablePermits());
    var expected = new ArrayList<Integer>();
    for (int i = 1; i <= 100; ++i) {
      publisher.emit(i);
      expected.add(i);
    }
    assertEquals(0, publisher.getAvailablePermits());
    publisher.complete();

    var published = publisher.flux().collectList().block();
    assertEquals(expected, published);
  }

  /**
   * Verifies that calling {@link BlockingPublisher#allow} increases the number of permits available
   * and calling {@link BlockingPublisher#emit} decreases them.
   *
   * @throws InterruptedException pass through if thrown during test
   */
  @Test
  void shouldAdjustPermitsAsExpected() throws InterruptedException {
    var publisher = new BlockingPublisher<Integer>(100);

    publisher.emit(3);
    assertEquals(99, publisher.getAvailablePermits());

    publisher.emit(3);
    assertEquals(98, publisher.getAvailablePermits());

    publisher.allow(2);
    assertEquals(100, publisher.getAvailablePermits());
  }
}
