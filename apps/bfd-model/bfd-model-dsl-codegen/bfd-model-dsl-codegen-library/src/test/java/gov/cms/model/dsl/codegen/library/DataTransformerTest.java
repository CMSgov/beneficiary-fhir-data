package gov.cms.model.dsl.codegen.library;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import com.google.common.collect.ImmutableList;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests proper operation of the {@link DataTransformer} class. */
public class DataTransformerTest {
  /** The transformer under test. */
  private DataTransformer transformer;
  /** The copied data to test. */
  private List<Object> copied;

  /** Sets the test up. */
  @BeforeEach
  public void setUp() {
    transformer = new DataTransformer();
    copied = new ArrayList<>();
  }

  /** Tests the {@link DataTransformer#validateAtLeastOneIsPresent} method. */
  @Test
  public void testValidateAtLeastOneIsPresent() {
    assertEquals(
        true, transformer.validateAtLeastOneIsPresent("ok-first-1", "1", "ok-second-1", ""));
    assertEquals(
        true, transformer.validateAtLeastOneIsPresent("ok-first-2", "", "ok-second-2", "2"));
    assertEquals(
        true, transformer.validateAtLeastOneIsPresent("ok-first-3", "1", "ok-second-3", null));
    assertEquals(
        true, transformer.validateAtLeastOneIsPresent("ok-first-4", null, "ok-second-4", "2"));
    assertEquals(
        true, transformer.validateAtLeastOneIsPresent("ok-first-5", "a", "ok-second-5", "b"));
    assertEquals(
        false,
        transformer.validateAtLeastOneIsPresent("neither-first-1", "", "neither-second-1", ""));
    assertEquals(
        false,
        transformer.validateAtLeastOneIsPresent("neither-first-2", null, "neither-second-2", ""));
    assertEquals(
        false,
        transformer.validateAtLeastOneIsPresent("neither-first-3", "", "neither-second-3", null));
    assertEquals(
        false,
        transformer.validateAtLeastOneIsPresent("neither-first-4", null, "neither-second-4", null));
    assertEquals(
        ImmutableList.of(
            new DataTransformer.ErrorMessage(
                "neither-first-1",
                "expected either neither-first-1 or neither-second-1 to have value but neither did"),
            new DataTransformer.ErrorMessage(
                "neither-first-2",
                "expected either neither-first-2 or neither-second-2 to have value but neither did"),
            new DataTransformer.ErrorMessage(
                "neither-first-3",
                "expected either neither-first-3 or neither-second-3 to have value but neither did"),
            new DataTransformer.ErrorMessage(
                "neither-first-4",
                "expected either neither-first-4 or neither-second-4 to have value but neither did")),
        transformer.getErrors());
  }

  /** Tests the {@link DataTransformer#copyString} method. */
  @Test
  public void testCopyString() {
    transformer
        .copyString("length-one-ok", false, 1, 5, "1", copied::add)
        .copyString("length-five-ok", false, 1, 5, "12345", copied::add)
        .copyString("length-below-min", false, 2, 5, "1", copied::add)
        .copyString("length-above-max", false, 1, 5, "123456", copied::add)
        .copyString("null-ok", true, 1, 5, null, copied::add)
        .copyString("null-bad", false, 1, 5, null, copied::add);

    assertEquals(ImmutableList.of("1", "12345"), copied);
    assertEquals(
        ImmutableList.of(
            new DataTransformer.ErrorMessage(
                "length-below-min", "invalid length: expected=[2,5] actual=1"),
            new DataTransformer.ErrorMessage(
                "length-above-max", "invalid length: expected=[1,5] actual=6"),
            new DataTransformer.ErrorMessage("null-bad", "is null")),
        transformer.getErrors());
  }

