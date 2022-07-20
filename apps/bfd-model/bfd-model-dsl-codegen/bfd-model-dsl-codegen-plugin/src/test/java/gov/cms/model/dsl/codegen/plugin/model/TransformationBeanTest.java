package gov.cms.model.dsl.codegen.plugin.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link TransformationBean}. */
public class TransformationBeanTest {
  /** Verify the logic in selecting a {@code to} field name. */
  @Test
  public void testGetTo() {
    TransformationBean bean = new TransformationBean();
    bean.setFrom("src");
    assertEquals("src", bean.getTo());
    bean.setFrom("src.field");
    assertEquals("field", bean.getTo());
    bean.setTo("dst");
    assertEquals("dst", bean.getTo());
  }

  /** Verify the logic for determining if a transformer has been defined. */
  @Test
  public void testHasTransformer() {
    TransformationBean bean = new TransformationBean();
    assertFalse(bean.hasTransformer());
    bean.setTransformer("");
    assertFalse(bean.hasTransformer());
    bean.setTransformer("merge");
    assertTrue(bean.hasTransformer());
  }

  /** Verify the logic for accessing transformer options. */
  @Test
  public void testTransformerOptions() {
    TransformationBean bean =
        TransformationBean.builder()
            .transformerOption("a", "A")
            .transformerOption("b", "")
            .transformerOption("c", "1 ,2, 3")
            .build();
    assertEquals(Optional.empty(), bean.transformerOption("x"));
    assertEquals(Optional.of(""), bean.transformerOption("b"));
    assertEquals(Optional.of("A"), bean.transformerOption("a"));
    assertEquals(Optional.of("1 ,2, 3"), bean.transformerOption("c"));
    assertEquals(Optional.of(List.of("1", "2", "3")), bean.transformerListOption("c"));

    assertThrows(IllegalArgumentException.class, () -> bean.findTransformerOption("x"));
    assertEquals("A", bean.findTransformerOption("a"));
  }
}
