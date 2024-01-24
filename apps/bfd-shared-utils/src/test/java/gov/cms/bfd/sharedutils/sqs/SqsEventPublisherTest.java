package gov.cms.bfd.sharedutils.sqs;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for {@link SqsEventPublisher}. */
@ExtendWith(MockitoExtension.class)
public class SqsEventPublisherTest {
  /** Used to simulate the SQS calls. */
  @Mock private SqsDao sqsDao;

  /** Reports progress and verifies correct JSON is transmitted to the queue. */
  @Test
  void reportProgressAndVerifyMessageText() {
    final var sample1 = new SampleBean1(42, "just testing", null);
    final var sample2 =
        new SampleBean1(
            87,
            "nested sample",
            new SampleBean2("child bean", SampleEnum.Started, List.of("row1", "row2")));
    final var queueUrl = "queue-url";
    final var publisher = spy(new SqsEventPublisher(sqsDao, queueUrl));
    publisher.publishEvent(sample1);
    publisher.publishEvent(sample2);
    verify(sqsDao).sendMessage(queueUrl, "{\"count\":42,\"text\":\"just testing\"}");
    verify(sqsDao)
        .sendMessage(
            queueUrl,
            "{\"child\":{\"contents\":[\"row1\",\"row2\"],\"status\":\"Started\",\"title\":\"child bean\"},\"count\":87,\"text\":\"nested sample\"}");
  }

  /** Sample enum used by tests. */
  private enum SampleEnum {
    /** Just a sample enum value. */
    Started,
    /** Just a sample enum value. */
    Completed
  }

  /**
   * Sample parent bean used by tests. Contains fields of various types to verify JSON conversion is
   * performed correctly.
   *
   * @param count an integer field
   * @param text a string field
   * @param child a nested bean field
   */
  private record SampleBean1(int count, String text, SampleBean2 child) {}

  /**
   * Sample child bean used by tests. Contains fields of various types to verify JSON converstion is
   * performed correctly.
   *
   * @param title a string field
   * @param status an enum field
   * @param contents a list field
   */
  private record SampleBean2(String title, SampleEnum status, List<String> contents) {}
}
