package gov.cms.model.rda.codegen.plugin.transformer;

import static org.junit.Assert.assertEquals;

import com.squareup.javapoet.CodeBlock;
import gov.cms.model.rda.codegen.plugin.model.ColumnBean;
import gov.cms.model.rda.codegen.plugin.model.MappingBean;
import gov.cms.model.rda.codegen.plugin.model.RootBean;
import gov.cms.model.rda.codegen.plugin.model.TransformationBean;
import org.junit.Test;

public class DateFieldTransformerTest {
  @Test
  public void requiredField() {
    ColumnBean column = ColumnBean.builder().name("beneDob").nullable(true).sqlType("date").build();
    TransformationBean transformation =
        TransformationBean.builder().optional(false).from("beneDob").build();
    MappingBean mapping =
        MappingBean.builder()
            .entityClassName("gov.cms.bfd.model.rda.PreAdjFissClaim")
            .transformation(transformation)
            .build();
    RootBean model = RootBean.builder().mapping(mapping).build();

    DateFieldTransformer generator = new DateFieldTransformer();
    CodeBlock block =
        generator.generateCodeBlock(
            mapping,
            column,
            transformation,
            GrpcFromCodeGenerator.Instance,
            StandardToCodeGenerator.Instance);
    assertEquals(
        "transformer.copyDate(namePrefix + gov.cms.bfd.model.rda.PreAdjFissClaim.Fields.beneDob, true, from.getBeneDob(), to::setBeneDob);\n",
        block.toString());
  }

  @Test
  public void optionalField() {
    ColumnBean column = ColumnBean.builder().name("beneDob").nullable(true).sqlType("date").build();
    TransformationBean transformation = TransformationBean.builder().from("beneDob").build();
    MappingBean mapping =
        MappingBean.builder()
            .entityClassName("gov.cms.bfd.model.rda.PreAdjFissClaim")
            .transformation(transformation)
            .build();
    RootBean model = RootBean.builder().mapping(mapping).build();

    DateFieldTransformer generator = new DateFieldTransformer();
    CodeBlock block =
        generator.generateCodeBlock(
            mapping,
            column,
            transformation,
            GrpcFromCodeGenerator.Instance,
            StandardToCodeGenerator.Instance);
    assertEquals(
        "transformer.copyOptionalDate(namePrefix + gov.cms.bfd.model.rda.PreAdjFissClaim.Fields.beneDob, from::hasBeneDob, from::getBeneDob, to::setBeneDob);\n",
        block.toString());
  }
}
