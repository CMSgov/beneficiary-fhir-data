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
        .copyString("length-one-ok", false, 1, 5, "1", copied::add)
        .copyString("length-five-ok", false, 1, 5, "12345", copied::add)
        .copyString("length-below-min", false, 2, 5, "1", copied::add)
        .copyString("length-above-max", false, 1, 5, "123456", copied::add)
        .copyString("null-ok", true, 1, 5, null, copied::add)
        .copyString("null-bad", false, 1, 5, null, copied::add);

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
        .copyDate("2021-03-01", false, "valid-1", copied::add)
        .copyDate("2021/03/01", true, "invalid-1", copied::add)
        .copyDate("2021-06-19T18:24:30", true, "invalid-2", copied::add)
        .copyDate("2021-10-21", true, "valid-2", copied::add)
        .copyDate(null, true, "null-ok", copied::add)
        .copyDate(null, false, "null-bad", copied::add);
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
        .copyAmount("valid-1", false, "123.05", copied::add)
        .copyAmount("invalid-1", true, "not a number", copied::add)
        .copyAmount("invalid-2", true, "123a.00", copied::add)
        .copyAmount("valid-2", true, "-456.98", copied::add)
        .copyAmount("valid-3", true, "16", copied::add)
        .copyAmount("null-ok", true, null, copied::add)
        .copyAmount("null-bad", false, null, copied::add);
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
