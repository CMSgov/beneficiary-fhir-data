package gov.cms.model.dsl.codegen.plugin.transformer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.squareup.javapoet.CodeBlock;
import gov.cms.model.dsl.codegen.plugin.accessor.GrpcGetter;
import gov.cms.model.dsl.codegen.plugin.accessor.StandardSetter;
import gov.cms.model.dsl.codegen.plugin.model.ColumnBean;
import gov.cms.model.dsl.codegen.plugin.model.MappingBean;
import gov.cms.model.dsl.codegen.plugin.model.TransformationBean;
import org.junit.jupiter.api.Test;

/** Unit test for {@link StringFieldTransformer}. */
public class StringFieldTransformerTest {
  /** Verifies that required fields use {@code copyString}. */
  @Test
  public void testRequiredField() {
    ColumnBean column =
        ColumnBean.builder().name("hicNo").nullable(true).sqlType("varchar(12)").build();
    TransformationBean transformation =
        TransformationBean.builder().optional(false).from("hicNo").build();
    MappingBean mapping =
        MappingBean.builder()
            .entityClassName("gov.cms.test.Entity")
            .transformation(transformation)
            .build();

    StringFieldTransformer generator = new StringFieldTransformer();
    CodeBlock block =
        generator.generateCodeBlock(
            mapping, column, transformation, GrpcGetter.Instance, StandardSetter.Instance);
    assertEquals(
        "transformer.copyString(namePrefix + gov.cms.test.Entity.Fields.hicNo, true, 1, 12, from.getHicNo(), to::setHicNo);\n",
        block.toString());
  }

  /** Verifies that optional fields use {@code copyOptionalString}. */
  @Test
  public void testOptionalField() {
    ColumnBean column =
        ColumnBean.builder().name("hicNo").nullable(true).sqlType("varchar(12)").build();
    TransformationBean transformation = TransformationBean.builder().from("hicNo").build();
    MappingBean mapping =
        MappingBean.builder()
            .entityClassName("gov.cms.test.Entity")
            .transformation(transformation)
            .build();

    StringFieldTransformer generator = new StringFieldTransformer();
    CodeBlock block =
        generator.generateCodeBlock(
            mapping, column, transformation, GrpcGetter.Instance, StandardSetter.Instance);
    assertEquals(
        "transformer.copyOptionalString(namePrefix + gov.cms.test.Entity.Fields.hicNo, 1, 12, from::hasHicNo, from::getHicNo, to::setHicNo);\n",
        block.toString());
  }
}
