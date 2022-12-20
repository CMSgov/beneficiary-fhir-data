package gov.cms.model.dsl.codegen.plugin.accessor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.squareup.javapoet.CodeBlock;
import gov.cms.model.dsl.codegen.plugin.model.ColumnBean;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link StandardSetter}. */
public class StandardSetterTest {
  /** Verifies output of {@link StandardSetter#createSetRef}. */
  @Test
  public void testCreateSetRef() {
    final ColumnBean notNullable = ColumnBean.builder().name("niceData").nullable(false).build();
    final ColumnBean nullable = ColumnBean.builder().name("niceData").nullable(true).build();
    assertEquals("to::setNiceData", StandardSetter.Instance.createSetRef(notNullable).toString());
    assertEquals("to::setNiceData", StandardSetter.Instance.createSetRef(nullable).toString());
  }

  /** Verifies output of {@link StandardSetter#createSetCall}. */
  @Test
  public void testCreateSetCall() {
    final ColumnBean notNullable = ColumnBean.builder().name("niceData").nullable(false).build();
    final ColumnBean nullable = ColumnBean.builder().name("niceData").nullable(true).build();
    final CodeBlock value = CodeBlock.of("value");
    assertEquals(
        "to.setNiceData(value);\n",
        StandardSetter.Instance.createSetCall(notNullable, value).toString());
    assertEquals(
        "to.setNiceData(value);\n",
        StandardSetter.Instance.createSetCall(nullable, value).toString());
  }
}
