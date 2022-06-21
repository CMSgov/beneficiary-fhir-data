package gov.cms.model.dsl.codegen.plugin.transformer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.squareup.javapoet.CodeBlock;
import gov.cms.model.dsl.codegen.plugin.accessor.GrpcGetter;
import gov.cms.model.dsl.codegen.plugin.accessor.StandardSetter;
import gov.cms.model.dsl.codegen.plugin.model.ColumnBean;
import gov.cms.model.dsl.codegen.plugin.model.EnumTypeBean;
import gov.cms.model.dsl.codegen.plugin.model.MappingBean;
import gov.cms.model.dsl.codegen.plugin.model.TransformationBean;
import org.junit.jupiter.api.Test;

/** Unit test for {@link EnumValueIfPresentTransformer}. */
public class EnumValueIfPresentTransformerTest {
  /** Verify that options are loaded and code generated properly. */
  @Test
  public void testCodeGeneratedProperly() {
    ColumnBean column =
        ColumnBean.builder().name("curr1Status").nullable(false).sqlType("char(1)").build();
    TransformationBean transformation =
        TransformationBean.builder()
            .from("enumTest")
            .transformerOption(EnumValueIfPresentTransformer.EnumNameOption, "TestEnum")
            .transformerOption(EnumValueIfPresentTransformer.EnumValueOption, "Value1")
            .build();
    MappingBean mapping =
        MappingBean.builder()
            .entityClassName("gov.cms.test.Entity")
            .enumType(
                EnumTypeBean.builder().name("TestEnum").value("Value1").value("Value12").build())
            .transformation(transformation)
            .build();

    EnumValueIfPresentTransformer generator = new EnumValueIfPresentTransformer();
    CodeBlock block =
        generator.generateCodeBlock(
            mapping, column, transformation, GrpcGetter.Instance, StandardSetter.Instance);
    assertEquals(
        "if (from.hasEnumTest()) {\n"
            + "  to.setCurr1Status(gov.cms.test.Entity.TestEnum.Value1);\n"
            + "}\n",
        block.toString());
  }
}
