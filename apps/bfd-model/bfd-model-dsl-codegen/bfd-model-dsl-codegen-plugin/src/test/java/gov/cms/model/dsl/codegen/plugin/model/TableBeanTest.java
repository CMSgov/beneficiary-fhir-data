package gov.cms.model.dsl.codegen.plugin.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

public class TableBeanTest {
  /** Test query methods used to check various boolean conditions. */
  @Test
  public void testValueCheckingMethods() {
    TableBean bean = TableBean.builder().build();
    assertFalse(bean.hasComment());
    assertFalse(bean.hasSchema());
    assertFalse(bean.hasPrimaryKey());
    assertFalse(bean.isPrimaryKey("a"));
    assertFalse(
        bean.isPrimaryKey(
            JoinBean.builder().joinType(JoinBean.JoinType.OneToOne).fieldName("a").build()));

    bean =
        TableBean.builder()
            .comment("info needed")
            .schema("working")
            .column(ColumnBean.builder().name("a").build())
            .column(ColumnBean.builder().name("b").dbName("db").build())
            .primaryKeyColumn("b")
            .build();
    assertTrue(bean.hasComment());
    assertTrue(bean.hasSchema());
    assertTrue(bean.hasPrimaryKey());
    assertTrue(bean.isPrimaryKey("b"));
    assertFalse(
        bean.isPrimaryKey(
            JoinBean.builder().joinType(JoinBean.JoinType.OneToMany).fieldName("b").build()));
    assertTrue(
        bean.isPrimaryKey(
            JoinBean.builder().joinType(JoinBean.JoinType.OneToOne).fieldName("b").build()));
  }

  /** Test methods for finding particular columns. */
  @Test
  public void testFindColumn() {
    ColumnBean columnA = ColumnBean.builder().name("a").build();
    ColumnBean columnB = ColumnBean.builder().name("b").dbName("dbb").build();
    ColumnBean columnC = ColumnBean.builder().name("c").build();
    ColumnBean columnD = ColumnBean.builder().name("d").dbName("dbd").build();
    TableBean bean =
        TableBean.builder().column(columnA).column(columnB).column(columnC).column(columnD).build();

    assertThrows(IllegalArgumentException.class, () -> bean.findColumnByName("x"));
    assertThrows(IllegalArgumentException.class, () -> bean.findColumnByNameOrDbName("x"));

    assertSame(columnA, bean.findColumnByName("a"));
    assertSame(columnD, bean.findColumnByName("d"));
    assertThrows(IllegalArgumentException.class, () -> bean.findColumnByName("dbd"));
    assertSame(columnD, bean.findColumnByNameOrDbName("dbd"));
  }

  /** Test method for getting lookup {@link Set} of equals method column names. */
  @Test
  public void testGetColumnsForEqualsMethod() {
    TableBean bean = TableBean.builder().primaryKeyColumn("a").primaryKeyColumn("b").build();
    assertEquals(Set.of("a", "b"), bean.getColumnsForEqualsMethod());

    bean =
        TableBean.builder()
            .primaryKeyColumn("a")
            .primaryKeyColumn("b")
            .primaryKeyColumn("c")
            .equalsColumn("a")
            .equalsColumn("c")
            .build();
    assertEquals(java.util.Set.of("a", "c"), bean.getColumnsForEqualsMethod());
  }

  @Test
  public void testQuoteName() {
    TableBean bean = TableBean.builder().build();
    assertTrue(bean.isQuoteNames());
    assertEquals("`benefit`", bean.quoteName("benefit"));
    bean.setQuoteNames(false);
    assertEquals("benefit", bean.quoteName("benefit"));
  }
}
