package gov.cms.model.dsl.codegen.plugin.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

public class EnumTypeBeanTest {
  /** Test {@link EnumTypeBean#findValue} method. */
  @Test
  public void testFindValue() {
    EnumTypeBean enumTypeBean = EnumTypeBean.builder().value("a").value("b").value("c").build();
    assertEquals("b", enumTypeBean.findValue("b"));
    assertThrows(IllegalArgumentException.class, () -> enumTypeBean.findValue("d"));
  }
}