  /** Tests the {@link DataTransformer#copyOptionalString} method. */
  @Test
  public void testCopyOptionalString() {
    transformer
        .copyOptionalString("value-not-present", 1, 5, () -> false, () -> null, copied::add)
        .copyOptionalString("null-value-present", 1, 5, () -> true, () -> null, copied::add)
        .copyOptionalString("empty-value-present", 1, 5, () -> true, () -> "", copied::add)
        .copyOptionalString("non-empty-value-present", 1, 5, () -> true, () -> "A", copied::add)
        .copyOptionalString("length-below-min", 2, 5, () -> true, () -> "1", copied::add)
        .copyOptionalString("length-above-max", 2, 5, () -> true, () -> "123456", copied::add);

    assertEquals(ImmutableList.of("A"), copied);
    assertEquals(
        ImmutableList.of(
            new DataTransformer.ErrorMessage("null-value-present", "is null"),
            new DataTransformer.ErrorMessage(
                "empty-value-present", "invalid length: expected=[1,5] actual=0"),
            new DataTransformer.ErrorMessage(
                "length-below-min", "invalid length: expected=[2,5] actual=1"),
            new DataTransformer.ErrorMessage(
                "length-above-max", "invalid length: expected=[2,5] actual=6")),
        transformer.getErrors());
  }

  /** Tests the {@link DataTransformer#copyOptionalNonEmptyString} method. */
  @Test
  public void testCopyOptionalNonEmptyString() {
    transformer
        .copyOptionalNonEmptyString("value-not-present", 1, 5, () -> false, () -> null, copied::add)
        .copyOptionalNonEmptyString("null-value-present", 1, 5, () -> true, () -> null, copied::add)
        .copyOptionalNonEmptyString("empty-value-present", 1, 5, () -> true, () -> "", copied::add)
        .copyOptionalNonEmptyString(
            "non-empty-value-present", 1, 5, () -> true, () -> "A", copied::add)
        .copyOptionalNonEmptyString("length-below-min", 2, 5, () -> true, () -> "1", copied::add)
        .copyOptionalNonEmptyString(
            "length-above-max", 2, 5, () -> true, () -> "123456", copied::add);

    assertEquals(ImmutableList.of("A"), copied);
    assertEquals(
        ImmutableList.of(
            new DataTransformer.ErrorMessage(
                "length-below-min", "invalid length: expected=[2,5] actual=1"),
            new DataTransformer.ErrorMessage(
                "length-above-max", "invalid length: expected=[2,5] actual=6")),
        transformer.getErrors());
  }

  /** Tests the {@link DataTransformer#copyCharacter} method. */
  @Test
  public void testCopyCharacter() {
    transformer
        .copyCharacter("length-one-ok", "1", copied::add)
        .copyCharacter("length-below-min", "", copied::add)
        .copyCharacter("length-above-max", "12", copied::add)
        .copyCharacter("length-one-ok", "A", copied::add);
    assertEquals(ImmutableList.of('1', 'A'), copied);
    assertEquals(
        ImmutableList.of(
            new DataTransformer.ErrorMessage(
                "length-below-min", "invalid length: expected=[1,1] actual=0"),
            new DataTransformer.ErrorMessage(
                "length-above-max", "invalid length: expected=[1,1] actual=2")),
        transformer.getErrors());
  }

  /** Tests the {@link DataTransformer#copyOptionalCharacter} method. */
  @Test
  public void testCopyOptionalCharacter() {
    transformer
        .copyOptionalCharacter("not-present", () -> false, () -> "1", copied::add)
        .copyOptionalCharacter("length-one-ok", () -> true, () -> "1", copied::add)
        .copyOptionalCharacter("length-below-min", () -> true, () -> "", copied::add)
        .copyOptionalCharacter("length-above-max", () -> true, () -> "12", copied::add)
        .copyOptionalCharacter("length-one-ok", () -> true, () -> "A", copied::add);
    assertEquals(ImmutableList.of('1', 'A'), copied);
    assertEquals(
        ImmutableList.of(
            new DataTransformer.ErrorMessage(
                "length-below-min", "invalid length: expected=[1,1] actual=0"),
            new DataTransformer.ErrorMessage(
                "length-above-max", "invalid length: expected=[1,1] actual=2")),
        transformer.getErrors());
  }

