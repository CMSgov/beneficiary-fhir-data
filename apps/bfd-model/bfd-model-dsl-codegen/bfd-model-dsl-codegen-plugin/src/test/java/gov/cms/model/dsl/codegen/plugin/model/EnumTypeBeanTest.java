package gov.cms.model.dsl.codegen.plugin.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class EnumTypeBeanTest {
  /** Test {@link EnumTypeBean#findValue} method. */
  @Test
  public void testFindValue() {
    EnumTypeBean enumTypeBean = EnumTypeBean.builder().value("a").value("b").value("c").build();
    assertEquals("b", enumTypeBean.findValue("b"));
    assertThrows(IllegalArgumentException.class, () -> enumTypeBean.findValue("d"));
  }
}
