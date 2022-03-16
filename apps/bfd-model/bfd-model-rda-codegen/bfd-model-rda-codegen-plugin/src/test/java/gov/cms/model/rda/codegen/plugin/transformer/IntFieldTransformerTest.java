package gov.cms.model.rda.codegen.plugin.transformer;

import static org.junit.Assert.assertEquals;

import com.squareup.javapoet.CodeBlock;
import gov.cms.model.rda.codegen.plugin.model.ColumnBean;
import gov.cms.model.rda.codegen.plugin.model.MappingBean;
import gov.cms.model.rda.codegen.plugin.model.RootBean;
import gov.cms.model.rda.codegen.plugin.model.TransformationBean;
import org.junit.Test;

public class IntFieldTransformerTest {
  @Test(expected = IllegalArgumentException.class)
  public void requiredField() {
    ColumnBean column =
        ColumnBean.builder().name("idrDtlCnt").nullable(true).sqlType("int").build();
    TransformationBean transformation =
        TransformationBean.builder().optional(false).from("idrDtlCnt").build();
    MappingBean mapping =
        MappingBean.builder()
            .entityClassName("gov.cms.bfd.model.rda.PreAdjFissClaim")
            .transformation(transformation)
            .build();
    RootBean model = RootBean.builder().mapping(mapping).build();

    IntFieldTransformer generator = new IntFieldTransformer();
    generator.generateCodeBlock(
        mapping,
        column,
        transformation,
        GrpcFromCodeGenerator.Instance,
        StandardToCodeGenerator.Instance);
  }

  @Test
  public void optionalField() {
    ColumnBean column =
        ColumnBean.builder().name("idrDtlCnt").nullable(true).sqlType("int").build();
    TransformationBean transformation = TransformationBean.builder().from("idrDtlCnt").build();
    MappingBean mapping =
        MappingBean.builder()
            .entityClassName("gov.cms.bfd.model.rda.PreAdjFissClaim")
            .transformation(transformation)
            .build();
    RootBean model = RootBean.builder().mapping(mapping).build();

    IntFieldTransformer generator = new IntFieldTransformer();
    CodeBlock block =
        generator.generateCodeBlock(
            mapping,
            column,
            transformation,
            GrpcFromCodeGenerator.Instance,
            StandardToCodeGenerator.Instance);
    assertEquals(
        "transformer.copyOptionalInt(from::hasIdrDtlCnt, from::getIdrDtlCnt, to::setIdrDtlCnt);\n",
        block.toString());
  }
}