  /** Tests the {@link DataTransformer#copyDate} method. */
  @Test
  public void testCopyDate() {
    transformer
        .copyDate("valid-1", false, "2021-03-01", copied::add)
        .copyDate("invalid-1", true, "2021/03/01", copied::add)
        .copyDate("invalid-2", true, "2021-06-19T18:24:30", copied::add)
        .copyDate("valid-2", true, "2021-10-21", copied::add)
        .copyDate("valid-rif-8", true, "20210805", copied::add)
        .copyDate("valid-rif-11", true, "07-AUG-2021", copied::add)
        .copyDate("null-ok", true, null, copied::add)
        .copyDate("null-bad", false, null, copied::add);
    assertEquals(
        ImmutableList.of(
            LocalDate.of(2021, 3, 1),
            LocalDate.of(2021, 10, 21),
            LocalDate.of(2021, 8, 5),
            LocalDate.of(2021, 8, 7)),
        copied);
    assertEquals(
        ImmutableList.of(
            new DataTransformer.ErrorMessage("invalid-1", "invalid date"),
            new DataTransformer.ErrorMessage("invalid-2", "invalid date"),
            new DataTransformer.ErrorMessage("null-bad", "is null")),
        transformer.getErrors());
  }

  /** Tests the {@link DataTransformer#copyOptionalDate} method. */
  @Test
  public void testCopyOptionalDate() {
    transformer
        .copyOptionalDate("not-present", () -> false, () -> "2021-03-01", copied::add)
        .copyOptionalDate("valid-1", () -> true, () -> "2021-03-01", copied::add)
        .copyOptionalDate("invalid-1", () -> true, () -> "2021/03/01", copied::add)
        .copyOptionalDate("invalid-2", () -> true, () -> "2021-06-19T18:24:30", copied::add)
        .copyOptionalDate("valid-2", () -> true, () -> "2021-10-21", copied::add)
        .copyOptionalDate("valid-rif-8", () -> true, () -> "20210805", copied::add)
        .copyOptionalDate("valid-rif-11", () -> true, () -> "07-AUG-2021", copied::add)
        .copyOptionalDate("null-bad", () -> true, () -> null, copied::add);
    assertEquals(
        ImmutableList.of(
            LocalDate.of(2021, 3, 1),
            LocalDate.of(2021, 10, 21),
            LocalDate.of(2021, 8, 5),
            LocalDate.of(2021, 8, 7)),
        copied);
    assertEquals(
        ImmutableList.of(
            new DataTransformer.ErrorMessage("invalid-1", "invalid date"),
            new DataTransformer.ErrorMessage("invalid-2", "invalid date"),
            new DataTransformer.ErrorMessage("null-bad", "is null")),
        transformer.getErrors());
  }

  /** Tests the {@link DataTransformer#copyIntString} method. */
  @Test
  public void testCopyIntString() {
    transformer
        .copyIntString("valid-1", false, "123", copied::add)
        .copyIntString("invalid-1", true, "not a number", copied::add)
        .copyIntString("invalid-2", true, "123a", copied::add)
        .copyIntString("valid-2", true, "-456", copied::add)
        .copyIntString("null-ok", true, null, copied::add)
        .copyIntString("null-bad", false, null, copied::add);
    assertEquals(ImmutableList.of(123, -456), copied);
    assertEquals(
        ImmutableList.of(
            new DataTransformer.ErrorMessage("invalid-1", "invalid integer"),
            new DataTransformer.ErrorMessage("invalid-2", "invalid integer"),
            new DataTransformer.ErrorMessage("null-bad", "is null")),
        transformer.getErrors());
  }

  /** Tests the {@link DataTransformer#copyOptionalIntString} method. */
  @Test
  public void testCopyOptionalIntString() {
    transformer
        .copyOptionalIntString("not-present", () -> false, () -> "99", copied::add)
        .copyOptionalIntString("valid-1", () -> true, () -> "123", copied::add)
        .copyOptionalIntString("invalid-1", () -> true, () -> "not a number", copied::add)
        .copyOptionalIntString("invalid-2", () -> true, () -> "123a", copied::add)
        .copyOptionalIntString("valid-2", () -> true, () -> "-456", copied::add)
        .copyOptionalIntString("null-bad", () -> true, () -> null, copied::add);
    assertEquals(ImmutableList.of(123, -456), copied);
    assertEquals(
        ImmutableList.of(
            new DataTransformer.ErrorMessage("invalid-1", "invalid integer"),
            new DataTransformer.ErrorMessage("invalid-2", "invalid integer"),
            new DataTransformer.ErrorMessage("null-bad", "is null")),
        transformer.getErrors());
  }

