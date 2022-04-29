package gov.cms.bfd.pipeline.rda.grpc.source;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import com.google.common.collect.ImmutableList;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests proper operation of the {@link DataTransformer} class. */
public class DataTransformerTest {
  private DataTransformer transformer;
  private List<Object> copied;

  @BeforeEach
  public void setUp() throws Exception {
    transformer = new DataTransformer();
    copied = new ArrayList<>();
  }

  /** Tests the {@link DataTransformer#validateAtLeastOneIsPresent} method. */
  @Test
  public void testValidateAtLeastOneIsPresent() {
    assertEquals(true, transformer.validateAtLeastOneIsPresent("ok1-1", "1", "ok1-2", ""));
    assertEquals(true, transformer.validateAtLeastOneIsPresent("ok2-1", "", "ok2-2", "2"));
    assertEquals(true, transformer.validateAtLeastOneIsPresent("ok3-1", "1", "ok3-2", null));
    assertEquals(true, transformer.validateAtLeastOneIsPresent("ok4-1", null, "ok4-2", "2"));
    assertEquals(true, transformer.validateAtLeastOneIsPresent("both-1", "a", "both-2", "b"));
    assertEquals(0, transformer.getErrors().size());

    assertEquals(
        false, transformer.validateAtLeastOneIsPresent("neither1-1", "", "neither1-2", ""));
    assertEquals(
        false, transformer.validateAtLeastOneIsPresent("neither2-1", null, "neither2-2", ""));
    assertEquals(
        false, transformer.validateAtLeastOneIsPresent("neither3-1", "", "neither3-2", null));
    assertEquals(
        false, transformer.validateAtLeastOneIsPresent("neither4-1", null, "neither4-2", null));
    assertEquals(
        ImmutableList.of(
            new DataTransformer.ErrorMessage(
                "neither1-1",
                "expected either neither1-1 or neither1-2 to have value but neither did"),
            new DataTransformer.ErrorMessage(
                "neither2-1",
                "expected either neither2-1 or neither2-2 to have value but neither did"),
            new DataTransformer.ErrorMessage(
                "neither3-1",
                "expected either neither3-1 or neither3-2 to have value but neither did"),
            new DataTransformer.ErrorMessage(
                "neither4-1",
                "expected either neither4-1 or neither4-2 to have value but neither did")),
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

  /** Tests the {@link DataTransformer#copyDate} method. */
  @Test
  public void testCopyDate() {
    transformer
        .copyDate("valid-1", false, "2021-03-01", copied::add)
        .copyDate("invalid-1", true, "2021/03/01", copied::add)
        .copyDate("invalid-2", true, "2021-06-19T18:24:30", copied::add)
        .copyDate("valid-2", true, "2021-10-21", copied::add)
        .copyDate("null-ok", true, null, copied::add)
        .copyDate("null-bad", false, null, copied::add);
    assertEquals(ImmutableList.of(LocalDate.of(2021, 3, 1), LocalDate.of(2021, 10, 21)), copied);
    assertEquals(
        ImmutableList.of(
            new DataTransformer.ErrorMessage("invalid-1", "invalid date"),
            new DataTransformer.ErrorMessage("invalid-2", "invalid date"),
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
}
