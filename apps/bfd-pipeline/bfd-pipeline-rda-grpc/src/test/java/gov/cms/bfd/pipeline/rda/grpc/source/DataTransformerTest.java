package gov.cms.bfd.pipeline.rda.grpc.source;

import static org.junit.Assert.*;

import com.google.common.collect.ImmutableList;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

public class DataTransformerTest {
  private DataTransformer transformer;
  private List<Object> copied;

  @Before
  public void setUp() throws Exception {
    transformer = new DataTransformer();
    copied = new ArrayList<>();
  }

  @Test
  public void copyString() {
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

  @Test
  public void copyCharacter() {
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

  @Test
  public void copyDate() {
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

  @Test
  public void copyAmount() {
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

  @Test
  public void copyEnumAsCharacter() {
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

  @Test
  public void copyEnumAsString() {
    transformer
        .copyEnumAsString(
            "no-value",
            false,
            0,
            10,
            new EnumStringExtractor.Result(EnumStringExtractor.Status.NoValue),
            copied::add)
        .copyEnumAsString(
            "no-value-ok",
            true,
            0,
            10,
            new EnumStringExtractor.Result(EnumStringExtractor.Status.NoValue),
            copied::add)
        .copyEnumAsString(
            "invalid-value",
            true,
            0,
            10,
            new EnumStringExtractor.Result(EnumStringExtractor.Status.InvalidValue),
            copied::add)
        .copyEnumAsString(
            "null-value-ok",
            true,
            0,
            10,
            new EnumStringExtractor.Result((String) null),
            copied::add)
        .copyEnumAsString(
            "null-value-bad",
            false,
            0,
            10,
            new EnumStringExtractor.Result((String) null),
            copied::add)
        .copyEnumAsString(
            "good-value", false, 0, 10, new EnumStringExtractor.Result("boo!"), copied::add);
    assertEquals(ImmutableList.of("boo!"), copied);
    assertEquals(
        ImmutableList.of(
            new DataTransformer.ErrorMessage("no-value", "no value set"),
            new DataTransformer.ErrorMessage("invalid-value", "unrecognized enum value"),
            new DataTransformer.ErrorMessage("null-value-bad", "is null")),
        transformer.getErrors());
  }

  @Test
  public void copyExpectedValue() {
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