  /** Tests the {@link DataTransformer#copyShortString} method. */
  @Test
  public void testCopyShortString() {
    transformer
        .copyShortString("valid-1", false, "123", copied::add)
        .copyShortString("invalid-1", true, "not a number", copied::add)
        .copyShortString("invalid-2", true, "123a", copied::add)
        .copyShortString(
            "invalid-3", true, String.valueOf(((int) Short.MAX_VALUE) + 1), copied::add)
        .copyShortString(
            "invalid-4", true, String.valueOf(((int) Short.MIN_VALUE) - 1), copied::add)
        .copyShortString("valid-2", true, "-456", copied::add)
        .copyShortString("null-ok", true, null, copied::add)
        .copyShortString("null-bad", false, null, copied::add);
    assertEquals(ImmutableList.of((short) 123, (short) -456), copied);
    assertEquals(
        ImmutableList.of(
            new DataTransformer.ErrorMessage("invalid-1", "invalid short"),
            new DataTransformer.ErrorMessage("invalid-2", "invalid short"),
            new DataTransformer.ErrorMessage("invalid-3", "invalid short"),
            new DataTransformer.ErrorMessage("invalid-4", "invalid short"),
            new DataTransformer.ErrorMessage("null-bad", "is null")),
        transformer.getErrors());
  }

  /** Tests the {@link DataTransformer#copyOptionalShortString} method. */
  @Test
  public void testCopyOptionalShortString() {
    transformer
        .copyOptionalShortString("not-present", () -> false, () -> "99", copied::add)
        .copyOptionalShortString("valid-1", () -> true, () -> "123", copied::add)
        .copyOptionalShortString("invalid-1", () -> true, () -> "not a number", copied::add)
        .copyOptionalShortString("invalid-2", () -> true, () -> "123a", copied::add)
        .copyOptionalShortString(
            "invalid-3", () -> true, () -> String.valueOf(((int) Short.MAX_VALUE) + 1), copied::add)
        .copyOptionalShortString(
            "invalid-4", () -> true, () -> String.valueOf(((int) Short.MIN_VALUE) - 1), copied::add)
        .copyOptionalShortString("valid-2", () -> true, () -> "-456", copied::add)
        .copyOptionalShortString("null-bad", () -> true, () -> null, copied::add);
    assertEquals(ImmutableList.of((short) 123, (short) -456), copied);
    assertEquals(
        ImmutableList.of(
            new DataTransformer.ErrorMessage("invalid-1", "invalid short"),
            new DataTransformer.ErrorMessage("invalid-2", "invalid short"),
            new DataTransformer.ErrorMessage("invalid-3", "invalid short"),
            new DataTransformer.ErrorMessage("invalid-4", "invalid short"),
            new DataTransformer.ErrorMessage("null-bad", "is null")),
        transformer.getErrors());
  }

  /** Tests the {@link DataTransformer#copyLongString} method. */
  @Test
  public void testCopyLongString() {
    transformer
        .copyLongString("valid-1", false, "123", copied::add)
        .copyLongString("invalid-1", true, "not a number", copied::add)
        .copyLongString("invalid-2", true, "123a", copied::add)
        .copyLongString("valid-2", true, "-456", copied::add)
        .copyLongString("null-ok", true, null, copied::add)
        .copyLongString("null-bad", false, null, copied::add);
    assertEquals(ImmutableList.of(123L, -456L), copied);
    assertEquals(
        ImmutableList.of(
            new DataTransformer.ErrorMessage("invalid-1", "invalid long"),
            new DataTransformer.ErrorMessage("invalid-2", "invalid long"),
            new DataTransformer.ErrorMessage("null-bad", "is null")),
        transformer.getErrors());
  }

