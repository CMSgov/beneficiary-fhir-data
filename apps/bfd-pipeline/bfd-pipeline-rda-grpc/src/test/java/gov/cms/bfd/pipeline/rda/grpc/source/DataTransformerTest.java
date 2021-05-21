package gov.cms.bfd.pipeline.rda.grpc.source;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
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
        .copyString("length-one-ok", "1", false, 1, 5, copied::add)
        .copyString("length-five-ok", "12345", false, 1, 5, copied::add)
        .copyString("length-below-min", "1", false, 2, 5, copied::add)
        .copyString("length-above-max", "123456", false, 1, 5, copied::add)
        .copyString("null-ok", null, true, 1, 5, copied::add)
        .copyString("null-bad", null, false, 1, 5, copied::add);

    Assert.assertEquals(Arrays.asList("1", "12345"), copied);
    Assert.assertEquals(
        Arrays.asList(
            new DataTransformer.ErrorMessage(
                "length-below-min", "invalid length: expected=[2,5] actual=1"),
            new DataTransformer.ErrorMessage(
                "length-above-max", "invalid length: expected=[1,5] actual=6"),
            new DataTransformer.ErrorMessage("null-bad", "is null")),
        transformer.getErrors());
  }

  @Test
  public void copyHashedString() {
    transformer
        .copyHashedString("length-one-ok", "1", false, 1, 5, copied::add)
        .copyHashedString("length-five-ok", "12345", false, 1, 5, copied::add)
        .copyHashedString("length-below-min", "1", false, 2, 5, copied::add)
        .copyHashedString("length-above-max", "123456", false, 1, 5, copied::add)
        .copyHashedString("null-ok", null, true, 1, 5, copied::add)
        .copyHashedString("null-bad", null, false, 1, 5, copied::add);

    Assert.assertEquals(
        Arrays.asList(
            "6b86b273ff34fce19d6b804eff5a3f5747ada4eaa22f1d49c01e52ddb7875b4b",
            "5994471abb01112afcc18159f6cc74b4f511b99806da59b3caf5a9c173cacfc5"),
        copied);
    Assert.assertEquals(
        Arrays.asList(
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
    Assert.assertEquals(Arrays.asList('1', 'A'), copied);
    Assert.assertEquals(
        Arrays.asList(
            new DataTransformer.ErrorMessage(
                "length-below-min", "invalid length: expected=[1,1] actual=0"),
            new DataTransformer.ErrorMessage(
                "length-above-max", "invalid length: expected=[1,1] actual=2")),
        transformer.getErrors());
  }

  @Test
  public void copyDate() {
    transformer
        .copyDate("valid-1", "2021-03-01", false, copied::add)
        .copyDate("invalid-1", "2021/03/01", true, copied::add)
        .copyDate("invalid-2", "2021-06-19T18:24:30", true, copied::add)
        .copyDate("valid-2", "2021-10-21", true, copied::add)
        .copyDate("null-ok", null, true, copied::add)
        .copyDate("null-bad", null, false, copied::add);
    Assert.assertEquals(
        Arrays.asList(LocalDate.of(2021, 3, 1), LocalDate.of(2021, 10, 21)), copied);
    Assert.assertEquals(
        Arrays.asList(
            new DataTransformer.ErrorMessage("invalid-1", "invalid date"),
            new DataTransformer.ErrorMessage("invalid-2", "invalid date"),
            new DataTransformer.ErrorMessage("null-bad", "is null")),
        transformer.getErrors());
  }

  @Test
  public void copyAmount() {
    transformer
        .copyAmount("valid-1", "123.05", false, copied::add)
        .copyAmount("invalid-1", "not a number", true, copied::add)
        .copyAmount("invalid-2", "123a.00", true, copied::add)
        .copyAmount("valid-2", "-456.98", true, copied::add)
        .copyAmount("valid-3", "16", true, copied::add)
        .copyAmount("null-ok", null, true, copied::add)
        .copyAmount("null-bad", null, false, copied::add);
    Assert.assertEquals(
        Arrays.asList(new BigDecimal("123.05"), new BigDecimal("-456.98"), new BigDecimal("16")),
        copied);
    Assert.assertEquals(
        Arrays.asList(
            new DataTransformer.ErrorMessage("invalid-1", "invalid amount"),
            new DataTransformer.ErrorMessage("invalid-2", "invalid amount"),
            new DataTransformer.ErrorMessage("null-bad", "is null")),
        transformer.getErrors());
  }
}
