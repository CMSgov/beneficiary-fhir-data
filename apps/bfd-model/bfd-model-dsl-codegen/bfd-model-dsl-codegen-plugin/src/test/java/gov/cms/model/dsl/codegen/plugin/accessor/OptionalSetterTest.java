package gov.cms.model.dsl.codegen.plugin.accessor;

import static org.junit.Assert.assertEquals;

import com.squareup.javapoet.CodeBlock;
import gov.cms.model.dsl.codegen.plugin.model.ColumnBean;
import org.junit.Test;

/** Unit tests for {@link OptionalSetter} */
public class OptionalSetterTest {
  /** Verifies output of {@link OptionalSetter#createSetRef}. */
  @Test
  public void testCreateSetRef() {
    final ColumnBean notNullable = ColumnBean.builder().name("niceData").nullable(false).build();
    final ColumnBean nullable = ColumnBean.builder().name("niceData").nullable(true).build();
    assertEquals("to::setNiceData", OptionalSetter.Instance.createSetRef(notNullable).toString());
    assertEquals(
        "value -> to.setNiceData(java.util.Optional.ofNullable(value))",
        OptionalSetter.Instance.createSetRef(nullable).toString());
  }

  /** Verifies output of {@link OptionalSetter#createSetCall}. */
  @Test
  public void testCreateSetCall() {
    final ColumnBean notNullable = ColumnBean.builder().name("niceData").nullable(false).build();
    final ColumnBean nullable = ColumnBean.builder().name("niceData").nullable(true).build();
    final CodeBlock value = CodeBlock.of("value");
    assertEquals(
        "to.setNiceData(value);\n",
        OptionalSetter.Instance.createSetCall(notNullable, value).toString());
    assertEquals(
        "to.setNiceData(java.util.Optional.ofNullable(value));\n",
        OptionalSetter.Instance.createSetCall(nullable, value).toString());
  }
}