  /** Tests the {@link DataTransformer#copyOptionalLongString} method. */
  @Test
  public void testCopyOptionalLongString() {
    transformer
        .copyOptionalLongString("not-present", () -> false, () -> "99", copied::add)
        .copyOptionalLongString("valid-1", () -> true, () -> "123", copied::add)
        .copyOptionalLongString("invalid-1", () -> true, () -> "not a number", copied::add)
        .copyOptionalLongString("invalid-2", () -> true, () -> "123a", copied::add)
        .copyOptionalLongString("valid-2", () -> true, () -> "-456", copied::add)
        .copyOptionalLongString("null-bad", () -> true, () -> null, copied::add);
    assertEquals(ImmutableList.of(123L, -456L), copied);
    assertEquals(
        ImmutableList.of(
            new DataTransformer.ErrorMessage("invalid-1", "invalid long"),
            new DataTransformer.ErrorMessage("invalid-2", "invalid long"),
            new DataTransformer.ErrorMessage("null-bad", "is null")),
        transformer.getErrors());
  }

  /**
   * Tests the {@link DataTransformer#copyBase64String(String, boolean, int, int, String, Consumer)}
   * method.
   */
  @Test
  public void testCopyBase64() {
    transformer
        .copyBase64String("not-present-required", false, 1, 43, null, copied::add)
        .copyBase64String("not-present-nullable", true, 1, 43, null, copied::add)
        .copyBase64String(
            "present-required", false, 1, 43, "a longer decoded string value  1", copied::add)
        .copyBase64String("present-nullable", true, 1, 43, "decoded string", copied::add);
    assertEquals(
        ImmutableList.of("YSBsb25nZXIgZGVjb2RlZCBzdHJpbmcgdmFsdWUgIDE", "ZGVjb2RlZCBzdHJpbmc"),
        copied);
    assertEquals(
        ImmutableList.of(new DataTransformer.ErrorMessage("not-present-required", "is null")),
        transformer.getErrors());
  }

  /** Tests the {@link DataTransformer#copyTimestamp(String, boolean, String, Consumer)} method. */
  @Test
  public void testCopyTimestamp() {
    final String VALID_ONE = "2021-03-01T01:01:01.1Z";
    final String VALID_TWO = "2021-03-01T01:01:01.111111Z";
    final String VALID_THREE = "2021-03-01T01:01:01.1+00:00";

    transformer
        .copyTimestamp("valid-1", false, VALID_ONE, copied::add)
        .copyTimestamp("valid-2", true, VALID_TWO, copied::add)
        .copyTimestamp("valid-3", true, VALID_THREE, copied::add)
        .copyTimestamp("invalid-1", true, "20210301T010101Z", copied::add)
        .copyTimestamp("invalid-2", true, "2021-03-01 01:01:01.1Z", copied::add)
        .copyTimestamp("invalid-3", true, "2021-03-01T01:01:01.1", copied::add)
        .copyTimestamp("null-ok", true, null, copied::add)
        .copyTimestamp("null-bad", false, null, copied::add);

    assertEquals(
        List.of(
            OffsetDateTime.parse(VALID_ONE).toInstant(),
            OffsetDateTime.parse(VALID_TWO).toInstant(),
            OffsetDateTime.parse(VALID_THREE).toInstant()),
        copied);
    assertEquals(
        ImmutableList.of(
            new DataTransformer.ErrorMessage("invalid-1", "invalid timestamp"),
            new DataTransformer.ErrorMessage("invalid-2", "invalid timestamp"),
            new DataTransformer.ErrorMessage("invalid-3", "invalid timestamp"),
            new DataTransformer.ErrorMessage("null-bad", "is null")),
        transformer.getErrors());
  }

