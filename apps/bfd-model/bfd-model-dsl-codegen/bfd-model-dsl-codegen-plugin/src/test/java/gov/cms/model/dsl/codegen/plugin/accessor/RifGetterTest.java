package gov.cms.model.dsl.codegen.plugin.accessor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import gov.cms.model.dsl.codegen.plugin.model.TransformationBean;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link RifGetter}. */
public class RifGetterTest {
  /** Verifies output of {@link RifGetter#createHasRef}. */
  @Test
  public void testHasRef() {
    final var simple = TransformationBean.builder().from("x").build();
    final var nested = TransformationBean.builder().from("x.y").build();
    assertEquals("() -> from.hasValue(\"x\")", RifGetter.Instance.createHasRef(simple).toString());
    assertThrows(IllegalArgumentException.class, () -> RifGetter.Instance.createHasRef(nested));
  }

  /** Verifies output of {@link RifGetter#createHasCall}. */
  @Test
  public void testHasCall() {
    final var simple = TransformationBean.builder().from("x").build();
    final var nested = TransformationBean.builder().from("x.y").build();
    assertEquals("from.hasValue(\"x\")", RifGetter.Instance.createHasCall(simple).toString());
    assertThrows(IllegalArgumentException.class, () -> RifGetter.Instance.createHasCall(nested));
  }

  /** Verifies output of {@link RifGetter#createGetRef}. */
  @Test
  public void testGetRef() {
    final var simple = TransformationBean.builder().from("x").build();
    final var nested = TransformationBean.builder().from("x.y").build();
    assertEquals(
        "() -> from.getValue(\"x\", null)", RifGetter.Instance.createGetRef(simple).toString());
    assertThrows(IllegalArgumentException.class, () -> RifGetter.Instance.createGetRef(nested));
  }

  /** Verifies output of {@link RifGetter#createGetCall}. */
  @Test
  public void testGetCall() {
    final var simple = TransformationBean.builder().from("x").build();
    final var nested = TransformationBean.builder().from("x.y").build();
    assertEquals("from.getValue(\"x\", null)", RifGetter.Instance.createGetCall(simple).toString());
    assertThrows(IllegalArgumentException.class, () -> RifGetter.Instance.createGetCall(nested));
  }
}
