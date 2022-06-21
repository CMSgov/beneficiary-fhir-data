package gov.cms.model.dsl.codegen.plugin.transformer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.squareup.javapoet.CodeBlock;
import gov.cms.model.dsl.codegen.plugin.accessor.GrpcGetter;
import gov.cms.model.dsl.codegen.plugin.accessor.StandardSetter;
import gov.cms.model.dsl.codegen.plugin.model.ColumnBean;
import gov.cms.model.dsl.codegen.plugin.model.MappingBean;
import gov.cms.model.dsl.codegen.plugin.model.TransformationBean;
import org.junit.jupiter.api.Test;

/** Unit test for {@link LongStringFieldTransformer}. */
public class LongStringFieldTransformerTest {
  /** Verifies that required fields use {@code copyLongString}. */
  @Test
  public void testRequiredField() {
    ColumnBean column =
        ColumnBean.builder().name("idrDtlCnt").nullable(true).sqlType("char(1)").build();
    TransformationBean transformation =
        TransformationBean.builder().optional(false).from("idrDtlCnt").build();
    MappingBean mapping =
        MappingBean.builder()
            .entityClassName("gov.cms.test.Entity")
            .transformation(transformation)
            .build();

    LongStringFieldTransformer generator = new LongStringFieldTransformer();
    CodeBlock block =
        generator.generateCodeBlock(
            mapping, column, transformation, GrpcGetter.Instance, StandardSetter.Instance);
    assertEquals(
        "transformer.copyLongString(namePrefix + gov.cms.test.Entity.Fields.idrDtlCnt, true, from.getIdrDtlCnt(), to::setIdrDtlCnt);\n",
        block.toString());
  }

  /** Verifies that optional fields use {@code copyOptionalLongString}. */
  @Test
  public void testOptionalField() {
    ColumnBean column =
        ColumnBean.builder().name("idrDtlCnt").nullable(true).sqlType("char(1)").build();
    TransformationBean transformation =
        TransformationBean.builder().optional(true).from("idrDtlCnt").build();
    MappingBean mapping =
        MappingBean.builder()
            .entityClassName("gov.cms.test.Entity")
            .transformation(transformation)
            .build();

    LongStringFieldTransformer generator = new LongStringFieldTransformer();
    CodeBlock block =
        generator.generateCodeBlock(
            mapping, column, transformation, GrpcGetter.Instance, StandardSetter.Instance);
    assertEquals(
        "transformer.copyOptionalLongString(namePrefix + gov.cms.test.Entity.Fields.idrDtlCnt, from::hasIdrDtlCnt, from::getIdrDtlCnt, to::setIdrDtlCnt);\n",
        block.toString());
  }
}