  /** Tests the {@link DataTransformer#copyRifTimestamp} method. */
  @Test
  public void testCopyRifTimestamp() {
    transformer
        .copyRifTimestamp("valid-1", false, "03-JAN-2021 06:07:08", copied::add)
        .copyRifTimestamp("invalid-1", true, "2021/03/01", copied::add)
        .copyRifTimestamp("invalid-2", true, "2021-06-19T18:24:30", copied::add)
        .copyRifTimestamp("valid-2", true, "21-OCT-2021 11:27:38", copied::add)
        .copyRifTimestamp("null-ok", true, null, copied::add)
        .copyRifTimestamp("null-bad", false, null, copied::add);
    assertEquals(
        ImmutableList.of(
            ZonedDateTime.of(2021, 1, 3, 6, 7, 8, 0, ZoneOffset.UTC).toInstant(),
            ZonedDateTime.of(2021, 10, 21, 11, 27, 38, 0, ZoneOffset.UTC).toInstant()),
        copied);
    assertEquals(
        ImmutableList.of(
            new DataTransformer.ErrorMessage("invalid-1", "invalid timestamp"),
            new DataTransformer.ErrorMessage("invalid-2", "invalid timestamp"),
            new DataTransformer.ErrorMessage("null-bad", "is null")),
        transformer.getErrors());
  }

  /** Tests the {@link DataTransformer#copyOptionalRifTimestamp} method. */
  @Test
  public void testCopyOptionalRifTimestamp() {
    transformer
        .copyOptionalRifTimestamp("not-present", () -> false, () -> "2021-03-01", copied::add)
        .copyOptionalRifTimestamp("valid-1", () -> true, () -> "03-JAN-2021 06:07:08", copied::add)
        .copyOptionalRifTimestamp("invalid-1", () -> true, () -> "2021/03/01", copied::add)
        .copyOptionalRifTimestamp("invalid-2", () -> true, () -> "2021-06-19T18:24:30", copied::add)
        .copyOptionalRifTimestamp("valid-2", () -> true, () -> "21-OCT-2021 11:27:38", copied::add)
        .copyOptionalRifTimestamp("null-bad", () -> true, () -> null, copied::add);
    assertEquals(
        ImmutableList.of(
            ZonedDateTime.of(2021, 1, 3, 6, 7, 8, 0, ZoneOffset.UTC).toInstant(),
            ZonedDateTime.of(2021, 10, 21, 11, 27, 38, 0, ZoneOffset.UTC).toInstant()),
        copied);
    assertEquals(
        ImmutableList.of(
            new DataTransformer.ErrorMessage("invalid-1", "invalid timestamp"),
            new DataTransformer.ErrorMessage("invalid-2", "invalid timestamp"),
            new DataTransformer.ErrorMessage("null-bad", "is null")),
        transformer.getErrors());
  }

  /** Tests the {@link DataTransformer#copyAmount} method. */
  @Test
  public void testCopyAmount() {
    transformer
        .copyAmount("valid-1", false, "123.05", copied::add)
        .copyAmount("invalid-1", true, "not a number", copied::add)
        .copyAmount("invalid-2", true, "123a.00", copied::add)
        .copyAmount("valid-2", true, "-456.98", copied::add)
        .copyAmount("valid-3", true, "16", copied::add)
        .copyAmount("null-ok", true, null, copied::add)
        .copyAmount("null-bad", false, null, copied::add);
    assertEquals(
        ImmutableList.of(new BigDecimal("123.05"), new BigDecimal("-456.98"), new BigDecimal("16")),
        copied);
    assertEquals(
        ImmutableList.of(
            new DataTransformer.ErrorMessage("invalid-1", "invalid amount"),
            new DataTransformer.ErrorMessage("invalid-2", "invalid amount"),
            new DataTransformer.ErrorMessage("null-bad", "is null")),
        transformer.getErrors());
  }

  /** Tests the {@link DataTransformer#copyOptionalAmount} method. */
  @Test
  public void testCopyOptionalAmount() {
    transformer
        .copyOptionalAmount("not-present", () -> false, () -> "98.65", copied::add)
        .copyOptionalAmount("valid-1", () -> true, () -> "123.05", copied::add)
        .copyOptionalAmount("invalid-1", () -> true, () -> "not a number", copied::add)
        .copyOptionalAmount("invalid-2", () -> true, () -> "123a.00", copied::add)
        .copyOptionalAmount("valid-2", () -> true, () -> "-456.98", copied::add)
        .copyOptionalAmount("valid-3", () -> true, () -> "16", copied::add)
        .copyOptionalAmount("null-bad", () -> true, () -> null, copied::add);
    assertEquals(
        ImmutableList.of(new BigDecimal("123.05"), new BigDecimal("-456.98"), new BigDecimal("16")),
        copied);
    assertEquals(
        ImmutableList.of(
            new DataTransformer.ErrorMessage("invalid-1", "invalid amount"),
            new DataTransformer.ErrorMessage("invalid-2", "invalid amount"),
            new DataTransformer.ErrorMessage("null-bad", "is null")),
        transformer.getErrors());
  }

