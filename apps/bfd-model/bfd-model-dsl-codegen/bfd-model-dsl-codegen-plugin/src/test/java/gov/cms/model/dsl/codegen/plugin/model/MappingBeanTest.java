package gov.cms.model.dsl.codegen.plugin.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Tests the {@link MappingBean}. */
public class MappingBeanTest {
  /** Verify that entity class name can be parsed into package and simple names. */
  @Test
  public void testEntityClassNameParsing() {
    MappingBean mapping =
        MappingBean.builder().entityClassName("gov.cms.model.dsl.codegen.TestEntity").build();
    assertEquals("gov.cms.model.dsl.codegen", mapping.getEntityClassPackage());
    assertEquals("TestEntity", mapping.getEntityClassSimpleName());
  }

  /** Verify that methods properly indicate whether certain values are present or not. */
  @Test
  public void testAccessorsThatDetermineIfValuesAreDefined() {
    MappingBean mapping = new MappingBean();
    assertFalse(mapping.hasTransformer());
    assertFalse(mapping.hasExternalTransformations());
    assertFalse(mapping.hasEntityInterfaces());

    mapping.setTransformerClassName("");
    assertFalse(mapping.hasTransformer());
    mapping.setTransformerClassName("a");
    assertTrue(mapping.hasTransformer());

    mapping.getExternalTransformations().add(new ExternalTransformationBean());
    assertTrue(mapping.hasExternalTransformations());

    mapping.getEntityInterfaces().add("a");
    assertTrue(mapping.hasEntityInterfaces());
  }

  /** Verify that finding an enum by name works correctly. */
  @Test
  public void testFindEnum() {
    MappingBean mapping =
        MappingBean.builder()
            .enumType(EnumTypeBean.builder().name("a").build())
            .enumType(EnumTypeBean.builder().name("b").build())
            .build();
    assertEquals("a", mapping.findEnum("a").getName());
    assertThrows(IllegalArgumentException.class, () -> mapping.findEnum("x"));
  }

  /** Verify finding a join by field name works correctly. */
  @Test
  public void testFindJoinByFieldName() {
    MappingBean mapping =
        MappingBean.builder()
            .table(TableBean.builder().join(JoinBean.builder().fieldName("a").build()).build())
            .build();
    assertTrue(mapping.findJoinByFieldName("a").isPresent());
    assertFalse(mapping.findJoinByFieldName("x").isPresent());
  }

  /** Verify that joins associated with array fields are properly filtered. */
  @Test
  public void testGetNonArrayJoins() {
    JoinBean joinA = JoinBean.builder().fieldName("a").build();
    JoinBean joinB = JoinBean.builder().fieldName("b").build();
    JoinBean joinC = JoinBean.builder().fieldName("c").build();
    ArrayBean arrayB = ArrayBean.builder().to("b").build();
    MappingBean mapping =
        MappingBean.builder()
            .table(TableBean.builder().join(joinA).join(joinB).join(joinC).build())
            .array(arrayB)
            .build();
    assertEquals(ImmutableList.of(joinA, joinC), mapping.getNonArrayJoins());
  }

  /** Verify that joined columns can be found using a column name. */
  @Test
  public void testGetJoinedColumnForColumnName() {
    // ClaimIds table that contains all claim_ids and is joined from claims table
    ColumnBean claimIdColumn = ColumnBean.builder().name("claimId").dbName("claim_id").build();
    TableBean claimIdsTable = TableBean.builder().column(claimIdColumn).build();
    MappingBean claimIdsMapping =
        MappingBean.builder().table(claimIdsTable).entityClassName("test.ClaimId").build();

    // claims table that joins with the claimIds table
    TableBean claimsTable =
        TableBean.builder()
            .column(ColumnBean.builder().name("findMe").build())
            .join(
                JoinBean.builder()
                    .entityClass(claimIdsMapping.getEntityClassName())
                    .joinColumnName(claimIdColumn.getDbName())
                    .fieldName("claimIdRecord")
                    .joinType(JoinBean.JoinType.ManyToOne)
                    .build())
            .build();
    MappingBean claimsMapping =
        MappingBean.builder().entityClassName("test.Claim").table(claimsTable).build();

    // detail table that joins with the claim table (thus indirectly to claimIds table)
    TableBean detailsTable =
        TableBean.builder()
            .join(
                JoinBean.builder()
                    .entityClass(claimsMapping.getEntityClassName())
                    .joinColumnName(claimIdColumn.getDbName())
                    .fieldName("claimRecord")
                    .joinType(JoinBean.JoinType.ManyToOne)
                    .build())
            .build();
    MappingBean detailsMapping =
        MappingBean.builder().entityClassName("test.Detail").table(detailsTable).build();

    RootBean root = new RootBean(Arrays.asList(claimIdsMapping, claimsMapping, detailsMapping));
    Optional<ColumnBean> found = claimsMapping.getRealOrJoinedColumnByColumnName(root, "findMe");
    assertTrue(found.isPresent());
    assertEquals("findMe", found.get().getName());

    found = claimsMapping.getJoinedColumnByColumnName(root, "claim_id");
    assertTrue(found.isPresent());
    assertEquals("claimId", found.get().getName());

    found = detailsMapping.getRealOrJoinedColumnByColumnName(root, "findMe");
    assertFalse(found.isPresent());

    found = detailsMapping.getJoinedColumnByColumnName(root, "claim_id");
    assertTrue(found.isPresent());
    assertEquals("claimId", found.get().getName());
  }
}
