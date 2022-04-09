package gov.cms.model.dsl.codegen.plugin.transformer;

import static org.junit.Assert.assertEquals;

import com.squareup.javapoet.CodeBlock;
import gov.cms.model.dsl.codegen.plugin.model.ColumnBean;
import gov.cms.model.dsl.codegen.plugin.model.MappingBean;
import gov.cms.model.dsl.codegen.plugin.model.RootBean;
import gov.cms.model.dsl.codegen.plugin.model.TransformationBean;
import org.junit.Test;

public class CharFieldTransformerTest {
  @Test
  public void requiredField() {
    ColumnBean column =
        ColumnBean.builder().name("curr1Status").nullable(false).sqlType("char(1)").build();
    TransformationBean transformation =
        TransformationBean.builder().optional(false).from("curr1Status").build();
    MappingBean mapping =
        MappingBean.builder()
            .entityClassName("gov.cms.bfd.model.rda.PreAdjFissClaim")
            .transformation(transformation)
            .build();
    RootBean model = RootBean.builder().mapping(mapping).build();

    CharFieldTransformer generator = new CharFieldTransformer();
    CodeBlock block =
        generator.generateCodeBlock(
            mapping,
            column,
            transformation,
            GrpcFromCodeGenerator.Instance,
            StandardToCodeGenerator.Instance);
    assertEquals(
        "transformer.copyCharacter(namePrefix + gov.cms.bfd.model.rda.PreAdjFissClaim.Fields.curr1Status, from.getCurr1Status(), to::setCurr1Status);\n",
        block.toString());
  }

  @Test
  public void optionalField() {
    ColumnBean column =
        ColumnBean.builder().name("idrDtlCnt").nullable(true).sqlType("char(1)").build();
    TransformationBean transformation = TransformationBean.builder().from("idrDtlCnt").build();
    MappingBean mapping =
        MappingBean.builder()
            .entityClassName("gov.cms.bfd.model.rda.PreAdjFissClaim")
            .transformation(transformation)
            .build();
    RootBean model = RootBean.builder().mapping(mapping).build();

    CharFieldTransformer generator = new CharFieldTransformer();
    CodeBlock block =
        generator.generateCodeBlock(
            mapping,
            column,
            transformation,
            GrpcFromCodeGenerator.Instance,
            StandardToCodeGenerator.Instance);
    assertEquals(
        "transformer.copyOptionalCharacter(namePrefix + gov.cms.bfd.model.rda.PreAdjFissClaim.Fields.idrDtlCnt, from::hasIdrDtlCnt, from::getIdrDtlCnt, to::setIdrDtlCnt);\n",
        block.toString());
  }
}