  /** Tests the {@link DataTransformer#copyInt} method. */
  @Test
  public void testCopyInt() {
    transformer.copyInt(() -> 3, i -> copied.add(String.valueOf(i)));
    assertEquals(ImmutableList.of("3"), copied);
    assertEquals(ImmutableList.of(), transformer.getErrors());
  }

  /** Tests the {@link DataTransformer#copyOptionalInt} method. */
  @Test
  public void testCopyOptionalInt() {
    transformer
        .copyOptionalInt(() -> false, () -> 3, i -> copied.add(String.valueOf(i)))
        .copyOptionalInt(() -> true, () -> 7, i -> copied.add(String.valueOf(i)));
    assertEquals(ImmutableList.of("7"), copied);
    assertEquals(ImmutableList.of(), transformer.getErrors());
  }

  /** Tests the {@link DataTransformer#copyEnumAsCharacter} method. */
  @Test
  public void testCopyEnumAsCharacter() {
    transformer
        .copyEnumAsCharacter(
            "no-value",
            new EnumStringExtractor.Result(EnumStringExtractor.Status.NoValue),
            copied::add)
        .copyEnumAsCharacter(
            "invalid-value",
            new EnumStringExtractor.Result(EnumStringExtractor.Status.InvalidValue),
            copied::add)
        .copyEnumAsCharacter(
            "null-value", new EnumStringExtractor.Result((String) null), copied::add)
        .copyEnumAsCharacter("empty-string", new EnumStringExtractor.Result(""), copied::add)
        .copyEnumAsCharacter("long-string", new EnumStringExtractor.Result("boo!"), copied::add)
        .copyEnumAsCharacter("good-value", new EnumStringExtractor.Result("Z"), copied::add);
    assertEquals(ImmutableList.of('Z'), copied);
    assertEquals(
        ImmutableList.of(
            new DataTransformer.ErrorMessage("no-value", "no value set"),
            new DataTransformer.ErrorMessage("invalid-value", "unrecognized enum value"),
            new DataTransformer.ErrorMessage("null-value", "is null"),
            new DataTransformer.ErrorMessage(
                "empty-string", "invalid length: expected=[1,1] actual=0"),
            new DataTransformer.ErrorMessage(
                "long-string", "invalid length: expected=[1,1] actual=4")),
        transformer.getErrors());
  }

  /** Tests the {@link DataTransformer#copyEnumAsString} method. */
  @Test
  public void testCopyEnumAsString() {
    transformer
        .copyEnumAsString(
            "no-value",
            false,
            10,
            new EnumStringExtractor.Result(EnumStringExtractor.Status.NoValue),
            copied::add)
        .copyEnumAsString(
            "no-value-ok",
            true,
            10,
            new EnumStringExtractor.Result(EnumStringExtractor.Status.NoValue),
            copied::add)
        .copyEnumAsString(
            "invalid-value",
            true,
            10,
            new EnumStringExtractor.Result(EnumStringExtractor.Status.InvalidValue),
            copied::add)
        .copyEnumAsString(
            "null-value-ok", true, 10, new EnumStringExtractor.Result((String) null), copied::add)
        .copyEnumAsString(
            "null-value-bad", false, 10, new EnumStringExtractor.Result((String) null), copied::add)
        .copyEnumAsString(
            "unsupported-value-bad",
            false,
            10,
            new EnumStringExtractor.Result(EnumStringExtractor.Status.UnsupportedValue, "boo!"),
            copied::add)
        .copyEnumAsString(
            "good-value", false, 10, new EnumStringExtractor.Result("boo!"), copied::add);
    assertEquals(ImmutableList.of("boo!"), copied);
    assertEquals(
        ImmutableList.of(
            new DataTransformer.ErrorMessage("no-value", "no value set"),
            new DataTransformer.ErrorMessage("invalid-value", "unrecognized enum value"),
            new DataTransformer.ErrorMessage("null-value-bad", "is null"),
            new DataTransformer.ErrorMessage("unsupported-value-bad", "unsupported enum value")),
        transformer.getErrors());
  }

