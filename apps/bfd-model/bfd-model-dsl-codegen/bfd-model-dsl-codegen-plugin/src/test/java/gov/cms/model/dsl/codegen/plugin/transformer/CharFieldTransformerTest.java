package gov.cms.model.dsl.codegen.plugin.transformer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.squareup.javapoet.CodeBlock;
import gov.cms.model.dsl.codegen.plugin.accessor.GrpcGetter;
import gov.cms.model.dsl.codegen.plugin.accessor.StandardSetter;
import gov.cms.model.dsl.codegen.plugin.model.ColumnBean;
import gov.cms.model.dsl.codegen.plugin.model.MappingBean;
import gov.cms.model.dsl.codegen.plugin.model.TransformationBean;
import org.junit.jupiter.api.Test;

/** Unit test for {@link CharFieldTransformer}. */
public class CharFieldTransformerTest {
  /** Verifies that required fields use {@code copyCharacter}. */
  @Test
  public void testRequiredField() {
    ColumnBean column =
        ColumnBean.builder().name("curr1Status").nullable(false).sqlType("char(1)").build();
    TransformationBean transformation =
        TransformationBean.builder().optional(false).from("curr1Status").build();
    MappingBean mapping =
        MappingBean.builder()
            .entityClassName("gov.cms.test.Entity")
            .transformation(transformation)
            .build();

    CharFieldTransformer generator = new CharFieldTransformer();
    CodeBlock block =
        generator.generateCodeBlock(
            mapping, column, transformation, GrpcGetter.Instance, StandardSetter.Instance);
    assertEquals(
        "transformer.copyCharacter(namePrefix + gov.cms.test.Entity.Fields.curr1Status, from.getCurr1Status(), to::setCurr1Status);\n",
        block.toString());
  }

  /** Verifies that optional fields use {@code copyOptionalCharacter}. */
  @Test
  public void testOptionalField() {
    ColumnBean column =
        ColumnBean.builder().name("idrDtlCnt").nullable(true).sqlType("char(1)").build();
    TransformationBean transformation = TransformationBean.builder().from("idrDtlCnt").build();
    MappingBean mapping =
        MappingBean.builder()
            .entityClassName("gov.cms.test.Entity")
            .transformation(transformation)
            .build();

    CharFieldTransformer generator = new CharFieldTransformer();
    CodeBlock block =
        generator.generateCodeBlock(
            mapping, column, transformation, GrpcGetter.Instance, StandardSetter.Instance);
    assertEquals(
        "transformer.copyOptionalCharacter(namePrefix + gov.cms.test.Entity.Fields.idrDtlCnt, from::hasIdrDtlCnt, from::getIdrDtlCnt, to::setIdrDtlCnt);\n",
        block.toString());
  }
}
