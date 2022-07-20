package gov.cms.model.dsl.codegen.plugin.accessor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import gov.cms.model.dsl.codegen.plugin.model.TransformationBean;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link GrpcGetter}. */
public class GrpcGetterTest {
  /** Verifies output of {@link GrpcGetter#createHasRef}. */
  @Test
  public void testHasRef() {
    final var simple = TransformationBean.builder().from("x").build();
    final var nested = TransformationBean.builder().from("x.y").build();
    assertEquals("from::hasX", GrpcGetter.Instance.createHasRef(simple).toString());
    assertEquals(
        "() -> from.hasX() && from.getX().hasY()",
        GrpcGetter.Instance.createHasRef(nested).toString());
  }

  /** Verifies output of {@link GrpcGetter#createHasCall}. */
  @Test
  public void testHasCall() {
    final var simple = TransformationBean.builder().from("x").build();
    final var nested = TransformationBean.builder().from("x.y").build();
    assertEquals("from.hasX()", GrpcGetter.Instance.createHasCall(simple).toString());
    assertEquals(
        "(from.hasX() && from.getX().hasY())",
        GrpcGetter.Instance.createHasCall(nested).toString());
  }

  /** Verifies output of {@link GrpcGetter#createGetRef}. */
  @Test
  public void testGetRef() {
    final var simple = TransformationBean.builder().from("x").build();
    final var nested = TransformationBean.builder().from("x.y").build();
    assertEquals("from::getX", GrpcGetter.Instance.createGetRef(simple).toString());
    assertEquals("() -> from.getX().getY()", GrpcGetter.Instance.createGetRef(nested).toString());
  }

  /** Verifies output of {@link GrpcGetter#createGetCall}. */
  @Test
  public void testGetCall() {
    final var simple = TransformationBean.builder().from("x").build();
    final var nested = TransformationBean.builder().from("x.y").build();
    assertEquals("from.getX()", GrpcGetter.Instance.createGetCall(simple).toString());
    assertEquals("from.getX().getY()", GrpcGetter.Instance.createGetCall(nested).toString());
  }
}