  /** Tests the {@link DataTransformer#copyStringWithExpectedValue} method. */
  @Test
  public void testCopyStringWithExpectedValue() {
    transformer
        .copyStringWithExpectedValue("both-null", true, 1, 1, null, null, copied::add)
        .copyStringWithExpectedValue("both-same", true, 1, 10, "abcdef", "abcdef", copied::add)
        .copyStringWithExpectedValue("too-short", true, 1, 10, "abcdef", "abc", copied::add)
        .copyStringWithExpectedValue("too-long", true, 1, 10, "abcdef", "abcdefgh", copied::add)
        .copyStringWithExpectedValue("start-mismatch", true, 1, 10, "abcdef", "qwcdef", copied::add)
        .copyStringWithExpectedValue(
            "middle-mismatch", true, 1, 10, "abcdef", "abqwef", copied::add)
        .copyStringWithExpectedValue("end-mismatch", true, 1, 10, "abcdef", "abcdqw", copied::add)
        .copyStringWithExpectedValue(
            "long-random-mismatches",
            true,
            1,
            30,
            "abcdefghijklmnopqrstuvwxyz",
            "a0cd4fg6ijklm4opqr5tuvw9yQ",
            copied::add);
    assertEquals(ImmutableList.of("abcdef"), copied);
    assertEquals(
        ImmutableList.of(
            new DataTransformer.ErrorMessage("too-short", "value mismatch: masked=...---"),
            new DataTransformer.ErrorMessage("too-long", "value mismatch: masked=......++"),
            new DataTransformer.ErrorMessage("start-mismatch", "value mismatch: masked=##...."),
            new DataTransformer.ErrorMessage("middle-mismatch", "value mismatch: masked=..##.."),
            new DataTransformer.ErrorMessage("end-mismatch", "value mismatch: masked=....##"),
            new DataTransformer.ErrorMessage(
                "long-random-mismatches", "value mismatch: masked=.#..#..#.....#....#....#.#")),
        transformer.getErrors());
    try {
      transformer.throwIfErrorsPresent();
      fail("exception not thrown");
    } catch (DataTransformer.TransformationException ex) {
      assertEquals("failed with 6 errors", ex.getMessage());
      assertEquals(ex.getErrors(), transformer.getErrors());
    }
  }

  /**
   * Tests the {@link DataTransformer#copyUIntToShort} and {@link
   * DataTransformer#copyOptionalUIntToShort} methods.
   */
  @Test
  public void testCopyUIntToShort() {
    transformer
        .copyUIntToShort("ok-zero", 0, copied::add)
        .copyUIntToShort("ok-max", Short.MAX_VALUE, copied::add)
        .copyUIntToShort("negative", -1, copied::add)
        .copyUIntToShort("too-large", Short.MAX_VALUE + 1, copied::add)
        .copyOptionalUIntToShort("opt-present", () -> true, () -> 100, copied::add)
        .copyOptionalUIntToShort("opt-not-present", () -> false, () -> 250, copied::add)
        .copyOptionalUIntToShort(
            "opt-too-large", () -> true, () -> Short.MAX_VALUE + 1, copied::add);
    assertEquals(ImmutableList.of((short) 0, Short.MAX_VALUE, (short) 100), copied);
    assertEquals(
        ImmutableList.of(
            new DataTransformer.ErrorMessage("negative", "is signed"),
            new DataTransformer.ErrorMessage("too-large", "is too large"),
            new DataTransformer.ErrorMessage("opt-too-large", "is too large")),
        transformer.getErrors());
  }
}
